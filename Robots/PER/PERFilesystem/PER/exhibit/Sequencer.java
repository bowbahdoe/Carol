/*
 * Sequencer.java
 *
 * Created on January 15, 2003, 9:28 AM
 */

package PER.exhibit;

import PER.rover.*;
import java.util.Vector;
import java.awt.image.BufferedImage;

/**
 * Controls the sequence of actions in a rover mission.
 *
 * @author  Rachel Gockley
 * @version 3.0
 */
public class Sequencer {
    /** no missions have been run; sequencer is waiting for a new mission */
    public static final int NO_MISSION = 0;
    /** mission is still running */
    public static final int RUNNING = 1;
    /** mission completed successfully */
    public static final int SUCCESS = 2;
    /** mission terminated early: no rock was found */
    public static final int NO_ROCK = 3;
    /** mission terminated early: tried to leave the exhibit */
    public static final int INCOMPLETE = 4;
    /** mission terminated early: ran into something early (also for e-stop) */
    public static final int INTERRUPTED = 5;
    /** mission ended due to rover error */
    public static final int ERROR = 6;
    /** mission ended due to a possible error. More checks are required
     * to determine if this is a real error. For example when a driveToAction
     * is interrupted within the buffer distance, it is not really an error. If this
     * is the sequencer status, the mission should not be considered complete.
     */
    public static final int POSSIBLE_ERROR = 7;
    
    private Rover r;
    
    private Action currAction;
    
    private boolean stop;
    private int status;
    
    private boolean gotMission;
    private Object missionLock;
    
    private int angle, dist;
    private boolean findRock;
    
    private int lastMissionTime;
    private int lastMissionDist;
    private int lastMissionDistToRock;
    private int lastMissionAngleToRock;
    private BufferedImage lastMissionImage;
    private BufferedImage lastMissionNoUVImage;
    
    private PER.exhibit.GUI.MissionProgressPanel mpp;
    
    /** Creates a new instance of Sequencer */
    public Sequencer(Rover rov) {
        r = rov;
        stop = false;
        status = NO_MISSION;
        angle = dist = 0;
        lastMissionTime = 0;
        lastMissionDist = lastMissionDistToRock = 0;
        lastMissionAngleToRock = 0;
        currAction = null;
        mpp = null;
        
        missionLock = new Object();
        gotMission = false;
        
        Thread myThread = new Thread() {
            public void run() {
                runThread();
            }
        };
        myThread.start();
        
        Log.println("Sequencer: initialized.");
    }
    
