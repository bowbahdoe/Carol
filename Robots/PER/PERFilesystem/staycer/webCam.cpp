#include "webCam.h"

int wcThreadStatus = WC_NO_CMD;
  
void grabPic(struct pictureCmd * cmd);
void doMean(Datapack *pack, Vector *picVector);
void saveMean(char meanData[9]);
void doMotion(Datapack *pack, Vector *picVector);
void doHLCycle(Datapack * pack, Vector *picVector, int special);
void doTakePics(Datapack * pack, Vector *picVector);
void doTrack(Datapack * pack, Vector *picVector);

unsigned int tminX, tminY, tmaxX, tmaxY, tmmX, tmmY, tpixels;

int eMeanData [9];
int ePixels;

void addTimestamp(char *buf);

void * WebCamThread(void * wc) {
  
  WebCam * webCam = (WebCam *) wc;
  struct pictureCmd * cmd;
  //  publicMeanData = new char [9];
  webCam->grabImage(176, 144, false, false); //first picture is really slow
  
  pthread_mutex_lock(&webCam->picVector->mutex);
  if(webCam->picVector->isEmpty())
    webCam->picVector->monitor->wait(&webCam->picVector->mutex);
  while(!exiting) {
    
    if(!webCam->picVector->isEmpty()) {
      webCam->threadProcessing = true; 
      cmd = (struct pictureCmd *) webCam->picVector->remove(0);
      if(cmd == NULL) {
	debug(50, "WebCamThread Vector error, NULL\n");
	continue;
      }
      pthread_mutex_unlock(&webCam->picVector->mutex);

      if(cmd->special != WC_NO_CMD) {
	wcThreadStatus = cmd->special;
	switch(cmd->special) {
	case WC_STOP:
	  wcThreadStatus = WC_NO_CMD; reply(SUCCESS, cmd->pack); break;
	case WC_CYCLE_PIC:
	case WC_CYCLE_NOPIC:
	  doHLCycle(cmd->pack, webCam->picVector, cmd->special); break;
	case WC_PIC:
	  doTakePics(cmd->pack, webCam->picVector); break;
	case WC_TRACK:
	  doTrack(cmd->pack, webCam->picVector); break;
	case WC_GRAB:
	  grabPic(cmd); break;
	case WC_PROPERTIES:
	  webCam->getProperties(cmd->pack); break;
	case WC_MEAN:
	  doMean(cmd->pack, webCam->picVector); break;
	case WC_MOTION:
	  doMotion(cmd->pack, webCam->picVector); break;
	default:
	  wcThreadStatus = WC_NO_CMD;
	}
	//cmd->pack = NULL; //don't delete it!
	goto WCTEnd;
      }
    WCTEnd:
      if(cmd->pack != NULL)
	delete cmd->pack;
      delete cmd;
      pthread_mutex_lock(&webCam->picVector->mutex);
    }

    if(webCam->picVector->isEmpty()) {
      webCam->threadProcessing = false;
      wcThreadStatus = WC_NO_CMD;
      webCam->picVector->monitor->wait(&webCam->picVector->mutex);
    }
  }
  pthread_mutex_unlock(&webCam->picVector->mutex);
  return NULL;
}

bool WebCam::getMotionBlocks(int *motion) {
  if(grabImage(TRACKWIDTH, TRACKHEIGHT, false, false)) {
    unsigned char *pix = (unsigned char *) map;
    int motionIndex, i, j, pi=0; //pi = pix index

    //clear the array
    bzero(motion, MOTION_SIZE*sizeof(int));

    for(i=0; i<TRACKHEIGHT; i++) {
      motionIndex = (i >> 4)*(TRACKWIDTH >> 4);
      for(j=0; j<TRACKWIDTH; j++) {
	motion[motionIndex] += pix[pi++];
	motionIndex += !(pi & 0xf);  //increment if at end of block
      }
    }
    return true;
  }
  return false;
}

