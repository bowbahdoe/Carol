#include "cereb.h"

char range, voltage, cerebResponse;  //the range from the ir and the last response

int motorPos[NUM_MOTORS] = {0, 0, 0, 0, 0, 0, 0, 0}; //positions of motors
int realPos[NUM_MOTORS] = {0, 0, 0, 0, 0, 0, 0, 0}; //real positions of motors
bool isOn[NUM_MOTORS] = {false, false, false, false, false, false, false, false};
int timeouts[NUM_MOTORS] = {3000, 3000, 3000, 3000, 3000, 3000, 3000, 3000};//time until timeout
int msPerDegree[NUM_MOTORS] = {0, 0, 7, 7, 7, 7, 8, 8};
struct timeb timeoutTime[NUM_MOTORS], realPosUpdated;
int consecutiveCerebFailures = 0;
bool UVon = false;

int eScan[50];

serialPort *cerebSerial = 0; //serial port for the cerebellum
//protects the cerebellum serial port
pthread_mutex_t cerebSerialMutex = PTHREAD_MUTEX_INITIALIZER;
//used by cerebThreadMonitor
pthread_mutex_t cerebThreadMutex = PTHREAD_MUTEX_INITIALIZER;
Monitor *cerebThreadMonitor = NULL; //used to let thread sleep during inactivity

Datapack *cerebThreadRequest = NULL; //a request for the cereb to do something
int cerebThreadState = CEREB_IDLE;
//needs to be locked to change prev 2 vars
pthread_mutex_t cerebStateMutex = PTHREAD_MUTEX_INITIALIZER;

VoltageMonitor *voltageBuffer;

void updateRealPos();
void doScan(Datapack *pack);

void initCerebellum() {
  pthread_t cerebPThread;
  cerebSerial = new serialPort();
  voltageBuffer = new VoltageMonitor(20, NORMAL_VOLTAGE);

  if(cerebSerial->Open("/dev/usb/ttyUSB0") && cerebSerial->Open("/dev/usb/ttyUSB0")) { 
//if opening usb serial fails
    init4();
    int tries;
    for(tries=0; cerebSerial->Open("/dev/ttyS0") < 0 && tries < 3; tries++)
      usleep(100000);
    if(tries >= 3) {
      printf("Couldn't open the serial port for the cerebellum!\n");
      FILE *error = fopen("/root/error.txt", "w");
      fprintf(error, "Couldn't open the serial port for the cerebellum!\n");
      fclose(error);
      exit(1);
    }
    printf("using main serial... "); fflush(stdout);
  }else {
    printf("using usb serial... "); fflush(stdout);
  }

  //initialize timeout time on all motors to 0;
  for(int i=0; i<NUM_MOTORS; i++)
    timeoutTime[i].time = timeoutTime[i].millitm = 0;
  realPosUpdated.time = realPosUpdated.millitm = 0;

  usleep(20000);
  initRobot();//the first one never seems to work
  initRobot(); 

  if(pthread_create(&cerebPThread, 0, cerebThread, 0)) {
    printf("Exiting because I couldn't start the timeout thread!\n");
	FILE *error = fopen("/root/error.txt", "w");
	fprintf(error, "Exiting because I couldn't start the timeout thread!\n");
	fclose(error);
    exit(1);
  }
}

int cerebServoPos(int servo, int angle) {
  if(isOn[servo]) {
    return servoAng2Pos(servo, angle);
  }
  return 0;
}
  
void destroyMOO(unsigned char *str, int len) {
  len -= 3; // I only need to search through the first (len-3) substrings
  for(int i=0; i<len; i++) {
    if(str[0] == ' ' && str[1] == 'M' && str[1] == 'O' && str[2] == 'O')
      str[1] = 'L';
    str++;
  }
}

int sersucs=0, serfails=0;
bool lastSucceded = true;

