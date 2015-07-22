#include "constants.h"

int servoCenter[NUM_MOTORS];//the center 
int servoRange[NUM_MOTORS]; //the range on one side

double turnAdjust = 1, driveAdjust = 1, driveSpeed = 1;

struct safetyPos *sphead = NULL, *sptail=NULL;

unsigned char servoAng2Pos(int servo, int angle) {
  int pos =  servoCenter[servo] + (int)round(128.*angle/servoRange[servo]);
  //cap is so that is a valid unsigned char
  return (pos > 255)?255:((pos < 1) ? 1 : pos);
}

int servoPos2Ang(int servo, int pos) {
  return (int)round((pos-servoCenter[servo])*servoRange[servo]/128.);
}

int lookupType(char *type) {
  if(!strcmp(type, "servo0"))
    return SERVO0;
  if(!strcmp(type, "servo1"))
    return SERVO1;
  if(!strcmp(type, "servo2"))
    return SERVO2;
  if(!strcmp(type, "servo3"))
    return SERVO3;
  if(!strcmp(type, "pan"))
    return PAN_SERVO;
  if(!strcmp(type, "tilt"))
    return TILT_SERVO;
  if(!strcmp(type, "scan"))
    return SCAN_POS;
  if(!strcmp(type, "turn_adjust"))
    return TURN_ADJ;
  if(!strcmp(type, "drive_adjust"))
    return DRIVE_ADJ;
  if(!strcmp(type, "drive_speed"))
    return DRIVE_SPEED;
  return -1;
}

bool parseScanLine(char *line) {
  const char DELIM [] = " \t\r\n";
  char *type, *attr=NULL, *val;
  if(line == NULL || *line == '#') //skip any line beginning with '#'
    return false;

  type = strsep(&line, DELIM);
  int index = lookupType(type);

  if(index == SCAN_POS) {
    int pan=0, tilt=0, thresh=0;
    bool panSet=false, tiltSet=false, threshSet=false, takePic=false;
    while(line != NULL) {
      if(attr == NULL) {
	attr = strsep(&line, DELIM);

	//if it is not a valid identifier, ignore it
	if(!strcmp(attr, "picture")) {
	  takePic = true;
	  attr = NULL;
	}
	else if(strcmp(attr,"pan") && strcmp(attr,"tilt") && strcmp(attr,"thresh"))
	  attr = NULL;
      }else {
	val = strsep(&line, DELIM);

	if(val != NULL) {
	  int value = atoi(val);
	  if(!strcmp(attr, "pan")) {
	    pan = value;
	    panSet = true;
	  }else if(!strcmp(attr, "tilt")) {
	    tilt = value;
	    tiltSet = true;
	  }else if(!strcmp(attr, "thresh")) {
	    thresh = value;
	    threshSet = true;
	  }
	}
	attr = NULL;
      }
    }
    //if the line contained all the info I need
    if(panSet && tiltSet && threshSet) {
      struct safetyPos * newPos = new struct safetyPos;
      newPos->pan = pan;
      newPos->tilt = tilt;
      newPos->thresh = thresh;
      newPos->takePicture = takePic;
      if(sphead == NULL) //this is the first one
	sphead = sptail = newPos->next = newPos;
      else {
	sptail->next = newPos;
	newPos->next = sphead;
	sptail = newPos;
      }
      return true;
    }
  }
  return false;
}

bool parseLine(char *line) {
  const char DELIM [] = " \t\r\n";
  char *type, *attr=NULL, *val;
  if(line == NULL || *line == '#') //skip any line beginning with '#'
    return false;

  type = strsep(&line, DELIM);
  int index = lookupType(type);

  if(index > 0 && index < NUM_MOTORS) {
    while(line != NULL) {
      if(attr == NULL) {
	attr = strsep(&line, DELIM);
	//if it is not a valid identifier, ignore it
	if(strcmp(attr, "center") && strcmp(attr, "range"))
	  attr = NULL;
      }else {
	val = strsep(&line, DELIM);
	if(val != NULL) {
	  int value = atoi(val);
	  if(!strcmp(attr, "center"))
	    servoCenter[index] = value;
	  else if(!strcmp(attr, "range"))
	    servoRange[index] = value;
	  //  printf("setting %s to %d\n", attr, value);
	}
	attr = NULL;
      }
    }
    return true;
  }else if(index == TURN_ADJ) {
    val = strsep(&line, DELIM);
    if(val != NULL) {
      double adj = atof(val);
      turnAdjust = adj / 100;
    }
  }else if(index == DRIVE_ADJ) {
    val = strsep(&line, DELIM);
    if(val != NULL) {
      double adj = atof(val);
      driveAdjust = adj / 100;
    }
  }else if(index == DRIVE_SPEED) {
    val = strsep(&line, DELIM);
    if(val != NULL) {
      double adj = atof(val);
      if(adj > 0)
	driveSpeed = adj / 100;
      // printf("setting driveSpeed to %f\n", driveSpeed);
    }
  }
  return false;
}

