/*
 * ReceiveThread.java
 *
 * Created on October 19, 2003, 3:18 PM
 */

package PER.rover.control;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.Vector;

/** Contains the thread class for receiving and parsing  
 * information returned by the rover during color tracking, mean
 * color detection, and motion detection as well as pictures taken during
 * turnTo and driveTo commands. Also contains public variables and methods
 * for accessing this data
 *
 * @author  Eric Porter
 */
public class ReceiveThread extends Thread{
   
   /** use this variable in the [un]registerObject functions to be notified of new pictures */
   public static final int PICTURE_RECEIVE = 0;
   /** use this variable in the [un]registerObject functions to be notified of new tracking data */
   public static final int TRACK_RECEIVE = 1;
   /** use this variable in the [un]registerObject functions to be notified of new mean color data */
   public static final int MEAN_RECEIVE = 2;
   /** use this variable in the [un]registerObject functions to be notified of new motion data */
   public static final int MOTION_RECEIVE = 3;
   private static final int NUM_RCVS = 4;
   
   /** When tracking, this is the minimum x-coordinate where the object was detected */
   public int minX;
   /** When tracking, this is the minimum y-coordinate where the object was detected */
   public int minY;
   /** When tracking, this is the maximum x-coordinate where the object was detected */
   public int maxX;
   /** When tracking, this is the maximum y-coordinate where the object was detected */
   public int maxY;
   /** When tracking, this is the mean x-coordinate where the object was detected */
   public int x;
   /** When tracking, this is the mean y-coordinate where the object was detected */
   public int y;
   /** When tracking, this is the total number of pixels tracked */
   public int pixels;
   
   /** Updated by getMean, this is the average Y value.  The Y-channel 
    corresponds to intensity. */
   public int meanY;
   /** Updated by getMean, this is the average U value.  The U-channel roughly 
    corresponds to how blue the object is. */
   public int meanU;
   /** Updated by getMean, this is the average V value.  The V-channel roughly 
    corresponds to how red the object is. */
   public int meanV;
   
   /** This array stores the difference in intensity between frames.  It is 99 elements
    * in size. Each element in the array represents the average intensity difference
    * for a 16 by 16 block of pixels. A single frame is 11 blocks wide and 9 blocks
    * high (176x144 pixels). The data is stored row-wise, starting from the upper-left
    * block. Check <code>getMotionUpdateTime()</code> to see when it was last updated.
    */
   public int []motion; //stores an array with 9 rows and 11 columns row wise
   
   private Reliagram rgram;
   private RoverState state;
   private boolean keepGoing = true;
   private long javaPicTime = 0;
   private long javaTrackTime = 0;
   private long javaMeanTime = 0;
   private long javaMotionTime = 0;
   private long lastStargateTime = 0, lastJavaTime=0;
   private BufferedImage image;
   
   private static final int OLD_TRACK_SIZE = 16; //size of tracking data without pan/tilt
   private static final int TRACK_SIZE = 24; //size of tracking data with pan/tilt
   private static final int MEAN_SIZE = 9;
   private static final int MOTION_SIZE = 105;
   
   /** Stores the DataNotifyThreads that are to be notified when new data is received.
    * The threads are stored in Vectors by RECEIVE type.
    */
   private Vector [] notifyThreads;
   
   /** Creates a new instance of ReceiveThread */
   public ReceiveThread(Reliagram reliagram, RoverState State) {
      rgram = reliagram;
      state = State;
      notifyThreads = new Vector[NUM_RCVS];
      motion = new int[99];
      this.start();
   }
   