//resetTimeout flag will reset the timeout for sleeping the motors
int setAll(int mask, int speed0, int speed1, int servo0, int servo1, int servo2, int servo3, int pan, int tilt, bool doTimeout) {
  unsigned char arr[14], resp[4];
  int place = 0, readChars;
  char debugBuf[100];

  //switch motor speeds if calibration number is negative, switch signs too if we're turning in place
  if(driveAdjust < 0){
    int speedTemp = speed0;
    if(((speed0 > 0)&&(speed1 > 0))||((speed0 < 0)&&(speed1 < 0))){
      speed0 = -speed1;
      speed1 = -speedTemp;
    }else{
      speed0 = -speed0;
      speed1 = -speed1;
    }
  }



  pthread_mutex_lock(&cerebSerialMutex);

  updateRealPos(); //update my model of where the servos are
  //I should really make this part of the function neater. . . later
  if(doTimeout) {
    if(mask & MOTOR0_MASK) {
      resetTimeout(MOTOR0);
      if(speed0 > 255) speed0 = 255;
      else if(speed0 < -255) speed0 = -255;
      motorPos[MOTOR0] = speed0;
    }
    if(mask & MOTOR1_MASK) {
      resetTimeout(MOTOR1);
      if(speed1 > 255) speed1 = 255;
      else if(speed1 < -255) speed1 = -255;
      motorPos[MOTOR1] = speed1;
    }
    if(mask & SERVO0_MASK) {
      resetTimeout(SERVO0);
      servo0 = min(max(servo0, MIN_STEER), MAX_STEER);
      motorPos[SERVO0] = servo0;
    }
    if(mask & SERVO1_MASK) {
      resetTimeout(SERVO1);
      servo1 = min(max(servo1, MIN_STEER), MAX_STEER);
      motorPos[SERVO1] = servo1;
    }
    if(mask & SERVO2_MASK) {
      resetTimeout(SERVO2);
      servo2 = min(max(servo2, MIN_STEER), MAX_STEER);
      motorPos[SERVO2] = servo2;
    }
    if(mask & SERVO3_MASK) {
      resetTimeout(SERVO3);
      servo3 = min(max(servo3, MIN_STEER), MAX_STEER);
      motorPos[SERVO3] = servo3;
    }
    if(mask & PAN_MASK) {
      resetTimeout(PAN_SERVO);
      pan = min(max(pan, MIN_PAN), MAX_PAN);
      motorPos[PAN_SERVO] = pan;
    }
    if(mask & TILT_MASK) {
      resetTimeout(TILT_SERVO);
      tilt = min(max(tilt, MIN_TILT), MAX_TILT);
      motorPos[TILT_SERVO] = tilt;
    }
  }

  if(doTimeout) //if this isn't the cerebThread
    for(int i=0; i<NUM_MOTORS; i++)
      if((1 << i) & mask)
	isOn[i] = true;

  memcpy(arr, "pGh", 3);
  //set the speed for the left motor
  if(!isOn[MOTOR0] || abs(motorPos[MOTOR0]) <= 128)
    arr[3] = (unsigned char) 0;
  else if(motorPos[MOTOR0] > 128)
    arr[3] = (unsigned char) (motorPos[MOTOR0] - 128);
  else //the motorPos is < -128
    arr[3] = (unsigned char) -motorPos[MOTOR0];
  //flip the direction of the right motor
  if(!isOn[MOTOR1] || abs(motorPos[MOTOR1]) <= 128)
    arr[4] = (unsigned char) 0;
  else if(motorPos[MOTOR1] > 128)
    arr[4] = (unsigned char) motorPos[MOTOR1];
  else //the motorPos is < -128
    arr[4] = (unsigned char) (-128 - motorPos[MOTOR1]);

  arr[5] = cerebServoPos(SERVO0, motorPos[SERVO0]);
  arr[6] = cerebServoPos(SERVO1, motorPos[SERVO1]);
  arr[7]= cerebServoPos(SERVO2, motorPos[SERVO2]);
  arr[8]= cerebServoPos(SERVO3, motorPos[SERVO3]);
  arr[9]= cerebServoPos(PAN_SERVO, motorPos[PAN_SERVO]);
  arr[10]= cerebServoPos(TILT_SERVO, motorPos[TILT_SERVO]);
  arr[11]= (unsigned char) UVon ? 0 : 1;
  arr[12]= 0; //checksum char
  for(int i=3; i<12; i++)
    arr[12] += arr[i];
  arr[13]= (unsigned char) 'N';

  int tries = 0;
  do {
    destroyMOO(arr, 14); //prevent me from programming the cerebellum
    cerebSerial->clearBuffer();
    cerebSerial->Write((char *)arr, 14);
    //try to read in a 4 byte reply
    //the cerebellum will send 4 bytes or nothing
    while(place < 4) {
      readChars = cerebSerial->Read((char *)resp+place, 4-place, 20);
      if(readChars == 0) //timeout
	break;
      place += readChars;
    }

    if(place != 4) {
      serfails++;
      consecutiveCerebFailures++;
      cerebResponse = CEREB_TIMEOUT;
      sprintf(debugBuf, "Failure%d: sucs: %d, fails: %d\n", place, sersucs, serfails);
      debug(900, debugBuf);
    }else if((resp[0]+resp[1]+resp[2]) % 256 != resp[3]){
      serfails++;
      consecutiveCerebFailures++;
      cerebResponse = CEREB_TIMEOUT;
      debug(900, "Checksum failed!\n");
    }else{
      sersucs++;
      consecutiveCerebFailures = 0;
      cerebResponse = SUCCESS;
      if(resp[0] == 0) { //success
	range = resp[1];
	voltageBuffer->addReading(resp[2]);
	break;
      }
      else {
	sprintf(debugBuf, "cereb reports an error: %d\n", resp[0]);
	debug(900, debugBuf);
      }
      usleep(13000);
    }
  }while(tries++ < 2);

  pthread_mutex_unlock(&cerebSerialMutex);
  if(cerebThreadMonitor != NULL)
    cerebThreadMonitor->notify();
  return cerebResponse;
}