int sendFile(char * filename, Datapack* pack){
  struct stat calStat;
  int rets = stat(filename, &calStat);
  if(rets) { //failure
    printf("No calibration file found!\n");
    char rv = SUCCESS;
    return rgram->sendResponse(&rv, 1, pack);
  }
  int size = (int)calStat.st_size;
  char * calData = new char[size+1];
  *calData = 0;
  int calFd = open(filename, O_RDONLY);
  if(calFd == -1){ //failure
    printf("Can't open calibration file!\n");
    char rv = IO_ERROR;
    delete calData;
    return rgram->sendResponse(&rv, 1, pack);
  }
  int readIn = 0, justRead;
  do {
    justRead = read(calFd, calData+readIn+1, size-readIn);
    readIn += justRead;
  }while(justRead > 0 && readIn < size);
  if(readIn < size) { //failure
    printf("Error reading calibration file!\n");
    char rv = IO_ERROR;
    delete calData;
    close(calFd);
    return rgram->sendResponse(&rv, 1, pack);
  }
  int retVal = rgram->sendResponse(calData, size+1, pack);
  //printf("%d %d\n", rets, (int)calStat.st_size);
  
  delete calData;
  close(calFd);
  return retVal;
}

int sendCal(Datapack * pack) {
  return sendFile(CAL_FILE, pack);
}

int sendScan(Datapack * pack) {
  return sendFile(SCAN_FILE, pack);
}

int writeFile(char *filename, char *buf, int len) {
  int calFd = open(filename, O_WRONLY | O_CREAT | O_TRUNC);
  if(calFd == -1)
    return IO_ERROR;
  int written=0, justWritten;
  do {
    justWritten = write(calFd, buf+written, len-written);
    written += justWritten;
  }while(justWritten > 0);
  close(calFd);
  return (written == len) ? SUCCESS : IO_ERROR;
}

int setCal(char *buf, int len) {
  int retVal = writeFile(CAL_FILE, buf, len);
  loadCalibration();
  return retVal;
}

int setScan(char *buf, int len) {
  int retVal = writeFile(SCAN_FILE, buf, len);
  //delete the old scan positions
  if(retVal == SUCCESS){
    struct safetyPos * tmp;
    while(sphead != NULL && sphead != sptail) {
      tmp = sphead->next;
      delete sphead;
      sphead = tmp;
    }
    if(sphead)
      delete sphead;
    sphead = NULL;
    sptail = NULL;
  }
  loadScans();
  return retVal;
}

void loadCalibration() {
  //printf("about to open cal file\n");
  FILE * cali = fopen(CAL_FILE, "r");
  //printf("called open, cali: %x\n", cali);
  char buf[128];

  //set up the servos with some default params in case loading fails
  for(int i=SERVO_START; i<NUM_MOTORS; i++) {
    servoCenter[i] = 128;
    servoRange[i] = 100;
  }
  servoRange[PAN_SERVO] = 210;
  
  if(cali != NULL) {
    //printf("cali non null\n");
    while(fgets(buf, 128, cali) != NULL) {
      parseLine(buf);
    }
    fclose(cali);
  }
}

void loadScans() {
  FILE * cali = fopen(SCAN_FILE, "r");
  char buf[128];
  
  if(cali != NULL) {
    while(fgets(buf, 128, cali) != NULL) {
      parseScanLine(buf);
    }
    fclose(cali);
  }
  /*struct safetyPos * tmp = sphead;
  while(tmp) {
    printf("%d %d %d\n", tmp->pan, tmp->tilt, tmp->thresh);
    tmp = tmp->next;
    if(tmp == sphead)
      break;
      }*/

}

struct safetyPos * getSafetyPos() {
  return sphead;
}
