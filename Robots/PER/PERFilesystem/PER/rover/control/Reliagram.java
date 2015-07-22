/*
 * Reliagram.java
 *
 * Created on June 30, 2003, 10:11 AM
 */

package PER.rover.control;

import java.net.*;
import java.util.*;

/** External class for RoverController to handle communication.
 *
 * @author  Eric Porter
 */
public class Reliagram extends Thread {
   
   private DatagramSocket socket = null;
   private Vector outPackets, newPackets, responsePackets, incompletePackets, receiveWait;
   private RGHostFinder finder;
   
   private int nextSeqNum = 1;
   private int rcvTimeout;
   private boolean keepGoing = true;
   
   private static final int GC_TIME = 15000;
   private static final int RETRY_TIME = 5000;
   private static final int RETRY_WAIT = 50;
   
   public static final int DEBUG = -9999;
   
   /** Creates a new instance of Reliagram */
   public Reliagram() {
      outPackets = new Vector();
      newPackets = new Vector();
      responsePackets = new Vector();
      incompletePackets = new Vector();
      receiveWait = new Vector(); //holds Integers, receive's wait on these Integers
      finder = new RGHostFinder();
   }
   
   /**
    *@param timeout How long to wait for received data. 
    * 0=infinity, a negative number will cause the function to return immediately
    */
   public synchronized boolean connect(String ipAddr, int port, int timeout) {
      try {
         if(socket != null)
            socket.close();
         socket = new DatagramSocket();
         InetAddress address = InetAddress.getByName(ipAddr);
         socket.connect(address, port);
         rcvTimeout = timeout;
         if(!this.isAlive())
            start();
         return true;
      }catch(Exception e) {
         socket = null;
         return false;
      }
   }
   
   public synchronized boolean close() {
      if(socket == null)
         return false;
      socket.close();
      finder = new RGHostFinder();
      socket = null;
      return true;
   }
   
   /** Quits the thread that handles the receives; only call this if destroying
    * a copy of Reliagram.
    */
   public void quit() {
      close();
      keepGoing = false;
   }
   
   public int send(byte [] cmd, int len) {
      return send(cmd, len, null);
   }
   
   public synchronized int send(byte [] cmd, int len, Datapack resp) {
      if(socket == null)
         return -1;
      
      Reliapack rpack = new Reliapack(cmd, len, nextSeqNum++, resp);
      if(nextSeqNum > Reliapack.MAX_SEQUENCE_NUMBER)
         nextSeqNum = 1;
      
      try {
         DatagramPacket [] grams = rpack.getDatagrams();
         for(int i=0; i<grams.length; i++)
            socket.send(grams[i]);
         rpack.setRetryTime(System.currentTimeMillis()+50);
         outPackets.add(rpack);
         return rpack.getSeqNum();
      } catch(Exception e) {
         debug(20, "Error sending packet!\n");
         return -2;
      }
   }
   
   /** Waits for a new packet. Uses the timeout that was used the last time
    * <code>connect<code> was called.
    */
   public Datapack receive(int seqNum) {
      return receive(seqNum, rcvTimeout);
   }
   
   /** 
    * @param timeout How long to wait for - 0 means infinite timeout
    */
   public Datapack receive(int seqNum, int timeout) {
      try {
         synchronized(responsePackets) {
            Reliapack rp;
            for(int i=0; i<responsePackets.size(); i++) {
               rp = (Reliapack) responsePackets.get(i);
               if(rp.getResponseSeqNum() == seqNum) {
                  Datapack retPack = new Datapack(rp.getData(), rp.getSeqNum());
                  responsePackets.remove(i);
                  return retPack;
               }
            }
         }
         
         Integer monitored = new Integer(seqNum);
         synchronized (receiveWait) {
            receiveWait.add(monitored);
         }
         if(timeout >= 0) {
            synchronized(monitored) {
               monitored.wait(timeout);
            }
         }
         synchronized (receiveWait) {
            receiveWait.remove(monitored);
         }
         
         synchronized(responsePackets) {
            Reliapack rp;
            for(int i=0; i<responsePackets.size(); i++) {
               rp = (Reliapack) responsePackets.get(i);
               if(rp.getResponseSeqNum() == seqNum) {
                  Datapack retPack = new Datapack(rp.getData(), rp.getSeqNum());
                  responsePackets.remove(i);
                  return retPack;
               }
            }
         }
      } catch(Exception e) {}
      return null;
   }
   
