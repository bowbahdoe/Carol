#include "rover.h"

//keep this with 2 dots - it's how the java code recognizes the new versions
const char VERSION [] = "2.1.0";

Reliagram * rgram;
WebCam * webCam;
Datapack * responsePack;


//mutexes for the legs, pan, and tilt.  This makes sure that two threads won't
//clobber each other by using the same resource simulatneously.
bool legsLocked = false, panLocked=false, tiltLocked=false;
//this mutex must be locked before any thread changes the state of one of 
//the previous 3 variables.  It is OK to not have the mutex for reading.
pthread_mutex_t resourceLockMutex = PTHREAD_MUTEX_INITIALIZER;

bool exiting = false, showDebug = false, doDutyTest = false, autoStart = false;
bool doDutyTest2 = false, doDutyTest3 = false, wallFollow = false, lineFollow = false; 
bool trackObject = false, smartWander = false, demoExit = false, noDemo = false; 
//lineFollow and smartWander  will not execute unless explicitly mentioned on the command line
// Global Variables for Single Thread

void changeError(char *msg) {
  FILE *error = fopen("/root/error.txt", "w");-
  fprintf(error, msg);
  fclose(error);
}

int main(int argc, char **argv) {
  Datapack * pack;
  parseArgs(argc, argv);
  if(autoStart) //If started automatically, sleep to prevent race condition
    sleep(3);

  changeError("starting up\r\n");
  printf("Loading calibration... "); fflush(stdout);
  loadCalibration();    //load in the calibration constants.
  loadScans();

  //  double wierd = 3.14159265358979323846;
  
  //  printf("\n30, cos 30 %4.1f, wierd, %f, 30*180/pi %4.1f, cos 30*180/PIE %4.1f, wierd %f\n",  cos(30), wierd, 30*wierd/180, cos(30*180/wierd), wierd);
  //  printf("31, cos 31 %4.1f, wierd, %f, 31*180/pi %4.1f, cos 31*180/PIE %4.1f, wierd %f\n",  cos(31), wierd, 31*180/wierd, cos(31*180/wierd), wierd);

  changeError("starting camera\r\n");
  printf("camera... "); fflush(stdout);
  webCam = new WebCam();
  
  changeError("starting cereb\r\n");
  printf("cerebellum "); fflush(stdout);
  initCerebellum();

  changeError("starting network\r\n");
  printf("network..."); fflush(stdout);
  rgram = new Reliagram(10000);

  if(!rgram->bindTo(1701)) {
    changeError("can't bind\n");
    printf("Can't bind to port 1701\n");
    return -1;
  }

  signal(SIGINT, sigIntHandler);

  headMove(true, 0, true, 30);
  usleep(1000*timeToArrive(LOOK_MASK));
  headMove(true, 0, true, -30);
  usleep(1000*timeToArrive(LOOK_MASK));
  headMove(true, 0, true, 0);

  runDiagnostic();

  printf("\nInitialization complete\n"); fflush(stdout);
  changeError("all done\r\n");

  UVon = false;

  if(doDutyTest) 
    runDutyTest();
  else if(doDutyTest2) 
    runDutyTest2();
  else if(doDutyTest3) 
    runDutyTest3();
  else if(wallFollow) 
    runWallFollow();
  else if(smartWander) 
    runSmartWander();
  else if(lineFollow)
    runLineFollow();
  else if(trackObject) 
    runTrackObject();

  //int lastSeqNum = 0;
  while (!exiting) {
    if((pack = rgram->receiveData())){
      //      printf("I've got a packet to parse\n");
      parsePacket(pack);
    }
    else
      headMove(false, 0, false, 0);
  } // end while (true)  
  return 0;
}

