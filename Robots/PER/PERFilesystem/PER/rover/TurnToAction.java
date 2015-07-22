/*
 * TurnToAction.java
 *
 * Created on June 20, 2002, 9:33 AM
 */

package PER.rover;
import PER.rover.control.RoverState;

/**
 * Turns the Rover a specified number of degrees, dead-reckoned.
 *
 * @author  Eric Porter
 * @version 2.0
 */
public class TurnToAction implements Action {
    
    private int angle;
    private int time;
    private boolean takePics;
    
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success;
    transient private int ret;
    transient private boolean quit = false;
    transient private long starttime;
    transient private int [] scanArr, nearestObstacle = null;
    
    private static final double MAX_NONFATAL_ERRORS = 2;
    
    /** Creates new TurnToAction that takes pictures.
     *
     * @param degrees the angle in degrees of how far to turn
     */
    public TurnToAction(int degrees) {
        this(degrees, true);
    }
    
    /** Creates new TurnToAction
     *
     * @param degrees the angle in degrees of how far to turn
     * @param takePictures whether or not to take pictures while moving
     */
    public TurnToAction(int degrees, boolean takePictures) {
        angle = degrees;
        takePics = takePictures;
        success = false;
        ret = 0;
        starttime = 0;
        time = (300+Math.abs(degrees)*100);
        scanArr = new int[Math.abs(degrees)+1];
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean doAction(Rover r) {
        PER.rover.StatsLog.println(PER.rover.StatsLog.TURN,angle);
        rov = r;
        myThread = new Thread() {
            public void run() {
                success = false;
                
                starttime = System.currentTimeMillis();
                
                quit = false;
                success = turn();
                
                boolean isFirstNonzero = true;
                for(int i=scanArr.length-1; i>=0; i--) {
                    int dist = ScanAction.translateScan(scanArr[i]);
                    //System.out.println("i: "+i+" raw: "+scanArr[i]+" dist: "+dist);
                    double pan = Math.toRadians(scanArr.length-1 - i);
                    if(dist >= 20 && dist < 55) {
                        if(isFirstNonzero)
                            break;
                        nearestObstacle = new int []{(int)(dist*Math.cos(pan)), (int)(dist*Math.sin(pan))};
                        //System.out.println("the close obstacle has coords: "+nearestObstacle[0]+", "+nearestObstacle[1]);
                        break;
                    }
                    if(scanArr[i] > 0)
                        isFirstNonzero = false;
                }
                
                if (success) {
                    long endtime = System.currentTimeMillis();
                    time = (int)((endtime - starttime) );
                }
            };
        };
        myThread.start();
        for(int i=0; i<10 && !myThread.isAlive(); i++)
            Thread.yield();
        return true;
    }

    /** Sets the angle to turn to to the given value (in degrees).
     */
    public void setAngle(int ang) {
        scanArr = new int[Math.abs(ang)+1];
        angle = ang;
    }
    
    public int getTime() {
        return time;
    }
    
    public String getSummary() {
        if (angle == 0)
            return "Turn zero degrees.";
        
        if (angle == 180 || angle == -180)
            return "Turn around.";
        
        if (angle > 0)
            return "Turn " + (int)angle + " degrees to the left.";
        
        return "Turn " + -1 * (int)angle + " degrees to the right.";
    }
    
    public String getShortSummary() {
        return "Turn " + (int)angle + " degrees";
    }
    
    public int getReturnValue() {
        // degrees turned
        //return ret;
        
        if (success)
            return 0;
        else
            return ret;
    }
    
    private boolean turn() {
        rov.look(0, -15);
        
        for(int i=0; !rov.turnTo(angle, takePics); i++) {
            PER.rover.Log.println("turn failure! "+rov.highLevelState.getStatus(),true);
            if(rov.highLevelState.isTerminationCondition() || i >= MAX_NONFATAL_ERRORS) {
                ret = rov.highLevelState.getStatus();
                return false;
            }
        }
        
        int sendFailures = 0;
        do {
            if(quit) //quit is true if there was a request to kill the action
                rov.killHighLevel();
            else //otherwise, just get an update
                rov.updateHighLevel();
            
            try{Thread.sleep(50);}catch(Exception e) {}
            
            if(rov.highLevelState.getDist() <= Math.abs(angle))
                scanArr[rov.highLevelState.getDist()] = rov.highLevelState.getRange();
            
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
    
    /** This function returns the closest obstacle to straight ahead.
     * @return an array in the form of x, y in cm, or null if no obscacle detected.
     */
    public int [] getClosestObstacle() {
        return nearestObstacle;
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