void doMotion(Datapack *pack, Vector *picVector) {
  struct sockaddr remoteAddr = *pack->getAddress();
  int motionData [2][MOTION_SIZE];
  unsigned char response[MOTION_SIZE + 6];
  unsigned int imagesReceived = 1, i;
  
  //try to get first block
  if(webCam->getMotionBlocks(*motionData))
    reply(SUCCESS, pack);
  else {
    reply(CAMERA_TIMEOUT, pack);
    return;
  }


  pthread_mutex_lock(&resourceLockMutex);
  //I need both to be unlocked for this to work
  if(panLocked || tiltLocked) {
    reply(RESOURCE_CONFLICT, pack);
    pthread_mutex_unlock(&resourceLockMutex);
    return;
  }
  panLocked = tiltLocked = true;
  pthread_mutex_unlock(&resourceLockMutex);

  while(picVector->isEmpty()) {
    //switch between which buffer you store the data in
    if(webCam->getMotionBlocks(motionData[imagesReceived++ % 2])) {
      for(i=0; i<MOTION_SIZE; i++) {
	response[6+i] = (unsigned char)(abs(motionData[0][i]-motionData[1][i]) >> 8); //get absolute value of difference, then divide by 256 (window size)
	/*printf("%7d", motionData[imagesReceived % 2][i]);
	if((i%11) == 10)
	  printf("\n");*/
      }
      //printf("\n");
      addTimestamp((char *) response);
      rgram->sendToAddr((char *)response, 6+MOTION_SIZE, remoteAddr);
    }else 
      break;
  }

  pthread_mutex_lock(&resourceLockMutex);
  panLocked = tiltLocked = false; //unlock them both
  pthread_mutex_unlock(&resourceLockMutex);
}

bool WebCam::getMean(unsigned char *arr) {
  if(grabImage(TRACKWIDTH, TRACKHEIGHT, false, false)) {
    int numpix = TRACKWIDTH * TRACKHEIGHT;
    unsigned char *vi = (unsigned char *) map;
    unsigned char *vstop = (unsigned char *) map + numpix;
    unsigned int count = 0;
    //count the y
    for( ; vi < vstop; vi++)
      count += *vi;
    arr[6] = (unsigned char) (count / numpix);
    //count the u
    vstop += numpix / 4;
    for( ; vi < vstop; vi++)
      count += *vi;
    arr[7] = (unsigned char) (count / (numpix/4));
    //count the v
    vstop += numpix / 4;
    for( ; vi < vstop; vi++)
      count += *vi;
    arr[8] = (unsigned char) (count / (numpix/4));
    addTimestamp((char *) arr);
    return true;
  }
  return false;
}

void doMean(Datapack *pack, Vector *picVector) {
  struct sockaddr remoteAddr = *pack->getAddress();
  char * data = pack->getData();
  bool stream = data[1];
  char meanData[9];

  //  printf("doing mean, datapack address is %d\n", pack->getAddress()->sa_family);

  if(webCam->getMean((unsigned char *) meanData)) {
    saveMean(meanData);
    //send mean data
    rgram->sendToAddr(meanData, 9, remoteAddr);
    reply(SUCCESS, pack);
  }else {
    printf("didn't get mean, replying timeout\n");
    reply(CAMERA_TIMEOUT, pack);
    return;
  }
  if(!stream)
    return;
  
  while(picVector->isEmpty()) {
    if(webCam->getMean((unsigned char *) meanData)) {
      printf("picvector is empty, sending data\n");
      rgram->sendToAddr(meanData, 9, remoteAddr);
    }else 
      return;
  }
}

void saveMean(char meanData[9]){
  for(int i = 0; i < 9; i++)
    eMeanData[i] = meanData[i];
  printf("Mean data: %d %d %d\n", meanData[6], meanData[7], meanData[8]);
}

void WebCam::getProperties(Datapack * pack) {
  if (ioctl (webCam->fd, VIDIOCGPICT, &pic) < 0)
  {
    fprintf (stderr, " *** Not able to get properties\n");
    reply(CAMERA_TIMEOUT, pack);
    return;
  }

  uint32_t reply [7];
  reply[0] = htonl((uint32_t) pic.brightness);
  reply[1] = htonl((uint32_t) pic.hue);
  reply[2] = htonl((uint32_t) pic.colour);
  reply[3] = htonl((uint32_t) pic.contrast);
  reply[4] = htonl((uint32_t) pic.whiteness);
  reply[5] = htonl((uint32_t) pic.depth);
  reply[6] = htonl((uint32_t) pic.palette);
  
  rgram->sendResponse((char *)reply, 7*sizeof(uint32_t), pack);
}

