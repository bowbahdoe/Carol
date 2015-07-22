/*
 * Rover.java
 *
 * Created on December 17, 2002
 */

package PER.rover;

import PER.rover.control.RoverController;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.awt.Color;


/**
 * High-level control of the Rover.
 *
 * @author  Rachel Gockley
 *
 */
public class Rover extends RoverController {
    
    /** Tells my code whether we are using the mars rover or the prototype */
    public final static boolean USING_MARS_ROVER = true;
    
    /** Directory for saved files, logs etc. */
    public static String topLevelDir = PER.PERConstants.filesystemPath+"SavedRoverFiles";
    
    /** Default IP address */
    public static String defaultIP = "192.168.2.xx";
    
    /** The current IP address being used */
    private String IP = defaultIP;
    
    /* private Thread saveThread; */
    
    
    /** Creates a new Rover */
    public Rover() {
        super();
        
      /*
       // save the schedule and library every half hour
        saveThread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(30*60*1000);
                } catch (Exception e) {
                }
            }
        };
        //saveThread.start(); //this caused the schedule to be saved before it was loaded, hence erasing the schedule
       */
    }
    
    /*public void startSaveThread(){
        saveThread.start();
    }*/
    
    /** Sets the top level directory and makes sure the new directory exists.
     * If the directory does not exist, it is created. The top level directory
     * is the directory for saved rover files and logs.
     */
    public static void setTopLevelDir(String s) {
        topLevelDir = s;
        //make sure top level directory exists
        File f = new File(topLevelDir);
        if(!f.exists()){
            f.mkdirs();
        }
    }
    
    /** Returns a String that is the path for top level directory. The top
     * level directory is the directory for saved rover files and logs.
     */
    public static String getTopLevelDir() {
        return topLevelDir;
    }
    
    /** Returns the IP address currently being used. */
    public String getCurrentIP() {
        return IP;
    }
    
    /** Sets the current IP address being used. */
    public void setCurrentIP(String ip) {
        this.IP = ip;
    }
    
    /** Loads the last used IP address from a saved file. If the file can not
     * be opened, returns <code>defaultIP</code>.
     *
     *@return the last IP address saved or <code>defaultIP</code>
     */
    public static String getDefaultIP() {
        // load the default IP address
        try {
            FileInputStream istream = new FileInputStream(new File(topLevelDir, "lastIP"));
            ObjectInputStream p = new ObjectInputStream(istream);
            
            String IPaddress = (String)p.readObject();
            
            istream.close();
            
            return IPaddress;
        } catch (Exception e) {
            Log.println("Loading the IP failed: " + e);
            return defaultIP;
        }
    }
    
    /** Saves the given IP address to a file so that it can be loaded the
     * next time the program is executed.
     *
     *@see #getDefaultIP
     */
    public static void saveIP(String IP) {
        try {
            FileOutputStream ostream = new FileOutputStream(new File(topLevelDir, "lastIP"));
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            
            p.writeObject(IP);
            
            p.flush();
            ostream.close();
        } catch (Exception e) {
            Log.println("Saving the IP failed!");
        }
    }
    
    /** Returns the current system time in milliseconds.
     * @return The current system time in Milliseconds
     */
    public long getTime(){
        return System.currentTimeMillis();
    }
    
    /**
     * Saves a given BufferedImage to disk as a jpg at <code>filename</code>.
     * The <code>filename</code> string should be a complete path, for example:
     *<p>
     * <ul>
     *  <code>"c:/PERFilesystem/SavedRoverFiles/name.jpg"</code><br>
     *  or <code>Rover.getTopLevelDir()+"/name.jpg"</code>
     * </ul>
     *
     * @param filename Path at which to save the image
     * @param picture Image to save
     * @return false if the filename cannot be written to, or picture is NULL, or other error
     */
    public boolean saveImageToDisk(String filename, BufferedImage picture){
        try {
            File f = new File(filename);
            return saveImageToDisk(f,picture);
        }catch(Exception e){
            Log.println("Error writing to file "+filename+": "+e,true);
            return false;
        }
    }
    
