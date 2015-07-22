#ifndef _ROVER_H
#define _ROVER_H

#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include <netinet/in.h>

#include "cereb.h"
#include "constants.h"
#include "highLevel.h"
#include "serial.h"
#include "net/reliagram.h"
#include "webCam.h"

//the threshold to decide whether or not to keep going
#define LOW_BATTERY_THRESHOLD 122

//return values
#define SUCCESS                 0
#define HL_CONTINUE             5

#define INVALID_LENGTH          30
#define UNKNOWN_TYPE		31
#define BAD_INPUT               32

#define CEREB_TIMEOUT		50
#define CAMERA_TIMEOUT		51
#define IO_ERROR		60
#define BATTERY_LOW		70

#define KILLED			100
#define OBSTACLE_DETECTED       103

#define RESOURCE_CONFLICT       120

//the general debug level
#define DEBUG_LEVEL (9999)

//extern Sift *sift;
extern bool exiting;
extern Reliagram *rgram;
extern bool legsLocked, panLocked, tiltLocked;
extern pthread_mutex_t resourceLockMutex;

//void takePicture(Datapack * pack);
int roverHeadMove(Datapack *pack);
int RoverSetAll(Datapack *pack);
int RoverSpin(Datapack *pack);
int RoverCrab(Datapack *pack);
int RoverTurn(Datapack *pack);
int reply(int status, Datapack *pack);
void parsePacket(Datapack *pack);

void sigIntHandler(int sig);

void debug(int level, char * msg);

void parseArgs(int argc, char **argv);
void runDutyTest();
void runDutyTest2();
void runDutyTest3();
void runDiagnostic();
void runWallFollow();
void runSmartWander();
void runLineFollow();
void runTrackObject();

int * OGetMean(bool stream);
void OGoTo(int dist, int angle, int safetyLevel, bool takePics);
void OTurnTo(int degrees, bool takePics);
void OTrack(int minY, int maxY, int minU, int maxU, int minV, int maxV, int trackMethod, bool movePan, bool moveTilt, int driveMethod);
void OStopStreaming();
int * OScan(int tilt, int minPan, int maxPan, int step);

int restoreP2P();
int restoreCMU();

#endif