void parsePacket(Datapack * pack) {
  char *buf = pack->getData();
  int len = pack->getLength();

  printf("parsePacket datapack buf is %s, length is %d\n", buf, len);

  //check for a low battery.  If so, then reject most commands.
  if(voltageBuffer->getVoltage() < LOW_BATTERY_THRESHOLD) {
    headMove(true, 0, true, 90);
    //char type = *buf;
    reply(BATTERY_LOW, pack);
    //allow only spin, turn, and crab commands when voltage is low
    //if(type != 'p' && type !='c' && type != 'q') {
    delete pack;
    return;
      //}
  }

  switch(*buf) {
  case 'i':
    len == 1 ? reply(initRobot(), pack) : reply(INVALID_LENGTH, pack);
    break;
  case 'v': //get version
    len == 1 ? rgram->sendResponse((char *)VERSION, strlen(VERSION), pack) :
      reply(INVALID_LENGTH, pack);
    break;
  case 'g': // 'g' goto
    if(len == 12){ 
      addCerebRequest(pack); 
      return; 
    }else
      reply(INVALID_LENGTH, pack);
    break;
  case 't': // turnto
    if(len == 8){
      addCerebRequest(pack);
      return;
    }else
      reply(INVALID_LENGTH, pack);
    break;
  case 'k': // kill
    if(len == 1) {
      kill(KILLED);
      reply(highLevelStatus, pack);
    }else
      reply(INVALID_LENGTH, pack);
    break;
  case 'u': // get an update on the high level motion command
    len == 1 ? 
      reply(cerebThreadState==CEREB_IDLE?highLevelStatus:HL_CONTINUE, pack) 
      : reply(INVALID_LENGTH, pack);
    break;
  case 'l': 
    len == 40 ? RoverSetAll(pack) : reply(INVALID_LENGTH, pack);
    break;
  case 'h':
    len == 12 ? roverHeadMove(pack) : reply(INVALID_LENGTH, pack);
    break;
  case 'p':
    len == 8 ? RoverSpin(pack) : reply(INVALID_LENGTH, pack);
    break;
  case 'c':
    len == 12 ? RoverCrab(pack) : reply(INVALID_LENGTH, pack);
    break;
  case 'q':
    len == 12 ? RoverTurn(pack) : reply(INVALID_LENGTH, pack);
    break;
  case 'a': //take picture 
    if(len == 20) { 
      webCam->takePicture(pack);
      return;//return so that the packet is not deleted
    }else
      reply(INVALID_LENGTH, pack);
  case 'n': //scan for rock
    if(len == 20) {
      addCerebRequest(pack);
      return;
    }
    reply(INVALID_LENGTH, pack);
    break;
  case 'y':
    reply(setCal(pack->getData() + 1, pack->getLength() - 1), pack);
    break;
  case 'z':
    len == 1 ? sendCal(pack) : reply(INVALID_LENGTH, pack);
    break;
  case 11:
    reply(setScan(pack->getData() + 1, pack->getLength() - 1), pack);
    break;
  case 12:
    len == 1 ? sendScan(pack) : reply(INVALID_LENGTH, pack);
    break;
  case 'x':
    if(len == 2) {
      UVon = (buf[1] > 0);
      reply(headMove(false, 0, false, 0), pack);
    }
    else
      reply(INVALID_LENGTH, pack);
    break;
  case 'r': //track
    printf("track command called\n");
    webCam->doSpecial(WC_TRACK, pack);
    return;
  case 'b': //get camera properties
    if(len == 1) {webCam->doSpecial(WC_PROPERTIES, pack); return; }
    else {reply(INVALID_LENGTH, pack);  break;}
  case 13: //get mean
    if(len == 2) {webCam->doSpecial(WC_MEAN, pack); return; }
    else {reply(INVALID_LENGTH, pack);  break;}
  case 14: //get motion
    if(len == 1) {webCam->doSpecial(WC_MOTION, pack); return; }
    else {reply(INVALID_LENGTH, pack);  break;}
  case 'o': //stop streaming
    if(len == 1) {
      webCam->doSpecial(WC_STOP, pack);
      return;
    }else
      reply(INVALID_LENGTH, pack);
    break;
  case 'I': //internal packet
    printf("internal packet received\n");
    responsePack = pack;
    printf("contents of the response: %s\n", (pack->getData() + 1));
    return;
  default:
    reply(UNKNOWN_TYPE, pack);
  }
  if(pack != NULL)
    delete pack;
  //  printf("finished parsing packet\n");
}

/*****************************************************************************/

int roverHeadMove(Datapack * pack) {
  char *buf = pack->getData();
  int pan = (int)ntohl(*((int *) (buf+4)));
  int tilt = (int)ntohl(*((int *) (buf+8)));
  bool movePan = buf[1]!=0, moveTilt = buf[2]!=0;
  //give an error if what they need is already in use.
  if((movePan && panLocked) || (moveTilt && tiltLocked))
    return reply(RESOURCE_CONFLICT, pack);

  int status = headMove(movePan, pan, moveTilt, tilt);
  return reply(status, pack);
}

int RoverSetAll(Datapack * pack) {
  char *buf = pack->getData();
  int *arr = (int *) (buf+4);
  for(int i=0; i<9; i++) {
    arr[i] = ntohl(arr[i]);
    if(arr[i] == 0 && i > SERVO_START) //if zero, turn off mask
      arr[0] &= 255 - (1 << (i-1));
  }
  //printf("received set all %d %d %d\n", arr[0], arr[1], arr[2]);
  int status = setAll(arr[0], arr[1], arr[2], servoPos2Ang(SERVO0, arr[3]), servoPos2Ang(SERVO1, arr[4]), servoPos2Ang(SERVO2, arr[5]), servoPos2Ang(SERVO3, arr[6]), servoPos2Ang(PAN_SERVO, arr[7]), servoPos2Ang(TILT_SERVO, arr[8]), true);
  return reply(status, pack);
}

int RoverSpin(Datapack * pack) {
  char *buf = pack->getData();
  int speed = (int)ntohl(*((uint32_t *) (buf+4)));
  int status;
  if(legsLocked)
    status = RESOURCE_CONFLICT;
  else
    status = spin(speed);
  return reply(status, pack);
}

int RoverCrab(Datapack * pack) {
  char *buf = pack->getData();
  int speed = (int)ntohl(*((int *) (buf+4)));
  int angle = (int)ntohl(*((int *) (buf+8)));
  int status;
  if(legsLocked)
    status = RESOURCE_CONFLICT;
  else
    status = crab(speed, angle);
  return reply(status, pack);
}

int RoverTurn(Datapack * pack) {
  char *buf = pack->getData();
  int speed = (int)ntohl(*((int *) (buf+4)));
  double radius = (int)ntohl(*((int *) (buf+8)));
  int status;
  if(legsLocked)
    status = RESOURCE_CONFLICT;
  else
    status = quadTurn(speed, radius);
  return reply(status, pack);
}

int reply(int status, Datapack *pack) {
  unsigned char response[16];

  //I'm allowing certain commands to work with low batteries.  I set pack to 
  //NULL and respond with BATTERY_LOW
  if(pack == NULL) 
    return -1; 
   
  response[0] = status;//status byte
  response[1] = range;  //range reading
  response[2] = (motorPos[PAN_SERVO] >> 8) & 255;
  response[3] = motorPos[PAN_SERVO] & 255;
  response[4] = (motorPos[TILT_SERVO] >> 8) & 255;
  response[5] = motorPos[TILT_SERVO] & 255;
  response[6] = (distTraveled >> 8) & 255;
  response[7] = distTraveled & 255;
  response[8] = voltageBuffer->getVoltage();
  response[9] = (legsLocked ? 1 : 0)+(panLocked ? 2 : 0)+(tiltLocked ? 4 : 0) +
    (UVon ? 8 : 0);
  response[10] = cerebThreadState;
  response[11] = wcThreadStatus;
  
  return rgram->sendResponse((char *)response, 16, pack);
} // returnRoverResponse() //

