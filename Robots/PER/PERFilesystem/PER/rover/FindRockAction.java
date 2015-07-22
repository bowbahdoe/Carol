/*
 * FindRockAction.java
 *
 * Created on November 5, 2003, 2:03 PM
 */

package PER.rover;
import PER.rover.control.*;

/**An action to search for a rock intelligently.  If the rover had to stop
 * early because it detected an obstacle, the scan will cover a smaller range.
 * If the first scan, which only looks short
 *
 * @author  Eric Porter
 */
public class FindRockAction implements PER.rover.Action {
    
    private int time;
    private boolean obstacleDetect;
    
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success = false, completed = false;
    transient private int ret = 0;
    transient private boolean quit = false;
    transient private long starttime = 0;
    transient private ScanAction successfulScanAction = null;
    
    /** FindRockAction constructor
     * @param obstacleDetected should be set to true if the rover stopped early
     * because of a detected obstacle.
     */
    public FindRockAction(boolean obstacleDetected) {
        obstacleDetect = obstacleDetected;
        if(obstacleDetected)
            time = 2000;
        else
            time = 5000;
    }
    
    public boolean doAction(Rover r) {
        PER.rover.StatsLog.println(PER.rover.StatsLog.FIND_ROCK);
        
        rov = r;
        myThread = new Thread() {
            public void run() {
                success = false;
                
                starttime = System.currentTimeMillis();
                
                quit = false;
                ret = findRock();
                success = (ret == RoverState.SUCCESS);
                
                if (success) {
                    long endtime = System.currentTimeMillis();
                    time = (int)((endtime - starttime) );
                }
                completed = true;
            };
        };
        myThread.start();
        for(int i=0; i<10 && !myThread.isAlive(); i++)
            Thread.yield();
        return true;
    }
    
    public int getReturnValue() {
        return ret;
    }
    
    public String getShortSummary() {
        return "Tries to find a rock near the rover.";
    }
    
    public String getSummary() {
        return "Looks for rocks by first scanning near the rover, but if that is "+
        "unsuccessful, it will do a long range scan.";
    }
    
    public int getTime() {
        return time;
    }
    
    public int getTimeRemaining() {
        return time - (int)((System.currentTimeMillis() - starttime) );
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    /** Tells you whether the Action completed successfully.  To find out if a rock
     * was found call hasFoundRock();
     * @return true if the action completed successfully
     */
    public boolean isSuccess() {
        return success;
    }
    
    /** this action can't currently be killed */
    public void kill() {
    }
    
    private int findRock() {
        int nearTilt = PER.rover.Rover.USING_MARS_ROVER ? -22 : -18;
        ScanAction nearSA;
        if(obstacleDetect) // if rover stopped because of obstacle, scan a smaller area
            nearSA = new ScanAction(nearTilt, -75, 75, 5);
        else //otherwise, search a larger area
            nearSA = new ScanAction(nearTilt, -135, 135, 5);
        nearSA.doAction(rov);
        while(!nearSA.isCompleted())
            try{Thread.sleep(20);}catch(Exception e) {}
        if(quit == true)
            return ActionConstants.KILLED;
        if(!nearSA.isSuccess())
            return nearSA.getReturnValue();
        //if it found the rock and part of the rock is within 45 degrees of center
        if(nearSA.hasFoundRock() && //it has to have found the rock
        ((nearSA.getMinRockAngle() > -45 && nearSA.getMinRockAngle() < 45) ||
        (nearSA.getMaxRockAngle() > -45 && nearSA.getMaxRockAngle() < 45)))
            return rockFoundHelper(nearSA);
        //if the near scan didn't reveal a rock within 45 degrees of center, now
        //do a longer range scan and choose the rock
        
        
        ScanAction farSA = new ScanAction(-8, -135, 135, 3);
        farSA.doAction(rov);
        while(!farSA.isCompleted())
            try{Thread.sleep(20);}catch(Exception e) {}
        if(!farSA.isSuccess())
            return farSA.getReturnValue();
        
        if(nearSA.hasFoundRock() && !farSA.hasFoundRock()) //near scan found rock, far one didn't
            return rockFoundHelper(nearSA);
        else if(!nearSA.hasFoundRock() && farSA.hasFoundRock()) //far scan found rock, near one didn't
            return rockFoundHelper(farSA);
        else if(nearSA.hasFoundRock() && farSA.hasFoundRock()) { //they both found rocks - choose one closer to straight
            if(Math.abs(nearSA.getRockAngle()) <= Math.abs(farSA.getRockAngle()))
                return rockFoundHelper(nearSA);
            else
                return rockFoundHelper(farSA);
        }
        rov.look(0, 0);
        if(farSA != null && farSA.hasDetectedWall())
            return ActionConstants.HIT_WALL;
        else
            return ActionConstants.NO_ROCK;
    }
    
    private int rockFoundHelper(ScanAction succ) {
        successfulScanAction = succ;
        rov.look(succ.getRockAngle(), 0);
        return RoverState.SUCCESS;
    }
    
    
    /** Tells you whether the FindRockAction found a rock
     * @return true if rover found a rock, only valid once action is complete.
     */
    public boolean hasFoundRock() {
        return successfulScanAction != null;
    }
    
    /** Getter for property rockAngle.
     * @return The angle to the nearest rock in degrees, but only if hasFoundRock() == true.
     *
     */
    public int getRockAngle() {
        if(successfulScanAction == null)
            return 0;
        return successfulScanAction.getRockAngle();
    }
    
    /** Getter for property rockDist.
     * @return The distance to the nearest rock in cm, but only if hasFoundRock() == true.
     */
    public int getRockDist() {
        if(successfulScanAction == null)
            return 0;
        return successfulScanAction.getRockDist();
    }
    
    /** Gives you the minimum angle at which the rock was found
     * @return the minimum rock angle, but only if hasFoundRock() == true.
     */
    public int getMinRockAngle() {
        if(successfulScanAction == null)
            return 0;
        return successfulScanAction.getMinRockAngle();
    }
    
    /** Gives you the maximum angle at which the rock was found
     * @return the maximum rock angle, but only if hasFoundRock() == true.
     */
    public int getMaxRockAngle() {
        if(successfulScanAction == null)
            return 0;
        return successfulScanAction.getMaxRockAngle();
    }
    
    /** Gives you the minimum rock distance in cm.
     * @return the minimum rock distance, but only if hasFoundRock() == true.
     */
    public int closestReading() {
        if(successfulScanAction == null)
            return 0;
        return successfulScanAction.closestReading();
    }
    
    /** If the rover is near a rock and will turn into it, this function should
     *  make it so that the rover won't hit the rock
     * @param rov An active instantion of a Rover
     * @param turnAngle the angle that the rover is about to turn
     */
    public int moveFromRock(Rover rov, int turnAngle) {
        if(successfulScanAction == null)
            return RoverState.SUCCESS;
        return successfulScanAction.moveFromRock(rov, turnAngle);
    }
    
    /** This action does not take pictures so <code>getImageUpdateTime()</code> will
     * always return 0.
     */
    public long getImageUpdateTime() {
        return 0;
    }
    
    /** This action does not take pictures so <code>getRecentImage()</code> will
     * always return null.
     */
    public java.awt.image.BufferedImage getRecentImage() {
        return null;
    }
    
}
