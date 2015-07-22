#include "highLevel.h"

//the variable activeCmd is set to one of these
#define NO_CMD   0
#define GOTO     1
#define TURNTO   2
//#define RELATIVE 3
//#define ALIGN    4

//int ix, iy, itheta; //integrated x, y, theta
int highLevelStatus = 0;
//double x, y, theta; //for internal use
//double objx, objy, bufferedx, bufferedy;
struct timeb timeoutHL; //the timeout for high-level commands
struct timeb startHL; //the timeout for high-level commands

int highLevelCmd = NO_CMD; //the currently active command
int vel, distTraveled, timeDivider;
int goToAngle;
//bool takePictures = true;
struct safetyPos *currSafety;

//int lastITime;          //the last time the integration function integrated
/*
//vars for landmark longitudinal
int noPixCount=0, myError=0, dist;
struct timeb guessUpdated; //the time the buffered X and Y were last updated
double bufferedX = 0, bufferedY = 0;
int goodPan, goodTilt; //the last good pan and tilt values
bool relativeKeepTurning; //true if stopAtMark should keep turning, false if closer than 50 inches to landmark 
*/
//void relativeStep();
//void alignStep();
//void resetIntegration();
//void integrate();
  

//sets everything up for executing a high level command.
//data is the data packet from the java side
//cmd is the type of command that is going to be executed
void initHL(int cmd, char safetyLevel, bool takePics, Datapack * pack) {
  highLevelCmd = cmd;
  highLevelStatus = HL_CONTINUE;
  ftime(&startHL);
  distTraveled = 0;
  if(safetyLevel == CYCLE_SAFETY) {
    Datapack *newPack = new Datapack(pack);
    webCam->doSpecial(takePics ? WC_CYCLE_PIC : WC_CYCLE_NOPIC, newPack);
  }else if(takePics) {
    Datapack *newPack = new Datapack(pack);
    webCam->doSpecial(WC_PIC, newPack);
  }
}

//call this command to kill the current high level command
//this is needed to keep things consistent
//returns true if there was a command running else false
bool kill(int status) {
  char buf[100];
  killRobot();
  //FIX THIS SO THAT IT DOESN'T ALWAYS KILL ANYTHING THE CAMERA IS DOING
  webCam->doSpecial(WC_STOP, NULL); //stop whatever the webCam thread is doing
  highLevelCmd = NO_CMD;
  highLevelStatus = status;
  if(highLevelCmd != NO_CMD) {
    sprintf(buf, "kill %d called with status %d\n", highLevelCmd, status);
    //printf("kill %d called with status %d\n", highLevelCmd, status);
    debug(200, buf);
    return true;
  }
  return false;
}

void updateDistTraveled() {
  struct timeb now;
  ftime(&now);
  int timeDiff = timebDiff(now, startHL);

  //the following adjustments compensate for the stopping time which is part of the
  //turn time calculation (TURN_CONST_B etc)
  if(highLevelCmd == TURNTO)
    distTraveled = abs((int) timeDiff / timeDivider) + 4;
  else
    distTraveled = abs((int) timeDiff / timeDivider) + 1;

  //  printf("distTraveled: %d \n", distTraveled);
}

int adjustTimeDivider(int initialDivider, int dist, bool isTurn) {
  //use the global motor adjustment
  if(isTurn) {
    timeDivider = (int) round(initialDivider * turnAdjust);
    return abs((int) round(initialDivider * dist * turnAdjust));
  }else {
    timeDivider = (int) round(initialDivider * driveAdjust);
    return abs((int) round(initialDivider * dist * driveAdjust));
  }
}