void sigIntHandler(int sig) {
  if(exiting) //for some reason, crtl+C sends more than 1 sigint
    return;   //this keeps me from trying to delete things twice.
  exiting = true;
  printf("Exiting. . .\n");
  //delete camSerial;
  delete cerebSerial;
  delete webCam;
  //pthread_mutex_destroy(&cerebSerialMutex);
  exit(0);
}

void debug(int level, char * msg) {
  if(level < DEBUG_LEVEL && showDebug)
    fprintf(stderr, msg);
}

void parseArgs(int argc, char **argv) {
  for(int i=1; i<argc; i++) {
    if(!strcmp(argv[i], "debug"))
      showDebug = true;
    else if(!strcmp(argv[i], "duty"))
      doDutyTest = true;
    else if(!strcmp(argv[i], "duty2"))
      doDutyTest2 = true;
    else if(!strcmp(argv[i], "duty3"))
      doDutyTest3 = true;
    else if(!strcmp(argv[i], "wallFollow"))
      wallFollow = true;
    else if(!strcmp(argv[i], "smartWander"))
      smartWander = true;
    else if(!strcmp(argv[i], "lineFollow"))
      lineFollow = true;
    else if(!strcmp(argv[i], "trackObject"))
      trackObject = true;
    else if(!strcmp(argv[i], "auto"))
      autoStart = true;
    else
      printf("unrecognized command line argument: %s\n", argv[i]);
  }
}

void simulatePanorama() {
  for(int i=0; i<8; i++) 
    for(int j=0; j<2; j++) {
      headMove(true, 157-i*45, true, -30+j*15);
      usleep(timeToArrive(LOOK_MASK)*1000+ 400000);
    }
}

void reportCyclesRun(int cycles, struct timeb start) {
  FILE *duty = fopen("/root/duty.txt", "w");
  struct timeb now;
  ftime(&now);
  int secs = now.time - start.time;
  int minutes = secs / 60;
  secs = secs % 60;
  int hours = minutes / 60;
  minutes = minutes % 60;
  fprintf(duty, "Duty test ran for %d cycles.\n", cycles);
  fprintf(duty, "That's %d hours, %d minutes and %d seconds\n", hours, minutes, secs);
  fclose(duty);
}

void runDutyTest() {
  int timesRun = 0;
  struct timeb start;
  ftime(&start);
  printf("\nstarting duty test\n");
  while(voltageBuffer->getVoltage() >= LOW_BATTERY_THRESHOLD) {
    simulatePanorama();
    crab(200 * !(timesRun % 10), 0);
    simulatePanorama();
    spin(200 * !(timesRun % 10));
    timesRun++;
    reportCyclesRun(timesRun, start);
  }
}

void runDutyTest2() {
  int timesRun = 0;
  struct timeb start;
  ftime(&start);
  printf("\nstarting duty test\n");
  while(voltageBuffer->getVoltage() >= LOW_BATTERY_THRESHOLD) {
    crab(0, 0);
    sleep(6);
    spin(0);
    sleep(6);
    timesRun++;
    reportCyclesRun(timesRun, start);
  }
}

void runDutyTest3() {
  int timesRun = 0;
  struct timeb start;
  ftime(&start);
  printf("\nstarting duty test\n");
  while(voltageBuffer->getVoltage() >= LOW_BATTERY_THRESHOLD) {
    headMove(false, 0, true, 0);
    usleep(500000);
    headMove(false, 0, true, -45);
    usleep(500000);
    headMove(false, 0, true, 0);
    usleep(500000);
    headMove(false, 0, true, 45);
    usleep(500000);
    timesRun++;
    reportCyclesRun(timesRun, start);
  }
}

//should be a smart wall follow - using a couple of states or a timer?
//doesn't work correctly if it's running for the second time...something isn't initialized correctly the second time
void runWallFollow()
{
  int* scanResults;
  int len, len2, len3, maxThree, maxFour, oldMaxThree = 0;
  bool objectFound;
  len = ((120+120)/24)+2;
  len2 = ((0+90)/10)+2;
  len3 = ((90+20)/10)+2;
  printf("Starting Wall Follow\n");
  sleep(3);
  //find object
  while(!demoExit && !exiting)
  {
    objectFound = false;
    while(!objectFound && !demoExit)
    {
      //max = 0, maxTwo = 0;
      scanResults = OScan(-30, -120, 120, 24);
      usleep(60000);
      printf("I just scanned for an object!\n");

      quadTurn(160, 0);

      for(int i = 1; i < len; i++)
	if (scanResults[i] > 80)
	{
	  objectFound = true;
	  break;
	}
    }

    printf("OK! We found an object and we turned parallel to it!\n");
    maxFour = 0;
    while(!demoExit)
    {   //now follow wall by arcing
      maxThree = 0;
      scanResults = OScan(-30, -20, 90, 10);
      usleep(60000);
      for(int k = 0; k < len3; k++)
	if (scanResults[k] > scanResults[maxThree])
	  maxThree = k;
      //printf("%d maxThree = %d and oldMaxThree = %d and scanResults[maxThree] = %d\n", maxFour, maxThree, oldMaxThree, scanResults[maxThree]);
      //quadTurn(180,0);
      //nothing in here tells it what to do if it has lost a wall...
      if (oldMaxThree != 0) //if oldMaxThree equals zero, oldMaxThree is not initialized, so we can't do any tests.  we're supposed to be parallel anyway  
      { 
	
	  if (scanResults[maxThree] < 80)
	{
	  if (scanResults[maxThree] < 70) //this will only really happen if we need to turn a corner
	    quadTurn(180, 20);
	  else  
	    quadTurn(180, 40); 
	}
	else if (scanResults[maxThree] > 90)//ie, we are too close to the wall
	{
	  if (scanResults[maxThree] > 110)//if we're really close, get OUT of there!
	  {
	    if (scanResults[maxThree] > 120)
	      quadTurn(160, -5);
	    else
	      quadTurn(170, -15);
	  }
	  else
	    quadTurn(170, -40);
	}
	  
	else if ((scanResults[maxThree] <= 90 && scanResults[maxThree] >= 80) && maxThree > oldMaxThree)
	  quadTurn(180, -1800);
	 
	else if ((scanResults[maxThree] <= 90 && scanResults[maxThree] >= 80) && maxThree < oldMaxThree)
	  quadTurn(180, 1800);
	  
	else //ie...we're just right and staying that way...
	  quadTurn(200, 0);
	
	/*
	if (scanResults[maxThree] >= 90)
	  radius =(int)(-100.0 / (sqrt(scanResults[maxThree]-88) + 1));
	else if (scanResults[maxThree] <= 80)
	  radius =(int)(100.0 / (sqrt(82-scanResults[maxThree]) + 1));
        else if (maxThree > oldMaxThree)
	  radius = -1800;
        else if (maxThree < oldMaxThree)
	  radius = 1800;
        else
	  radius = 0;
	printf("scanResults[maxThree] = %d and radius = %d\n", scanResults[maxThree], radius);
        quadTurn(180, radius);
	*/
      }
      usleep(500);
      oldMaxThree = maxThree;
      maxFour++;
    } 
    if(demoExit)
      break;
    //prevent refinding the same wall if there is room to turn!
    scanResults = OScan(-30, -120, 120, 24);
    if (scanResults[11] < 90)
      OTurnTo(-45, 0);

    //do a little dance!
    headMove(true, 90, false, 0);
    headMove(true, -180, false, 0);
    headMove(true, 90, false, 0);
    OGoTo(10, 0, 1, 0);
    OTurnTo(45, 0);
    OTurnTo(-90, 0);
    OTurnTo(45, 0);
    //OGoTo(40, 0, 1, 0);
  }
}

