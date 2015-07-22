#ifndef _RELIAGRAM_H_
#define _RELIAGRAM_H_

#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <fcntl.h>
#include <sys/poll.h>
#include <stdio.h>
#include <unistd.h>

#include "../util/vector.h"
#include "../util/monitor.h"
#include "reliapack.h"

#define GC_TIME 15000
#define RETRY_TIME 5000
#define RETRY_WAIT 250

#define LAST_NUMS_SIZE 200

void * ReliagramThread(void * reliagram);

//for now all this helps me do is make sure I don't pass the same command 
//multiple times to the higher level
struct hostData {
  u_int16_t port;
  u_int32_t addr;
  int lastUsedNums[LAST_NUMS_SIZE];
  struct hostData *next;
} ;

class Reliagram {
  friend void * ReliagramThread(void * reliagram);
 public:
  Reliagram(int timeout);

  bool bindTo(int port);
  bool connectTo(char * ipAddr, int port, int timeout);
  bool closeDatagram();
  //int sendData(char * cmd, int len);
  int sendToAddr(char * cmd, int len, struct sockaddr addr);
  int sendResponse(char * cmd, int len, Datapack * resp);
  //Datapack * receiveResponse(int seqNum);
  Datapack * receiveData();
  bool hasNewPacket();

 private: 
  void handleReceivedPacket(char * buf, int len, struct sockaddr_in sender);
  void resendPackets();
  void garbageCollect();
  int waitTime();
  Vector * outPackets, * newPackets, * incompletePackets;
  // receiveWait, responsePackets; 
  int nextSeqNum;
  int rcvTimeout;

  int sock;

  struct hostData *hosts;
};


#endif //_RELIAGRAM_H_
