/*
 * Reliapack.java
 *
 * Created on July 2, 2003, 12:21 PM
 */

package PER.rover.control;

import java.net.*;

/** Internal class for communication - it is called by reliagram and handles
 * the low level packet transceiving and tracking.
 *
 * @author  Eric Porter
 */
public class Reliapack {
   
   public final static int HEADER_LENGTH = 16;
   private final static int MAX_DATA_LENGTH = 1400;
   public final static int MAX_SEQUENCE_NUMBER = 65535; //2 bytes, unsigned
   
   private DatagramPacket [] packets;
   private int sequenceNum;
   
   private long retryTime; //what time to resend
   private long lastEventTime; //when the packet was created 
      //events = {got an ack, got a new part of the packet }
   
   /** Creates a new instance of Reliapack */
   public Reliapack(byte [] data, int dataLength, int seqNum, Datapack resp) {
      /*if(length <= MAX_DATA_LENGTH) {
         int packetLength = length + HEADER_LENGTH;
         byte [] buf = new byte[packetLength];
         fillPacket(buf, data, 0, length, seqNum, length, resp);
         packet[0] = new DatagramPacket(buf, packetLength);
      }else {*/
      int numPackets = (dataLength + MAX_DATA_LENGTH - 1) /MAX_DATA_LENGTH;
      numPackets = (numPackets == 0) ? 1 : numPackets; //can't have a length of 0
      packets = new DatagramPacket[numPackets];
      sequenceNum = seqNum;
      
      int dataOffset = 0; //be careful with this variable
      int bufLength = MAX_DATA_LENGTH + HEADER_LENGTH;
      //this can be done to all packets
      for(int i=0; i<packets.length; i++) {
         if(i == (packets.length - 1)) //if this is the last packet, change the buffer length
            bufLength = dataLength - dataOffset + HEADER_LENGTH;
         int oldDataOffset = dataOffset;
         
         //copy into buffer in DatagramPacket
         byte [] buf = new byte[bufLength];
         for(int bufPlace=HEADER_LENGTH; bufPlace < bufLength; )
            buf[bufPlace++] = data[dataOffset++];
         packets[i] = new DatagramPacket(buf, bufLength);
         
         setSeqNum(packets[i], seqNum);
         setRetryNum(packets[i], 0);
         setResponseSeqNum(packets[i], resp == null ? 0 : resp.getSequenceNumber());
         setResponseRetryNum(packets[i], 0); //the retry number should only be set for ack packets
         setOffset(packets[i], oldDataOffset);
         setTotalLength(packets[i], dataLength);
      }
      lastEventTime = System.currentTimeMillis();
   }
   
   public Reliapack(DatagramPacket pack) {
      int totalLen = Reliapack.getTotalLength(pack);
      //if this is already a complete packet
      if(totalLen == pack.getLength() - Reliapack.HEADER_LENGTH) {
         packets = new DatagramPacket[] {pack};
      }else {
         int numPackets = (totalLen + MAX_DATA_LENGTH - 1) /MAX_DATA_LENGTH;
         packets = new DatagramPacket[numPackets];
         for(int i=0; i<numPackets; i++)
            packets[i] = null;
         addPacket(pack);
      }
      sequenceNum = Reliapack.getSeqNum(pack);
      lastEventTime = System.currentTimeMillis();
   }
   
   public void addPacket(DatagramPacket pack) {
      int offset = getOffset(pack);
      int index = offset / MAX_DATA_LENGTH;
      if(index >= packets.length) {
         Reliagram.debug(15, "error, received packet with an offset too high.\n");
         return;
      }
      packets[index] = pack;
      lastEventTime = System.currentTimeMillis();
   }
   
   public boolean isComplete() {
      for(int i=0; i<packets.length; i++)
         if(packets[i] == null)
            return false;
      return true;
   }
   
   public DatagramPacket [] getDatagrams() {
      return packets;
   }
   
   //how long it has been since anything has happened
   public long idleTime() {
      return System.currentTimeMillis() - lastEventTime;
   }
   