void grabPic(struct pictureCmd * cmd) {
  bool origUVon = UVon;

  pthread_mutex_lock(&resourceLockMutex);
  //I need both to be unlocked for this to work
  if(panLocked || tiltLocked) {
    reply(RESOURCE_CONFLICT, cmd->pack);
    pthread_mutex_unlock(&resourceLockMutex);
    return;
  }
  panLocked = tiltLocked = true;
  pthread_mutex_unlock(&resourceLockMutex);

  UVon = cmd->lightOn;
  int tta = 0; 
  //if head is moving wait a minimum time
  if(cmd->pan != motorPos[PAN_SERVO])
    tta = 250;
  else if(cmd->tilt != motorPos[TILT_SERVO])
    tta = 150;
  headMove(true, cmd->pan, true, cmd->tilt);
  tta = max(timeToArrive(LOOK_MASK), tta);
  //printf("moving to %d, %d.  tta=%d\n", cmd->pan, cmd->tilt, tta);
  if(tta < 500)
    tta += 150;
  if(cmd->lightOn && !origUVon && tta < 1500)
    tta = 1500;
  //printf("req %d %d %d\n", cmd->pan, cmd->tilt, tta);
  if(consecutiveCerebFailures > 2) {
    reply(CEREB_TIMEOUT, cmd->pack);
    //printf("error moving head\n");
    goto WCTPicEnd;
  }
  usleep(tta*1000); //get head to position
  //printf("starting to grab image, waited %dms\n", tta);
  if(!webCam->grabImage(cmd->width, cmd->height, !cmd->raw, true)) {
    reply(CAMERA_TIMEOUT, cmd->pack);
    //printf("error grabbing image\n");
    goto WCTPicEnd;
  }
  if(cmd->raw) 
    rgram->sendResponse(webCam->getBuf(), cmd->width*cmd->height*3/2, cmd->pack);
  else
    rgram->sendResponse(webCam->getJpg(), webCam->getJpgSize(), cmd->pack);
 WCTPicEnd:
  UVon = origUVon;

  pthread_mutex_lock(&resourceLockMutex);
  panLocked = tiltLocked = false; //unlock them both
  pthread_mutex_unlock(&resourceLockMutex);
}

void doHLCycle(Datapack * pack, Vector *picVector, int special) {
  struct sockaddr remoteAddr = *pack->getAddress();
 
  while(picVector->isEmpty()) {
    if(currSafety == NULL) {
       return;
    }
    if(range > currSafety->thresh) {
      //refresh the state twice
      int resp1 = headMove(true, currSafety->pan+goToAngle, true, currSafety->tilt);
      int range2 = range;
      int resp2 = headMove(true, currSafety->pan+goToAngle, true, currSafety->tilt);
      int range3 = range;
      if(resp1 != SUCCESS || resp2 != SUCCESS)
	kill(CEREB_TIMEOUT);
      else if(range2 > currSafety->thresh || range3 > currSafety->thresh)
	kill(OBSTACLE_DETECTED);
    }
    //move to the next position
    if(currSafety->takePicture && special == WC_CYCLE_PIC) {
      usleep(250000);
      webCam->sendWithTimestamp(320, 240, remoteAddr);
    }
    currSafety = currSafety->next;
    if(headMove(true, currSafety->pan+goToAngle, true, currSafety->tilt) != SUCCESS)
      kill(CEREB_TIMEOUT);
    usleep(timeToArrive(LOOK_MASK) * 1000);
  }
}

void doTakePics(Datapack * pack, Vector *picVector){
  struct sockaddr remoteAddr = *pack->getAddress();
  /*struct timeb now;
  ftime(&now);
  printf("starting takePics %d %d\n", (int)now.time, (int)now.millitm);*/
  while(picVector->isEmpty()) {
      webCam->sendWithTimestamp(320, 240, remoteAddr);
  }
  //ftime(&now);
  //printf("stopping takePics %d %d\n", (int)now.time, (int)now.millitm);
}

//add a timestamp to the beginning of the array
//because I use UDP, streaming could cause images to arrive out of order
void addTimestamp(char *buf) {
    struct timeb now;
    ftime(&now);
    uint32_t netTime = htonl((uint32_t) now.time);
    uint16_t netMillitm = htons((uint16_t) now.millitm);
    memcpy(buf, &netTime, 4);
    memcpy(buf+4, &netMillitm, 2);
}

