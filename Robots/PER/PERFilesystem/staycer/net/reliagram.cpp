#include "reliagram.h"

/** Creates a new instance of Reliagram */
Reliagram::Reliagram(int timeout) {
  outPackets = new Vector();
  newPackets = new Vector();
  //responsePackets = new Vector();
  incompletePackets = new Vector();
  //receiveWait = new Vector(); //holds Integers, receive's wait on these Integers
  
  sock = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP);
  fcntl(sock, F_SETFL, O_NONBLOCK);

  pthread_t rgramPThread;
  if(pthread_create(&rgramPThread, 0, ReliagramThread, this)) 
    printf("Can't start the Religram receive thread!\n");
  nextSeqNum = 1;
  rcvTimeout = timeout;
  hosts = NULL;
}

bool Reliagram::bindTo(int port) {
  struct sockaddr_in local;
  // Fill out the local socket's address information.
  local.sin_family = AF_INET;
  local.sin_port = htons (port);  
  local.sin_addr.s_addr = htonl (INADDR_ANY);
   
  // Associate the local address with WinSocket.
  if (bind (sock,  (struct sockaddr *) &local, sizeof(local))) {
    printf("Binding socket to port %d failed.\n", port);
    close(sock);
    return false;
  } 
  return true;
}
   
bool Reliagram::connectTo(char * ipAddr, int port, int timeout) {
  /*try {
    if(socket != null)
    socket.close();
    socket = new DatagramSocket(1701);
    InetAddress address = InetAddress.getByName(ipAddr);
    socket.connect(address, port);
    rcvTimeout = timeout;
    if(!this.isAlive())
    start();
    return true;
    }catch(Exception e) {
    socket = null;
    return false;
    }*/
  return true;
}
   
bool Reliagram::closeDatagram() {
  /*if(socket == null)
    return false;
  socket.close();
  socket = null;*/
  return true;
}
   
/*int Reliagram::sendData(char * cmd, int len) {
  return sendResponse(cmd, len, null);
  }*/

int Reliagram::sendToAddr(char * cmd, int len, struct sockaddr addr) {
  //creating a Datapack with seq num 0 means a new packet
  Datapack * newPack = new Datapack(NULL, 0, 0,*((struct sockaddr_in*) &addr));
  int seqNum = sendResponse(cmd, len, newPack);
  delete newPack;
  return seqNum;
}
   
int Reliagram::sendResponse(char * cmd, int len, Datapack * resp) {
  //printf("I've made it to sendResponse!\n");
  Reliapack *rpack;
  rpack = new Reliapack(cmd, len, nextSeqNum++, resp);
  if(nextSeqNum > MAX_SEQUENCE_NUMBER)
    nextSeqNum = 1;
  //struct sockaddr_in addr_in = *((struct sockaddr_in *)rpack->getAddress());
  //printf("sending to %d.%d.%d.%d, port %d\n", addr_in.sin_addr.s_addr >> 24, (addr_in.sin_addr.s_addr >> 16) & 255, (addr_in.sin_addr.s_addr >> 8) & 255, addr_in.sin_addr.s_addr & 255, ntohs(addr_in.sin_port));
      
  char ** buffers = rpack->getBuffers();
  int * lengths = rpack->getLengths();
  for(int i=0; i<rpack->getNumPackets(); i++) {
    sendto(sock, buffers[i], (size_t) lengths[i], 0, rpack->getAddress(), sizeof(struct sockaddr));
    printf("%s\n", buffers[i]);
  }
  rpack->setRetryTime(RETRY_WAIT);
  // printf("I've sent some stuff\n");
  pthread_mutex_lock(&outPackets->mutex);
  outPackets->add(rpack);
  pthread_mutex_unlock(&outPackets->mutex);
  //printf("I'm about to return %d\n",(int) rpack->getPackSeqNum());
  return rpack->getPackSeqNum();
}

/*Datapack * Reliagram::receiveResponse(int seqNum) {
  try {
    synchronized(responsePackets) {
      Reliapack rp;
      for(int i=0; i<responsePackets.size(); i++) {
	rp = (Reliapack) responsePackets.get(i);
	if(rp.getResponseSeqNum() == seqNum) {
	  Datapack * retPack = rp.getData();
	  responsePackets.remove(i);
	  return retPack;
	}
      }
    }
         
    Integer monitored = new Integer(seqNum);
    synchronized (receiveWait) {
      receiveWait.add(monitored);
    }
    synchronized(monitored) {
      monitored.wait(rcvTimeout);
    }
    synchronized (receiveWait) {
      receiveWait.remove(monitored);
    }
         
    synchronized(responsePackets) {
      Reliapack rp;
      for(int i=0; i<responsePackets.size(); i++) {
	rp = (Reliapack) responsePackets.get(i);
	if(rp.getResponseSeqNum() == seqNum) {
	  Datapack * retPack = rp.getData();
	  responsePackets.remove(i);
	  return retPack;
	}
      }
    }
  } catch(Exception e) {}
  return null;
  }*/
   
