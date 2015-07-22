#ifndef _WEBCAM_H_
#define _WEBCAM_H_

#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/types.h> //for fork
#include <sys/wait.h>
#include <sys/timeb.h>
#include <unistd.h>

#include <linux/videodev.h>
#include <sys/mman.h>

#include "net/reliapack.h"
#include "util/vector.h"
#include "util/v4l.h"
#include "rover.h"
#include "cereb.h"
#include "highLevel.h"
#include "track.h"

#define VIDEO_DEV "/dev/video0"
#define WC_NO_CMD 0      //web cam thread doing nothing
#define WC_STOP 1        //web cam thread doing nothing
#define WC_GRAB 2        //grab a new picture
#define WC_PROPERTIES 3  //get camera properties

/* these are commands the user can call that put the rover into streaming mode
   If the user calls stopStreaming() when one of these isn't running, they
   will get a RESOURCE_CONFLICT error. */
#define WC_SIFT 7         //do keypoint analysis
#define WC_CYCLE_PIC 50   //web cam thread cycling through safety positions
#define WC_CYCLE_NOPIC 51 //same as previous, but no pictures taken
#define WC_PIC 52         //just takes constant pictures
#define WC_TRACK 53       //do tracking
#define WC_MEAN 54        //get mean
#define WC_MOTION 55      //get motion
#define WC_USER_STREAM_START WC_CYCLE_PIC

extern int wcThreadStatus;


struct pictureCmd {
  int special; //if not WC_NO_CMD, it ignores rest of struct and does special
  int pan, tilt;
  int width, height;
  bool lightOn;
  bool raw;
  Datapack * pack;
};

void * WebCamThread(void * wc);

class WebCam {
  friend void * WebCamThread(void * wc);
 public:
  WebCam();
  ~WebCam();

  bool grabImage(int width, int height, bool convert2jpg, bool convert2png);
  bool sendWithTimestamp(int width, int height, struct sockaddr remoteAddr);
  bool isInUse();
  bool takeRecentPicture(int width, int height);
  void takePicture(Datapack * pack);
  bool doSpecial(int wcCmd, Datapack * pack);
  char *getJpg();
  int getJpgSize();
  char *getBuf();
  int *getEMean();
  int getEPixels();
  int getTmmX();
  int getTmmY();

  void getProperties(Datapack * pack);
  bool getMean(unsigned char *arr);
  bool getMotionBlocks(int *motion);
  //bool hasRecentPicRequest();

 private:
  void initCamera();
  int fd;
  struct video_mbuf buf;
  struct video_picture pic;

  unsigned char * rgbFrame;
  char *map, *jpgBuf;
  int jpgSize, pngSize;
  pthread_mutex_t jpgMutex;

  //  char * eMeanData;

  struct video_mmap vmap;
  //bool imageWaiting;
  bool threadProcessing;


  //one picture to store normal pictures to be taken
  //goToVector stores requests for pictures during a goTo
  Vector *picVector;
};

extern WebCam * webCam;

#endif // _WEBCAM_H_