//starts the goTo function.  After this it will keep going.
int doGoTo(Datapack *pack) {
  char * data = pack->getData(), safety;
  distTraveled = 0;

  int cm = (int)ntohl(*((uint32_t *) (data+4)));
  goToAngle = (int)ntohl(*((uint32_t *) (data+8)));

  printf("cm %d goToAngle %d \n", cm, goToAngle);

  if(cm > 0)
    safety = data[1];
  else //if going backwards, don't turn safety on because I can't see
    safety = NO_SAFETY;

  if(cm == 0) {
    kill(SUCCESS);
    reply(SUCCESS, pack); //started OK
    //    delete pack;
    endCerebAction(pack, false, false, false);
    return SUCCESS;
  }

  if(goToAngle > 90 || goToAngle < -90) {
    kill(BAD_INPUT);
    reply(BAD_INPUT, pack); 
    delete pack;
    return BAD_INPUT;
  }

  if(safety == CYCLE_SAFETY) {
    currSafety = getSafetyPos();
    if(currSafety != NULL) //prevent seg fault
      headMove(true, currSafety->pan+goToAngle, true, currSafety->tilt);
    else
      safety = NO_SAFETY;
  }
  
  //if they're using safety, I need the head
  if(initCerebAction(pack, true, safety != NO_SAFETY, safety != NO_SAFETY, CEREB_DRIVETO)) 
    return RESOURCE_CONFLICT; //quit if I can't lock the resources I need
  reply(SUCCESS, pack);
  if(safety == STATIC_SAFETY) //look down farther if using static safety mode
    headMove(true, 0, true, SS_ANGLE);
  else
    headMove(true, 0, true, -15);

  int cerebResp = crab(0, goToAngle);
  /* In this while loop, it is ESSENTIAL to refresh the state.  Communication
     with the cerebellum updates the predicted positions of the servos and 
     keeps the cerebellum from timing out and putting everything to sleep! */
  while(timeToArrive(ALL_MASK) > 0)
    cerebResp = headMove(false, 0, false, 0); // refresh state

  vel = (cm > 0) ? SPEED : -SPEED;
  vel = (int)(vel * (driveSpeed));
  printf("drive velocity after drivespeed adjustment: %d, driveSpeed: %f\n", vel, driveSpeed);

  //drive time calculation
  resetAndAdd(&timeoutHL, adjustTimeDivider(GOTO_CONST, cm, false));
    
  initHL(GOTO, safety, data[2] == 0, pack);
  int retVal = SUCCESS;
  while(!timeoutb(timeoutHL)) {
    cerebResp = crab(vel, goToAngle);
    updateDistTraveled();
    if(cerebResp != SUCCESS) {
      retVal = CEREB_TIMEOUT;
      break;
    }
    if(safety == STATIC_SAFETY && range > SS_THRESH) {
      retVal = OBSTACLE_DETECTED;
      break;
    }
    if(highLevelStatus != HL_CONTINUE) {//something else has called kill
      crab(0, goToAngle);
      break;
    }
    usleep(45000);
  }
  if(highLevelStatus == HL_CONTINUE) //if this is the case, its not killed
    kill(retVal);
  if(safety != NO_SAFETY) {
    webCam->doSpecial(WC_STOP, NULL); //stop whatever the webCam thread is doing
    //give the camera thread time to stop
    for(int i=0; i<20 && wcThreadStatus != WC_NO_CMD; i++) {
      headMove(false, 0, false, 0);
      usleep(25000);
    }
  }

  endCerebAction(pack, true, safety != NO_SAFETY, safety != NO_SAFETY);
  return SUCCESS;
}

