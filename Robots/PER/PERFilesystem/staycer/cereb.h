#ifndef _CEREB_H_
#define _CEREB_H_
#include <math.h>
#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/timeb.h>
#include <unistd.h>

#include "serial.h"
#include "constants.h"
#include "highLevel.h"
#include "rover.h"
#include "track.h" //for min and max
#include "util/monitor.h"
#include "net/reliagram.h"

//indecies of the motors in the isOn and timeout arrays	
#define MOTOR0      0
#define MOTOR1      1
#define SERVO0      2	
#define SERVO1      3
#define SERVO2      4
#define SERVO3      5
#define PAN_SERVO   6
#define TILT_SERVO  7
#define NUM_MOTORS  8           //the total number of motors
#define SERVO_START 2   //where the servos start in the array
#define SCAN_POS    100 //used while reading in config file
#define TURN_ADJ    101 //used while reading in config file
#define DRIVE_ADJ   102 //used while reading in config file
#define DRIVE_SPEED 103 //used while reading in config file

//the masks that I use to send to the cerebellum
//for convience, they are equal to (1 << (motorPos index))
#define MOTOR0_MASK 1
#define MOTOR1_MASK 2
#define SERVO0_MASK 4
#define SERVO1_MASK 8
#define SERVO2_MASK 16
#define SERVO3_MASK 32
#define PAN_MASK    64
#define TILT_MASK   128

#define MOTORS_MASK 3
#define SERVOS_MASK 60
#define DRIVE_MASK  63
#define LOOK_MASK   192
#define ALL_MASK    255

#define MIN_PAN     (-180)
#define MAX_PAN     180
#define MIN_TILT    (-50)
#define MAX_TILT    90
#define MIN_STEER   (-90)
#define MAX_STEER   90

//how many ms it takes a servo to move one degree
//#define MS_PER_DEGREE 5

//variables for what the cereb thread is doing right now
#define CEREB_IDLE     0 //no requests have been received
#define CEREB_WAIT     1 //a request has been received, but it hasn't started
#define CEREB_SCAN     2 //doing a scan
#define CEREB_TURNTO   3 //doing a turn to
#define CEREB_DRIVETO  4 //doing a drive to

#define WALL_FOLLOW_SCAN_LENGTH 10

extern int cerebThreadState;

extern char range, cerebResponse;
extern int motorPos[]; //the positions of the motors
extern bool isOn[NUM_MOTORS], UVon;
extern int timeouts[]; //how long for the motor to time out, in ms
extern int consecutiveCerebFailures;

extern serialPort *cerebSerial; //serial port for the cerebellum

void initCerebellum();

int setAll(int mask, int speed0, int speed1, int servo0, int servo1, int servo2, int servo3, int pan, int tilt, bool doTimeout);
int initRobot();
int killRobot();
int headMove(bool movePan, int panAngle, bool moveTilt, int tiltAngle);
//int drive(unsigned char turn, int speed);

int spin(int speed);
int crab(int speed, int angle);
int quadTurn(int speed, double radius);

int addCerebRequest(Datapack *pack);
int initCerebAction(Datapack *pack, bool needLegs, bool needPan, bool needTilt, int actionID); 
void endCerebAction(Datapack *pack, bool needLegs, bool needPan, bool needTilt);

int timeToArrive(int mask);

void saveScan(char[9]);
int * cerebOScan(Datapack *pack);

void resetTimeout(int id);
void resetAndAdd(struct timeb *tp, int addTime);
bool timedOut(int motorId);
bool timeoutb(struct timeb tp);
void copyTimeb(struct timeb *dest, struct timeb src);
int timebDiff(struct timeb time1, struct timeb time2);

void * cerebThread(void *null); //constantly communicates with cerebellum

//functions that buffer the voltage from the cereb
#define NORMAL_VOLTAGE 158
class VoltageMonitor {
 public:
  VoltageMonitor(int readingsBuffered, int normalVoltage);
  ~VoltageMonitor();
  void addReading(int voltage);
  int getVoltage();

 private:
  int size; //the size of the array
  int *voltages; //an array holding the last [size] voltages
  int place; //the place to put the next item in the array
  int sum; //the current sum of the array
  int bufferedVoltage; //the average of the values in the array
  struct timeb lastUpdated;
};

extern VoltageMonitor *voltageBuffer;

#endif