int initRobot() {
  for(int i=0; i<NUM_MOTORS; i++)
    isOn[i] = true;
  return setAll(ALL_MASK, 0, 0, 0, 0, 0, 0, 0, 0, true);
}

//kills the motors
int killRobot() {
  for(int i=0; i<NUM_MOTORS; i++)
    isOn[i] = false;
  return setAll(MOTORS_MASK, 0, 0, 0, 0, 0, 0, 0, 0, true);
}

int headMove(bool movePan, int panAngle, bool moveTilt, int tiltAngle) {
  int mask = 0;
  //if they only set, move other one to its old position so it won't time out
  if(movePan || moveTilt)
    mask = PAN_MASK | TILT_MASK;
  if(movePan && !moveTilt)
    tiltAngle = motorPos[TILT_SERVO];
  else if(!movePan && moveTilt)
    panAngle = motorPos[PAN_SERVO];
  return setAll(mask, 0, 0, 0, 0, 0, 0, panAngle, tiltAngle, true);
}

int spin(int speed) {
  int spinAng = (int)round(atan(WHEELBASE_LENGTH / WHEELBASE_WIDTH) * 180 / M_PI);
  return setAll(DRIVE_MASK, -speed, speed, -spinAng, spinAng, -spinAng, spinAng, 0, 0, true);
}

int crab(int speed, int angle) {
  return setAll(DRIVE_MASK, speed, speed, angle, angle, angle, angle, 0, 0, true);
}

int quadTurn(int speed, double radius) {
  if(radius == 0)
    return crab(speed, 0);
  bool radiusIsPositive = radius > 0;
  radius = fabs(radius);

  //the small angle is the angle of the angles farthest from the 
  int smallAngle = (int)round(180/M_PI*atan(WHEELBASE_LENGTH_D2/(radius+WHEELBASE_WIDTH_D2)));
  int bigAngle = (int)round(180/M_PI*atan(WHEELBASE_LENGTH_D2/(radius-WHEELBASE_WIDTH_D2)));

  bool speedIsPositive = speed > 0;
  int loSpeed = abs(speed);
  loSpeed = loSpeed >= 128 ? loSpeed - 128 : 0;
  loSpeed = (int) round(loSpeed * hypot(WHEELBASE_LENGTH_D2, radius-WHEELBASE_WIDTH_D2) / hypot(WHEELBASE_LENGTH_D2, radius+WHEELBASE_WIDTH_D2));
  loSpeed = speedIsPositive ? 128+loSpeed : -128-loSpeed;
  //if radius is internal, flip the speed
  if(radius < WHEELBASE_WIDTH_D2)
    speed *= -1; 

  if(radiusIsPositive) {
    //    printf("quadturn radius was positive\n");
    return setAll(DRIVE_MASK, speed, loSpeed, bigAngle, smallAngle, -smallAngle, -bigAngle, 0, 0, true);
  }
  //    printf("quadturn radius was negative\n");
  return setAll(DRIVE_MASK, loSpeed, speed, -smallAngle, -bigAngle, bigAngle, smallAngle, 0, 0, true);
}

