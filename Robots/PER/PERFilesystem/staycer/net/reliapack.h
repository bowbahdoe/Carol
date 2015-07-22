#ifndef _RELIAPACK_H_
#define _RELIAPACK_H_

#include <sys/timeb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>

#define HEADER_LENGTH 16
#define MAX_DATA_LENGTH 1400
#define MAX_SEQUENCE_NUMBER 65535 //2 bytes, unsigned

class Datapack {
 public:
  Datapack(char * buffer, int length, int seqNum, struct sockaddr_in paddr);
  Datapack(Datapack *oldPack);
  ~Datapack();

  char * getData();
  int getLength();
  int getSequenceNumber();
  const struct sockaddr * getAddress();

 private:
  char * buf;
  int length;
  int sequenceNumber;
  struct sockaddr_in address;
};

class Reliapack {
 public:
  Reliapack(char * data, int dataLength, int seqNum, Datapack * resp);
  Reliapack(char *buf, int length, struct sockaddr_in address);
  ~Reliapack();

  void addPacket(char *buf, int length);
  bool isComplete();
  char ** getBuffers();
  int * getLengths();
  int getNumPackets();

  int idleTime();
  bool isFullyAcked(char * ackBuf);
  Datapack * getData();
  static char * getAckPacket(char *packet);
  void setRetryTime(int fromNow);
  bool shouldRetry();
  const struct sockaddr * getAddress();

  static void setSeqNum(char * p, int seqNum);
  static void setRetryNum(char * p, int retryNum);
  static void setResponseSeqNum(char * p, int seqNum);
  static void setResponseRetryNum(char * p, int retryNum);
  static void setOffset(char * p, int offset);
  static void setTotalLength(char * p, int length);
  int getPackSeqNum();
  static int getSeqNum(char * p);
  static int getRetryNum(char * p);
  int getPackResponseSeqNum();
  static int getResponseSeqNum(char * p);
  static int getResponseRetryNum(char * p);
  static int getOffset(char * p);
  int getPackTotalLength();
  static int getTotalLength(char * p);

 private:
   
  char ** packets;
  int * lengths; //the length of the buffer, including the header
  int numPackets;
  struct sockaddr_in address;

  int sequenceNum;
   
  struct timeb retryTime; //what time to resend
  struct timeb lastEventTime; //when the packet was created 
  //events = {got an ack, got a new part of the packet }
};

#endif //_RELIAPACK_H_