//should run smartwander 

void runSmartWander(){
  int angle = 0;
  int direction = 0;
  srand( time(0));

  char buf[100];
  unsigned char *camBuf;
  int avgY, i, lastY = -1;

  printf("\n starting Smart Wander\n");
 
  /*  while(!exiting){
    OGoTo(1000000, 0, 2, false);  

    angle = (int) (360 * (rand()/(RAND_MAX + 1.0))) - 180;

    OTurnTo(angle, false);
    }*/
  /*pseudo:
  while(!exiting)
  drive until obstacle is detected
  turn randomly
  */
  headMove(true, 0, true, -35);
  while(!exiting && !demoExit) {
      if(webCam->grabImage(176, 144, false, false)) {
	camBuf = (unsigned char *)webCam->getBuf();
	avgY = 0;
	for(i=0; i<(176*144); i++)
	  avgY += camBuf[i];
	avgY /= (176*144);
	if(lastY == -1)
	  lastY = avgY;
	sprintf(buf, "the average Y is %d\n", avgY);
	debug(150, buf);
	if(avgY < 35) {
	  printf("camera covered\n");
	  OStopStreaming();
	  break;
	  //	exiting = true;
	}
      }
      if(range < 70)
	crab(255, 0);
      else {
	angle = (int) (1000 * (rand()/(RAND_MAX + 1.0)));
	direction = (int) (2 * (rand()/(RAND_MAX + 1.0)));

	printf("angle: %d, range: %d\n", angle, range);
	for(int i = 0; i < angle; i++){
	  if(direction == 0){
	    spin(200);
	  }
	  else{
	    spin(-200);
	  }
	}
      }
      usleep(5000);
      crab(0, 0);
  }
}

//follow a line based on the color of an object placed in front of the camera