void doTrack(Datapack * pack, Vector *picVector) {

  struct sockaddr remoteAddr = *pack->getAddress();
  unsigned char *data = (unsigned char *) pack->getData();
  trackMovePan = (data[8] & 2) == 2;
  trackMoveTilt= (data[8] & 1) == 1;
  minY = data[1];
  maxY = data[2];
  minU = data[3];
  maxU = data[4];
  minV = data[5];
  maxV = data[6];
  //delete pack; - deleted elsewhere

  pthread_mutex_lock(&resourceLockMutex);
  if((panLocked && trackMovePan) || (tiltLocked && trackMoveTilt)) {
    reply(RESOURCE_CONFLICT, pack);
    pthread_mutex_unlock(&resourceLockMutex);
    return;
  }
  panLocked |= trackMovePan;
  tiltLocked |= trackMoveTilt;
  pthread_mutex_unlock(&resourceLockMutex);

  reply(SUCCESS, pack);

  char trackPacket[24];
  while(picVector->isEmpty()) {
    webCam->grabImage(TRACKWIDTH, TRACKHEIGHT, false, false);
    struct trackData bestTrack = bestBlob(webCam->getBuf());
    tminX = bestTrack.minX;
    tmaxX = bestTrack.maxX;
    tminY = bestTrack.minY;
    tmaxY = bestTrack.maxY;
    if(bestTrack.pixels) {
      tmmX = bestTrack.totX / bestTrack.pixels;
      tmmY = bestTrack.totY / bestTrack.pixels;
    }else
      tmmX = tmmY = 0;
    tpixels = bestTrack.pixels;

    ePixels = tpixels;

    addTimestamp(trackPacket);
    trackPacket[6] = tmmX;
    trackPacket[7] = tmmY;
    trackPacket[8] = tminX;
    trackPacket[9] = tminY;
    trackPacket[10] = tmaxX;
    trackPacket[11] = tmaxY;
    //    printf("trackPacket: %d %d %d %d %d %d\n", webCam->tmmX, webCam->tmmY, webCam->tminX, webCam->tminY, webCam->tmaxX, webCam->tmaxY);
    *(unsigned int *)(trackPacket + 12) = htonl(tpixels);
    *(unsigned int *)(trackPacket + 16) = htonl(motorPos[PAN_SERVO]);
    *(unsigned int *)(trackPacket + 20) = htonl(motorPos[TILT_SERVO]);
    rgram->sendToAddr(trackPacket, 24, remoteAddr);
    //sleep(1);
  }
  debug(450, "stopping tracking\n");

  pthread_mutex_lock(&resourceLockMutex);
  panLocked &= !trackMovePan;
  tiltLocked &= !trackMoveTilt; 
  pthread_mutex_unlock(&resourceLockMutex);
}

WebCam::WebCam() {
  //fd = -1;
  //printf("before open\n");
  fd = open (VIDEO_DEV, O_RDONLY);
  if(fd < 0) {
    initCamera();
    fd = open (VIDEO_DEV, O_RDONLY);
    //break;
  }
  //printf("after open\n");
  if (fd < 0)
  {
    fprintf (stderr, "*** Open error\n");
    perror(VIDEO_DEV);
    exit(1);
  }

  // get video buffer
  if (ioctl (fd, VIDIOCGMBUF, &buf) < 0)
  {
    fprintf (stderr, "*** Not able to get mbuf\n");
    exit(1);
  }
  //printf("size: %d, frames: %d, offset0: %d, offset1: %d\n", buf.size, buf.frames, buf.offsets[0], buf.offsets[1]);
  // get video properties
  if (ioctl (fd, VIDIOCGPICT, &pic) < 0)
  {
    fprintf (stderr, " *** Not able to get properties\n");
    exit(1);
  }
  
  rgbFrame = new unsigned char [640*480*3];
  map = (char *) mmap (0, buf.size, PROT_READ,MAP_SHARED, fd, 0);
  if ((unsigned char *)-1 == (unsigned char *)map) {
    printf("mmap failed!\n");
    exit(-1);
  }

  jpgBuf = new char [640*480*3 + 1024];
  if(pthread_mutex_init(&jpgMutex, NULL))
    printf("error initializing jpg mutex!\n");
  
  picVector = new Vector();

  pthread_t wcamPThread;
  if(pthread_create(&wcamPThread, 0, WebCamThread, this)) 
    printf("Can't start the web cam thread!\n");
}