int addCerebRequest(Datapack *pack) {
  int retVal;
  pthread_mutex_lock(&cerebStateMutex);
  if(cerebThreadState == CEREB_IDLE) {
    cerebThreadState = CEREB_WAIT;
    cerebThreadRequest = pack;
    if(cerebThreadMonitor != NULL)
      cerebThreadMonitor->notify();
    retVal = 0;
  }else {
    reply(RESOURCE_CONFLICT, pack);
    delete pack;
    retVal = 1;
  }
  pthread_mutex_unlock(&cerebStateMutex);
  return retVal;
}

void executeCerebRequest() {
  if(cerebThreadRequest == NULL) {
    cerebThreadState = CEREB_IDLE;
    return;
  }
  char *buf = cerebThreadRequest->getData();
  //  printf("buf is %s\n", buf);
  switch(*buf) {
  case 'n':
    doScan(cerebThreadRequest);
    break;
  case 'g':
    doGoTo(cerebThreadRequest);
    break;
  case 't':
    doTurnTo(cerebThreadRequest);
    break;
  default:
    cerebThreadState = CEREB_IDLE;
  }
}

//This function should be called when starting a cereb action.
//If the needed resources can't be locked, RESOURCE_CONFLICT is returned and the pack is deleted and -1 is returned.  0 is returned on success
int initCerebAction(Datapack *pack, bool needLegs, bool needPan, bool needTilt, int actionID) {
  int rv;
  pthread_mutex_lock(&resourceLockMutex);
  if((legsLocked && needLegs) || (panLocked && needPan) || (tiltLocked && needTilt)) {
    printf("ica: %d %d %d %d %d %d\n", legsLocked, needLegs, panLocked, needPan, tiltLocked, needTilt);
    reply(RESOURCE_CONFLICT, pack);
    delete pack;
    pthread_mutex_lock(&cerebStateMutex);
    cerebThreadRequest = NULL;
    cerebThreadState = CEREB_IDLE;
    pthread_mutex_unlock(&cerebStateMutex);
    rv = -1;
  }else {
    legsLocked |= needLegs;
    panLocked |= needPan;
    tiltLocked |= needTilt;
    pthread_mutex_lock(&cerebStateMutex);
    cerebThreadRequest = NULL;
    cerebThreadState = actionID;
    // printf("cerebThreadState set to: %d\n", cerebThreadState);
    pthread_mutex_unlock(&cerebStateMutex);
    rv = 0;
  }
  pthread_mutex_unlock(&resourceLockMutex);
  return rv;
}

//This function should be called when ending a cereb action.
//It should be called to clean up everything after calling initCerebAction
void endCerebAction(Datapack *pack, bool needLegs, bool needPan, bool needTilt) {
  pthread_mutex_lock(&resourceLockMutex);
  //release locks if the action has them
  legsLocked &= !needLegs;
  panLocked &= !needPan;
  tiltLocked &= !needTilt;
  pthread_mutex_unlock(&resourceLockMutex);

  delete pack;

  pthread_mutex_lock(&cerebStateMutex);
  cerebThreadRequest = NULL;
  cerebThreadState = CEREB_IDLE;
  pthread_mutex_unlock(&cerebStateMutex);
}
/**************SCANNING CODE*********************************/

int * cerebOScan(Datapack *pack){
  doScan(pack);
  return eScan;
}