void runLineFollow(){
  double dist, tilt, pan, radius, rovX, rovY;
  int minY, maxY, minU, maxU, minV, maxV;
  //  int aveDiffY, aveDiffU, aveDiffV;
  int* receivedMean;
  //  int leftY, leftU, leftV;
  //  int rightY, rightU, rightV;
  int centerY, centerU, centerV;
  double wierd = 3.14159265358979323846;

  char buf[100];
  unsigned char *camBuf;
  int avgY, i, lastY = -1;

  printf("\n starting Line Follow\n");
  printf("demoExit is %d\n", demoExit);
  sleep(3);

  /*  headMove(true, -45, true, -25);
  usleep(timeToArrive(LOOK_MASK)*1000);
  receivedMean = OGetMean(false);
  
  leftY = receivedMean[6];
  leftU = receivedMean[7];
  leftV = receivedMean[8];
  
  headMove(true, 45, true, -25);
  usleep(timeToArrive(LOOK_MASK)*1000);
  receivedMean = OGetMean(false);

  rightY = receivedMean[6];
  rightU = receivedMean[7];
  rightV = receivedMean[8];*/

  headMove(true, 0, true, -25);
  usleep(timeToArrive(LOOK_MASK)*1000);
  receivedMean = OGetMean(false);

  centerY = receivedMean[6];
  centerU = receivedMean[7];
  centerV = receivedMean[8];

  //  printf("centerV %d rightV %d leftV %d\n", centerV, rightV, leftV);
  /*  aveDiffY = (int) (.5 * (2 * centerY - rightY - leftY));
  aveDiffU = (int) (.5 * (2 * centerU - rightU - leftU));
  aveDiffV = (int) (.5 * (2 * centerV - rightV - leftV));

  if(aveDiffY > 0){
    minY = (int) (centerY - .5 * aveDiffY);
    maxY = 255;
  } else {
    minY = 0;
    maxY = (int) (centerY + .5 * aveDiffY);
  }
  
  if(aveDiffU > 0){
    minU = (int) (centerU - .5 * aveDiffU);
    maxU = 255;
  } else {
    minU = 0;
    maxU = (int) (centerU + .5 * aveDiffU);
  }
  
  if(aveDiffV > 0){
    minV = (int) (centerV - .5 * aveDiffV);
    maxV = 255;
  } else {
    minV = 0;
    maxV = (int) (centerV + .5 * aveDiffV);
  }
  */
  /*  if(centerY > 180){
    printf("bright image\n");
    minY = 0; 
    maxY = 100;
    minU = 100;
    maxU = 255;
    minV = 100;
    maxV = 255;
    } else {*/
  printf("dark image\n");
  minY = 220; 
  maxY = 255;
  minU = 0;
  maxU = 255;
  minV = 0;
  maxV = 255;
  //}
    

  OTrack(minY, maxY, minU, maxU, minV, maxV, 0, true, false, 0);
  printf("tracking values - minY: %d, maxY: %d, minU: %d, maxU %d, minV %d, maxV %d\n", minY, maxY, minU, maxU, minV, maxV);

  while(!demoExit && !exiting){
    if(webCam->grabImage(176, 144, false, false)) {
      camBuf = (unsigned char *)webCam->getBuf();
      avgY = 0;
      for(i=0; i<(176*144); i++)
	avgY += camBuf[i];
      avgY /= (176*144);
      if(lastY == -1)
	lastY = avgY;
      sprintf(buf, "the average Y is %d\n", avgY);
      debug(150, buf);
      if(avgY < 35) {
	/* If currWheel isn't NULL, then I'm interested in whether or not the
	   camera is covered and I don't want the thread to quit.  This is how
	   I make the rover reset to the default P2P configuration. */
	printf("camera covered\n");
	OStopStreaming();
	break;
	//	exiting = true;
      }
    }
    int picksels = webCam->getEPixels();
    printf("getEPixels() = %d\n", picksels);
    printf("Range = %d\n", range);
    if((webCam->getEPixels() > 30) && (range < 120)){
      pan = motorPos[PAN_SERVO];
      pan += ((88-webCam->getTmmX()) * 45 / 176); //TRACK_WIDTH/2 - TrackX * FOV_WIDTH / TRACK_WIDTH;
      tilt = motorPos[TILT_SERVO];
      tilt += ((72-webCam->getTmmY()) * 34 / 144); //TRACK_HEIGHT/2 - TrackY * FOV_HEIGHT / TRACK_HEIGHT
      dist = 32/tan(tilt*wierd/180);
      rovX = dist * cos(pan*wierd/180);
      rovY = dist * sin(pan*wierd/180);
      if(rovX == 0)
	radius = 0;
      else
	radius = -((rovX*rovX + rovY*rovY)/(2*rovY));
      quadTurn(255, radius);
      //      printf("radius %4.1f, rovx %4.1f, rovy %4.1f, dist %4.1f, pan %4.1f, tilt %4.1f, rovX sq %4.1f, rovY sq %4.1f, cos pan %4.1f\n", radius, rovX, rovY, dist, pan, tilt, (rovX*rovX), (rovY*rovY), cos(pan*wierd/180));
    } else {
      //      printf("I should be stopping: range %d\n", range);
      setAll(3, 0,0,0,0,0,0,0,0,true);
    }
  }
}

//////////////////////////////////////////////////////////////////////////////////////////
void runTrackObject(){
  int minY, maxY, minU, maxU, minV, maxV;

  char buf[100];
  unsigned char *camBuf;
  int avgY, i, lastY = -1;

  printf("running track object\n");
  sleep(3);
  headMove(true, 0, true, 30);
  usleep(timeToArrive(LOOK_MASK)*1000);
  UVon = true;
  while(range < 130){
    printf("waiting for object, range: %d\n", range);
    headMove(true, 0, true, 30);
    usleep(timeToArrive(LOOK_MASK)*1000);
  }
  headMove(true, 0, true, 33);
  usleep(timeToArrive(LOOK_MASK)*1000);
  headMove(true, 0, true, 28);
  usleep(timeToArrive(LOOK_MASK)*1000);
  headMove(true, 0, true, 30);
  usleep(timeToArrive(LOOK_MASK)*1000);

  //  int *receivedMean = OGetMean(false);
  usleep(1000);

  minY = 100;//receivedMean[6] - 30;
  maxY = 255;//receivedMean[6] + 30;
  minU = 0;//receivedMean[7] - 30;
  maxU = 150;//receivedMean[7] + 30;
  minV = 0;
  maxV = 100;
  
  printf("tracking values - minY: %d, maxY: %d, minU: %d, maxU %d, minV %d, maxV %d\n", minY, maxY, minU, maxU, minV, maxV);

  OTrack(minY, maxY, minU, maxU, minV, maxV, 0, true, true, 0);
  while(!demoExit) {
    while(!exiting){
      if(webCam->grabImage(176, 144, false, false)) {
	camBuf = (unsigned char *)webCam->getBuf();
	avgY = 0;
	for(i=0; i<(176*144); i++)
	  avgY += camBuf[i];
	avgY /= (176*144);
	if(lastY == -1)
	  lastY = avgY;
	sprintf(buf, "the average Y is %d\n", avgY);
	debug(150, buf);
	if(avgY < 35) {
	  /* If currWheel isn't NULL, then I'm interested in whether or not the
	     camera is covered and I don't want the thread to quit.  This is how
	     I make the rover reset to the default P2P configuration. */
	  printf("camera covered\n");
	  OStopStreaming();
	  UVon = false;
	  break;
	  //exiting = true;
	}
      }
    }
  }
}

int * OGetMean(bool stream){
  char * fakeBuf = new char [2];
  struct sockaddr_in fakeSocket;

  fakeSocket.sin_family = 0;

  fakeBuf[0] = 13; //setting the command type to getMean
  fakeBuf[1] = stream; //setting stream (boolean, 0 or 1)

  Datapack * fakeDPack = new Datapack(fakeBuf, 2, 0, fakeSocket);

  parsePacket(fakeDPack);
  sleep(1);

  int * receivedMean = webCam->getEMean();
  return receivedMean;
}

