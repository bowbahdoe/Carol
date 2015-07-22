/*
 * DanceAction.java
 *
 * Created on May 1, 2004, 2:17 PM
 */

package PER.rover;
import PER.rover.control.*;

/**Action which moves to a particular location
 * and orientation specified in cartesian coordinates
 *
 * @author  Brian
 */
public class MoveToAction implements PER.rover.Action {
    
    private MoveToAction moveToAct;
    private DriveToAction driveAct;
    private TurnToAction turnActInitial;
    private TurnToAction turnActFinal;
    private int time;
    
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success;
    transient private int ret;
    transient private boolean quit = false;
    transient private long starttime;
    
    int destinationX, destinationY, destinationTheta;
    
    /**Constructor
     * @param destX the X coordinate of the destination
     * @param destY the Y coordinate of the destination
     * @param destTheta the angle of the destination
     */
    
    public MoveToAction(int destX, int destY, int destTheta){
        destinationX = destX;
        destinationY = destY;
        destinationTheta = destTheta;    
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
        return "Moving to x: " + " y: " + " theta: ";
    };
    
    /** Provides a shortened version of the summary returned by getSummary. For
     * example, getSummary may return something like "Turn left and drive about
     * 39 inches toward the red landmark," whereas getShortSummary might just
     * return "Drive toward a landmark."
     */
    public String getShortSummary(){
        return "MovingTo.";
    };
    
    private boolean dance(){
        if(rov.highLevelState.getPosition() == null) System.out.println("position null");
        
        double currentPosition[] = rov.highLevelState.getPosition();
        int currentThetaInt = (int) currentPosition[2];
        int distance = 0;
        
        int needsMovedAngle = (int) Math.toDegrees(Math.atan2(-currentPosition[1] + destinationY, -currentPosition[0] + destinationX));
        if((((int)destinationX - (int)rov.highLevelState.getPosition()[0]) == 0)&&(((int)destinationY - (int)rov.highLevelState.getPosition()[1]) == 0)){
       //    System.out.println("we're where we wanted to go, making first turn zero");
             needsMovedAngle = 0;
        }            

        turnActInitial = new TurnToAction(needsMovedAngle - currentThetaInt);
        
        turnActInitial.doAction(rov);
        while(!turnActInitial.isCompleted()){
            if(quit){
                turnActInitial.kill();
                ret = RoverState.KILLED;
                return false;
            }
            try{Thread.sleep(20);}catch(Exception e) {}
        }
        if(!turnActInitial.isSuccess()){
            //error occurred during turn
            ret = turnActInitial.getReturnValue();
            return false;
        }
        
        if(((Math.abs((int)rov.highLevelState.getPosition()[2])) > 45)&&((Math.abs((int)rov.highLevelState.getPosition()[2])) < 135)){
        //    System.out.println("higher angle case");
            distance = (int) Math.abs((destinationY - rov.highLevelState.getPosition()[1]) / Math.sin(Math.toRadians((int)(rov.highLevelState.getPosition()[2]))));
        }else{
            distance = (int) Math.abs((destinationX - rov.highLevelState.getPosition()[0]) / Math.cos(Math.toRadians((int)(rov.highLevelState.getPosition()[2]))));
        }

        driveAct = new DriveToAction(distance);
        
        driveAct.doAction(rov);
        while(!driveAct.isCompleted()){
            if(quit){
                driveAct.kill();
                ret = RoverState.KILLED;
                return false;
            }
            try{Thread.sleep(20);}catch(Exception e) {}
        }
  
        turnActFinal = new TurnToAction((int)(destinationTheta - rov.highLevelState.getPosition()[2]));
        
        turnActFinal.doAction(rov);
        while(!turnActFinal.isCompleted()){
            if(quit){
                turnActFinal.kill();
                ret = RoverState.KILLED;
                return false;
            }
            try{Thread.sleep(20);}catch(Exception e) {}
        }
        if(!turnActFinal.isSuccess()){
            //error occurred during turn
            ret = turnActFinal.getReturnValue();
            return false;
        }
        
        ret = RoverState.SUCCESS;
        return true;};
}