   public boolean isFullyAcked(DatagramPacket ackPacket) {
      if(ackPacket != null && getResponseSeqNum(ackPacket) == getSeqNum()) {
         int ackOffset = getOffset(ackPacket);
         for(int i=0; i<packets.length; i++)
            if(packets[i] != null && getOffset(packets[i]) == ackOffset)
               packets[i] = null;
         lastEventTime = System.currentTimeMillis();
      }
      for(int i=0; i<packets.length; i++)
         if(packets[i] != null)
            return false;
      return true;
   }
   
   public byte [] getData() {
      int totalLength = getTotalLength();
      byte [] retBuf = new byte[totalLength];
      for(int i=0; i<packets.length; i++) {
         if(packets[i] == null)
            return null;
         else {
            byte [] packetBuf = packets[i].getData();
            int packetLen = packets[i].getLength();
            int retBufPlace = getOffset(packets[i]); //the offset of where to put this array
            for(int j=HEADER_LENGTH; j<packetLen; )
               retBuf[retBufPlace++] = packetBuf[j++];
         }
      }
      return retBuf;
   }
   
   public static DatagramPacket getAckPacket(DatagramPacket p) {
      byte [] buf = new byte[HEADER_LENGTH];
      byte [] data = p.getData();
      //copy the sequence number and retry number into new packet.
      for(int i=0; i<4; i++)
         buf[i+4] = data[i];
      for(int i=8; i<HEADER_LENGTH; i++)
         buf[i] = data[i];
      DatagramPacket ack = new DatagramPacket(buf, HEADER_LENGTH);
      setResponseSeqNum(ack, getSeqNum(p));
      setResponseRetryNum(ack, getRetryNum(p));
      return ack;
   }
   
   public void setRetryTime(long time) {
      retryTime = time;
   }
   
   public long getRetryTime() {
      return retryTime;
   }
   
   
   public static void setSeqNum(DatagramPacket p, int seqNum){
      ByteUtil.intToNetworkShort(p.getData(), seqNum, 0);
   }
   
   public static void setRetryNum(DatagramPacket p, int retryNum){
      ByteUtil.intToNetworkShort(p.getData(), retryNum, 2);
   }
   
   public static void setResponseSeqNum(DatagramPacket p, int seqNum) {
      ByteUtil.intToNetworkShort(p.getData(), seqNum, 4); //the response sequence number
   }
   
   public static void setResponseRetryNum(DatagramPacket p, int retryNum) {
      ByteUtil.intToNetworkShort(p.getData(), retryNum, 6); //the response retry number
   }
   
   public static void setOffset(DatagramPacket p, int offset) {
      ByteUtil.intToNetworkLong(p.getData(), offset, 8);  //the offset of the bytes being sent
   }
   
   public static void setTotalLength(DatagramPacket p, int length) {
      ByteUtil.intToNetworkLong(p.getData(), length, 12); //the length of bytes being sent
   }
   
   public int getSeqNum() {
      return sequenceNum; //I made a var for this one because incomplete packets may have
   }                      //the 0th packet null
   
   public static int getSeqNum(DatagramPacket p){
      return ByteUtil.networkShortToUnsignedInt(p.getData(), 0);
   }
   
   public static int getRetryNum(DatagramPacket p){
      return ByteUtil.networkShortToUnsignedInt(p.getData(), 2);
   }
   
   public int getResponseSeqNum() {
      return getResponseSeqNum(packets[0]);
   }
   
   public static int getResponseSeqNum(DatagramPacket p) {
      return ByteUtil.networkShortToUnsignedInt(p.getData(), 4);
   }
   
   public static int getResponseRetryNum(DatagramPacket p) {
      return ByteUtil.networkShortToUnsignedInt(p.getData(), 6);
   }
   
   public static int getOffset(DatagramPacket p) {
      return ByteUtil.networkLongToInt(p.getData(), 8);  //the offset of the bytes in the packet
   }
   
   public int getTotalLength() {
      return getTotalLength(packets[0]);
   }
   
   public static int getTotalLength(DatagramPacket p) {
      return ByteUtil.networkLongToInt(p.getData(), 12);
   }
}