    /** This is the run() function of the sequencer's main thread. */
    private void runThread() {
        boolean retval;
        long startTime = 0;
        
        while (true) {
            // clean up the previous mission
            lastMissionTime = (int)((System.currentTimeMillis() - startTime) );
            if (mpp != null){
                mpp.setMissionCompleted();
            }
            // wait for a mission
            while (!gotMission) {
                synchronized (missionLock) {
                    try {
                        missionLock.wait();
                    } catch (Exception e) {
                    }
                }
            }
            
            Log.println("Sequencer: starting a new mission (turn: " + angle + ", drive: " + dist + ", find rock: "+ findRock +").");
            
            gotMission = false;
            stop = false;
            
            startTime = System.currentTimeMillis();
            lastMissionDist = 0;
            lastMissionDistToRock = 0;
            lastMissionAngleToRock = 0;
            lastMissionImage = null;
            
            // execute a turn action
            if(angle != 0){
                currAction = new TurnToAction(angle);
                if (mpp != null)
                    mpp.startTurn(currAction);
                retval = executeAction(currAction);
                
                if (!retval) {
                    status = ERROR;
                    continue;
                }
                
                //this block of code will move the rover away if it is close to a rock
                int [] nearestObstacle = ((TurnToAction)currAction).getClosestObstacle();
                if(nearestObstacle != null && nearestObstacle[1] < 18) {
                    int wheelAngle = angle > 0 ? -80 : 80;
                    DriveToAction dta = new DriveToAction(nearestObstacle[1]-20, wheelAngle, false);
                    //System.out.println("Avoiding by moving "+(nearestObstacle[1]-20)+" cm "+nearestObstacle[0]);
                    dta.doAction(r);
                    while(!dta.isCompleted())
                        try{Thread.sleep(10);}catch(Exception e) {}
                }
            }
            
            // execute a drive action
            if(dist != 0){
                currAction = new DriveToAction(dist);
                if (mpp != null)
                    mpp.startDrive(currAction);
                retval = executeAction(currAction);
                
                lastMissionDist += r.state.getDist();
                
                if (!retval) {
                    // check for whether interrupted or incomplete, and set status appropriately
                    if (currAction.getReturnValue() == ActionConstants.OBSTACLE_DETECTED){
                        if(lastMissionDist < dist - 150){
                            status = INTERRUPTED;
                            continue;
                        }else{
                            PER.rover.Log.println("Interrupted within 150 cm of desired distance. Probably at target rock.",true);
                            //update dist to rock for report
                            lastMissionDistToRock = lastMissionDist - dist; //negative
                        }
                    }else{
                        //some other rover error
                        status = ERROR;
                        continue;
                    }
                    // might want to back up and/or turn around
                    //continue;
                }
            }
            
            //check if the rover should search for a rock or not
            if(!findRock){
                status = SUCCESS;
                continue;
            }
            
            // execute a find rock action - and tell it if the go to already found a rock
            if(currAction == null)
                currAction = new FindRockAction(false);
            else
                currAction = new FindRockAction(currAction.getReturnValue() == ActionConstants.OBSTACLE_DETECTED);
            if (mpp != null)
                mpp.startFind(currAction);
            retval = executeAction(currAction);
            
            //if(((FindRockAction)currAction).hasFoundRock()){
            if(retval){
                lastMissionAngleToRock = ((FindRockAction)currAction).getRockAngle(); //get this below as well?
            }
            
            if (!retval){
                if (currAction.getReturnValue() == ActionConstants.HIT_WALL){
                    status = INCOMPLETE; //out of bounds
                    continue;
                }else if (currAction.getReturnValue() == ActionConstants.NO_ROCK){
                    status = NO_ROCK;
                    continue;
                }else{
                    status = ERROR;
                    continue;
                }
            }
           /* if (!retval) {
                status = NO_ROCK;
                continue;
            }
            if(!retval){
                status = ERROR;
                continue;
            }*/
            
            // approach the rock
            currAction = new ApproachAction((FindRockAction)currAction);
            if (mpp != null)
                mpp.startApproach(currAction);
            retval = executeAction(currAction);
            
            lastMissionDist += r.highLevelState.getDist();
            lastMissionDistToRock += r.highLevelState.getDist();
            //get angle to rock here from ApproachAction? //***CHANGEME***//
            //lastMissionAngleToRock = ((ApproachAction)currAction).getRockAngle();
            
            if (!retval) {
                if(currAction.getReturnValue() == ActionConstants.NO_ROCK){
                    status = NO_ROCK;
                    continue;
                }else if (currAction.getReturnValue() == ActionConstants.HIT_WALL){
                    status = INCOMPLETE; //out of bounds
                    continue;
                }else if (currAction.getReturnValue() == ActionConstants.OBSTACLE_DETECTED){
                    PER.rover.Log.println("Obstacle detected during rock approach. Assume it is the target rock.",true);
                }else{
                    status = ERROR;
                    continue;
                }
            }
            
            // execute an analyze rock action
            //***randomize here*** //***CHANGEME***//
            java.util.Random rand = new java.util.Random();
            //boolean light = rand.nextBoolean();
            boolean light = true;
            currAction = new AnalyzeRockAction(light, 320, 240); // back up in this action
            if (mpp != null)
                mpp.startAnalyze(currAction);
            retval = executeAction(currAction);
            
            //set the final image
            lastMissionImage = ((AnalyzeRockAction)currAction).getImage();
            lastMissionNoUVImage = ((AnalyzeRockAction)currAction).getNoUVImage();
            
            if (!retval) {
                if(currAction.getReturnValue() == ActionConstants.NO_ROCK)
                    status = NO_ROCK;
                else
                    status = ERROR;
                continue;
            }
            
            status = SUCCESS;
        }
    }
    
    /** Begin execution of a mission. Missions are always of the form:
     *  <ol>
     *    <li>turn the given number of degrees
     *    <li>drive the given number of centimeters
     *    <li>search for a nearby rock
     *    <li>if a rock is found, drive toward it and 'analyze' it
     *  </ol>
     *
     * @param turnDegrees the number of degrees to turn (greater than 0 is left)
     * @param driveCm     the number of centimeters to drive
     * @return a boolean indicating whether the mission began (false implies that something
     *         else is already running; call <code>stop</code> first)
     */
    public synchronized boolean runMission(int turnDegrees, int driveCm) {
        return runMission(turnDegrees, driveCm, true);
    }
    