   /** Waits for a new packet. Uses the timeout that was used the last time
    * <code>connect<code> was called.
    */
   public Datapack receive() {
      return receiveWithTimeout(rcvTimeout);
   }
   
   
   /** Waits for a new packet with the specified timeout in milliseconds.
   * If timeout is 0, it will block until new data is available
   */
   public Datapack receiveWithTimeout(int timeout) {
      try {
         synchronized(newPackets) {
            if(newPackets.isEmpty())
               newPackets.wait(timeout);
            if(!newPackets.isEmpty()) {
               Reliapack rp = (Reliapack)newPackets.remove(0);
               return new Datapack(rp.getData(), rp.getSeqNum());
            }
         }
      } catch(Exception e) {
         debug(20, "unexpceted exception in receive()\n");
      }
      return null;
   }
   
   public void run() {
      byte [] rcvBuf = new byte[1500];
      DatagramPacket packet = new DatagramPacket(rcvBuf, rcvBuf.length);
      for(;keepGoing;) {
         try {
            socket.setSoTimeout(waitTime());
            socket.receive(packet);
            
            if(packet.getLength() < Reliapack.HEADER_LENGTH) {
               debug(20, "error, received too short a packet\n");
               continue;
            }
            
            /*System.out.println("got a packet of length "+packet.getLength()+
            " seq num: "+Reliapack.getSeqNum(packet)+" reply seq num: "+Reliapack.getResponseSeqNum(packet));*/
            if (Reliapack.getSeqNum(packet) != 0)
               socket.send(Reliapack.getAckPacket(packet)); //send an ack
            //System.out.println("got a packet from "+packet.getAddress().getHostAddress()+" port "+packet.getPort());
            handleReceivedPacket(packet);
            
            rcvBuf = new byte[1500];
            packet = new DatagramPacket(rcvBuf, rcvBuf.length);
         } catch(SocketTimeoutException e) {
         } catch(Exception e) {
            debug(25, "unexpected socket error in Religram.run: "+e.toString());
         }
         resendPackets();
         garbageCollect();
      }
   }
   
   //return true if I'm using the buffer
   private boolean handleReceivedPacket(DatagramPacket newPacket) {
      int responseSeqNum = Reliapack.getResponseSeqNum(newPacket);
      int seqNum = Reliapack.getSeqNum(newPacket);
      
      //remove packets from out vector if fully acknowledged
      Reliapack rp = null;
      synchronized(outPackets) {
         for(int i=0; i<outPackets.size(); i++) {
            rp = (Reliapack)outPackets.get(i);
            if(rp.getSeqNum() == responseSeqNum && rp.isFullyAcked(newPacket)) {
               outPackets.remove(i);
               break;
            }
         }
      }
      
      //exit if just an ack by checking if seq num is 0.
      if(seqNum == 0)
         return false;
      
      //If I've gotten the same packet recently, don't process further
      if(finder.seenRecently(newPacket, seqNum)) {
         //System.out.println("check saved me from adding "+seqNum);
         return false;
      }
      
      //build a new reliapack out of this, to be stored in the incompletePackets vector, or processed further
      Reliapack rpack = null;
      if(Reliapack.getTotalLength(newPacket) == newPacket.getLength() - Reliapack.HEADER_LENGTH) {
         //if it is a single part packet
         rpack = new Reliapack(newPacket);
      }else { // it is only part of a Reliapack
         synchronized(incompletePackets) {
            for(int i=0; i<incompletePackets.size(); i++) {
               rp = (Reliapack) incompletePackets.get(i);
               if(rp.getSeqNum() == seqNum) {
                  rp.addPacket(newPacket);
                  if(!rp.isComplete()) //if the packet is not complete, I should stop here
                     return true;
                  else {
                     incompletePackets.remove(i);
                     rpack = rp;
                     break;
                  }
               }
            }
            if(rpack == null) {//there's nothing in the incompletePackets array yet
               //add a new packet and return - the packet's incomplete - there's nothing else to do
               incompletePackets.add(new Reliapack(newPacket));
               return true;
            }
         }
      }
      
      //let the finder know that this packet is complete
      finder.packetComplete(newPacket, seqNum);
      
      if(responseSeqNum == 0) { //new packet
         synchronized(newPackets) {
            newPackets.add(rpack);
            newPackets.notify();
         }
         return true;
      }else {
         synchronized(responsePackets) {
            responsePackets.add(rpack);
         }
         
         //go through and wake up anyone waiting on this
         Integer mon = null;
         synchronized(receiveWait) {
            for(int i=0; i<receiveWait.size(); i++) {
               mon = (Integer)receiveWait.get(i);
               if(mon.intValue() == responseSeqNum)
                  break;
               else
                  mon = null;
            }
         }
         if(mon != null)
            synchronized(mon) {
               mon.notify();
            }
            return true;
      }
   }
   