void OTrack(int minY, int maxY, int minU, int maxU, int minV, int maxV, int trackMethod, bool movePan, bool moveTilt, int driveMethod){
  char * fakeBuf = new char [10];
  struct sockaddr_in fakeSocket;

  fakeSocket.sin_family = 0;

  fakeBuf[0] = 'r'; //setting the command type to getMean
  fakeBuf[1] = minY;
  fakeBuf[2] = maxY;
  fakeBuf[3] = minU;
  fakeBuf[4] = maxU;
  fakeBuf[5] = minV;
  fakeBuf[6] = maxV;
  fakeBuf[7] = trackMethod;
  fakeBuf[8] = ((movePan ? 2 : 0) + (moveTilt ? 1 : 0));
  fakeBuf[9] = driveMethod;

  Datapack * fakeDPack = new Datapack(fakeBuf, 10, 0, fakeSocket);

  parsePacket(fakeDPack);
}


void OGoTo(int dist, int angle, int safetyLevel, bool takePics){
  char * fakeBuf = new char [12];
  struct sockaddr_in fakeSocket;

  fakeSocket.sin_family = 0;

  fakeBuf[0] = 'g'; //setting the command type to goTo
  fakeBuf[1] = safetyLevel; //setting stream (boolean, 0 or 1)
  fakeBuf[2] = takePics ? 0 : 1;
  fakeBuf[3] = 0;
  fakeBuf[7] = (dist & 255);
  fakeBuf[6] = ((dist >> 8) & 255);
  fakeBuf[5] = ((dist >> 16) & 255);
  fakeBuf[4] = ((dist >> 24) & 255);
  fakeBuf[11] = (angle & 255);
  fakeBuf[10] = ((angle >> 8) & 255);
  fakeBuf[9] = ((angle >> 16) & 255);
  fakeBuf[8] = ((angle >> 24) & 255);
  

  Datapack * fakeDPack = new Datapack(fakeBuf, 12, 0, fakeSocket);

  parsePacket(fakeDPack);

  sleep((int)(abs(dist)/2.7 + .5));
}

void OTurnTo(int degrees, bool takePics){
  char * fakeBuf = new char [8];
  struct sockaddr_in fakeSocket;

  fakeSocket.sin_family = 0;

  fakeBuf[0] = 't'; //setting the command type to turnTo
  fakeBuf[1] = 0; //setting stream (boolean, 0 or 1)
  fakeBuf[2] = takePics ? 0 : 1;
  fakeBuf[3] = 0;
  fakeBuf[7] = (degrees & 255);
  fakeBuf[6] = ((degrees >> 8) & 255);
  fakeBuf[5] = ((degrees >> 16) & 255);
  fakeBuf[4] = ((degrees >> 24) & 255);

  Datapack * fakeDPack = new Datapack(fakeBuf, 8, 0, fakeSocket);

  parsePacket(fakeDPack);

  sleep((int)(abs(degrees)/10) + 1);

}

void OStopStreaming(){
  char * fakeBuf = new char [1];
  struct sockaddr_in fakeSocket;

  fakeSocket.sin_family = 0;

  fakeBuf[0] = 'o'; //setting the command type to getMean

  Datapack * fakeDPack = new Datapack(fakeBuf, 1, 0, fakeSocket);

  parsePacket(fakeDPack);
}

int * OScan(int tilt, int minPan, int maxPan, int step){
  char * fakeBuf = new char [20];
  struct sockaddr_in fakeSocket;
  int * results;

  //  printf("OScan in rover.cpp trying to send the following to cerebOScan: tilt %d minPan %d maxPan %d step %d\n", tilt, minPan, maxPan, step);

  fakeSocket.sin_family = 0;

  fakeBuf[0] = 'n'; //setting the command type to scan
  fakeBuf[1] = 0;
  fakeBuf[2] = 0;
  fakeBuf[3] = 0;
  fakeBuf[7] = (tilt & 255);
  fakeBuf[6] = ((tilt >> 8) & 255);
  fakeBuf[5] = ((tilt >> 16) & 255);
  fakeBuf[4] = ((tilt >> 24) & 255);
  fakeBuf[11] = (minPan & 255);
  fakeBuf[10] = ((minPan >> 8) & 255);
  fakeBuf[9] = ((minPan >> 16) & 255);
  fakeBuf[8] = ((minPan >> 24) & 255);
  fakeBuf[15] = (maxPan & 255);
  fakeBuf[14] = ((maxPan >> 8) & 255);
  fakeBuf[13] = ((maxPan >> 16) & 255);
  fakeBuf[12] = ((maxPan >> 24) & 255);
  fakeBuf[19] = (step & 255);
  fakeBuf[18] = ((step >> 8) & 255);
  fakeBuf[17] = ((step >> 16) & 255);
  fakeBuf[16] = ((step >> 24) & 255);

  Datapack * fakeDPack = new Datapack(fakeBuf, 20, 0, fakeSocket);

  //  parsePacket(fakeDPack);  
  /*  char *buf = fakeDPack->getData();
  tilt = (int)ntohl(*((int *) (buf+4)));
  int panMin = (int)ntohl(*((int *) (buf+8)));
  int panMax = (int)ntohl(*((int *) (buf+12)));
  step = (int)ntohl(*((int *) (buf+16)));

  printf("OScan in rover.cpp trying to send the following to cerebOScan: tilt %d minPan %d maxPan %d step %d\n", tilt, panMin, panMax, step);*/

  results = cerebOScan(fakeDPack);
  return results;  
}

bool cameraWasCovered = false, diagThreadExited = false, *currWheel = NULL;

