/*
 * ActionTemplate.java
 *
 * Created on //insert date here
 */

package PER.rover;

import PER.rover.control.*;

/**
 * Template for creating an Action. Replace all instances of "ActionTemplate"
 * with the name of your Action. You will want to modify the ActionTemplate
 * constructor and the doit, setTime, getSummary, and getShortSummary methods.
 * If your Action takes pictures, you will also want to modify the getRecentImage
 * and getImageUpdateTime methods.  
 *
 *@see PER.rover.Action
 *
 * @author  //insert name here
 */
public class ActionTemplate implements Action {
    private int time;
    transient private boolean quit = false;
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success = false;
    transient private int ret;
    transient private long starttime;
    
    
    /**
     * Creates a new ActionTemplate.
     */
    public ActionTemplate( /* Specify arguments */ ) {
        /* initialization */
        
        setTime();
        success = false;
        ret = 0;
        starttime = 0;
    }
    
    
    public boolean doAction(Rover r) {
        rov = r;
        
        myThread = new Thread() {
            public void run() {
                starttime = System.currentTimeMillis();
                ret = RoverState.SUCCESS;
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
    
    /** Does the task specified by this Action.
     *
     * @return true when the task completes successfully, false if there is 
     * an error or the action is killed
     */
    private boolean doit() {
        /* Specify the task to be done by this action. Make sure that
         * if quit is ever set to true, the task will stop. */
        
        if(quit){ //quit is true if there was a request to kill the action
            ret = RoverState.KILLED;
            return false;
        }
        
        ret = rov.state.getStatus();
        return ret == RoverState.SUCCESS;
    }
    
    public int getTime() {
        return time;
    }
    
    private void setTime(){
        /* Specify how long the action is expected to take in milliseconds.
         * This could be fixed or dependant on an action variable such as
         * how far the rover has to drive. */
        time = 0;
    }
    
    public String getSummary() {
        /* Specify a complete summary string for the action. */
        return "ActionTemplate";
    }
    
    public String getShortSummary() {
        /* Specify a brief summary string for the action. */
        return "ActionTemplate";
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
    
    public boolean isCompleted() {
        return myThread != null && !myThread.isAlive();
    }
    
    public void kill() {
        quit = true;
    }
    
    public int getTimeRemaining() {
        return time - (int)((System.currentTimeMillis() - starttime) );
    }

    public java.awt.image.BufferedImage getRecentImage(){
        //If your action takes pictures, return the latest picture here
        return null;
    }
    
    public long getImageUpdateTime(){
        //If your action takes pictures, return the last time a picture was taken
        return 0;
    }

}