   public void run() {
      while(keepGoing) {
         Datapack pack = rgram.receive();
         if(pack != null && pack.getLength() > 6) {
            //there is a timestamp on the packets sent from the Stargate
            long stargateTime = decodestargateTime(pack.getData());
            //If the time is higher, than is must be newer.  It is possible that the rover
            //was reset, so if I haven't gotten anything for 5 seconds, that must be what happened.
            if(stargateTime > lastStargateTime || System.currentTimeMillis() - lastJavaTime > 5000) {
               lastStargateTime = stargateTime;
               lastJavaTime = System.currentTimeMillis();
               
               //System.out.println("got new packet, time is "+stargateTime+"\t"+System.currentTimeMillis());
               if(pack.getLength() == TRACK_SIZE || pack.getLength() == OLD_TRACK_SIZE) {
                  byte data [] = pack.getData();
                  x = ByteUtil.unsign(data[6]);
                  y = ByteUtil.unsign(data[7]);
                  minX = ByteUtil.unsign(data[8]);
                  minY = ByteUtil.unsign(data[9]);
                  maxX = ByteUtil.unsign(data[10]);
                  maxY = ByteUtil.unsign(data[11]);
                  pixels = ByteUtil.networkLongToInt(data, 12);
                  if(pack.getLength() == TRACK_SIZE) {
                     state.setPan(ByteUtil.networkLongToInt(data, 16));
                     state.setTilt(ByteUtil.networkLongToInt(data, 20));
                  }
                  //System.out.println(minX+" "+maxX+" "+minY+" "+maxY+" "+x+" "+y+" "+pixels);
                  
                  javaTrackTime = lastJavaTime;
                  wakeUpNotifyThreads(TRACK_RECEIVE);
               }else if(pack.getLength() == MEAN_SIZE) {
                  byte data [] = pack.getData();
                  meanY = ByteUtil.unsign(data[6]);
                  meanU = ByteUtil.unsign(data[7]);
                  meanV = ByteUtil.unsign(data[8]);
                  
                  javaMeanTime = lastJavaTime;
                  wakeUpNotifyThreads(MEAN_RECEIVE);
               }else if(pack.getLength() == MOTION_SIZE) {
                  byte data [] = pack.getData();
                  for(int i=0; i<99; i++) {
                     motion[i] = ByteUtil.unsign(data[6+i]);
                     /*System.out.print(motion[i]+"\t");
                     if((i%11) == 10)
                        System.out.println();*/
                  }
                  //System.out.println("");
                  javaMotionTime = lastJavaTime;
                  wakeUpNotifyThreads(MOTION_RECEIVE);
               }else if(pack.getLength() > 1000){ //must be a picture
                  BufferedImage newImage;
                  try {
                     /*FileOutputStream fos = new FileOutputStream("stream/image"+(imagesRead++)+".jpg");
                     fos.write(pack.getData(), 6, pack.getLength()-6);
                     fos.close();*/
                     ByteArrayInputStream bais = new ByteArrayInputStream(pack.getData(), 6, pack.getLength()-6);
                     newImage = ImageIO.read(bais);
                     bais.close();
                  } catch(Exception e) {
                     newImage = null;
                     //System.out.println("Error reading in image! "+System.currentTimeMillis());
                  }
                  if(newImage != null) {
                     image = newImage;
                     /* write image out for webcast */
                /*     try {
                       ImageIO.write(image,"jpg",new java.io.File(rover.Rover.perPath+"rovercam.jpg"));
                             java.io.File oldFile = new java.io.File(rover.Rover.perPath+"rovercam.jpg");
                             java.io.File newFile = new java.io.File(rover.Rover.perPath+"streamcam.jpg");
                           if(oldFile.exists()){
                               newFile.delete();
                               oldFile.renameTo(new java.io.File(rover.Rover.perPath+"streamcam.jpg"));
                       }
                     }catch(java.io.IOException e){ System.out.println("Error writing to file "+rover.Rover.perPath+"rovercam.jpg"); }
                 */
                     javaPicTime = lastJavaTime;
                     wakeUpNotifyThreads(PICTURE_RECEIVE);
                  }/*else {
                  System.out.println("failed up to date check "+System.currentTimeMillis()+"st: "+stargateTime+" spt: "+stargatePicTime);
               }*/
               }
            }
         }
      }
   }
   
   /** Do not call this function.  It stops the thread running for this class.  
    *  This function should only be called when closing the communication in 
    *  RoverController.
    */
   public void quit() {
      keepGoing = false;
   }
   
   /**
    * Gets the most recent image taken on the stargate.
    * @return the most recent picture the stargate has send back.
    */
   public BufferedImage getRecentImage() {
      return image;
   }
   
   /**
    * Lets you know when the last image came back from the rover.
    * @return the system time in ms when the last image came back from the rover.
    */
   public long getImageUpdateTime() {
      return javaPicTime;
   }
   
   /**
    * Lets you know when the last packet containing tracking data came back from 
    * the rover.
    * @return the system time in ms when the last tracking data came back from
    * the rover.
    */
   public long getTrackUpdateTime() {
      return javaTrackTime;
   }
   
