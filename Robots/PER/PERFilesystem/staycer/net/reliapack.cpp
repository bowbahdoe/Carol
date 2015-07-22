#include "reliapack.h"

Reliapack::Reliapack(char * data, int dataLength, int seqNum, Datapack *resp) {
  
  numPackets = (dataLength + MAX_DATA_LENGTH - 1) /MAX_DATA_LENGTH;
  numPackets = (numPackets == 0) ? 1 : numPackets; //can't have a length of 0
  packets = new char *[numPackets];
  lengths = new int[numPackets];
  sequenceNum = seqNum;
      
  int dataOffset = 0; //be careful with this variable
  int bufLength = MAX_DATA_LENGTH + HEADER_LENGTH;
  //this can be done to all packets
  for(int i=0; i<numPackets; i++) {
    if(i == (numPackets - 1)) //if this is the last packet, change the buffer length
      bufLength = dataLength - dataOffset + HEADER_LENGTH;
    int oldDataOffset = dataOffset;
         
    //copy into buffer in DatagramPacket
    packets[i] = new char[bufLength];
    lengths[i] = bufLength;
    for(int bufPlace=HEADER_LENGTH; bufPlace < bufLength; ) 
      packets[i][bufPlace++] = data[dataOffset++];
      
    setSeqNum(packets[i], seqNum);
    setRetryNum(packets[i], 0);
    setResponseSeqNum(packets[i], resp == 0 ? 0 : resp->getSequenceNumber());
    setResponseRetryNum(packets[i], 0); //the retry number should only be set for ack packets
    setOffset(packets[i], oldDataOffset);
    setTotalLength(packets[i], dataLength);
  }
  memcpy(&address, resp->getAddress(), sizeof(struct sockaddr_in));
  ftime(&lastEventTime);
}
   
Reliapack::Reliapack(char *buf, int length, struct sockaddr_in paddr) {
  int totalLen = getTotalLength(buf);
  //printf("received new packet of length %d\n", totalLen);
  //if this is already a complete packet
  /*  if(totalLen == length - HEADER_LENGTH) {
    packets = new char * [1];
    lengths = new int[1];
    *packets = buf;
    *lengths = totalLength;
    numPackets = 1;
    }else {*/
    numPackets = (totalLen + MAX_DATA_LENGTH - 1) / MAX_DATA_LENGTH;
    numPackets = numPackets ? numPackets : 1;
    packets = new char * [numPackets];
    lengths = new int[numPackets];
    for(int i=0; i<numPackets; i++) {
      packets[i] = NULL;
      lengths[i] = 0;
    }
    addPacket(buf, length);
    //}
  sequenceNum = getSeqNum(buf);
  memcpy(&address, &paddr, sizeof(struct sockaddr_in));
  ftime(&lastEventTime); //reset the last event time
}

Reliapack::~Reliapack() {
  for(int i=0; i<numPackets; i++)
    if(packets[i] != NULL)
      delete packets[i];
  delete packets;
  delete lengths;
}
   
void Reliapack::addPacket(char *buf, int length) {
  int offset = getOffset(buf);
  int index = offset / MAX_DATA_LENGTH;
  if(index >= numPackets) {
    printf("error, received packet with an offset too high.\n");
    return;
  }
  packets[index] = new char[length];
  memcpy(packets[index], buf, length);
  lengths[index] = length;
  ftime(&lastEventTime);
}
   
bool Reliapack::isComplete() {
  for(int i=0; i<numPackets; i++)
    if(packets[i] == NULL)
      return false;
  return true;
}
   
char ** Reliapack::getBuffers() {
  return packets;
}

int * Reliapack::getLengths() {
  return lengths;
}

int Reliapack::getNumPackets() {
  return numPackets;
}
   
  //how long it has been since anything has happened
int Reliapack::idleTime() {
  struct timeb now;
  ftime(&now);
  int diff = (int)now.time - (int) lastEventTime.time;
  return diff*1000 + (int)now.millitm - (int)lastEventTime.millitm;
}
   
bool Reliapack::isFullyAcked(char * ackBuf) {
  if(ackBuf != 0 && getResponseSeqNum(ackBuf) == getPackSeqNum()) {
    int ackOffset = getOffset(ackBuf);
    for(int i=0; i<numPackets; i++)
      if(packets[i] != NULL && getOffset(packets[i]) == ackOffset) {
	delete packets[i];
	packets[i] = NULL;
      }
    ftime(&lastEventTime);
  }
  for(int i=0; i<numPackets; i++)
    if(packets[i] != NULL)
      return false;
  return true;
}
   