    /** Begin execution of a mission. Missions are always of the form:
     *  <ol>
     *    <li>turn the given number of degrees
     *    <li>drive the given number of centimeters
     *    <li>if findRock, search for a nearby rock
     *    <li>if a rock is found, drive toward it and 'analyze' it
     *  </ol>
     *
     * @param turnDegrees the number of degrees to turn (greater than 0 is left)
     * @param driveCm     the number of centimeters to drive
     * @param findRock    indicates whether or not to search for a rock at the end of
     * the mission
     * @return a boolean indicating whether the mission began (false implies that something
     *         else is already running; call <code>stop</code> first)
     */
    public synchronized boolean runMission(int turnDegrees, int driveCm, boolean findRock) {
        if (status == RUNNING) {
            Log.println("Sequencer: still running previous mission; cannot start a new one!");
            return false;
        }
        
        synchronized (missionLock) {
            angle = turnDegrees;
            dist = driveCm;
            this.findRock = findRock;
            gotMission = true;
            status = RUNNING; // so that the status is correct when this call returns
            if (mpp != null)
                mpp.resetPanel();
            missionLock.notify();
        }
        return true;
    }
    
    /** Return the current status of the sequencer (for exmaple: <code>RUNNING</code>). */
    public synchronized int getStatus() {
        return status;
    }
    
    /** hack for displaying error message when error in panorama */
    public synchronized void setStatus(int s){
        status = s;
    }
    
    /** Immediately stop the current mission. */
    public synchronized void stop() {
        Log.println("Sequencer: got a stop command!");
        stop = true;
    }
    
    /** Returns the Action that is currently being executed, or null if
     *  no such Action exists. Returns the last Action to begin execution,
     *  if the Mission failed.
     */
    public synchronized Action getCurrentAction() {
        return currAction;
    }
    
    /** Set the MissionProgressPanel that the sequencer should use as a visual display
     * of the current mission's status.
     */
    public synchronized void setMissionProgressPanel(PER.exhibit.GUI.MissionProgressPanel panel) {
        mpp = panel;
    }
    
    /** Return the time (in milliseconds) that the last mission took to complete. */
    public synchronized int getLastMissionTime() {
        return lastMissionTime;
    }
    
    /** Return the distance (in centimeters) that the rover traveled in the last mission.
     * This includes any distance to get to a rock, beyond the distance the rover was told
     * to travel.
     */
    public synchronized int getLastMissionDist() {
        return lastMissionDist;
    }
    
    /** Return the distance (in centimeters) that the rover traveled beyond the original
     * distance it was told. Returns the absolute value of the distance so that the number
     * is always positive even if the rover stopped short.
     */
    public synchronized int getLastMissionDistToRock() {
        return Math.abs(lastMissionDistToRock);
    }
    
    /** Returns the angle (in degrees) that the rover had to turn to find the rock,
     * after turning and driving as told.
     */
    public synchronized int getLastMissionAngleToRock() {
        return lastMissionAngleToRock;
    }
    
    /** Returns the image taken by AnalyzeRock or, if that image is null,
     * the last image taken.
     */
    public BufferedImage getLastMissionImage(){
        if(lastMissionImage == null)
            return r.takeRecentPicture();
        return lastMissionImage;
    }
    /** Returns the image taken by AnalyzeRock or, if that image is null,
     * the last image taken.
     */
    public BufferedImage getLastMissionNoUVImage(){
        if(lastMissionNoUVImage == null)
            return r.takeRecentPicture();
        return lastMissionNoUVImage;
    }
    
    /** returns true iff the action completes successfully. */
    private boolean executeAction(Action a) {
        Log.println("Sequencer: about to execute action: " + a.getSummary(), true);
        
        boolean started;
        if (stop)
            return false;
        
        started = a.doAction(r);
        if (!started)
            return false;
        
        status = RUNNING;
        while (!stop && !a.isCompleted()) 
            try { Thread.sleep(10); } catch (Exception e) {}
        
        
        if (stop) {
            Log.println("Sequencer.executeAction: stopping the current action!");
            ActionConstants.logErrorStats(ActionConstants.KILLED);
            a.kill();
        }
        
        Log.println("Sequencer.executeAction: action completed (" + ActionConstants.getErrorText(a.getReturnValue()) + ").", true);
        //log stats for errors
        if(!a.isSuccess()){
            ActionConstants.logErrorStats(a.getReturnValue());
            status = POSSIBLE_ERROR;
        }
        
        return (!stop && a.isSuccess());
    }
    
}