   private synchronized void resendPackets() {
      synchronized(outPackets) {
         Reliapack rpack;
         for(int i=0; i<outPackets.size(); i++) {
            rpack = (Reliapack)outPackets.get(i);
            
            if(System.currentTimeMillis() > rpack.getRetryTime() && rpack.idleTime() > RETRY_WAIT) {
               rpack.setRetryTime(rpack.getRetryTime()+RETRY_WAIT);
               try {
                  debug(55, "resending packet\n");
                  DatagramPacket [] grams = rpack.getDatagrams();
                  for(int j=0; j<grams.length; j++)
                     if(grams[j] != null)
                        socket.send(grams[j]);
               }catch(Exception e) {
                  debug(20, "error resending packet\n");
               }
            }
         }
      }
   }
   
   private void garbageCollect() {
      Reliapack rp;
      synchronized(outPackets) {
         for(int i=0; i<outPackets.size(); i++) {
            rp = (Reliapack)outPackets.get(i);
            if(rp.idleTime() > RETRY_TIME) {
               outPackets.remove(i);
               i--;
            }
         }
      }
      
      synchronized (newPackets) {
         for(int i=0; i<newPackets.size(); i++){
            rp = (Reliapack)newPackets.get(i);
            if(rp.idleTime() > GC_TIME) {
               newPackets.remove(i);
               i--;
            }
         }
      }
      
      synchronized (responsePackets) {
         for(int i=0; i<responsePackets.size(); i++){
            rp = (Reliapack)responsePackets.get(i);
            if(rp.idleTime() > GC_TIME) {
               responsePackets.remove(i);
               i--;
            }
         }
      }
      
      synchronized(incompletePackets) {
         for(int i=0; i<incompletePackets.size(); i++){
            rp = (Reliapack)incompletePackets.get(i);
            if(rp.idleTime() > GC_TIME) {
               incompletePackets.remove(i);
               i--;
            }
         }
      }
   }
   
   private synchronized int waitTime() {
      if(outPackets.isEmpty())
         return 50;
      long minTime = 50;
      for(int i=0; i<outPackets.size(); i++)
         minTime = Math.min(minTime, ((Reliapack)outPackets.get(i)).getRetryTime()
         -System.currentTimeMillis());
      if(minTime <= 0)
         minTime = 1;
      return (int) minTime;
   }
   
   public static void debug(int level, String msg) {
      if(level < DEBUG)
         System.out.print(msg);
   }
   
   public boolean wasReceived(int seqNum) {
      Reliapack rp;
      synchronized(outPackets) {
         for(int i=0; i<outPackets.size(); i++) {
            rp = (Reliapack)outPackets.get(i);
            if(rp.getSeqNum() == seqNum)
               return false;
         }
      }
      return true;
   }
   
   //by using a circular buffer, it lets me know if a seq num has been used recently
   private class RGHost {
      private static final int LEN = 250;
      private String address;
      private int port;
      public int [] seqNums;
      public long [] times;
      
      public RGHost(String address, int port) {
         this.address = address;
         this.port = port;
         seqNums = new int [LEN];
         times = new long[LEN];
         for(int i=0; i<seqNums.length; i++)
            seqNums[i] = 0;
      }
      
      public java.lang.String getAddress() {
         return address;
      }
      
      public int getPort() {
         return port;
      }
      
      public boolean seenRecently(int seqNum) {
         int index = seqNum % LEN;
         return seqNums[index] == seqNum && (System.currentTimeMillis()-times[index] < 2*RETRY_TIME);
      }
      
      public void addNum(int seqNum) {
         int index = seqNum % LEN;
         seqNums[index] = seqNum;
         times[index] = System.currentTimeMillis();
      }
   }
   
   private class RGHostFinder {
      private Vector hosts;
      
      public RGHostFinder() {
         hosts = new Vector();
      }
      
      public RGHost find(DatagramPacket pack) {
         String address = pack.getAddress().getHostAddress();
         int port = pack.getPort();
         RGHost host = null;
         for(int i=0; i<hosts.size(); i++) {
            RGHost tmp = (RGHost) hosts.get(i);
            if(address.equals(tmp.getAddress()) && port == tmp.getPort()) {
               host = tmp;
               break;
            }
         }
         if(host == null) {
            //System.out.println("adding new host");
            host = new RGHost(address, port);
            hosts.add(host);
         }
         return host;
      }
      
      //returns true if the sequence number has been seen before
      public boolean seenRecently(DatagramPacket pack, int seqNum) {
         RGHost host = find(pack);
         return host.seenRecently(seqNum);
      }
      
      //tell the finder that a packet with this sequence number is complete
      public void packetComplete(DatagramPacket pack, int seqNum) {
         RGHost host = find(pack);
         host.addNum(seqNum);
      }
   }
}