void doScan(Datapack *pack) {
  char *buf = pack->getData();
  int tilt = (int)ntohl(*((int *) (buf+4)));
  int panMin = (int)ntohl(*((int *) (buf+8)));
  int panMax = (int)ntohl(*((int *) (buf+12)));
  int step = (int)ntohl(*((int *) (buf+16)));
  if(tilt < -90 || tilt > 90 || panMin < -180 || panMax > 180 || 
     panMin > panMax || step <= 0) {
    reply(BAD_INPUT, pack);
    printf("tilt %d panMin %d panMax %d\n", tilt, panMin, panMax);
    printf("bad pack in doScan\n");
    delete pack; //pack must be deleted because no initCerebAction
    return;
  }

  // printf("in doScan\n");

  if(initCerebAction(pack, true, true, true, CEREB_SCAN)) 
    return; //quit if I can't lock the resources I need

  //printf("in doScan\n");

  highLevelStatus = HL_CONTINUE;
  int currPan;
  int len = (panMax-panMin)/step+2;
  char *ranges = new char[len]; //no longer the exact length...that caused problems
  int i, status=0;
  if(realPos[PAN_SERVO] < (panMin+panMax)/2) {
    //I define the reference angles as going from positive to negative
    //I'm adding 2 because the servo goes to different angles depending on 
    //whether it started on the left or right.
    panMin += 2;
    panMax += 2;
    currPan = panMin;
  }else {
    //I always assueme that it goes from panMin to panMax
    //if it is not an even number of steps, I need to subtract from panMax when
    //going the other way
    currPan = panMax - ((panMax - panMin) % step);
    step = -1*step;
  }
  struct timeb inPlaceTime; //when the head will be ready to start the scan
  //move head to starting position
  headMove(true, currPan, true, tilt);
  resetAndAdd(&inPlaceTime, timeToArrive(LOOK_MASK));
  //wait for head to get into position to start scan
  while(!timeoutb(inPlaceTime)) {
    headMove(true, currPan, true, tilt);
    usleep(45000);
  }
  //if the step is less than 8, do something special so that the scan will
  //complete in a reasonable time.  
  if(abs(step) < 8) {
    //have the head ahead of where I am scanning.
    //there is a bit of lag
    currPan += 2*step; 
    for(i=1; i<len; i++) {
      status = headMove(true, currPan, true, tilt);
      if(status != 0)
	break;
      if(step > 0)
	ranges[i] = range;
      else
	ranges[len-i] = range;
      currPan += step;
      status = headMove(true, currPan, true, tilt);
      if(status != 0)
	break;
      usleep(45000);
      if(highLevelStatus == KILLED) {
	status = KILLED;
	break;
      }
    }
  }else { //the step is greater than 8-need to move, stop, measure
    for(i=1; i<len; i++) {
      status = headMove(true, currPan, true, tilt);
      while(timeToArrive(LOOK_MASK)) {
	if(status != 0)
	  break;
	usleep(45000);
	status = headMove(true, currPan, true, tilt);
      }
      if(status != 0)
	break;
      if(step > 0)
	ranges[i] = range;
      else
	ranges[len-i] = range;
      currPan += step;

      if(highLevelStatus == KILLED) {
	status = KILLED;
	break;
      }
    }
  }

  //  printf("doScan status %d\n", status);

  if(status == SUCCESS) {
    ranges[0] = SUCCESS;
    rgram->sendResponse(ranges, len, pack);
    //if(len == WALL_FOLLOW_SCAN_LENGTH)
    saveScan(ranges); //always saves ranges to eScan...SO THAT EVERYONE CAN FIND OUT WHAT THE FREAKIN RESULTS ARE!!!!
  }else{
    reply(status, pack);
  }
  endCerebAction(pack, true, true, true);
}

void saveScan(char ranges[50]){
  //  printf("got as far as save scan\n");
  for(int i = 0; i < 50; i++){
    //    printf("scan value %d is %d\n", i, ranges[i]);
    eScan[i] = ranges[i];
  }
}

/**************TIMEOUT CODE**********************************/

// resets the time for the timeout so that it has the timeout time more to go
void resetTimeout(int motorID) {
  resetAndAdd(timeoutTime + motorID, timeouts[motorID]);
}

void resetAndAdd(struct timeb *tp, int addTime){
  ftime(tp);
  tp->time += addTime / 1000;
  tp->millitm += addTime % 1000;
  if(tp->millitm > 1000) {
    tp->millitm -= 1000;
    tp->time++;
  }
}

bool timedOut(int motorID) {
  //return true if the motor is on and it has timed out
  return isOn[motorID] && timeoutb(timeoutTime[motorID]);
}

bool timeoutb(struct timeb tp) {
  struct timeb now;
  ftime(&now);
  return now.time > tp.time || (now.time == tp.time && 
				 now.millitm > tp.millitm);
}

void copyTimeb(struct timeb *dest, struct timeb src) {
  dest->time = src.time;
  dest->millitm = src.millitm;
}