//waits for a new packet
Datapack * Reliagram::receiveData() {
  pthread_mutex_lock(&newPackets->mutex);
  if(newPackets->isEmpty())
    newPackets->monitor->timedwait(&newPackets->mutex, rcvTimeout);
  if(!newPackets->isEmpty()) {
    Reliapack * rp = (Reliapack *)newPackets->remove(0);
    if(rp == NULL) {
      printf("error in receiveData, rp is NULL\n");
      pthread_mutex_unlock(&newPackets->mutex);
      return NULL;
    }

    Datapack * retPack = rp->getData();
    delete rp;
    pthread_mutex_unlock(&newPackets->mutex);
    return retPack;
  }
  pthread_mutex_unlock(&newPackets->mutex);
  return NULL;
}

bool Reliagram::hasNewPacket() {
  return !newPackets->isEmpty();
}
   
void *ReliagramThread(void *reliagram) {
  Reliagram * rgram = (Reliagram *) reliagram;
  const int bufLen = MAX_DATA_LENGTH + HEADER_LENGTH;
  int len;
  socklen_t fromLen = sizeof(struct sockaddr);
  char buf[bufLen];
  struct pollfd pfd;
  struct sockaddr_in sender;
  for(;;) {
    pfd.fd = rgram->sock;
    pfd.events = POLLIN;
    if(poll(&pfd, 1, rgram->waitTime())) {
      len = recvfrom(rgram->sock, buf, bufLen, 0, (struct sockaddr *) &sender, &fromLen);
            
      if(len < HEADER_LENGTH)
	printf("error, received too short a packet\n");
      else {
	if (Reliapack::getSeqNum(buf) != 0) {
	  char * ackBuf = Reliapack::getAckPacket(buf);
	  //send an ack
	  sendto(rgram->sock, ackBuf, HEADER_LENGTH, 0, (const struct sockaddr *) &sender, fromLen); 
	  delete ackBuf;
	}
	rgram->handleReceivedPacket(buf, len, sender);
      }
    }
            
    rgram->resendPackets();
    rgram->garbageCollect();
  }
}
   
void Reliagram::handleReceivedPacket(char * buf, int len, struct sockaddr_in sender) {
  int responseSeqNum = Reliapack::getResponseSeqNum(buf);
  int seqNum = Reliapack::getSeqNum(buf);
      
  //remove packets from out vector if fully acknowledged
  Reliapack *rp = NULL;
  pthread_mutex_lock(&outPackets->mutex);
  for(int i=0; i<outPackets->size(); i++) {
    rp = (Reliapack *)outPackets->get(i);
    if(rp == NULL) {
      printf("error in handleReceivedPacket (1), rp is null\n");
      break;
    }
    if(rp->getPackSeqNum() == responseSeqNum && rp->isFullyAcked(buf)) {
      outPackets->remove(i);
      delete rp;
      rp = NULL;
      break;
    }
  }
  pthread_mutex_unlock(&outPackets->mutex);
  
      
  //exit if just an ack by checking if seq num is 0.
  if(seqNum == 0)
    return;
      
  //build a new reliapack out of this, to be stored in the incompletePackets vector, or processed further

  struct hostData *senderData = hosts;
  while(senderData != NULL && (senderData->port != sender.sin_port || senderData->addr != sender.sin_addr.s_addr))
    senderData = senderData->next;
  if(senderData == NULL) {
    senderData = new struct hostData;
    if(senderData != NULL) {
      senderData->port = sender.sin_port; 
      senderData->addr = sender.sin_addr.s_addr;
      for(int i=0; i<LAST_NUMS_SIZE; i++)
	senderData->lastUsedNums[i] = 0;
      senderData->next = hosts;
      hosts = senderData;
      //printf("new computer connected.\n");
    }
  }
  //return if I've seen this packet before
  if(senderData != NULL && senderData->lastUsedNums[seqNum % LAST_NUMS_SIZE] == seqNum)
    return;

  Reliapack * rpack = NULL;
  //printf("len: %d, totalLen: %d\n", len, Reliapack::getTotalLength(buf));
  if(Reliapack::getTotalLength(buf) == len - HEADER_LENGTH) {
    //if it is a single part packet
    rpack = new Reliapack(buf, len, sender);
  }else { // it is only part of a Reliapack
    printf("got an incomplete packet from java side\n");
    pthread_mutex_lock(&incompletePackets->mutex);
    for(int i=0; i<incompletePackets->size(); i++) {
      rp = (Reliapack *) incompletePackets->get(i);
      if(rp == NULL) {
	printf("error in handleReceivedPacket (1), rp is null\n");
	break;
      }
      if(rp->getPackSeqNum() == seqNum) {
	rp->addPacket(buf, len);
	if(!rp->isComplete()) { //if the packet is not complete, I should stop here
	  pthread_mutex_unlock(&incompletePackets->mutex);
	  return;
	}else { //it is complete, remove it from incomplete vector
	  incompletePackets->remove(i);
	  rpack = rp;
	  break;
	}
      }
    }
    if(rpack == 0) {//there's nothing in the incompletePackets array yet
      //add a new packet and return - the packet's incomplete - there's nothing else to do
      rpack = new Reliapack(buf, len, sender);
      incompletePackets->add(rpack);
      pthread_mutex_unlock(&incompletePackets->mutex);
      return;
    }
    pthread_mutex_unlock(&incompletePackets->mutex);
  }
      
  pthread_mutex_lock(&newPackets->mutex);
  if(responseSeqNum == 0) { //new packet
    newPackets->add(rpack);
    if(senderData != NULL)
      senderData->lastUsedNums[seqNum % LAST_NUMS_SIZE] = seqNum;
    newPackets->monitor->notify();
  }else {
    if(rpack != NULL)
      delete rpack;
  }
  pthread_mutex_unlock(&newPackets->mutex);
}
   
