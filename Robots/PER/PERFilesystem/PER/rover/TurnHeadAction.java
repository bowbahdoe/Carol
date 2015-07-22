/*
 * TurnHeadAction.java
 *
 * Created on January 24, 2003, 9:42 AM
 */

package PER.rover;

/**
 * Turns the rover's head to an assigned pan and tilt. This action calls Rover.look
 * but if errors occur, it will try up to five times before giving up.
 *
 * @author  Rachel Gockley
 */
public class TurnHeadAction implements Action {
    private int pan, tilt;
    private boolean success;
    private transient Thread myThread;
    
    Rover t;
    
    /** Creates a new instance of TurnHeadAction with pan and tilt set to zero.
     */
    public TurnHeadAction() {
        pan = tilt = 0;
        success = false;
        myThread = null;
    }
    
    /** Creates a new instance of TurnHeadAction that sets the pan and tilt to
     * the given angles.
     */
    public TurnHeadAction(int p, int t) {
        pan = p;
        tilt = t;
        success = false;
    }
    
    public boolean doAction(Rover trike) {
        PER.rover.StatsLog.println(PER.rover.StatsLog.TURN_HEAD);
        success = false;
        //final Rover t = trike;
        t = trike;
        
        myThread = new Thread() {
            public void run() {
                // try up to five times before giving up
                for (int cnt = 0; cnt < 5 && !success; cnt++)
                    success = t.look(pan, tilt);
                
                try { Thread.sleep(500); } catch (Exception e) {}
            }
        };
        
        myThread.start();
        Thread.yield();
        return true;
    }
    
    public int getReturnValue() {
        if (success)
            return 0;
        else{
            if(t != null)
                return t.state.getStatus();
            else return 0;  //this is not a success case but is here to avoid a null pointer exception
            //return ActionConstants.COMM_DEAD;
        }
    }
    
    public String getShortSummary() {
        return "Move the head.";
    }
    
    public String getSummary() {
        return "Pan the head to " + pan + " degrees, and tilt to " + tilt + " degrees.";
    }
    
    /** How long the action will take, in milliseconds. This action always
     * returns 1.
     */
    public int getTime() {
        return 1;
    }
    
    public boolean isCompleted() {
        return (myThread != null && !myThread.isAlive());
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    /** Emergency stop - end the action immediately, if it's running. Not currently
     * implemented for this action.
     */
    public void kill() {
        // nothing to do!
    }
    
    /** Sets the pan angle.
     */
    public void setPan(int p) {
        pan = p;
    }
    
    /** Sets the tilt angle.
     */
    public void setTilt(int t) {
        tilt = t;
    }
    
    /** How much time until the action finishes (in milliseconds), if it has already
     * started. Undefined behavior if the action has not yet begun, or has finished.
     * This action always returns a time remaining of 0.
     */
    public int getTimeRemaining() {
        return 0;
    }
    
    public long getImageUpdateTime() {
        return 0;
    }
    
    public java.awt.image.BufferedImage getRecentImage() {
        return null;
    }
    
}