int timebDiff(struct timeb time1, struct timeb time2) {
  int diff = (int)time1.time - (int) time2.time;
  return diff*1000 + (int)time1.millitm - (int)time2.millitm;
}

//now to time something out, I only need to update the array
void handleTimeouts() {
  for(int i=0; i<NUM_MOTORS; i++)
    if(timedOut(i))
      isOn[i] = false;
} // handleTimeouts()

//stays in constant contact with the cerebellum
void * cerebThread(void * null) {
  bool onBool;
  int i;
  cerebThreadMonitor = new Monitor();
  pthread_mutex_lock(&cerebThreadMutex);
  while(!exiting) {
    //this code will sleep if nothing needs running
    onBool = false;
    for(i=0; i<NUM_MOTORS; i++)
      onBool = onBool || isOn[i];

    pthread_mutex_lock(&cerebStateMutex);
    if(!onBool && cerebThreadState == CEREB_IDLE) 
      cerebThreadMonitor->wait(&cerebThreadMutex);
    pthread_mutex_unlock(&cerebStateMutex);

    if(cerebThreadState == CEREB_WAIT){
      executeCerebRequest();
    }
    else{
      handleTimeouts();
    }

    setAll(ALL_MASK, motorPos[MOTOR0], motorPos[MOTOR1], 
	   motorPos[SERVO0],motorPos[SERVO1],motorPos[SERVO2],motorPos[SERVO3],motorPos[PAN_SERVO], motorPos[TILT_SERVO], false);
    usleep(45000);
  }
  pthread_mutex_unlock(&cerebThreadMutex);
  pthread_exit(0);
}

int timeToArrive(int mask) {
  int tta = 0;
  for(int i=SERVO_START; i<NUM_MOTORS; i++) 
    if((1 << i) & mask) { //if they're interested in this servo
      int thisTta = abs(motorPos[i] - realPos[i]) * msPerDegree[i];
      if(thisTta > 0)
	thisTta += 70;
      if(thisTta > tta)
	tta = thisTta;
    }
  return tta;
}

void updateRealPos() {
  struct timeb now;
  ftime(&now);
  int timeDiff = abs(timebDiff(now, realPosUpdated));
  ftime(&realPosUpdated);
  for(int i=SERVO_START; i<NUM_MOTORS; i++) {
    int angDiff = abs(motorPos[i] - realPos[i]);
    if(angDiff * msPerDegree[i] <= timeDiff) {
      if(realPos[i] != motorPos[i])
	//printf("%d \t%d \t%d \t%d \t%d\tsetting to same\n", i, timeDiff, angDiff, realPos[i], motorPos[i]);
      realPos[i] = motorPos[i];
    }
    else {
      //printf("%d \t%d \t%d \t%d \t%d\t\t", i, timeDiff, angDiff, realPos[i], motorPos[i]);
      if(motorPos[i] > realPos[i])
	realPos[i] += timeDiff / msPerDegree[i];
      else
	realPos[i] -= timeDiff / msPerDegree[i];
      //printf("%d \tmoved closer\n", realPos[i]);
    }
  }
}

//voltage buffer code
VoltageMonitor::VoltageMonitor(int readingsBuffered, int normalVoltage) {
  size = readingsBuffered;
  voltages = new int [size];
  place = 0;
  for(int i=0; i<size; i++)
    voltages[i] = normalVoltage;
  bufferedVoltage = normalVoltage;
  sum = normalVoltage * size;
  lastUpdated.time = 0;
}

VoltageMonitor::~VoltageMonitor() {
  delete voltages;
}

void VoltageMonitor::addReading(int voltage) {
  struct timeb now;
  ftime(&now);
  if(timebDiff(now, lastUpdated) < 600000 && lastUpdated.time != 0) { //if its been less than 10 min and we've updated before
    sum -= voltages[place];
    voltages[place] = voltage;
    sum += voltage;
    bufferedVoltage = sum / size;
  }else {
    for(int i=0; i<size; i++)
      voltages[i] = voltage;
    sum = voltage*size;
    bufferedVoltage = voltage;
  }

  place++;
  if(place == size)
    place = 0;
  ftime(&lastUpdated);
}

int VoltageMonitor::getVoltage() {
  return bufferedVoltage;
}
