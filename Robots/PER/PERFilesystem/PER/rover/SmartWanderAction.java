/*
 * SmartWanderAction.java
 *
 * Created on April 7, 2004, 4:42 PM
 */

package PER.rover;

import PER.rover.control.RoverState;

/**
 * Allows the rover to explore an area without running in to things. The rover will
 * drive in a straight line until it encounters an obstacle, then it turns on the
 * light to examine the obstacle. The rover then chooses a random angle, checks 
 * if the new heading is unobstructed, and drives in the new direction. If the rover
 * does not see a clear area after checking several angles, it will turn 
 * in the hope of finding a better path from the new vantage point.
 *
 *
 * @author Emily Hamner
 */

public class SmartWanderAction implements Action {
    /** The length of time to wander in seconds. */
    private int secs;
    
    /** The time this action takes to complete in milliseconds. */
    private int time;
    
    private DriveToAction driveAct;
    private TurnToAction turnAct;
    private TurnHeadAction turnHeadAct;
    private java.util.Random rand;
    private boolean takePics;
    /** The last time an image was taken by the <code>examine</code> function, if the action takes pictures. */
    private long examImageTime;
    /** The last image taken by the <code>examine</code> function, if the action takes pictures. */
    private java.awt.image.BufferedImage examImage;
    
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success;
    transient private int ret;
    transient private boolean quit = false;
    transient private long starttime;
    
    /** Creates a new instance of SmartWanderAction that takes pictures
     * as it moves. Use Rover.receive.getRecentImage() to get the pictures
     * as the robot is driving.
     *
     * @param seconds the length of time to wander in seconds
     */
    public SmartWanderAction(int seconds) {
        this(seconds, true);
    }
    
    /** Creates a new instance of SmartWanderAction. If the rover is taking pictures,
     * use Rover.receive.getRecentImage() to get the pictures as the robot
     * is driving.
     *
     * @param seconds the length of time to wander in seconds
     * @param takePictures if true, the rover will take pictures while
     * turning and driving
     */
    public SmartWanderAction(int seconds, boolean takePictures) {
        this.secs = seconds;
        time = secs * 1000;
        takePics = takePictures;
        driveAct = new DriveToAction(Integer.MAX_VALUE,0,DriveToAction.CYCLE_SAFETY,takePics);
        turnAct = new TurnToAction(0,takePics);
        turnHeadAct = new TurnHeadAction(0,-5);
        rand = new java.util.Random();
        examImageTime = 0;
        examImage = null;
    }
    
    public boolean doAction(Rover r) {
        //PER.rover.StatsLog.println(PER.rover.StatsLog.DRIVE,dist);
        rov = r;
        myThread = new Thread() {
            public void run() {
                starttime = System.currentTimeMillis();
                
                quit = false;
                success = wander();
                
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
    
    public int getReturnValue() {
        return ret;
    }
    
    public String getShortSummary() {
        return "Smart Wander.";
    }
    
    public String getSummary() {
        return "Smart wander for "+secs+" seconds.";
    }
    
    public int getTime() {
        return time;
    }
    
    public int getTimeRemaining() {
        return time - (int)((System.currentTimeMillis() - starttime) );
    }
    
    public boolean isCompleted() {
        return myThread != null && !myThread.isAlive();
    }
    
    /** Returns true if the time limit expired without the occurance
     * of any errors.
     */
    public boolean isSuccess() {
        return success;
    }
    
    public void kill() {
        quit = true;
    }
    
    private boolean wander(){
        int ang = 0;
        double d = 0;
        boolean obs = false;
        int count = 0;
        
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
            if(driveAct.isSuccess()
            || driveAct.getReturnValue() == RoverState.OBSTACLE_DETECTED){
                //examine the obstacle
                examine();
                
                count = 0;
                do{
                    ////turn randomly
                    ////d = rand.nextGaussian(); //bias towards larger turns
                    
                    //select a random angle between 10 and 180 (or -180 and -10)
                    d = rand.nextDouble();
                    ang = (int)(d * 170) + 10;
                    if(rand.nextBoolean())
                        ang *= -1;
                    
                    //turn the head to check for obstacles before turning to the new angle
                    turnHeadAct.setPan(ang);
                    turnHeadAct.doAction(rov);
                    while(!turnHeadAct.isCompleted()){
                        if(quit){
                            turnHeadAct.kill();
                            ret = RoverState.KILLED;
                            return false;
                        }
                        try{Thread.sleep(20);}catch(Exception e) {}
                    }
                    if(!turnHeadAct.isSuccess()){
                        //error occurred during head turn
                        ret = turnHeadAct.getReturnValue();
                        return false;
                    }
                    
                    //slight pause after turning the head - looks more natural
                    try{Thread.sleep(500+Math.abs(ang));}catch(Exception e) {}
                    rov.refresh();
                    
                    //get IR reading and check for obstacle
                    int cm = rov.state.getRangeCM();
                    if (cm > 50)
                        obs = false;
                    else obs = true;
                    count++;
                    //System.out.println("count "+count+" ang "+ang+"; cm "+cm+"; obs "+obs);
                }while(obs == true && count < 7);
                
                //turn
                turnAct.setAngle(ang);
                turnAct.doAction(rov);
                while(!turnAct.isCompleted()){
                    if(quit){
                        turnAct.kill();
                        ret = RoverState.KILLED;
                        return false;
                    }
                    try{Thread.sleep(20);}catch(Exception e) {}
                }
                if(!turnAct.isSuccess()){
                    //error occurred during turn
                    ret = turnAct.getReturnValue();
                    return false;
                }
                
                //check for obstacles seen during the turn and move away?
                
            }else{
                //error occurred during drive
                ret = driveAct.getReturnValue();
                return false;
            }
        }
        
        ret = RoverState.SUCCESS;
        return true;
    }
    
    /** "Examines" the obstacle in front of the robot by turning on the light,
     * taking a picture (if this action is one that takes pictures), and tilting
     * the head.
     */
    private void examine(){
        //turn on light
        rov.setLight(true);
        //take picture
        if(takePics){
            examImage = rov.takePicture(rov.state.getPan(),rov.state.getTilt(),320,240,true);
            examImageTime = System.currentTimeMillis();
        }
        //tilt head to simulate looking at object
        rov.setTilt(15);
        try{Thread.sleep(1000);}catch(Exception e) {}
        rov.setTilt(-35);
        try{Thread.sleep(1000);}catch(Exception e) {}
        //turn off light
        rov.setLight(false);
    }
    
    public long getImageUpdateTime() {
        if(rov != null){
            if(examImageTime != 0){
                return Math.max(examImageTime, rov.receive.getImageUpdateTime());
            }else return rov.receive.getImageUpdateTime();
        }else return 0;
    }
    
    public java.awt.image.BufferedImage getRecentImage() {
        if (rov != null){
            if(examImageTime > rov.receive.getImageUpdateTime()){
                return examImage;
            }else return rov.receive.getRecentImage();
        }else return null;
    }
    
}