//starts the turnTo function.  After this it will keep going.
int doTurnTo(Datapack *pack) {
  char * data = pack->getData();
  distTraveled = 0;
 
  int degrees = (int)ntohl(*((uint32_t *) (data+4)));  //the numbero of degrees to turn
  bool takePics = data[1] == 0;

  printf("in doturnto, degrees is %d \n", degrees);

  while(degrees > 180)
    degrees -= 360;
  while(degrees < -180)
    degrees += 360;

  if(degrees == 0) {
    kill(SUCCESS);
    reply(SUCCESS, pack); //started OK
    //    delete pack;
    endCerebAction(pack, false, false, false);
    return SUCCESS;
  }

  /* This is a bit of a hack that really needs explaining.
     The Java exhibit code takes a picture before calling TurnTo.
     This section of code will try and wait for the picture to finish before
     starting the turnTo.  It will wait for at most about a second.     
     I'm doing it this way because requiring a fix on the java code would be 
     hard to write and send out. */
  for(int i=0; i<20 && wcThreadStatus == WC_GRAB; i++) {
    headMove(false, 0, false, 0);
    usleep(50000);
  }

  //if they're using safety, I need the head
  if(initCerebAction(pack, true, takePics, takePics, CEREB_TURNTO)) 
    return RESOURCE_CONFLICT; //quit if I can't lock the resources I need
  reply(SUCCESS, pack);
  //printf("starting a turn to of %d degrees\n", degrees);
  int cerebResp = spin(0);
  /* In this while loop, it is ESSENTIAL to refresh the state.  Communication
     with the cerebellum updates the predicted positions of the servos and 
     keeps the cerebellum from timing out and putting everything to sleep! */
  while(timeToArrive(DRIVE_MASK) > 0)
    cerebResp = headMove(false, 0, false, 0); // refresh state
  
  vel = (degrees > 0) ? SPEED : -SPEED;

  //actual turning time calculation
  int turnTime = adjustTimeDivider(TURN_CONST, degrees, true) + TURN_CONST_B;
  if(turnTime < 0)
    turnTime = 0;
  resetAndAdd(&timeoutHL, turnTime);
  initHL(TURNTO, NO_SAFETY, takePics, pack);

  int retVal = SUCCESS;
  while(!timeoutb(timeoutHL)) {
    cerebResp = spin(vel);
    updateDistTraveled();
    if(cerebResp != SUCCESS) {
      retVal = CEREB_TIMEOUT;
      break;
    }
    if(highLevelStatus != HL_CONTINUE) {//something else has called kill
      spin(0);
      break;
    }
    usleep(45000);
  }
  if(highLevelStatus == HL_CONTINUE) //if this is the case, its not killed
    kill(retVal);
  if(takePics){
    webCam->doSpecial(WC_STOP, NULL); //stop whatever the webCam thread is doing
    //give the camera thread time to stop
    for(int i=0; i<20 && wcThreadStatus != WC_NO_CMD; i++) {
      headMove(false, 0, false, 0);
      usleep(25000);
    }
  }
  endCerebAction(pack, true, takePics, takePics);
  return SUCCESS;
}

/***************************************************************************
 * CODE FOR DRIVE TO AND TURN TO
 ***************************************************************************/
