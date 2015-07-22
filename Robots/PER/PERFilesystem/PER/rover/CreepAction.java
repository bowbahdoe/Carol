/*
 * CreepAction.java
 *
 * Created on April 21, 2004, 9:53 AM
 */

package PER.rover;

import PER.rover.control.*;
import java.awt.image.BufferedImage;
import PER.rover.ScanAction;

/**
 * Drives the trikebot a specified distance at a specified angle and speed.
 * This action is different from the DriveToAction in that it uses Omnidirectional
 * steering rather than Ackerman steering. The changes in speed are roughly, but not
 * exactly, linear. Therefor, the rover will drive at the specified speed, but
 * the distance covered will only be approximate and will vary slightly depending
 * on the speed. For a set cm distance, the actual distance covered will be more for
 * slower speeds and less for faster speeds.
 *
 * @author  Emily Hamner
 */
public class CreepAction implements Action {
    private int dist, angle;
    /** The speed to drive as a percentage of full speed. */
    private double speed;
    /** The speed to drive as a raw motor pulse width. */
    private int rawSpeed;
    private int time;
    private boolean takingPictures;
    private byte safety;
    /** The ScanAction used to check for obstacles. */
    private ScanAction scan = null;
    /** The last time an image was taken by the action if the action takes pictures. */
    private long imageTime;
    /** The last image taken by the action if the action takes pictures. */
    private BufferedImage image;
    /** The pan angle to use when taking a picture. */
    private int picPan;
    /** The tilt angle to use when taking a picture. */
    private int picTilt;
    
    transient private boolean quit = false;
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success = false;
    transient private int ret;
    transient private long starttime;
    
    /** The safety level where no obstacle checking is done */
    public static final byte NO_SAFETY = 0;
    /** The safety level where the rover will pan its head back and forth to
     * scan for obstacles in its path */
    public static final byte CYCLE_SAFETY = 1;
    /** The safety level where the rover will check for obstacles with
     * its head fixed at pan=angle of travel, tilt=-35 degrees */
    public static final byte STATIC_SAFETY = 2;
    
    /**
     * Creates a new CreepAction that does not take pictures and has safety
     * turned off.
     *
     * @param dist  centimeters to move
     * @param ang   the angle to drive at. The minimum angle is -90 and
     * the maximum angle is 90. Anything outside this range will be set
     * to the closest valid value. (0=straight, 90=left, -90=right)
     * @param speed the speed to drive at. Should be between 0 and 1, where 0 is
     * stopped and 1 is full speed. Anything less than 0 will be treated as 0.
     * Anything greater than 1 will be treated as 1.
     */
    public CreepAction(int dist, int ang, double speed) {
        this(dist,ang,speed,NO_SAFETY,false);
    }
    
    /**
     * Creates a new CreepAction with saftey turned off. If the rover is
     * taking pictures, use getRecentImage to get the pictures taken while
     * the rover is driving. Pictures will be taken at pan = angle of
     * travel and tilt = 0.
     *
     * @param dist     centimeters to move
     * @param ang   the angle to drive at. The minimum angle is -90 and
     * the maximum angle is 90. Anything outside this range will be set
     * to the closest valid value. (0=straight, 90=left, -90=right)
     * @param speed the speed to drive at. Should be between 0 and 1, where 0 is
     * stopped and 1 is full speed. Anything less than 0 will be treated as 0.
     * Anything greater than 1 will be treated as 1.
     * @param takePics if true, the rover will take pictures as it drives
     *
     * @see #getRecentImage
     * @see #getImageUpdateTime
     */
    public CreepAction(int dist, int ang, double speed, boolean takePics) {
        this(dist,ang,speed,NO_SAFETY,takePics);
    }
    
