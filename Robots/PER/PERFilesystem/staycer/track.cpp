#include "track.h"

unsigned char trackArr[TRACKHEIGHT][TRACKWIDTH];

unsigned char minY, maxY, minU, maxU, minV, maxV;
bool trackMovePan, trackMoveTilt;
IntQueue * intQueue = NULL;

/*
 * convert a YUV420P to an image where a 1 indicates it is the color to be
 * tracked and a 0 for all other pixels.
 */
void yuv420p2thresh(char *p_yuv) {
  const int numpix = TRACKWIDTH * TRACKHEIGHT;
  int h, w, y00, y01, y10, y11, u, v;
  unsigned char *pY = (unsigned char *) p_yuv;
  unsigned char *pY1 = pY + TRACKWIDTH;
  unsigned char *pU = pY + numpix;
  unsigned char *pV = pU + numpix / 4;
  unsigned char *pOut = (unsigned char *) trackArr;
  unsigned char *pOut1 = pOut + TRACKWIDTH;

  for (h = 0; h <= TRACKHEIGHT - 2; h += 2) {
    for (w = 0; w <= TRACKWIDTH - 2; w += 2) {
      y00 = *(pY++);
      y01 = *(pY++);
      y10 = *(pY1++);
      y11 = *(pY1++);
      u = (*pU++);
      v = (*pV++);
      
      if(u > minU && u < maxU && v > minV && v < maxV) {
	*(pOut++) = y00 > minY && y00 < maxY;
	*(pOut++) = y01 > minY && y01 < maxY;
	*(pOut1++) = y10 > minY && y10 < maxY;
	*(pOut1++) = y11 > minY && y11 < maxY;
      }else {
	*(pOut++) = 0;
	*(pOut++) = 0;
	*(pOut1++) = 0;
	*(pOut1++) = 0;
      }

    }
    pY += TRACKWIDTH;
    pY1 += TRACKWIDTH;
    pOut += TRACKWIDTH;
    pOut1 += TRACKWIDTH;
  }
}

struct trackData expand(int x, int y, unsigned char filler) {
  struct trackData currTrack;
  int i, ie, j, je, js;
  currTrack.minX = currTrack.maxX = currTrack.totX = x;
  currTrack.minY = currTrack.maxY = currTrack.totY = y;
  currTrack.pixels = 1;
  trackArr[y][x] = filler;
  intQueue->push(x+y*TRACKWIDTH);
  //printf("pushed %d\n", x+y*TRACKWIDTH);

  while(intQueue->size()) {
    intQueue->pop(&y);
    //printf("popped %d\n", y);
    x = y % TRACKWIDTH;
    y /= TRACKWIDTH;
    i = max(y-1, 0);
    ie = min(y+1, TRACKHEIGHT-1);
    js = max(x-1, 0);
    je = min(x+1, TRACKWIDTH-1);
    //printf("%d %d %d %d %d\n", i, ie
    for(; i<=ie; i++)
      for(j=js; j<=je; j++)
	if(trackArr[i][j] == 1) {
	  trackArr[i][j] = filler;
	  intQueue->push(j+i*TRACKWIDTH);
	  currTrack.minX = min(j, currTrack.minX);
	  currTrack.maxX = max(j, currTrack.maxX);
	  currTrack.totX += j;
	  currTrack.minY = min(i, currTrack.minY);
	  currTrack.maxY = max(i, currTrack.maxY);
	  currTrack.totY += i;
	  currTrack.pixels++;
	}
  }
  return currTrack;
}

struct trackData bestBlob(char *p_yuv) {
  int h, w;
  if(intQueue == NULL)
    intQueue = new IntQueue(5000);
  yuv420p2thresh(p_yuv);
  struct trackData bestTrack;
  bestTrack.minX = bestTrack.maxX = bestTrack.minY = bestTrack.maxY = 0;
  bestTrack.totX = bestTrack.totY = bestTrack.pixels = 0;

  unsigned char whichTrack = 2;
  //    printf("trackwidth %d, trackheight %d\n", TRACKWIDTH, TRACKHEIGHT);
  for (h = 0; h < TRACKHEIGHT; h ++) {
    for (w = 0; w < TRACKWIDTH; w++) {
      if(trackArr[h][w] == 1) {
	//printf("found a pixel to expand on\n");
	struct trackData currTrack = expand(w, h, whichTrack++);
	if(currTrack.pixels > bestTrack.pixels) {
	  bestTrack = currTrack;
	  //	  if(bestTrack.pixels > 1){
	    //printf("better with %d pixels\n", bestTrack.pixels);
	}
	if(whichTrack == 255)
	  goto end;
      }
    }
  }
 end:

  if(bestTrack.pixels > 5) {
    int mmx = bestTrack.totX / bestTrack.pixels;
    int mmy = bestTrack.totY / bestTrack.pixels;
    if(trackMovePan && abs(mmx-TRACKWIDTH/2) > TRACKWIDTH/6) {
      int diff = mmx-TRACKWIDTH/2;
      diff += (diff > 0) ? (-TRACKWIDTH/8) : (TRACKWIDTH/8);
      motorPos[PAN_SERVO] -= 16*(diff) / TRACKWIDTH;
      motorPos[PAN_SERVO] = min(motorPos[PAN_SERVO], 180);
      motorPos[PAN_SERVO] = max(motorPos[PAN_SERVO], -180);
    }
    if(trackMoveTilt && abs(mmy-TRACKHEIGHT/2) > TRACKHEIGHT/6) {
      int diff = mmy-TRACKHEIGHT/2;
      diff += (diff > 0) ? (-TRACKHEIGHT/8) : (TRACKHEIGHT/8);
      motorPos[TILT_SERVO] -= 16*(diff) / TRACKHEIGHT;
      motorPos[TILT_SERVO] = min(motorPos[TILT_SERVO], 90);
      motorPos[TILT_SERVO] = max(motorPos[TILT_SERVO], -50);
    }
    headMove(true, motorPos[PAN_SERVO], true, motorPos[TILT_SERVO]);
  }

  return bestTrack;
}

IntQueue::IntQueue(int initialSize) {
  arrSize = initialSize;
  head = tail = elements = 0;
  arr = new int [arrSize];
}

IntQueue::~IntQueue() {
  delete arr;
}

int IntQueue::push(int num) {
  if(elements == arrSize) {
    int *newArr = new int[2*arrSize];
    for(int i=0; i<arrSize; i++)
      newArr[i] = arr[(i+head)%arrSize];
    delete arr;
    arr = newArr;
    arrSize = 2*arrSize;
    head = tail = 0;
  }
  arr[tail] = num;
  tail = (tail+1) % arrSize;
  elements++;
  return 0;
}

int IntQueue::pop(int *num) {
  if(elements) {
    *num = arr[head];
    head = (head+1) % arrSize;
    elements--;
    return 0;
  }else
    return -1;
}

int IntQueue::size() {
  return elements;
}
