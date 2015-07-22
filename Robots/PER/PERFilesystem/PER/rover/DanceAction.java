/*
 * DanceAction.java
 *
 * Created on May 1, 2004, 2:17 PM
 */

package PER.rover;
import PER.rover.control.*;

/**dumb action which just calls some movement commands
 *
 * @author  Brian
 */
public class DanceAction implements PER.rover.Action {
    
    private DriveToAction driveAct;
    private TurnToAction left;
    private TurnToAction right;
    private int time;
    private int secs;
    
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success;
    transient private int ret;
    transient private boolean quit = false;
    transient private long starttime;
    
    public DanceAction(int seconds){
        this.secs = seconds;
        time = secs * 1000;
        driveAct = new DriveToAction(5);
        left = new TurnToAction(10);
        right = new TurnToAction(-10);
    }
    
    /**
     * Tries to start the action. Returns whether the action started.
     */
    public boolean doAction(Rover r){  // should be blocking
        rov = r;
        myThread = new Thread() {
            public void run() {
                starttime = System.currentTimeMillis();
                
                quit = false;
                success = dance();
                
                long endtime = System.currentTimeMillis();
                if (success)
                    time = (int)((endtime - starttime) );
            }
        };
        
        myThread.start();
        for(int i=0; i<10 && !myThread.isAlive(); i++)
            Thread.yield();
        return true;
    };
    /**
     * Emergency stop - end the action immediately, if it's running.
     */
    public void kill(){
        quit = true;
    };
    
    public long getImageUpdateTime() {
        return 0;
    }
    
    public java.awt.image.BufferedImage getRecentImage() {
        return null;
    }
    
    /** Whether the action completed successfully. Undefined until isCompleted() returns
     * true.
     */
    public boolean isSuccess(){
        return success;
    };
    
    /** Whether the action has completed. Undefined until doAction has been called.
     */
    public boolean isCompleted(){
        return myThread != null && !myThread.isAlive();
    };
    
    /** How long the action will take, in milliseconds. */
    public int getTime(){
        return time;
    };
    
    /** How much time until the action finishes (in milliseconds), if it has already
     * started. Undefined behavior if the action has not yet begun, or has finished.
     */
    public int getTimeRemaining(){
        return time - (int)((System.currentTimeMillis() - starttime) );
    };
    /** The return value of the Action. Zero is a success. Negative implies one
     * of this class's constants. Other values should be interpreted as
     * appropriate.
     */
    public int getReturnValue(){
        return ret;
    };
    
    /** Provides a textual explanation of the Action, such as "turn 90 degrees" */
    public String getSummary(){
        return "Dancing for "+secs+" seconds.";
    };
    
    /** Provides a shortened version of the summary returned by getSummary. For
     * example, getSummary may return something like "Turn left and drive about
     * 39 inches toward the red landmark," whereas getShortSummary might just
     * return "Drive toward a landmark."
     */
    public String getShortSummary(){
        return "Dancing.";
    };
    
    private boolean dance(){
        int ang = 0;
        double d = 0;
        
        while(getTimeRemaining() > 0){
            
            //drive until an obstacle
            driveAct.doAction(rov);
            while(!driveAct.isCompleted()){
                if(quit){
                    driveAct.kill();
                    ret = RoverState.KILLED;
                    return false;
                }
                try{Thread.sleep(20);}catch(Exception e) {}
            }
            driveAct.doAction(rov);
            while(!driveAct.isCompleted()){
                if(quit){
                    driveAct.kill();
                    ret = RoverState.KILLED;
                    return false;
                }
                try{Thread.sleep(20);}catch(Exception e) {}
            }
            if(driveAct.isSuccess()
            || driveAct.getReturnValue() == RoverState.OBSTACLE_DETECTED){
                
                left.doAction(rov);
                while(!left.isCompleted()){
                    if(quit){
                        left.kill();
                        ret = RoverState.KILLED;
                        return false;
                    }
                    try{Thread.sleep(20);}catch(Exception e) {}
                }
                if(!left.isSuccess()){
                    //error occurred during turn
                    ret = left.getReturnValue();
                    return false;
                }
                
                right.doAction(rov);
                while(!right.isCompleted()){
                    if(quit){
                        right.kill();
                        ret = RoverState.KILLED;
                        return false;
                    }
                    try{Thread.sleep(20);}catch(Exception e) {}
                }
                if(!right.isSuccess()){
                    //error occurred during turn
                    ret = right.getReturnValue();
                    return false;
                }
                
            }else{
                //error occurred during drive
                ret = driveAct.getReturnValue();
                return false;
            }
        }
        
        ret = RoverState.SUCCESS;
        return true;};
}
