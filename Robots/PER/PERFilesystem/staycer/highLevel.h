#ifndef _HIGH_LEVEL_H_
#define _HIGH_LEVEL_H_

#include <math.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/timeb.h>
#include <unistd.h>

#include "net/reliapack.h"
#include "constants.h"
#include "cereb.h"
#include "rover.h"
#include "webCam.h"

//comment this out to use the real, uncomment for old prototype
//#define EX_USE_NEW

//for the real rover 
#ifndef EX_USE_NEW
#define TURN_CONST 97
//#define TURN_CONST_B (0)
//#define TURN_CONST_B (-197)
#define TURN_CONST_B (-297) //because wheels keep moving after you cut power
#define GOTO_CONST 311
#define SPEED 205
#define WHEELBASE_WIDTH     25.4
#define WHEELBASE_LENGTH    25.0 
#define SS_ANGLE (-35) //static safety tilt angle
#define SS_THRESH 135  //static safety range threshold
#endif

//for the old prototype
#ifdef EX_USE_NEW
//#define TURN_CONST 84 //calculated to be 84.26
#define TURN_CONST 70
#define TURN_CONST_B (-297) //calculated to be -196.8
#define GOTO_CONST 206
#define SPEED 255
#define WHEELBASE_WIDTH     21.5
#define WHEELBASE_LENGTH    28.1
#define SS_ANGLE (-25)
#define SS_THRESH 120
#endif

#define WHEELBASE_WIDTH_D2  (WHEELBASE_WIDTH/2)
#define WHEELBASE_LENGTH_D2 (WHEELBASE_LENGTH/2)

//safety levels
#define NO_SAFETY     0
#define CYCLE_SAFETY  1
#define STATIC_SAFETY 2

//relavent trikebot params, in inches
//#define AXLE_LENGTH 16 //distance between rear wheels
//#define WHEEL_OFFSET 11 //x-dist between wheels
//the dist in x-dir from axle to camera
//#define AXLE_TO_CAM 7
//the height of the camera off the floor while level
//I subtracted 2.5 inches to account for landmark height
//#define CAM_HEIGHT 16
//the distance from the center of rotation of the camera to its lens
//#define CAM_LENGTH 2.5

//extern int ix, iy, itheta; //integrated x, y, theta
extern int highLevelStatus, distTraveled;
extern int goToAngle; //needed so that obstacle avoidance can point in right direction
extern struct safetyPos *currSafety;

bool highLevelRunning();

bool safetyOn();

bool kill(int status);

int doGoTo(Datapack *pack);

int doTurnTo(Datapack *pack);

//void relativeStart(char *data);

//void alignStart(char *data);


//double objPan(double pan, int tx);
//double objTilt(double tilt, int ty);
//void getObjectXY(int camX, int camY, int panDeg, int tiltDeg);

#endif