void * diagnosticThread(void * timeToRun) {
  char buf[100];
  unsigned char *camBuf;
  int runTime = *((int *) timeToRun);
  int avgY, i, lastY = -1;
  struct timeb start, now;
  cameraWasCovered = diagThreadExited = false;
  ftime(&start);
  ftime(&now);
  //printf("runtime is %d\n", runTime);
  //wait for 5 seconds or until someone tries to connect
  while(!rgram->hasNewPacket()) {
    if(webCam->grabImage(176, 144, false, false)) {
      camBuf = (unsigned char *)webCam->getBuf();
      avgY = 0;
      for(i=0; i<(176*144); i++)
	avgY += camBuf[i];
      avgY /= (176*144);
      if(lastY == -1)
	lastY = avgY;
      sprintf(buf, "the average Y is %d\n", avgY);
      debug(150, buf);
      if(avgY < 35) {
	/* If currWheel isn't NULL, then I'm interested in whether or not the
	   camera is covered and I don't want the thread to quit.  This is how
	   I make the rover reset to the default P2P configuration. */
	if(currWheel != NULL) {
	  printf("currWheel was NULL\n");
	  *currWheel = true;
	  UVon = !UVon; // change the state of the UV tube 
	  headMove(false, 0, false, 0); 
	}
	else {
	  printf("camera covered\n");
	  cameraWasCovered = true;
	  break;
	}
      }
      lastY = avgY;
    }else
      usleep(20000);
    ftime(&now);
    //if timeout is nonzero and it has timed out. . .
    if(runTime > 0 && timebDiff(now, start) >= runTime)
      break;
  }
  UVon = false;
  headMove(false, 0, false, 0); //turn the UV tube off
  diagThreadExited = true;
  printf("Diagnostic Thread exiting...\n");
  pthread_exit(0);
}

void * demoThread(void * timeToRun) {
  char buf[100];
  unsigned char *camBuf;
//int runTime = *((int *) timeToRun);
  int avgY, i, lastY = -1;
//bool keepDemoing = true;
//struct timeb start, now;
//ftime(&start);
//ftime(&now);
  // printf("we're in demoThread.  I hope this works!\n");
  //while(!rgram->hasNewPacket()) { 
  sleep(3); //start up slowly, so the user has a chance to move their hand
  while(!exiting) {
    if(webCam->grabImage(176, 144, false, false)) {
      printf("Took a picture!\n");
      camBuf = (unsigned char *)webCam->getBuf();
      avgY = 0;
      for(i = 0; i < (176*144); i++)
	avgY += camBuf[i];
      avgY /= (176*144);
      if(lastY == -1)
	lastY = avgY;
      sprintf(buf, "the average Y is %d\n", avgY);
      printf("the average Y is %d\n", avgY);
      debug(150, buf);
      if(avgY < 35) {
	printf("Changing demo\n");
	//twitch to alert user that the camera was covered
	for(i=0; i<=25; i++)
	  setAll(ALL_MASK, 0, 0, i, i, i, i, 0, 0, true);
	for(i=5; i>=-25; i--)
	  setAll(ALL_MASK, 0, 0, i, i, i, i, 0, 0, true);
	for(i=-5; i<=0; i++)
	  setAll(ALL_MASK, 0, 0, i, i, i, i, 0, 0, true);

	demoExit = true;
	//usleep(10000);
	sleep(4);
	demoExit = false;
	if(wallFollow)
	{
	  wallFollow = false;
	  lineFollow = true;
	  runLineFollow();
	}
	//add more tests for as many demos as you want to cycle through
	else if(lineFollow)
	{
	  lineFollow = false;
	  OStopStreaming();
	  noDemo = true;
	  pthread_exit(0);
	}
      }
      lastY = avgY;
    }
    else
      usleep(20000);
//ftime(&now);

//if(runTime > 0 && timebDiff(now, start) >= runTime)
//break;
    sleep(3);
  }
  printf("I'm about to die!\n");
  pthread_exit(0);
}