WebCam::~WebCam() {
  delete rgbFrame;
  delete jpgBuf;
  delete picVector;
  munmap(map, buf.size);
  printf("closing camera port status: %d\n", close(fd));
  pthread_mutex_destroy(&jpgMutex);
}

bool WebCam::grabImage(int width, int height, bool convert2jpg, bool convert2png) {
  int vzeros, tries = 0;

  do {
    // set mmap attributes
    vmap.format = pic.palette;
    vmap.frame  = 0;
    vmap.width  = width;
    vmap.height = height;

    // grab frame 
    if (ioctl (fd, VIDIOCMCAPTURE, &vmap) < 0) {
      debug(100, " *** Not able to capture frame\n");
      return false;
    }
 
    // sync frame
    if (ioctl (fd, VIDIOCSYNC, &vmap.frame) < 0) {
      debug(100, " *** Not able to sync frame\n");
      return false;
    }

    //corruption detection code
    int numpix = width*height;
    char *vi = map + numpix*5/4;
    char *vstop  = vi + numpix/4;
    vzeros = 0;
    do{
      if(*vi == 0)
	vzeros++;
    }while(vi++ < vstop);
    
    //if(vzeros > 1)
    //printf("corruption detected %d, tries = %d\n", vzeros, tries);
  }while(tries++ < 2 && vzeros > 1);

  if(!picVector->isEmpty()) {
    struct pictureCmd * cmd = (struct pictureCmd *) picVector->get(0);
    if(cmd != NULL && cmd->special == WC_GRAB) {
      headMove(true, cmd->pan, true, cmd->tilt);
    }
  }
  //ftime(&now);
  //printf("times: %d %d ", timebDiff(now, start), timebDiff(grab, start));
  //memset(rgbFrame, 0, width*height*3);
  if(convert2jpg) {
    v4l_yuv420p2rgb((unsigned char *)rgbFrame, (unsigned char *) map, width, height, 24);
    pthread_mutex_lock(&jpgMutex);
    jpgSize = WriteJPEGToMemoryRGB(width, height, rgbFrame, 80, jpgBuf+6, width*height*3+1018);

    addTimestamp(jpgBuf);
    pthread_mutex_unlock(&jpgMutex);
  }
  /*
  if(convert2png) {
    v4l_yuv420p2rgb((unsigned char *)rgbFrame, (unsigned char *) map, width, height, 24);
    pngSize = WritePNGToFile(rgbFrame, width, height);

    //I could make the PNG add a timestamp, but I'd have to do it in WritePNGToFileRGB
  }
  */
  return true;
}

bool WebCam::sendWithTimestamp(int width, int height, struct sockaddr remoteAddr) {
  if(grabImage(width, height, true, false)) {
    rgram->sendToAddr(jpgBuf, jpgSize+6, remoteAddr);
    return true;
  }
  debug(100, "send with timestamp failed\n");
  return false;
}

bool WebCam::isInUse() {
  return (picVector->size() > 0) || threadProcessing;
}

bool WebCam::takeRecentPicture(int width, int height) {
  if(isInUse())
    return false;
  struct pictureCmd * cmd = new struct pictureCmd;
  cmd->width = width;
  cmd->height = height;
  cmd->pan = motorPos[PAN_SERVO];
  cmd->tilt = motorPos[TILT_SERVO];
  cmd->pack = NULL;

  pthread_mutex_lock(&picVector->mutex);
  picVector->add(cmd);
  picVector->monitor->notify();
  pthread_mutex_unlock(&picVector->mutex);
  return true;
}

