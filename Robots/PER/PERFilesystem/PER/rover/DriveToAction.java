/*
 * DriveToAction.java
 *
 * Created on June 20, 2002, 10:20 AM
 */

package PER.rover;
import PER.rover.control.*;
/**
 * Drives the trikebot a specified distance, after first turning by a set
 * angle (which can be 0).
 *
 * @author  Eric Porter
 * @version 3.1
 */
public class DriveToAction implements Action {
    
    private int dist, angle;
    private int time;
    private boolean takingPictures;
    private byte safety;
    
    transient private boolean quit = false;
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success = false;
    transient private int ret;
    transient private long starttime;
    
    private static final double MAX_NONFATAL_ERRORS = 2;
    
    /** The safety level where no checking is done */
    public static final byte NO_SAFETY = 0;
    /** The safety level where it will cycle through the specified safety points */
    public static final byte CYCLE_SAFETY = 1;
    /** The safety level where it will keep the head at pan=0, tilt=-35 */
    public static final byte STATIC_SAFETY = 2;
    
    /**
     * Creates a new DriveToAction that moves straight ahead
     * taking pictures with safety on.
     *
     * @param fwd     centimeters to move forward (after rotating)
     */
    public DriveToAction(int fwd) {
        this(fwd, 0, true);
    }
    
    
    /**
     * Creates a new DriveToAction that takes pictures.
     * Use Rover.receive.getRecentImage() to get the pictures as the robot
     * is driving.
     *
     * @param fwd     centimeters to move forward
     * @param ang   the angle to drive at (0=straight, 90=left, -90=right)
     * @param safety  if true, rover will stop based on the range finder
     */
    public DriveToAction(int fwd, int ang, boolean safety) {
        this(fwd, ang, safety ? CYCLE_SAFETY : NO_SAFETY, true);
    }
    
    /**
     * Creates a new DriveToAction.
     * Use Rover.receive.getRecentImage() to get the pictures as the robot
     * is driving.
     *
     * @param fwd     centimeters to move forward
     * @param ang   the angle to drive at (0=straight, 90=left, -90=right)
     * @param safetyLevel  CYCLE_SAFETY, STATIC_SAFETY, or NO_SAFETY
     * @param takePics if true, the rover will take pictures as it drives
     */
    public DriveToAction(int fwd, int ang, byte safetyLevel, boolean takePics) {
        dist = fwd;
        angle = ang;
        safety = safetyLevel;
        takingPictures = takePics;
        
        success = false;
        
        ret = 0;
        starttime = 0;
        //time = (200+Math.abs(dist)*311);  // time to drive, plus 5cm/s
        setTime();
    }
    
    /** Sets the distance to the given cm value and recalculates how long
     * the action will take.
     */
    public void setDistance(int distance) {
        dist = distance;
        setTime();
    }
    
    /** Sets the angle to the given degree (0=straight, 90=left, -90=right).
     */
    public void setAngle(int ang){
        angle = ang;
    }
    
    public boolean doAction(Rover r) {
        PER.rover.StatsLog.println(PER.rover.StatsLog.DRIVE,dist);
        rov = r;
        myThread = new Thread() {
            public void run() {
                starttime = System.currentTimeMillis();
                
                quit = false;
                success = doit();
                
                long endtime = System.currentTimeMillis();
                if (success)
                    time = (int)((endtime - starttime) );
            }
        };
        
        myThread.start();
        for(int i=0; i<10 && !myThread.isAlive(); i++)
            Thread.yield();
        return true;
    }
    
    public int getTime() {
        return time;
    }
    
    /** Sets the time relative to the current value of <code>dist</code>. This should
     * possibly calculate in the current value of <code>angle</code> also.
     */
    private void setTime(){
        time = (200+Math.abs(dist)*311);  // time to drive, plus 5cm/s
    }
    
    public String getSummary() {
        if (dist == 0)
            return "Drive nowhere.";
        if (angle != 0)
            return "Turn the wheels to " + angle + '\u00b0' + " and drive " + dist + "cm.";
        return "Drive " + dist + "cm.";
    }
    
    public String getShortSummary() {
        return "Drive " + dist + "cm at " + angle + '\u00b0' + ".";
    }
    
    public int getReturnValue() {
        // return 0 on success
        if (success)
            return 0;
        else
            return ret;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    private boolean doit() {
        //rov.MoveHeadTo(0, -15);
        
        for(int i=0; !rov.goTo(dist, angle, safety, takingPictures); i++) {
            if(rov.highLevelState.isTerminationCondition() || i >= MAX_NONFATAL_ERRORS) {
                ret = rov.highLevelState.getStatus();
                return false;
                //return (ret == ActionConstants.OBSTACLE_DETECTED); // If we stop short, we still scan.  Don't return error! (mblain)
            }
        }
        
        int sendFailures = 0;
        do {
            if(quit) //quit is true if there was a request to kill the action
                rov.killHighLevel();
            else //otherwise, just get an update
                rov.updateHighLevel();
            
            try{Thread.sleep(50);}catch(Exception e) {}
            
            if(rov.highLevelState.getStatus() == RoverState.HL_CONTINUE)
                sendFailures = 0;
            else if(rov.highLevelState.getStatus() != RoverState.SUCCESS &&
            ++sendFailures >= MAX_NONFATAL_ERRORS){
                rov.killHighLevel(); //prevent rover from running away
                ret = rov.highLevelState.getStatus();
                return false;
            }
        }while(!rov.highLevelState.isTerminationCondition());
        
        ret = rov.highLevelState.getStatus();
        return ret == RoverState.SUCCESS;
    }
    
    private void killIfNecessary(int status) {
        if(status != RoverState.SUCCESS && status != RoverState.KILLED &&
        status != RoverState.CEREB_TIMEOUT && status != RoverState.OBSTACLE_DETECTED)
            rov.killHighLevel();
    }
    
    public boolean isCompleted() {
        return myThread != null && !myThread.isAlive();
    }
    
    public void kill() {
        quit = true;
    }
    
    public int getTimeRemaining() {
        return time - (int)((System.currentTimeMillis() - starttime) );
    }
    
    public long getImageUpdateTime() {
        if(rov != null)
            return rov.receive.getImageUpdateTime();
        else return 0;
    }
    
    public java.awt.image.BufferedImage getRecentImage() {
        if (rov != null)
            return rov.receive.getRecentImage();
        else return null;
    }
    
}