void Reliagram::resendPackets() {
  pthread_mutex_lock(&outPackets->mutex);
  Reliapack * rpack;
  for(int i=0; i<outPackets->size(); i++) {
    rpack = (Reliapack *)outPackets->get(i);
    if(rpack == NULL) {
      printf("in resendPackets, got a NULL\n");
      break;
    }
    if(rpack->shouldRetry() && rpack->idleTime() > RETRY_WAIT) {
      rpack->setRetryTime(RETRY_WAIT);
	
      //printf("resending packet\n");
      char ** bufs = rpack->getBuffers();
      int * lengths = rpack->getLengths();
      for(int j=0; j<rpack->getNumPackets(); j++)
	if(bufs[j] != NULL)
	  sendto(sock, bufs[j], lengths[j], 0, rpack->getAddress(), sizeof(struct sockaddr));
	      
    }
  }
  pthread_mutex_unlock(&outPackets->mutex);
}
   
void Reliagram::garbageCollect() {
  Reliapack * rp;
  pthread_mutex_lock(&outPackets->mutex);
    for(int i=0; i<outPackets->size(); i++) {
      rp = (Reliapack *)outPackets->get(i);
      if(rp == NULL) {
	printf("error in garbageCollect (1)\n");
	break;
      }
      if(rp->idleTime() > RETRY_TIME) {
	outPackets->remove(i);
	i--;
	delete rp;
      }
    }
  pthread_mutex_unlock(&outPackets->mutex);
  
      
  pthread_mutex_lock(&newPackets->mutex);
    for(int i=0; i<newPackets->size(); i++){
      rp = (Reliapack *)newPackets->get(i);
      if(rp == NULL) {
	printf("error in garbageCollect (2)\n");
	break;
      }
      if(rp->idleTime() > GC_TIME) {
	newPackets->remove(i);
	i--;
	delete rp;
      }
    }
  pthread_mutex_unlock(&newPackets->mutex);
      
  pthread_mutex_lock(&incompletePackets->mutex);
    for(int i=0; i<incompletePackets->size(); i++){
      rp = (Reliapack *)incompletePackets->get(i);
      if(rp == NULL) {
	printf("error in garbageCollect (1)\n");
	break;
      }
      if(rp->idleTime() > GC_TIME) {
	incompletePackets->remove(i);
	i--;
	delete rp;
      }
    }
  pthread_mutex_unlock(&incompletePackets->mutex);
}
   
int Reliagram::waitTime() {
  /*
  if(outPackets->isEmpty())
    return 50;
  long minTime = 50;
  for(int i=0; i<outPackets->size(); i++)
    minTime = Math.min(minTime, ((Reliapack)outPackets->get(i)).getRetryTime()
		       -System.currentTimeMillis());
  if(minTime <= 0)
    minTime = 1;
    return (int) minTime;*/
  return RETRY_WAIT;
}

