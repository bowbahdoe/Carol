#ifndef _TRACK_H_
#define _TRACK_H_

#include <stdlib.h>
#include <stdio.h>

#include "cereb.h"

#define TRACKWIDTH 176
#define TRACKHEIGHT 144

//the size of the array I use for motion.  I detect motion in 16x16 blocks
//so this should be TRACKWIDTH*TRACKHEIGHT/(16*16)
#define MOTION_SIZE 99

#ifndef min 
#define min(a,b) ((a) < (b) ? (a) : (b))
#endif

#ifndef max
#define max(a,b) ((a) > (b) ? (a) : (b))
#endif

extern unsigned char minY, maxY, minU, maxU, minV, maxV;
extern bool trackMovePan, trackMoveTilt;

struct trackData {
  int minX, minY, maxX, maxY;
  int totX, totY, pixels;
};

void yuv420p2thresh(char *p_yuv);
struct trackData bestBlob(char *p_yuv);

//a class I created to give me O(1) access for push and pop
class IntQueue {
 public:
  IntQueue(int initialSize);
  ~IntQueue();

  int push(int num);
  int pop(int *num);
  int size();
  //bool isEmpty();
  //int capacity();
  
 private:
  int arrSize;
  int head;
  int tail;
  int elements;
  int *arr;
};

#endif // _TRACK_H_