    /**
     * Creates a new CreepAction with the specified safety level.
     * If the rover is taking pictures, use getRecentImage to get the
     * pictures taken while the rover is driving. Pictures will be taken
     * at pan = angle of travel and tilt = 0.
     *
     * @param dist     centimeters to move
     * @param ang   the angle to drive at. The minimum angle is -90 and
     * the maximum angle is 90. Anything outside this range will be set
     * to the closest valid value. (0=straight, 90=left, -90=right)
     * @param speed the speed to drive at. Should be between 0 and 1, where 0 is
     * stopped and 1 is full speed. Anything less than 0 will be treated as 0.
     * Anything greater than 1 will be treated as 1.
     * @param safetyLevel  CYCLE_SAFETY, STATIC_SAFETY, or NO_SAFETY
     * @param takePics if true, the rover will take pictures as it drives
     *
     * @see #getRecentImage
     * @see #getImageUpdateTime
     */
    public CreepAction(int dist, int ang, double speed, byte safetyLevel, boolean takePics) {
        this.dist = dist;
        //cap angle - on newer rover stayton code, this is done automatically but not in all of the old versions
        if(ang < -90)
            angle = -90;
        else if(ang > 90)
            angle = 90;
        else
            angle = ang;
        //cap speed
        if(speed < 0)
            this.speed = 0;
        else if(speed > 1)
            this.speed = 1;
        else
            this.speed = speed;
        rawSpeed = calculateRawSpeed(speed);
        setTime(); //dependant on dist, speed, and rover calibration
        safety = safetyLevel;
        initObstacleScan();
        takingPictures = takePics;
        picPan = angle;
        picTilt = 0;
        
        success = false;
        ret = 0;
        starttime = 0;
    }
    
    /**
     * Creates a new CreepAction with the specified safety level that
     * takes pictures at the given pan and tilt. Use getRecentImage to get the
     * pictures taken while the rover is driving.
     * <p>
     * The STATIC_SAFETY level does not work well when combined with a pan
     * other than the angle of travel or a tilt other than -35.
     *
     * @param dist     centimeters to move
     * @param ang   the angle to drive at. The minimum angle is -90 and
     * the maximum angle is 90. Anything outside this range will be set
     * to the closest valid value. (0=straight, 90=left, -90=right)
     * @param speed the speed to drive at. Should be between 0 and 1, where 0 is
     * stopped and 1 is full speed. Anything less than 0 will be treated as 0.
     * Anything greater than 1 will be treated as 1.
     * @param safetyLevel  CYCLE_SAFETY, STATIC_SAFETY, or NO_SAFETY
     * @param pan The pan angle (in degrees) to position the head at when taking pictures.
     * This value should be in the range of [-180, 180].  If you choose an angle outside
     * of this range, the pan will be set to the closest valid value.
     * Positive angles are to the left.
     * @param tilt The tilt angle (in degrees) to position the head at when taking pictures.
     * This value should be in the range of [-50, 90].  If you choose an angle outside
     * of this range, the tilt will be set to the closest valid value.
     *
     * @see #getRecentImage
     * @see #getImageUpdateTime
     */
    public CreepAction(int dist, int ang, double speed, byte safetyLevel, int pan, int tilt) {
        this(dist,ang,speed,safetyLevel,true);
        picPan = pan;
        picTilt = tilt;
    }
    
    /** Calculates the raw motor pulse value that is represented by the
     * given speed percentage.
     *
     *@param p Percentage of full speed to be translated to a raw value
     *@returns a raw speed value between 128 and 255
     */
    private int calculateRawSpeed(double p){
        return (int)(p * 127) + 128;
    }
    
    /** Sets the distance to the given cm value and recalculates how long
     * the action will take.
     */
    public void setDistance(int distance) {
        dist = distance;
        setTime();
    }
    
    /** Sets the angle to the given degree (0=straight, 90=left, -90=right).
     * The minimum angle is -90 and the maximum angle is 90. Anything outside
     * of this range will be set to the nearest valid value.
     */
    public void setAngle(int ang){
        //cap angle - on newer rover stayton code, this is done automatically but not in all of the old versions
        if(ang < -90)
            angle = -90;
        else if(ang > 90)
            angle = 90;
        else
            angle = ang;
        initObstacleScan();
    }
    