   /**
    * Lets you know when the last packet containing mean color data came back from 
    * the rover.
    * @return the system time in ms when the last mean color data came back from
    * the rover.
    */
   public long getMeanUpdateTime() {
      return javaMeanTime;
   }
   
   /**
    * Lets you know when the last packet containing motion data came back from
    * the rover.
    * @return the system time in ms when the last motion data came back from
    * the rover.
    */
   public long getMotionUpdateTime() {
      return javaMotionTime;
   }
   
   /* Because communication takes place over UDP, packets could arrive out of order
    * I'm having the stargate send its time to make sure that I don't replace current
    * information with out of date information.
    */
   private long decodestargateTime(byte [] arr) {
      long seconds = ByteUtil.networkLongToInt(arr, 0);
      long msecs = ByteUtil.networkShortToInt(arr, 4);
      return (seconds << 32) + msecs;
   }
   
   /** Registers an object to be notified when new data arrives that you are interested in.
    *  When new data comes of the type you specify, a lock will be acquired on
    *  that object and then <code>notify()</code> will be called.  See
    *  PER.rover.DetectMotionAction for an example of this function used.
    *  <p>Make sure to call unregisterObject when you are done.  This will free
    *  up the resources associated with notifying.
    *  @param o The object to be registered.
    *  @param type The type of data you want to be notified for.  See the *_RECEIVE
    *  constants from this class: PICTURE_RECEIVE, TRACK_RECEIVE, MEAN_RECEIVE, 
    *  and MOTION_RECEIVE.
    *  @return This function will return false if either the type is invalid or
    *  you try to register the same object twice for the same type.  Otherwise,
    *  it returns true.
    */
   public synchronized boolean registerObject(Object o, int type) {
      if(type < 0 || type >= NUM_RCVS)
         return false;
      if(notifyThreads[type] == null)
         notifyThreads[type] = new Vector();
      
      //don't let them register the same object twice for the same type.
      for(int i=0; i<notifyThreads[type].size(); i++)
         if(((DataNotifyThread) notifyThreads[type].get(i)).getObject() == o)
            return false;
      
      DataNotifyThread dnt = new DataNotifyThread(o);
      notifyThreads[type].add(dnt);
      return true;
   }
   
   /** Unregisters an object that was registered with the registerObject() function.
    *  Calling this function will free up the resources associated with notifying.
    *  @param o The object to be unregistered.
    *  @param type The type of data you want to be notified for.  See the *_RECEIVE
    *  constants from this class: PICTURE_RECEIVE, TRACK_RECEIVE, MEAN_RECEIVE, 
    *  and MOTION_RECEIVE.
    *  @return This function will return true only if the object was successfully
    *  unregistered.
    */
   public synchronized boolean unregisterObject(Object o, int type) {
      if(type < 0 || type >= NUM_RCVS)
         return false;
      if(notifyThreads[type] == null)
         notifyThreads[type] = new Vector();
      
      for(int i=0; i<notifyThreads[type].size(); i++){
         DataNotifyThread dnt = ((DataNotifyThread) notifyThreads[type].get(i));
         if(dnt.getObject() == o) {
            dnt.stopRunning();
            notifyThreads[type].remove(i);
            return true;
         }
      }
      return false;
   }
   
   private synchronized void wakeUpNotifyThreads(int type) {
      if(notifyThreads[type] == null)
         return;
      
      for(int i=0; i<notifyThreads[type].size(); i++){
         DataNotifyThread dnt = ((DataNotifyThread) notifyThreads[type].get(i));
         synchronized(dnt) {
            dnt.notify();
         }
      }
   }
   
   private class DataNotifyThread extends Thread {
      
      private Object obj; //the object that this thread will notify
      private boolean keepGoing = true;
      
      public DataNotifyThread(Object obj_) {
         obj = obj_;
         this.start();
      }
      
      public void run() {
         waitForData();
         while(keepGoing) {
            synchronized(obj) {
               obj.notify();
            }
            waitForData();
         }
      }
      
      public Object getObject() {
         return obj;
      }
      
      public synchronized void stopRunning() {
         keepGoing = false;
         this.notify();
      }
      
      public synchronized void moreData() {
         this.notify();
      }
      
      private synchronized void waitForData() {
         if(keepGoing)
            try{
               this.wait();
            }catch (InterruptedException ie) {}
      }
      
   }
   
}
