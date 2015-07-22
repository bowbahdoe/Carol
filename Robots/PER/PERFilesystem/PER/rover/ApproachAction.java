/*
 * ApproachAction.java
 *
 * Created on August 19, 2003, 9:43 AM
 */

package PER.rover;
import PER.rover.control.*;

/**
 * Scans to find the exact location of a rock, turns to align precisely,
 * and then drives up to the rock.
 *
 * @author  Eric Porter
 */
public class ApproachAction implements Action {
    private int time;
    private FindRockAction fra;
    
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success = false, completed = false;
    transient private int ret = 0;
    transient private boolean quit = false;
    transient private long starttime = 0;
    transient private int [] scanVals;
    
    /** Creates a new instance of ApproachAction
     * @param scan A completed FindRockAction that has found the rock.  If these
     * requirements are not met, running this aciton will result in error.
     */
    public ApproachAction(FindRockAction scan) {
        fra = scan;
        if(scan == null || !scan.hasFoundRock())
            time = 0;
        else
            time = 2000 + (84*Math.abs(scan.getRockAngle()) +
            236*Math.max(scan.getRockDist()-20, 10));
    }
    
    public boolean doAction(Rover r) {
        PER.rover.StatsLog.println(PER.rover.StatsLog.APPROACH_ROCK);
        
        if(fra == null || !fra.hasFoundRock()) {
            ret = RoverState.BAD_INPUT;
            completed = true;
            success = false;
            return false;
        }
        rov = r;
        myThread = new Thread() {
            public void run() {
                success = false;
                
                starttime = System.currentTimeMillis();
                
                quit = false;
                ret = approach();
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
        return "Approach the rock";
    }
    
    public String getSummary() {
        return "Approach rock by turning to face it then driving up to it";
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
    
    public boolean isSuccess() {
        return success;
    }
    
    public void kill() {
        quit = true;
    }
    
    private int approach() {
        int turnAngle = (fra.getMinRockAngle()+fra.getMaxRockAngle())/2;
        fra.moveFromRock(rov, turnAngle);
        TurnToAction tta = new TurnToAction(turnAngle);
        tta.doAction(rov);
        while(!tta.isCompleted()) {
            if(quit)
                tta.kill();
            try{Thread.sleep(20);}catch(Exception e) {}
        }
        if(!tta.isSuccess())
            return tta.getReturnValue();
        
        //now drive up to the rock
        //DriveToAction dta = new DriveToAction(hsa.getRockDist());
        DriveToAction dta = new DriveToAction(fra.getRockDist());
        dta.doAction(rov);
        while(!dta.isCompleted()) {
            if(quit)
                dta.kill();
            try{Thread.sleep(20);}catch(Exception e) {}
        }
        if(dta.isSuccess() || dta.getReturnValue() == RoverState.OBSTACLE_DETECTED)
            return RoverState.SUCCESS;
        return dta.getReturnValue();
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