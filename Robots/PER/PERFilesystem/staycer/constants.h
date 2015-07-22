#ifndef _CONSTANTS_H_
#define _CONSTANTS_H_

#define CAL_FILE "/root/rover.cal"
#define SCAN_FILE "/root/rover.scan"

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <netinet/in.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>

#include "cereb.h"
#include "rover.h"
#include "net/reliagram.h"

extern double turnAdjust, driveAdjust, driveSpeed;

struct safetyPos {
  int pan, tilt, thresh;
  bool takePicture;
  struct safetyPos *next;
};

unsigned char servoAng2Pos(int servo, int angle);
int servoPos2Ang(int servo, int pos);

int sendCal(Datapack * pack);
int setCal(char *buf, int len);
int sendScan(Datapack * pack);
int setScan(char *buf, int len);
void loadCalibration();
void loadScans();

//returns a pointer to a singly linked list that wraps around
struct safetyPos * getSafetyPos();

#endif