/*
//converts a pan and trackX to a real pan
//IMPORTANT: takes and returns angles in radians
double objPan(double pan, int tx) {
  return pan + (40 - tx) * .011; // .011=pi/180*50/80
}

//converts a tilt and trackX to a real tilt
//IMPORTANT: takes and returns angles in radians
double objTilt(double tilt, int ty) {
  return tilt + (ty - 72) * .004;//.006=pi/180*50/143
}

// objx is the distance from the rear axle to the landmark
// objy is the number of inches to the left of the trikebot
void getObjectXY(int camX, int camY, int panDeg, int tiltDeg) {
  if(camX == 0)
    camX = 40;
  if(camY == 0)
    camY = 72;
  //first, the pan tilt that the camera is at.
  double pan = panDeg * (M_PI/180);
  double tilt = tiltDeg * (M_PI/180);
  //now calculate where the camera is relative to the rear axle
  double camD = AXLE_TO_CAM + CAM_LENGTH*cos(pan);
  double camZ = CAM_HEIGHT + CAM_LENGTH*sin(tilt);
  //now make the pan and tilt represent where the object is
  pan  = objPan(pan, camX);
  tilt = objTilt(tilt, camY);
  double Dist = camZ/tan(-tilt);
  objx = camD + Dist*cos(pan);
  objy = Dist*sin(pan);
}

//buffers the data into the bufferedx and bufferedy vars
//returns true if there is new information, else false
//for this to work, the TimeoutThread needs to keep updating timeouts[THE_TIME]
int maxMoves = 0; //the number of times in a row I made the max movement
bool bufferOjbectXY(bool reset) {
  getObjectXY(trackX, trackY, motorPos[PAN_SERVO], motorPos[TILT_SERVO]);
  if(reset) {
    bufferedX = objx;
    bufferedY = objy;
    copyTimeb(&guessUpdated, trackUpdated);
  }else if(timebDiff(trackUpdated, guessUpdated) > 0) {

    //if(DEBUG) System.err.print("Time to update guess ");
    if(motorPos[TILT_SERVO] > 0) {
      //netCom->sends(DEBUG_SOCK, "Lost Tracking because head angle > 0\r\n");
      kill(TRACKING_HIGH);
      return false;
    }
    if(trackPix == 0) 
      noPixCount++;
    else
      noPixCount = 0;
      
    double maxMovement = timebDiff(trackUpdated, guessUpdated)*abs(vel)/1000.0; //in inches, twice as far as it should go
    if(fabs(objx-bufferedX) < maxMovement || maxMoves >= 5) {
      bufferedX = objx;
      maxMoves = 0;
      //if(DEBUG) System.err.println("change within limits "+guessX);
    }else {
      if(objx < bufferedX)
	bufferedX -= maxMovement;
      else
	bufferedX += maxMovement;
      //if(DEBUG) System.err.println("change too much "+guessX);
      maxMoves++;
    }
    bufferedY = objy;
    copyTimeb(&guessUpdated, trackUpdated);
    return true;
  }
  return false;
}

//starts the landmark-relative action, which drives to a certain distance from a landmark.
void relativeStart(char *data) {
  if(!trackOn) {
    char debug[100];
    sprintf(debug, "Not starting relative, trackOn = %s, trackpix = %d\r\n",
	    trackOn?"true":"false", trackPix);
    //netCom->sends(DEBUG_SOCK, debug);
    char returnCode = CANT_START;
    sendPacket(&returnCode, 1, MAIN_SOCK);
    stopTrack(false);
    return;
  }
  noPixCount = (trackPix == 0) ? 3 : 0;

  relativeKeepTurning = true;
  initHL(data, RELATIVE);
  goodPan = motorPos[PAN_SERVO];
  goodTilt = motorPos[TILT_SERVO];
	
  bufferOjbectXY(true);
  dist = *((int *)(data+4));
  vel = 200;
  //netCom->sends(DEBUG_SOCK, "Starting the stopAtMark\r\n");
  if(turnAndSleep(0) != -1) {
    setVel(vel);
    executeStep();
  }else
    kill(CEREB_TIMEOUT);
}

//handle the case where the tracking on the camera indicates no pixels.
void handleNoPix() {
  char debug[100];
  int n=sprintf(debug, "No pixels, times = %d.\r\n", noPixCount);
  //netCom->sends(DEBUG_SOCK, debug, n);
  //if(motorPos[MOTOR]) 
  //setVel(0);
  if(noPixCount > 70) { //about 4 seconds?
    //netCom->sends(DEBUG_SOCK, "Lost Tracking because noPixCount > 70\r\n");
    kill(LOST_LANDMARK);
  }
}

//handle the case where the camera hasn't sent anything in the last time period
void handleNoTrackData(int goodPan, int goodTilt) {
  char debug[100];
  struct timeb now;
  ftime(&now);
  int timeDiff = timebDiff(now, trackUpdated);
	
  if(timeDiff > 200) { //it's been a while since I've gotten new data
    int n=sprintf(debug, "No track data, time = %d\r\n", timeDiff);
    //netCom->sends(DEBUG_SOCK, debug, n);
    stopTrack(false);
    setVel(0);
    headMove(goodPan, goodTilt);
    usleep(250000);
    colorTrackAgain(); //call colorTrack with the params from last time
    //check 3 times for new tracking data then give up
    int trackWorked=0, mask = 0;
    if(handleTracking()) {
      trackWorked++; mask += 1;
    }
    if(handleTracking()) {
      trackWorked++; mask += 2;
    }
    if(handleTracking()) {
      trackWorked++; mask += 4;
    }
    if(handleTracking()) {
      trackWorked++; mask += 8;
    }
    int len=sprintf(debug, "times worked = %d, mask = %d\r\n", trackWorked, mask);
    //netCom->sends(DEBUG_SOCK, debug, len);
    if(trackWorked < 2) 
      kill(CAM_DEAD);
  }
}

//handles one step of a landmark-relative motion.
void relativeStep() {
  char debug[100];
  if(bufferOjbectXY(false)) {
    //goodPan = PanPos2Angle(motorPos[PAN_SERVO]); 
    //goodTilt = TiltPos2Angle(motorPos[TILT_SERVO]);

    if(noPixCount >= 3)
      handleNoPix();
    else if(bufferedX > dist) {
      double radius=0;
      int angle = 0;

      if(bufferedX < dist + 20)
	relativeKeepTurning = false;

      if(bufferedY != 0 && relativeKeepTurning) {
	//the radius I'm turning around.
	radius = (bufferedX*bufferedX + bufferedY*bufferedY)/((double)2*bufferedY);
	angle = (int) ((-360/M_PI)*atan(11/radius));
      }
      drive(angle, vel);
      if(returnRoverResponse(false) == -1) {
	usleep(50000);
	if(returnRoverResponse(false) == -1)
	  kill(CEREB_TIMEOUT);
      }
      if(timesStuck() > 5)  //if current has been > 30,000 5 times in a row
	kill(HIT_OBSTACLE);

      int n=sprintf(debug, "bufx: %f, bufy: %f, radius: %f, angle: %d\r\n",
		    bufferedX, bufferedY, radius, angle);
      //netCom->sends(DEBUG_SOCK, debug, n);
    }else if(bufferedX > 0)  //we're done
      kill(SUCCESS);
  }else 
    handleNoTrackData(goodPan, goodTilt);
  //integrate();
}
*/
/***************************************************************************
 * CODE FOR ALIGN TO
 ***************************************************************************/