void runDiagnostic() {
  pthread_t diagThread;
  int timeToRun = 6500, i, j;
  bool wheelCover[6] = {false, false, false, false, false, false};
  headMove(true, 0, true, 45);
  usleep(timeToArrive(LOOK_MASK)*1000);
  if(pthread_create(&diagThread, 0, diagnosticThread, (void *)&timeToRun)) {
    printf("Failed to start diagnostic thread\n");
    return;
  }
  if(pthread_join(diagThread, NULL))
    return;
  usleep(3000);
  headMove(true, 0, true, 0);
  if(cameraWasCovered) {
    //block to test all mechanical parts of rover

    //move steering servos so that you know you've covered the camera
    for(i=0; i<=25; i++)
      setAll(ALL_MASK, 0, 0, i, i, i, i, 0, 0, true);
    for(i=5; i>=-25; i--)
      setAll(ALL_MASK, 0, 0, i, i, i, i, 0, 0, true);
    for(i=-5; i<=0; i++)
      setAll(ALL_MASK, 0, 0, i, i, i, i, 0, 0, true);
    sleep(1);
    UVon = true; //turn uv on
    //move pan around
    headMove(true, 90, true, 0);
    usleep(timeToArrive(LOOK_MASK)*1000);
    headMove(true, -90, true, 0);
    usleep(timeToArrive(LOOK_MASK)*1000);
    headMove(true, 0, true, 0);
    usleep(timeToArrive(LOOK_MASK)*1000);
    //move tilt around
    headMove(true, 0, true, -45);
    usleep(timeToArrive(LOOK_MASK)*1000);
    headMove(true, 0, true, 45);
    usleep(timeToArrive(LOOK_MASK)*1000);
    headMove(true, 0, true, 0);
    usleep(timeToArrive(LOOK_MASK)*1000);
    UVon = false; //turn uv off

    //block to test for network change and demo runs
    timeToRun = 0;
    if(pthread_create(&diagThread, 0, diagnosticThread, &timeToRun)) {
      printf("Failed to start diagnostic thread\n");
      return;
    }
    usleep(50000);
    pthread_detach(diagThread); //free up its memory resources when it exits
    headMove(true, 0, true, 45);
    for(j=0; j<4; j++) {
      currWheel = wheelCover + j;
      setAll(DRIVE_MASK, 0, 0, 90*!j, 90*!(j-3), 90*!(j-2), 90*!(j-1), 0, 0, true);
      usleep(timeToArrive(DRIVE_MASK)*1000);
      setAll(DRIVE_MASK, 0, 0, -90*!j, -90*!(j-3), -90*!(j-2), -90*!(j-1), 0, 0, true);
      usleep(timeToArrive(DRIVE_MASK)*1000);
      setAll(DRIVE_MASK, 0, 0, 0, 0, 0, 0, 0, 0, true);
      usleep(timeToArrive(DRIVE_MASK)*1000);
    }
    //code to check for demos
    /*
    UVon = true;
    printf("watching for wall follow trigger\n");
    currWheel = wheelCover + 5;
    usleep(100000);
    UVon = false;
    usleep(100000);
    UVon = true;
    usleep(100000);
    UVon = false;
    usleep(100000);
    UVon = true;
    usleep(100000);
    sleep(5);

    headMove(true, -45, true, -45);
    usleep(timeToArrive(LOOK_MASK)*1000);
    headMove(true, 45, true, 45);
    usleep(timeToArrive(LOOK_MASK)*1000);
    headMove(true, 0, true, 45);
    usleep(timeToArrive(LOOK_MASK)*1000);


    UVon = false;
    printf("watching for line follow trigger\n");
    currWheel = wheelCover + 6;
    usleep(100000);
    UVon = true;
    usleep(100000);
    UVon = false;
    usleep(100000);
    UVon = true;
    usleep(100000);
    UVon = false;
    usleep(100000);
    sleep(5);
    */
    UVon = true;
    currWheel = NULL;
    headMove(true, 0, true, 0);
    int crabAngle = 0;
    /* if the camera was covered while it was moving the front left and front
       right wheels, change back to the default P2P configuration. */
    if(wheelCover[0] && !wheelCover[1] && !wheelCover[2] && wheelCover[3]) {
      crabAngle = restoreP2P();
      OGoTo(5, crabAngle, 0, false);
      usleep(timeToArrive(DRIVE_MASK)*1000);
    }else if(!wheelCover[0] && wheelCover[1] && wheelCover[2] && !wheelCover[3]) {
      crabAngle = restoreCMU();
      OGoTo(5, crabAngle, 0, false);
      usleep(timeToArrive(DRIVE_MASK)*1000);
    }
    //printf("got past 5 and 6.. \n");

    sleep(1);
    UVon = true; //turn uv on

     //drive forward, slow->fast->slow
    for(j=0; j<=256; j++)
      crab(255-abs(128-j), 0);
    //drive backwards, slow->fast->slow
    for(j=0; j<=256; j++)
      crab(abs(128-j)-255, 0);
    crab(0, 0); //straighten out the wheels

    while(!cameraWasCovered && !diagThreadExited) {
      if(range > 30 && range < 60)
	crab(255, 0);
      else if(range > 90 && range < 135)
	crab(-255, 0);
      else {
	crab(0, 0);
      }
      usleep(5000);
    }
    //twitch to alert user that the camera was covered
    for(i=0; i<=25; i++)
      setAll(ALL_MASK, 0, 0, i, i, i, i, 0, 0, true);
    for(i=5; i>=-25; i--)
      setAll(ALL_MASK, 0, 0, i, i, i, i, 0, 0, true);
    for(i=-5; i<=0; i++)
      setAll(ALL_MASK, 0, 0, i, i, i, i, 0, 0, true);

    wallFollow = true;
    if(pthread_create(&diagThread, 0, demoThread, &timeToRun))
    {
      printf("Couldn't start demoThread\n");
      return;
    }
    usleep(500000);
    pthread_detach(diagThread);
    crab(0, 0);
  }
}

//this function changes networks to the default P2P configuration
//it returns the angle at which to turn the motors
int restoreP2P() {
  pid_t pid;
  int status;FILE *error;

  switch(pid = fork()) {
  case -1: //failure
    printf("can't fork!, exiting. . .\n");
	error = fopen("/root/error.txt", "w");
	fprintf(error, "can't fork!, exiting. . .\n");
	fclose(error);
    exit(-1);
  case 0: //we're the child
    if(execl("/bin/bash", "bash", "/root/scripts/restoreP2P", 0)) {
      printf("The exec failed!\n");
	error = fopen("/root/error.txt", "w");
	fprintf(error, "The exec failed!\n");
	fclose(error);
      exit(-1);
    }
  default: //we're the parent
    waitpid(pid, &status, 0); //wait for child to die
    if(status != 0) {
      printf("Restoring default P2P failed!\n");
      error = fopen("/root/error.txt", "w");
      fprintf(error, "Restoring default P2P failed!\n");
      fclose(error);
      return -45;
    }
  }
  return -90;
}

//this function changes networks to CMU with DHCP on
//it returns the angle at which to turn the motors
int restoreCMU() {
  pid_t pid;
  int status;FILE *error;

  switch(pid = fork()) {
  case -1: //failure
    printf("can't fork!, exiting. . .\n");
	error = fopen("/root/error.txt", "w");
	fprintf(error, "can't fork!, exiting. . .\n");
	fclose(error);
    exit(-1);
  case 0: //we're the child
    if(execl("/bin/bash", "bash", "/root/scripts/netStation", 0)) {
      printf("The exec failed!\n");
	error = fopen("/root/error.txt", "w");
	fprintf(error, "The exec failed!\n");
	fclose(error);
      exit(-1);
    }
  default: //we're the parent
    waitpid(pid, &status, 0); //wait for child to die
    if(status != 0) {
      printf("Restoring CMu configuration failed!\n");
      error = fopen("/root/error.txt", "w");
      fprintf(error, "Restoring CMU configuration failed!\n");
      fclose(error);
      return 45;
    }
  }
  return 90;
}
