/*
 * DetectMotionAction.java
 *
 * Created on April 27, 2004, 9:29 AM
 */

package PER.rover;

import PER.rover.control.*;

/**
 * This Action makes it easy to detect motion.  The version of the Stargate/Stayton
 * code on the robot you use must be at least 2.0.0.
 * <p>
 * While trying to detect motion, the rover is constantly taking pictures
 * at 176x144.  It adds up the brightness in 16x16 pixel blocks.  The rover sends 
 * back the average difference between successive frames for all of the blocks.
 * The DetectMotionAction waits until the average difference is above the defined
 * threshold for at least a minimum number of blocks, or the specified timeout
 * is exceeded. The Action is considered a success if motion is detected
 * before the timeout is reached.
 *
 *
 * @author  Eric Porter
 */
public class DetectMotionAction implements PER.rover.Action {
    
    private int time;
    private int thresh; //the threshold for it say motion was detected
    private int minBlocks; //how many blocks need to have motion
    
    //If I don't get anything from the rover for TIMEOUT ms, quit.
    private final static int TIMEOUT = 5000;
    
    transient private boolean quit = false;
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success = false;
    transient private int ret;
    transient private long starttime;
    
    /** Creates a new instance of DetectMotionAction.
     *  Waits to detect motion in the specified time interval using default parameters.
     *  By default, 5 blocks need to change in brightness by at least 8 for
     *  it to be considered motion.
     *  @param timeToWait How many milliseconds to detect motion for.
     */
    public DetectMotionAction(int timeToWait) {
        this(timeToWait, 8, 5);
    }
    
    
    /** Creates a new instance of DetectMotionAction
     *  Waits to detect motion in the specified time interval using user-defined parameters.
     *  @param timeToWait How many milliseconds to detect motion for.
     *  @param threshold How much the brightness must change in a block for
     *  it to be considered motion.
     *  @param minimumBlocks How many blocks must be above the threshold for
     *  the Action to terminate.
     */
    public DetectMotionAction(int timeToWait, int threshold, int minimumBlocks) {
        time = timeToWait;
        thresh = threshold;
        minBlocks = minimumBlocks;
    }
    
    public boolean doAction(Rover r) {
        //PER.rover.StatsLog.println(PER.rover.StatsLog.DRIVE,dist);
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
    
    /** Gets the maximum time that it will try to detect motion for in milliseconds.
     *  @return If the action has not completed, the amount of time the user allowed
     *   the action to run is returned.  If the action has succeeded, the actual time
     *   run is returned.
     */
    public int getTime() {
        return time;
    }
    
    public String getSummary() {
        return "Determines if motion occured during a period of "+time+" milliseconds.  "+
        "At least "+minBlocks+" must have a brightness difference of "+thresh+".";
    }
    
    public String getShortSummary() {
        return "Detects motion for "+time+" milliseconds.";
    }
    
    public int getReturnValue() {
        // return 0 on success
        if (success)
            return 0;
        else
            return ret;
    }
    
    /** For this action, success is considered detecting motion.  This is only
     *  defined if isCompleted() returns true.
     * @return true only if motion was detected in the time interval.
     */
    public boolean isSuccess() {
        return success;
    }
    
    private boolean doit() {
        int ret = -1;
        //if we can't start
        if(!rov.startMotionDetection()) {
            ret = rov.state.getStatus();
            return false;
        }
        
        //register this class so it will be notified when there is new motion
        if(!rov.receive.registerObject(this, ReceiveThread.MOTION_RECEIVE)) {
            ret = RoverState.RESOURCE_CONFLICT; //an error should NEVER occur
            rov.stopStreaming();
            return false;
        }
        
        //The system time when this action should end if no motion was detected
        long endTime = System.currentTimeMillis() + time;
        
        do {
            if(quit) {//quit is true if there was a request to kill the action
                if(rov.stopStreaming())
                    ret = RoverState.KILLED;
                else
                    ret = rov.state.getStatus();
                break;
            }
            
            synchronized(this) {
                try{
                    //this should wake me up when there is new data, or the timeout expires
                    wait(Math.min(TIMEOUT, endTime-System.currentTimeMillis()));
                }catch(Exception e) {}
            }
            
            //if nothing new has come in last TIMEOUT ms
            if(System.currentTimeMillis() - rov.receive.getMotionUpdateTime() > TIMEOUT) {
                rov.stopStreaming();
                ret = RoverState.COMM_DEAD;
                break;
            }
            
            //count how many blocks are above the threshold
            int numAboveThresh = 0;
            for(int i=0; i<rov.receive.motion.length; i++)
                if(rov.receive.motion[i] > thresh)
                    numAboveThresh++;
            //if at least minBlocks are above the threshold, return success
            if(numAboveThresh >= minBlocks) {
                rov.stopStreaming();
                ret = RoverState.SUCCESS;
                break;
            }
            
        }while(System.currentTimeMillis() < endTime);
        
        //timeout case
        if(System.currentTimeMillis() >= endTime && ret == -1) {
            rov.stopStreaming();
            ret = ActionConstants.NO_MOTION_DETECTED;
        }
        
        rov.receive.unregisterObject(this, ReceiveThread.MOTION_RECEIVE);
        return ret == RoverState.SUCCESS;
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
        return 0;
    }
    
    public java.awt.image.BufferedImage getRecentImage() {
        return null;
    }
    
}