    /** Sets the speed to the given percent, where 0 is stopped and 1 is full speed.
     * Anything outside of this range will be set to the nearest valid value. Also
     * recalculates how long the action will take.
     */
    public void setSpeed(int s){
        //cap speed
        if(s < 0)
            speed = 0;
        else if(s > 1)
            speed = 1;
        else
            speed = s;
        rawSpeed = calculateRawSpeed(speed);
        setTime();
    }
    
    /** Sets the safety level to be used when driving. Valid options are
     * CYCLE_SAFETY, STATIC_SAFETY, or NO_SAFETY. Any other value will
     * be set to NO_SAFETY.
     *
     * @param level  CYCLE_SAFETY, STATIC_SAFETY, or NO_SAFETY
     */
    public void setSafetyLevel(byte level){
        if(level == CYCLE_SAFETY || level == STATIC_SAFETY)
            safety = level;
        else
            safety = NO_SAFETY;
        initObstacleScan();
    }
    
    /** Turns picture taking on or off.
     *
     *@param takePictures true to turn on picture taking, false to turn picture
     * taking off.
     */
    public void setTakePictures(boolean takePictures){
        takingPictures = takePictures;
    }
    
    /** Sets the pan and tilt angles to be used when taking a picture.
     *
     * @param pan The pan angle (in degrees) to position the head at when taking pictures.
     * This value should be in the range of [-180, 180].  If you choose an angle outside
     * of this range, the pan will be set to the closest valid value.
     * Positive angles are to the left.
     * @param tilt The tilt angle (in degrees) to position the head at when taking pictures.
     * This value should be in the range of [-50, 90].  If you choose an angle outside
     * of this range, the tilt will be set to the closest valid value.
     */
    public void setPictureAngles(int pan, int tilt){
        picPan = pan;
        picTilt = tilt;
    }
    