//this function must delete the pack
void WebCam::takePicture(Datapack * pack) {
  char *buf = pack->getData();
  if(buf[1] == 1){ //they are getting the most recent image generated from goTo
    pthread_mutex_lock(&jpgMutex);
    //printf("starting send %x", (int) pack); fflush(stdout);
    rgram->sendResponse(jpgBuf+6, jpgSize, pack);
    delete pack;
    //printf(" done\n"); fflush(stdout);
    pthread_mutex_unlock(&jpgMutex);
    //printf("received getRecent request\n");
  }
  else { //a pan, tilt, width, height are specified
    //if we are doing something besides taking pictures, it's a conflict  
    if(wcThreadStatus != WC_NO_CMD && wcThreadStatus != WC_STOP && wcThreadStatus != WC_GRAB) {
      reply(RESOURCE_CONFLICT, pack);
      delete pack; 

      return;
    }

    struct pictureCmd * cmd = new struct pictureCmd;
    cmd->pack = pack;
    cmd->special = WC_GRAB;
    cmd->pan = (int)ntohl(*((int *) (buf+4)));
    cmd->tilt = (int)ntohl(*((int *) (buf+8)));
    cmd->width = (int)ntohl(*((int *) (buf+12)));
    cmd->height = (int)ntohl(*((int *) (buf+16)));
    cmd->lightOn = buf[2] != 0;
    cmd->raw = (buf[1] == 2);
    if(cmd->width < 160)
      cmd->width = 160;
    if(cmd->width > 640)
      cmd->width = 640;
    if(cmd->height < 120)
      cmd->height = 120;
    if(cmd->height > 480)
      cmd->height = 480;
    if(cmd->pan < -180)
      cmd->pan = -180;
    if(cmd->pan > 180)
      cmd->pan = 180;
    if(cmd->tilt < -90)
      cmd->tilt = -90;
    if(cmd->tilt > 90)
      cmd->tilt = 90;

    pthread_mutex_lock(&picVector->mutex);
    picVector->add(cmd);
    picVector->monitor->notify();
    pthread_mutex_unlock(&picVector->mutex);
  }
}

bool WebCam::doSpecial(int wcCmd, Datapack * pack) {
  //if the command isn't to stop, and we're already doing something. . .
  if(wcCmd != WC_STOP && wcThreadStatus != WC_NO_CMD) {
    reply(RESOURCE_CONFLICT, pack);
    if(pack != NULL)
      delete pack;
    return false;
  } else if(wcCmd == WC_STOP) {
    //if they call stop and nothing is running, return SUCCESS now
    if(wcThreadStatus == WC_NO_CMD) {
      reply(SUCCESS, pack);
      if(pack != NULL)
	delete pack;
      return true;
    }else if(wcThreadStatus < WC_USER_STREAM_START) {
      //the camera is doing something else besides streaming
      reply(RESOURCE_CONFLICT, pack);
      if(pack != NULL)
	delete pack;
      return false;
    }
  }
  struct pictureCmd * cmd = new struct pictureCmd;
  cmd->special = wcCmd;
  cmd->pack = pack;

  pthread_mutex_lock(&picVector->mutex);
  picVector->add(cmd);
  picVector->monitor->notify();
  pthread_mutex_unlock(&picVector->mutex);
  return true;
}

char *WebCam::getJpg() {
  return jpgBuf+6;
}

int WebCam::getJpgSize() {
  return jpgSize;
}

char *WebCam::getBuf() {
  return map;
}

int *WebCam::getEMean() {
  return eMeanData;
}

int WebCam::getEPixels() {
  return ePixels;
}

int WebCam::getTmmX() {
  return tmmX;
}

int WebCam::getTmmY() {
  return tmmY;
}

//uses fork, exec and wait to set up camera.
void WebCam::initCamera() {

  pid_t pid;
  int status;
  
  switch(pid = fork()) {
  case -1: //failure
    printf("can't fork!, exiting. . .\n");
    exit(-1);
  case 0: //we're the child
    exit(execl("/bin/rm", "rm", VIDEO_DEV, 0));
  default: //we're the parent
    if(pid == wait(&status)) { //wait for child to die
      if(status != 0) 
	printf("Deleting %s failed!\n", VIDEO_DEV);
    }else
       printf("The wrong child died.\n");
  }

  switch(pid = fork()) {
  case -1: //failure
    printf("can't fork!, exiting. . .\n");
    exit(-1);
  case 0: //we're the child
    exit(execl("/bin/mknod", "mknod", VIDEO_DEV, "c", "81", "0", 0));
  default: //we're the parent
    if(pid == wait(&status)) { //wait for child to die
      if(status != 0) {
	printf("Can't create %s!  Exiting. . .\n", VIDEO_DEV);
	exit(-1);
      }
    }else
       printf("The wrong child died.\n");
  }
}