/*
//starts the action that aligns the trikebot to face the landmark
void alignStart(char *data) {
  if(!trackOn) {
    //netCom->sends(DEBUG_SOCK, "Not starting align, track not on.");
    char returnCode = CANT_START;
    sendPacket(&returnCode, 1, MAIN_SOCK);
    stopTrack(false);
    return;
  }
  noPixCount = (trackPix == 0) ? 3 : 0;

  initHL(data, ALIGN);
  //goodPan = PanPos2Angle(motorPos[PAN_SERVO]); 
  //goodTilt = TiltPos2Angle(motorPos[TILT_SERVO]);
  
  double pan = objPan(motorPos[PAN_SERVO] * (M_PI/180), trackX);
  bufferOjbectXY(true);
   
  if(pan < 0) 
    vel = 150;
  else 
    vel = -150;

  //netCom->sends(DEBUG_SOCK, "Starting the alignToMark\r\n");
  if(turnAndSleep(90) != -1) {
    setVel(vel);
    executeStep();
  }else
    kill(CEREB_TIMEOUT);
}

void alignStep() {
  char debug[100];
  //I'm only using this because it checks tracking
  if(bufferOjbectXY(false)) {
    goodPan = motorPos[PAN_SERVO]; 
    goodTilt = motorPos[TILT_SERVO];

    double pan = objPan(motorPos[PAN_SERVO] * (M_PI/180), trackX ? trackX : 40);
    int len = sprintf(debug, "panAng: %d, trackX: %d, pan: %d, pv:%f\r\n", 
		      motorPos[PAN_SERVO], trackX, (int)(180*pan/M_PI), pan*vel);
    //netCom->sends(DEBUG_SOCK, debug, len);

    if(noPixCount >= 3)
      handleNoPix();
    else if(pan*vel < -3) {
      len = sprintf(debug, " driving at %d ", vel);
      //netCom->sends(DEBUG_SOCK, debug, len);
      drive(90, vel);
      if(returnRoverResponse(false) == -1) {
	usleep(50000);
	if(returnRoverResponse(false) == -1)
	  kill(CEREB_TIMEOUT);
      }
      if(timesStuck() > 5)  //if current has been > 30,000 5 times in a row
	kill(HIT_OBSTACLE);

    }else  //we're done
      kill(SUCCESS);
  }else 
    handleNoTrackData(goodPan, goodTilt);

  //integrate();
}
*/
/***************************************************************************
 * CODE FOR ENCODERS
 ***************************************************************************/
