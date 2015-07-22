package PER.rover.control;

/** Contains the class for storing the buffer received and it's 
 * sequence number (which allows for keeping track of which packet goes
 * with which thread).
 *
 *@author Eric Porter
 */

public class Datapack {
   private byte [] buf;
   private int sequenceNumber;
   
   public Datapack(byte [] buffer, int seqNum) {
      buf = buffer;
      sequenceNumber = seqNum;
   }
   
   public byte [] getData() {
      return buf;
   }
   
   public int getLength() {
      return buf.length;
   }
   
   public int getSequenceNumber() {
      return sequenceNumber;
   }
   
}