    /**
     * Saves a given BufferedImage to a file as a jpg.
     *
     * @param f The file where the image should be saved
     * @param picture Image to save
     * @return false if the file cannot be written to, picture is NULL, or other error
     */
    public boolean saveImageToDisk(File f, BufferedImage picture){
        try {
            //make sure that the file directory structure exists
            if(!f.exists()){
                f.mkdirs();
            }
            //ensure that the file extension is jpg or jpeg
            String extension = Filter.getExtension(f);
            if(extension == null
            || (!extension.equalsIgnoreCase("jpg")
            && !extension.equalsIgnoreCase("jpeg"))){
                f = new File(f.getAbsolutePath()+".jpg");
            }
            
            //write the image to the file
            ImageIO.write(picture,"jpg",f);
        }catch(Exception e){
            Log.println("Error writing to file "+f+": "+e,true);
            return false;
        }
        return true;
    }
    
    /**
     * Sleeps for <code>duration</code> milliseconds.
     * @param duration Time to sleep, in milliseconds.
     * @return false if sleep was interrupted
     */
    public boolean sleep(long duration){
        try{
            Thread.sleep(duration);
        }catch(Exception e) {
            return false;
        }
        return true;
    }
    
    /**
     * Schedules the specified task for execution at the specified time.
     * @param task TimerTask function to execute
     * @param time Time at which to execute the function
     * @return null on error
     * @see TimerTask
     * @see Timer
     * @see Date
     */
    public Timer executeFunctionAtTime(TimerTask task, Date time ){
        Timer timer = new Timer();
        try{
            timer.schedule(task, time);
        }catch(Exception e){ return null; }
        return timer;
    }
    
    /**
     * Schedules the specified task for repeated fixed-delay execution, beginning
     * at the specified time.
     * @param task TimerTask function to execute
     * @param firstTime The time to begin execution
     * @param period Rate at which to execute the function, in milliseconds.
     * @param fixedRate True if task should be executed relative to the initial start time,
     *        false if it should be executed relative to when the last task ended.
     * @return null on error
     * @see TimerTask
     * @see Timer
     * @see Date
     */
    public Timer executeFunctionTimerAtTime(TimerTask task, Date firstTime, long period, boolean fixedRate ){
        Timer timer = new Timer();
        try{
            if(fixedRate)
                timer.scheduleAtFixedRate(task, firstTime, period);
            else
                timer.schedule(task, firstTime, period);
        }catch(Exception e){ return null; }
        return timer;
    }
    
    /**
     * Schedules the specified task for execution after the specified delay.
     * @param task TimerTask function to execute
     * @param delay Time to wait before executing the function, in milliseconds.
     * @return null on error
     * @see TimerTask
     * @see Timer
     */
    public Timer executeFunctionDelay(TimerTask task, long delay ){
        Timer timer = new Timer();
        try{
            timer.schedule(task, delay);
        }catch(Exception e){ return null; }
        return timer;
    }
    
    /**
     * Schedules the specified task for repeated fixed-delay execution, beginning after the specified delay.
     * @param task TimerTask function to execute
     * @param delay Time to wait before executing the function, in milliseconds.
     * @param period Rate at which to execute the function, in milliseconds.
     * @param fixedRate True if task should be executed relative to the initial start time,
     *        false if it should be executed relative to when the last task ended.
     * @return null on error
     * @see TimerTask
     * @see Timer
     */
    public Timer executeFunctionTimerDelay(TimerTask task, long delay, long period, boolean fixedRate ){
        Timer timer = new Timer();
        try{
            if(fixedRate)
                timer.scheduleAtFixedRate(task, delay, period);
            else
                timer.schedule(task, delay, period);
        }catch(Exception e){ return null; }
        return timer;
    }
    
    /**
     * Finishes executing a timer function.
     * @param timer The timer to kill.
     * @see Timer
     */
    public void stopExecuteFunction(Timer timer){
        if(timer != null)
            timer.cancel();
    }
    