Datapack * Reliapack::getData() {
  int totalLength = getPackTotalLength();
  char * retBuf = new char[totalLength];
  for(int i=0; i<numPackets; i++) {
    if(packets[i] == 0)
      return 0;
    else {
      int packetLen = lengths[i];
      int retBufPlace = getOffset(packets[i]); //the offset of where to put this array
      memcpy(retBuf+retBufPlace, packets[i]+HEADER_LENGTH, packetLen-HEADER_LENGTH);
    }
  }
  Datapack * retpack = new Datapack(retBuf, totalLength, sequenceNum, address);
  return retpack;
}
   
char * Reliapack::getAckPacket(char *packet) {
  char * buf = new char[HEADER_LENGTH];
  //copy the sequence number and retry number into new packet.
  for(int i=0; i<4; i++) {
    //buf[i+4] = packet[i];
    buf[i] = 0;
  }
  for(int i=8; i<HEADER_LENGTH; i++)
    buf[i] = packet[i];

  setResponseSeqNum(buf, getSeqNum(packet));
  setResponseRetryNum(buf, getRetryNum(packet));
  return buf;
}
   
void Reliapack::setRetryTime(int fromNow) {
  ftime(&retryTime);
  retryTime.millitm += fromNow;
  if(retryTime.millitm >= 1000) {
    retryTime.time += retryTime.millitm / 1000;
    retryTime.millitm %= 1000;
  }
}
   
bool Reliapack::shouldRetry() {
  struct timeb now;
  ftime(&now);
  return now.time > retryTime.time || (now.time == retryTime.time && 
				 now.millitm > retryTime.millitm);
}

const struct sockaddr * Reliapack::getAddress(){
  return (const struct sockaddr *) &address;
}
   
   
void Reliapack::setSeqNum(char * p, int seqNum){
  uint16_t netShort = htons(seqNum);
  memcpy(p, &netShort, 2);
}
   
void Reliapack::setRetryNum(char * p, int retryNum){
  uint16_t netShort = htons(retryNum);
  memcpy(p+2, &netShort, 2);
}
   
void Reliapack::setResponseSeqNum(char * p, int seqNum) {
  uint16_t netShort = htons(seqNum);
  memcpy(p+4, &netShort, 2);
}
   
void Reliapack::setResponseRetryNum(char * p, int retryNum) {
  uint16_t netShort = htons(retryNum);
  memcpy(p+6, &netShort, 2);
}
   
void Reliapack::setOffset(char * p, int offset) {
  unsigned int netLong = htonl(offset);
  memcpy(p+8, &netLong, 4);
}
   
void Reliapack::setTotalLength(char * p, int length) {
  unsigned int netLong = htonl(length);
  memcpy(p+12, &netLong, 4);
}
   
int Reliapack::getPackSeqNum() {
  return sequenceNum; //I made a var for this one because incomplete packets may have
}                      //the 0th packet null
   
int Reliapack::getSeqNum(char * p){
  return (int) ntohs(*((uint16_t *) p));
}
   
int Reliapack::getRetryNum(char * p){
  return (int) ntohs(*((uint16_t *) (p+2)));
}
   
int Reliapack::getPackResponseSeqNum() {
  return getResponseSeqNum(packets[0]);
}
   
int Reliapack::getResponseSeqNum(char * p) {
  return (int) ntohs(*((uint16_t *) (p+4)));
}
   
int Reliapack::getResponseRetryNum(char * p) {
  return (int) ntohs(*((uint16_t *) (p+6)));
}
   
int Reliapack::getOffset(char * p) {
  return (int) ntohl(*((uint32_t *) (p+8)));
}
   
int Reliapack::getPackTotalLength() {
  return getTotalLength(packets[0]);
}
   
int Reliapack::getTotalLength(char * p) {
  return (int) ntohl(*((uint32_t *) (p+12)));
}


Datapack::Datapack(char * buffer, int len, int seqNum, struct sockaddr_in paddr) {
  buf = buffer;
  length = len;
  sequenceNumber = seqNum;
  memcpy(&address, &paddr, sizeof(struct sockaddr));
}

Datapack::Datapack(Datapack *oldPack) {
  buf = new char[oldPack->getLength()];
  length = oldPack->getLength();
  sequenceNumber = oldPack->getSequenceNumber();
  address = *(struct sockaddr_in *)oldPack->getAddress();
}

Datapack::~Datapack() {
  if(buf != NULL)
    delete buf;
}
   
char * Datapack::getData() {
  return buf;
}
   
int Datapack::getLength() {
  return length;
}
   
int Datapack::getSequenceNumber() {
  return sequenceNumber;
}
   
const struct sockaddr * Datapack::getAddress() {
  return (const struct sockaddr *) &address;
}