/*
void resetIntegration() {
  ix = iy = itheta = lastITime = 0;
  x = y = theta = 0;
}

int round(double d) {
  if(d == 0)
    return 0;
  else if(d > 0)
    return (int) (d+.5);
  else
    return (int) (d-.5);
}

//return an angle in the range (-M_PI, M_PI]
double ModTheta(double ang) {
  while(ang <= -M_PI)
    ang += 2*M_PI;
  while(ang > M_PI)
    ang -= 2*M_PI;
  return ang;
}
*/
//updates encoders and if tracking is on, moves head!
/*void integrate() {
  double timeDiff = timeRunning[HIGH_LEVEL] - lastITime;
  lastITime = timeRunning[HIGH_LEVEL];

  //Even if the motor is off, I still call integrate
  //do nothing in this case.
  if(!isOn[MOTOR])
    return;

  //how far it's gone
  double inchesMoved = timeDiff / ((vel > 0) ? 92 : -78);
  inchesMoved *= fabs(vel)/20; //it is calibrated for a speed of 20
  double angDiff = 0;
  if(steerAngle == 0) {
    x+= inchesMoved*cos(theta);
    y+= inchesMoved*sin(theta);
  }else {
    double turnAngleRad = steerAngle * -M_PI/180; //convert to radians and flip
    double radius = WHEEL_OFFSET / tan(turnAngleRad); //loc of rot point
    double rotDist = sqrt(pow(WHEEL_OFFSET, 2)+pow(radius,2));
    double angleDiff = inchesMoved / rotDist; //how far, in radians
    if(radius < 0)
      angleDiff *= -1; // if turning right, theta goes down
    int sgn = (radius < 0) ? -1 : 1;  //swap pos/neg
    double cx = fabs(radius) * cos(theta + M_PI/2 * sgn) + x;  //center of circle robot is moving around
    double cy = fabs(radius) * sin(theta + M_PI/2 * sgn) + y;
    double rTheta = M_PI + theta + angleDiff + M_PI/2 * sgn;  //direction from center where robot is
    //debug("cx: " + cx + " cy: " + cy + " angle: "+(theta + Math.M_PI/2 * sgn)
    //+" rTheta: " + rTheta+" angleDiff: "+angleDiff+"\n",240);
    x = fabs(radius) * cos(rTheta) + cx;
    y = fabs(radius) * sin(rTheta) + cy;
    theta = ModTheta(theta + angleDiff);

    if(trackOn && isOn[MOTOR])
      trackCP += PanAngle2Pos(0) - PanAngle2Pos(round(angleDiff*(180/M_PI)));
    if(trackCP < 10) trackCP = 10;
    if(trackCP > 245) trackCP = 245;
  }
  ix = round(x);
  iy = round(y);
  itheta = round(theta*(180/M_PI)); // convert into degrees
}
*/