    public boolean doAction(Rover r) {
        //PER.rover.StatsLog.println(PER.rover.StatsLog.OMNIDRIVE,dist);
        rov = r;
        image = null;
        imageTime = 0;
        
        //update time because it uses the calibration from the rover
        setTime();
        
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
    
    /** Returns how long the action took to complete the last time it was
     * executed (in milliseconds), or how long the action is expected to
     * take if it has never been executed. Note that setDistance and
     * setSpeed recalculate the expected time based on the new distance or
     * new speed. Calling getTime after a call to setDistance or setSpeed will
     * return the new expected time.
     */
    public int getTime() {
        return time;
    }
    
    /** Sets the time relative to the current values of <code>dist</code>,
     * <code>speed</code>, and rover drive motor calibration.
     */
    private void setTime(){
        time = (int)(Math.abs(dist)*170/speed);
        //compensate for individual rover motor speed differences
        if(rov != null)
            time = time * rov.getDriveCalibration() / 100;
    }
    
    public String getSummary() {
        if (dist == 0)
            return "OmniDrive nowhere.";
        if (angle != 0)
            return "Using Omnidirectional steering, drive " + dist + " cm at " + angle + '\u00b0' + " with speed "+speed+".";
        return "Drive forward" + dist + " cm with speed "+speed+".";
    }
    
    public String getShortSummary() {
        return "OmniDrive " + dist + " cm at " + angle + '\u00b0' + " with speed "+speed+".";
    }
    
    /** Returns SUCCESS if the rover drove the entire distance without detecting an
     * obstacle or error. Otherwise, returns the appropriate status code.
     *
     *@see PER.rover.control.RoverState
     */
    public int getReturnValue() {
        // return 0 on success
        if (success)
            return 0;
        else
            return ret;
    }
    
    /** Returns true if the rover drove the entire distance without detecting an
     * obstacle or error. Undefined until isCompleted() returns true.
     */
    public boolean isSuccess() {
        return success;
    }
    
    private boolean doit() {
        do {
            if( !rov.crab(rawSpeed,angle) ){
                ret = rov.state.getStatus();
                return false;
            }
            if(quit){ //quit is true if there was a request to kill the action
                //stop crab
                rov.setAll(3, 0, 0, 0, 0, 0, 0, 0, 0);
                
                ret = RoverState.KILLED;
                return false;
                //rover state not killed?? rov.state.setStatus(RoverState.KILLED);
            }
            else{
                if(getTimeRemaining() > 200){
                    if(checkForObstacles()){
                        //obstacle detected
                        ret = ActionConstants.OBSTACLE_DETECTED;
                        //stop crab
                        rov.setAll(3, 0, 0, 0, 0, 0, 0, 0, 0);
                        return false;
                    }else if(ret != ActionConstants.SUCCESS){
                        //error during obstacle checking
                        //stop crab
                        rov.setAll(3, 0, 0, 0, 0, 0, 0, 0, 0);
                        return false;
                    }
                    //obstacle scan could take a long time so call crab again
                    if( !rov.crab(rawSpeed,angle) ){
                        ret = rov.state.getStatus();
                        return false;
                    }
                }
                
                if(takingPictures && !quit){
                    image = rov.takePicture(picPan,picTilt,320,240);
                    imageTime = System.currentTimeMillis();
                }    
            }
            
            try{Thread.sleep(50);}catch(Exception e) {}
            
        }while(getTimeRemaining() > 0);
        
        //stop crab
        rov.setAll(3, 0, 0, 0, 0, 0, 0, 0, 0);
        
        ret = rov.state.getStatus();
        return ret == RoverState.SUCCESS;
    }
    
    /** Initializes the scan action based on the current safety level and angle. */
    private void initObstacleScan(){
        switch (safety){
            case CYCLE_SAFETY:
                /*int lrange = 60; //the number of degrees to scan to either side of angle
                int srange = 45; //the number of degrees to scan to either side of angle
                //scan = new ScanAction(-22, angle - range, angle + range, 5);
                if(angle < 0){
                    //right
                    //scan = new ScanAction(-22, angle - srange, angle + lrange, 5);
                    scan = new ScanAction(-22, angle - lrange, angle + srange, 5);
                }
                else if(angle > 0){
                    //left
                    scan = new ScanAction(-22, angle - lrange, angle + srange, 5);
                    //scan = new ScanAction(-22, angle - srange, angle + lrange, 5);
                }
                else
                    scan = new ScanAction(-22, -srange, srange, 5);
                 */
                if (angle == 0)
                    scan = new ScanAction(-22, -65, 65, 5);
                else
                    scan = new ScanAction(-22, angle -70, angle + 70, 5);
                break;
            default:
                //NO_SAFETY or STATIC_SAFETY
                scan = null;
        }
    }
    
    /** Checks for obstacles based on the type of obstacle detection specified
     * in the action and the driving angle.
     *
     *@returns true if an obstacle is detected
     */
    private boolean checkForObstacles(){
        switch (safety){
            case CYCLE_SAFETY:
                //scan
                if (scan == null){
                    PER.rover.Log.println("CreepAction error: could not scan for obstacles because ScanAction is null.");
                    return false; //?
                }
                scan.doAction(rov);
                while(!scan.isCompleted()){
                    try{Thread.sleep(20);}catch (Exception e){}
                    if(quit == true){
                        scan.kill(); //although can't currently kill scan
                        ret = ActionConstants.KILLED;
                        return false;
                    }
                }
                if (scan.isSuccess())
                    return (scan.hasFoundRock() || scan.hasDetectedWall());
                else {
                    //error
                    ret = scan.getReturnValue();
                    return false;
                }
            case STATIC_SAFETY:
                rov.look(angle,-35);
                if(rov.state.getRange() > 90)
                    return true;
                else return false;
            default:
                //NO_SAFETY
                return false;
        }
    }
    
    public BufferedImage getRecentImage() {
        return image;
    }
    
    public long getImageUpdateTime() {
        return imageTime;
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
     
}