    /** Returns a TimerTask that takes pictures and saves them. The returned
     * TimerTask is suitable for the task scheduling functions to execute.
     * This can be used as the task for executeFunctionTimerAtTime
     * or executeFunctionTimerDelay.
     *
     * @param pan The pan value in degrees at which to take the picture
     * @param tilt The tilt vale in degrees at which to take the picture
     * @param width The width of the image in pixels
     * @param height The height of the image in pixels
     * @param UV True if the UV light should be on for the picture, false otherwise
     * @param pathPrefix The path where time-lapse pictures should be saved.  They
     *    will get a suffix of a sequence number plus ".jpg"
     *
     * @return a TimerTask which can be used as the task for
     *    executeFunctionTimerAtTime or executeFunctionTimerDelay
     *
     * @see TimerTask
     * @see #executeFunctionTimerAtTime
     * @see #executeFunctionTimerDelay
     */
    public TimerTask timeLapse(int pan, int tilt, int width, int height, boolean UV, String pathPrefix){
        return timeLapse(pan,tilt,width,height,UV,pathPrefix,null);
    }
    
    /** Returns a TimerTask that takes pictures and displays them to a specified
     * JLabel. The returned TimerTask is suitable for the task scheduling
     * functions to execute. This can be used as the task for
     * executeFunctionTimerAtTime or executeFunctionTimerDelay.
     *
     * @param pan The pan value in degrees at which to take the picture
     * @param tilt The tilt vale in degrees at which to take the picture
     * @param width The width of the image in pixels
     * @param height The height of the image in pixels
     * @param UV True if the UV light should be on for the picture, false otherwise
     * @param displayLabel An optional JLabel to display the images to as they
     *    are taken. 
     *
     * @return a TimerTask which can be used as the task for
     *    executeFunctionTimerAtTime or executeFunctionTimerDelay
     *
     * @see TimerTask
     * @see #executeFunctionTimerAtTime
     * @see #executeFunctionTimerDelay
     */
    public TimerTask timeLapse(int pan, int tilt, int width, int height, boolean UV, javax.swing.JLabel displayLabel){
        return timeLapse(pan,tilt,width,height,UV,null,displayLabel);
    }
    
    /** Returns a TimerTask that takes pictures, saves them to file,
     * and displays them to a specified JLabel. The returned
     * TimerTask is suitable for the task scheduling functions to execute.
     * This can be used as the task for executeFunctionTimerAtTime
     * or executeFunctionTimerDelay.
     *
     * @param pan The pan value in degrees at which to take the picture
     * @param tilt The tilt vale in degrees at which to take the picture
     * @param width The width of the image in pixels
     * @param height The height of the image in pixels
     * @param UV True if the UV light should be on for the picture, false otherwise
     * @param pathPrefix An optional path where time-lapse pictures should be saved.
     *    If <code>pathPrefix</code> is <code>null</code>, pictures will not be saved.
     *    If <code>pathPrefix</code> is not <code>null</code>, pictures will be saved
     *    with a suffix of a sequence number plus ".jpg"
     * @param displayLabel An optional JLabel to display the images to as they
     *    are taken. <code>displayLabel</code> can be null if images are not 
     *    to be displayed.
     *
     * @return a TimerTask which can be used as the task for
     *    executeFunctionTimerAtTime or executeFunctionTimerDelay
     *
     * @see TimerTask
     * @see #executeFunctionTimerAtTime
     * @see #executeFunctionTimerDelay
     */
    public TimerTask timeLapse(int pan, int tilt, int width, int height, boolean UV, String pathPrefix, javax.swing.JLabel displayLabel){
        
        class MyTask extends TimerTask {
            int myPan; int myTilt; int myWidth; int myHeight;
            boolean myUV; BufferedImage myImage;
            int myCount; String myPathPrefix; boolean waiting;
            javax.swing.JLabel myLabel;
            public MyTask(int pan, int tilt, int width, int height, boolean UV, String pathPrefix, javax.swing.JLabel displayLabel){
                myPan = pan; myTilt = tilt; myWidth = width; myHeight = height; myUV = UV;
                myPathPrefix = pathPrefix;
                myCount = 0; waiting = false;
                myLabel = displayLabel;
            }
            public void run() {
                if(!waiting){
                    waiting = true;
                    myImage = takePicture(myPan, myTilt, myWidth, myHeight, myUV);
                    if(myPathPrefix != null)
                        saveImageToDisk(myPathPrefix + (myCount++) + ".jpg", myImage);
                    if(myLabel != null){
                        myLabel.setIcon(new javax.swing.ImageIcon(myImage));
                    }
                    waiting = false;
                }
            }
        }
        return new MyTask(pan, tilt, width, height, UV, pathPrefix, displayLabel);
    }
    
    
}
