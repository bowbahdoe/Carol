/*
 * RoverController.java
 *
 * Created on April 12, 2002, 11:15 AM
 */

package PER.rover.control;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.StringTokenizer;


/**
 * Allows communication with and control of the robot. Generally programs should
 * access the functions in RoverController through its subclass, Rover.
 * <p>
 * For all of the functions in this class that set the motor speeds, the speed
 * should be a number in the range of [-255, 255].  On the cerebellum this is
 * represented by an 8-bit number and a sign bit.  This speed is the pulse width
 * that the motors are getting.  If the pulse is less than 50% duty cycle, the
 * motors won't turn on.  Therefore, if you want the rover to go backwards, set
 * the speed to be between -129 and -255.  If you want the rover to go forwards,
 * set the speed to be between 129 and 255.  The motor changes speed roughly
 * linearly withing these ranges.
 * Speeds outside of the range [-255, 255] are capped.
 *
 *@author Eric Porter
 */
public class RoverController {
    /**
     * This instance of the state is updated whenever you call a simple
     * command.
     */
    public RoverState state;
    
    /**
     * This instance of the state is updated only by goTo, turnTo, killHighLevel,
     * and updateHighLevel.
     */
    public RoverState highLevelState;
    
    /**
     * This class contains images sent back during DriveTo and TurnTo as well as
     * tracking data.
     */
    public ReceiveThread receive = null;
    
    /**
     * The command to be sent to the rover.  Any commands that use this variable
     * should synchronize on it to keep threads from conflicting.
     */
    private RoverCommand command;
    public Reliagram reliagram = null;
    
    /** The time in ms of how long to wait for the rover to respond. */
    public final static int READ_TIMEOUT = 5000;
    
    /** Creates a new RoverController */
    public RoverController() {
        command = new RoverCommand(); //holds the commands that are generated
        state = new RoverState();     //this state gets updated with most commands
        highLevelState = new RoverState(); //this state gets updated with high level commands
    }
    
    
    /**
     * Initializes the communication with the robot but does not check that the
     * rover is on or that it is responding.
     * @param ipaddr An IP address or hostname.
     * @return This command will only return false if you specify a hostname that
     * cannot be resolved.
     */
    public boolean initComm(String ipaddr) {
        if(reliagram != null)
            closeComm();
        reliagram = new Reliagram();
        if(reliagram.connect(ipaddr, 1701, READ_TIMEOUT)) {
            receive = new ReceiveThread(reliagram, state);
            return true;
        }else {
            reliagram.quit();
            reliagram = null;
            return false;
        }
    }
    
    /**
     * Closes communication with the rover.  Calling this function allows memory
     * being used by the communication functions to be freed.
     * @return true if you are connected, false if you aren't.
     */
    public boolean closeComm() {
        if(reliagram != null) {
            reliagram.quit();
            receive.quit();
            reliagram = null;
            return true;
        }
        return false;
    }
    
    /** Returns true if connected to a robot.
     */
    public boolean isConnected() {
        return reliagram != null;
    }
    
    /** Initalizes the rover; centers all of the servos and sets wheel velocities
     * to zero.
     *
     *@return false if not connected to a rover or the rover can not be initialized
     */
    public boolean initRobot() {
        int seqNum;
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        synchronized (command) {
            command.initRover();
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    /** Kills anything the rover is doing by calling initRobot.
     *
     *@see #initRobot
     */
    public boolean killRobot() {
        return initRobot();
    }
    
    /**
     * Tries to take a picture.  With the creative web cam, 320x240 is the best
     * resolution to take pictures at.  Getting the image from the camera only
     * takes about 300ms.  If you go to a higher resolution, it will take the
     * middle pixels from a 640x480 image.  For example, at 352x288, the field of
     * view is smaller than at 320x240 because of the way images are taken.
     * Because of a bug in the driver on the Stayton, all images at 160x120 are corrupted.
     * Upon failure, null is returned and the status code of <code>state</code> is set.
     * @param pan The pan value in degrees at which to take the picture.
     * @param tilt The tilt vale in degrees at which to take the picture.
     * @param width The width of the image in pixels.
     * @param height The hiehgt of the image in pixels.
     * @return A new BufferedImage upon success.
     */
    public BufferedImage takePicture(int pan, int tilt, int width, int height, boolean lightUV) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return null; }
        int seqNum, extraTime;
        synchronized(command) {
            command.takePicture(pan, tilt, width, height, lightUV);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        if(width > 320 || height > 240)
            extraTime = 3000;
        else
            extraTime = 1500;
        return takePictureCommon(seqNum, extraTime);
    }
    
    /**
     * This function is the same as the other takePicture function, but has the
     * UV light off.
     */
    public BufferedImage takePicture(int pan, int tilt, int width, int height) {
        return takePicture(pan, tilt, width, height, false);
    }
    
    /**
     * This function returns the raw YUV that the camera returns.  It has the same
     * parameters as the other functions.
     */
    public byte [] takeRawPicture(int pan, int tilt, int width, int height) {
        int seqNum;
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return null; }
        synchronized(command) {
            command.takeRawPicture(pan, tilt, width, height, false);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        Datapack dpack = null;
        if(seqNum > 0)
            dpack = reliagram.receive(seqNum, READ_TIMEOUT + 200);
        if(dpack == null || seqNum <= 0) {
            state.parsePacket(null);
            return null;
        }
        if(dpack.getLength() == RoverState.FULL_PACKET_LENGTH) {
            state.parsePacket(dpack.getData());
            return null;
        }
        return dpack.getData();
    }
    
    /**
     * Gives you the most recent picture that was taken by the rover.
     * DriveToAction and TurnToAction take pictures while they are doing the
     * Aciton.
     * Upon failure, null is returned and the status code of <code>state</code> is set.
     * @return The most recent picture the rover has taken.  If it is in the
     * process of taking a picture, that picture is returned.
     */
    public BufferedImage takeRecentPicture() {
        int seqNum;
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return null; }
        synchronized(command) {
            command.takeRecentPicture();
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return takePictureCommon(seqNum, 200);
    }
    
    /**
     * This is just a helper function for taking pictures.  It receives the pictures.
     * @param seqNum The sequence number of the packet that was sent.
     * @param extraTime Approximately how much more time it would take for this command to complete.
     * @return The image from the rover.
     */
    private BufferedImage takePictureCommon(int seqNum, int extraTime) {
        Datapack dpack = null;
        if(seqNum > 0)
            dpack = reliagram.receive(seqNum, READ_TIMEOUT + extraTime);
        if(dpack == null || seqNum <= 0) {
            state.parsePacket(null);
            return null;
        }
        if(dpack.getLength() == RoverState.FULL_PACKET_LENGTH) {
            state.parsePacket(dpack.getData());
            return null;
        }
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(dpack.getData(), 0, dpack.getLength());
            BufferedImage image = ImageIO.read(bais);
            bais.close();
            return image;
        } catch(Exception e) {
            return null;
        }
    }
    
    /**
     * Scans the area around where the rover is.  The scan is done starting with
     * the minPan and increments to maxPan.  Scans are done at positions from
     * minPan to maxPan inclusive.
     * Upon failure, null is returned and the status code of <code>state</code> is set.
     * @param tilt The tilt at which the scan is done.
     * @param minPan The pan angle at which the scan starts
     * @param maxPan The pan angle at which the scan ends.
     * @param step The angle difference between successive scan points.
     * @return An array of the scan points in terms of the range sensing data.
     *   There will be <code>(maxPan-minPan+step)/step</code> points in the array.
     */
    public int [] scan(int tilt, int minPan, int maxPan, int step) {
        if(tilt > 90 || tilt < -90 || minPan < -180 || maxPan > 180 ||
        minPan > maxPan || step <= 0) {
            state.setStatus(RoverState.BAD_INPUT);
            return null;
        }
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return null; }
        int seqNum;
        synchronized(command){
            command.scan(tilt, minPan, maxPan, step);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        Datapack dpack = null;
        //give it extra time because scanning can take a while
        //int extraTime = Math.max(2000, 1000+(maxPan-minPan)*50/step);
        int extraTime = 3 * Math.max(2000, 1000+(maxPan-minPan)*50/step); //for exhibit3.0 need to increase timeout for scan
        if(seqNum > 0){            
            dpack = reliagram.receive(seqNum, READ_TIMEOUT+extraTime);
        }
        if(dpack == null || seqNum <= 0 || dpack.getLength() == 0) {
            state.parsePacket(null);
            return null;
        }
        if(dpack.getData()[0] != RoverState.SUCCESS) { //error
            state.parsePacket(dpack.getData());
            return null;
        }
        byte [] data = dpack.getData();
        int length = dpack.getLength();
        if(length != ((maxPan-minPan)/step+2)){
            if(length == RoverState.FULL_PACKET_LENGTH)
                state.parsePacket(dpack.getData());
            else
                state.setStatus(RoverState.INVALID_PACKET_LENGTH);
            return null;
        }
        int [] scanVals = new int [length-1];
        for(int i=1; i<length; i++)
            scanVals[i-1] = ByteUtil.unsign(data[i]);
        return scanVals;
    }
    
    /**
     * This command starts the rover to go the specified distance while driving at
     * the specified angle.  Use the DriveToAction if you want it to wait for the
     * rover to finish.  Using this function, it has safety on so it won't hit things
     * and it will take pictures.
     * @param dist How many centimenters to drive
     * @param angle The angle for the rover to drive at.  This angle must be in
     * the range of [-90, 90] or else you will get a BAD_INPUT error.
     * @see PER.rover.DriveToAction
     */
    public boolean goTo(int dist, int angle) {
        return goTo(dist, angle, PER.rover.DriveToAction.CYCLE_SAFETY, true);
    }
    
    /**
     * This command starts the rover to go the specified distance while driving at
     * the specified angle.  Use the DriveToAction if you want it to wait for the
     * rover to finish.  Using this function, it has safety on so it won't hit things
     * and it will take pictures.
     * This function will always update the information in <code>highLevelState</code>
     * @param dist How many centimenters to drive
     * @param angle The angle for the rover to drive at.  This angle must be in
     * the range of [-90, 90] or else you will get a BAD_INPUT error.
     * @param safetyLevel See constants at top of PER.rover.DriveToAction
     * @see PER.rover.DriveToAction
     */
    public boolean goTo(int dist, int angle, byte safetyLevel, boolean takePics) {
        int seqNum;
        if(reliagram == null) {highLevelState.setStatus(RoverState.NOT_CONNECTED); return false; }
        
        // System.err.println("rov.goTo gets dist " + dist);
        
        if(dist < 0){
            highLevelState.setSign(-1);
            state.setSign(-1);
        } else if(dist > 0){
            highLevelState.setSign(1);
            state.setSign(1);
        }
        
        state.setCrabAngle(angle);
        highLevelState.setCrabAngle(angle);
        //        System.out.println("crab angle set to " + angle);
        
        synchronized (command) {
            command.goTo(dist, angle, safetyLevel, takePics);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doHighLevelReceive(seqNum);
    }
    
    /**
     * This command starts the rover to turn the specified number of degrees.
     * Use the TurnToAction if you want it to wait for the rover to finish.
     * If the angle is not in the range of [-180, 180], the rover will turn the
     * equivalent angle in that range.
     * This function will always update the information in <code>highLevelState</code>
     * @param degrees How many degrees to turn, with positive being to the left
     * @param takePics Whether or not to take pictures while driving.
     * @see PER.rover.TurnToAction
     */
    public boolean turnTo(int degrees, boolean takePics) {
        int seqNum;
        if(reliagram == null) {highLevelState.setStatus(RoverState.NOT_CONNECTED); return false; }
        
        degrees = degrees % 360;
        
        if(degrees > 180){
            degrees -= 360;
        }
        if(degrees < -180)
            degrees += 360;
        
//        System.out.println("degrees in turnTo are: " + degrees);
              
        if(degrees < 0){
            highLevelState.setSign(-1);
            state.setSign(-1);
        } else if(degrees > 0){
            highLevelState.setSign(1);
            state.setSign(1);
        }
        
        synchronized (command) {
            command.turnTo(degrees, takePics);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doHighLevelReceive(seqNum);
    }
    
    /**
     * This command starts the rover to turn the specified number of degrees.
     * Use the TurnToAction if you want it to wait for the rover to finish.
     * If the angle is not in the range of [-180, 180], the rover will turn the
     * equivalent angle in that range.
     * This function will always update all information in <code>highLevelState</code>
     * @param degrees How many degrees to turn, with positive being to the left
     * @see PER.rover.TurnToAction
     */
    public boolean turnTo(int degrees) {
        return turnTo(degrees,true);
    }
    
    /**
     * This function will kill any currently running turnTo, goTo or scan.  If you
     * are using TurnToAction or DriveToAction, you should use their kill() functions.
     * By calling killHighLevel, the action you are killing will return with an
     * error code of <code>KILLED</code>
     * This function will always update the information in <code>highLevelState</code>
     * @return true if there was something to be killed and it was
     */
    public boolean killHighLevel() {
        int seqNum;
        if(reliagram == null) {highLevelState.setStatus(RoverState.NOT_CONNECTED); return false; }
        synchronized (command) {
            command.killHL();
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doHighLevelReceive(seqNum);
    }
    
    /**
     * This function is intended for use in PER.rover.DriveToAction and
     * PER.rover.TurnToAction.  It will return the status of either the currently
     * running action or the last one to run.
     * This function will always update the information in <code>highLevelState</code>
     * @return true if the update was successful
     */
    public boolean updateHighLevel() {
        int seqNum;
        if(reliagram == null) {highLevelState.setStatus(RoverState.NOT_CONNECTED); return false; }
        synchronized (command) {
            command.getUpdate();
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doHighLevelReceive(seqNum);
    }
    
    /** Loads the calibration file from the rover and returns the drive adjustment
     * value. The drive adjustment value is used by the rover to ensure that <code>goTo</code>
     * commands work the same from rover to rover despite variation in the drive
     * motors. The adjustment value is a percentage. A value below 100 means drive
     * for a shorter time. A value above 100 means drive for a longer time.
     * <p>
     * Because <code>crab</code> and <code>quadTurn</code> do not make use of the
     * drive calibration, programs that use these commands may want to want to use
     * <code>getDriveCalibration</code> to ensure that the program behaves the
     * same from rover to rover.
     * <p>
     * This function will only update the status in <code>state</code> in case of error.
     *
     *@return The drive adjustment value from the rover's calibration file. Returns
     * a default value of 100 if the file can not be loaded or there is an error.
     */
    public int getDriveCalibration() {
        try{
            String calibration = getCalibration();
            //split string in to lines
            String[] line = calibration.split("[\r\n]");
            for(int i=0; i<line.length; i++){
                if(!line[i].startsWith("#")){ //skip comment lines beginning with #
                    String[] result = line[i].split("\\s"); //white space as delimeter
                    //the first value after "drive_adjust" should be the driveAdjustValue
                    if(result[0].equals("drive_adjust"))
                        return (new Integer(result[1])).intValue();
                }
            }
        }catch(Exception e){}//no calibration file is currently on the robot or there was an error
        return 100; //default
    }
    
    /** Loads the calibration file from the rover and returns the turn adjustment
     * value. The turn adjustment value is used by the rover to ensure that <code>turnTo</code>
     * commands work the same from rover to rover despite variation in the drive
     * motors. The adjustment value is a percentage. A value below 100 means turn
     * for a shorter time. A value above 100 means turn for a longer time.
     * <p>
     * Because <code>spin</code> does not make use of the turn calibration,
     * programs that use <code>spin</code> may want to want to use
     * <code>getTurnCalibration</code> to ensure that the program behaves the
     * same from rover to rover.
     * <p>
     * This function will only update the status in <code>state</code> in case of error.
     *
     *@return The turn adjustment value from the rover's calibration file. Returns
     * a default value of 100 if the file can not be loaded or there is an error.
     */
    public int getTurnCalibration() {
        try{
            String calibration = getCalibration();
            //split string in to lines
            String[] line = calibration.split("[\r\n]");
            for(int i=0; i<line.length; i++){
                if(!line[i].startsWith("#")){ //skip comment lines beginning with #
                    String[] result = line[i].split("\\s"); //white space as delimeter
                    //the first value after "turn_adjust" should be the turnAdjustValue
                    if(result[0].equals("turn_adjust"))
                        return (new Integer(result[1])).intValue();
                }
            }
        }catch(Exception e){}//no calibration file is currently on the robot or there was an error
        return 100; //default
    }
    
    
    /**
     * This funciton is useful if you want to see the servo calibration on the
     * rover.  It is used by PER.Calibration.  This file contains calibration for
     * the servos and the motors.
     * This function will only update the status in <code>state</code> in case of error.
     * @return The calibration file from the rover as a string.  Its location is
     * /root/rover.cal on the Stargate.
     */
    public String getCalibration() {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return null; }
        int seqNum;
        synchronized(command) {
            command.getCalibration();
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        Datapack dpack = null;
        if(seqNum > 0)
            dpack = reliagram.receive(seqNum, READ_TIMEOUT);
        if(dpack == null || seqNum <= 0) {
            state.parsePacket(null);
            return null;
        }
        if(dpack.getLength() == RoverState.FULL_PACKET_LENGTH) {
            state.parsePacket(dpack.getData());
            return null;
        }
        if(dpack.getLength() == 1) {
            state.setStatus(ByteUtil.unsign(dpack.getData()[0]));
            return null;
        }
        return new String(dpack.getData(), 1, dpack.getLength()-1);
    }
    
    /** Sets the calibration file on the robot.  This function is used by
     *  PER.Calibration.  The file is located at /root/rover.cal on the Stargate.
     *  If the calibration is messed up, the rover will not work right.
     *  Once you call this function, it replaces the current file on the Stargate
     *  and the calibration is immediately changed.
     *  This function will always update the information in <code>state</code>
     *  @param cal The calibration file as a String.
     *  @return true if this command worked.
     */
    public boolean setCalibration(String cal) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.setCalibration(cal);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    /**
     * This funciton is useful if you want to see the scan calibration on the
     * rover.  It is used by PER.Calibration.
     * This function will only update the status in <code>state</code> in case of error.
     * @return The scan calibration file from the rover as a string.  Its location is
     * /root/rover.scan on the Stargate.
     */
    public String getScanList() {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return null; }
        int seqNum;
        synchronized(command) {
            command.getScanList();
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        Datapack dpack = null;
        if(seqNum > 0)
            dpack = reliagram.receive(seqNum, READ_TIMEOUT);
        if(dpack == null || seqNum <= 0) {
            state.parsePacket(null);
            return null;
        }
        if(dpack.getLength() == RoverState.FULL_PACKET_LENGTH) {
            state.parsePacket(dpack.getData());
            return null;
        }
        if(dpack.getLength() == 1) {
            state.setStatus(ByteUtil.unsign(dpack.getData()[0]));
            return null;
        }
        return new String(dpack.getData(), 1, dpack.getLength()-1);
    }
    
    /**
     * This funciton sets the scan calibration on the rover.
     * It is used by PER.Calibration.
     * The file is located at /root/rover.scan on the Stargate.
     * If the scan calibration is messed up, the rover will not scan correctly
     * and may not scan at all if there are no valid lines.
     * Once you call this function, it replaces the current file on the Stargate
     * and the scan calibration is immediately changed.
     * This function will always update the information in <code>state</code>
     * @param cal The scan calibration file as a String.
     * @return true if this command worked.
     */
    public boolean setScanList(String cal) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.setScanList(cal);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    /**
     * Moves the pan and tilt on the PER.  This function does not wait for the
     * head servos to reach the desired position.
     * This function will always update the information in <code>state</code>
     * @param pan The pan angle in degrees for the PER to move its head to.  This value should
     * be in the range of [-180, 180].  If you choose an angle outside of this range,
     * the pan will be set to the closest valid value.  Positive angles are to the left.
     * @param tilt The tilt angle in degrees for the PER to move its head to.  This value should
     * be in the range of [-50, 90].  If you choose an angle outside of this range,
     * the tilt will be set to the closest valid value.
     * @return true if the command worked.
     */
    public boolean look(int pan, int tilt) {
        return headMove(true, pan, true, tilt);
    }
    
    /**
     * This command is just like look, but only sets the pan.
     * @param pan The desired pan angle.
     * @return true if the command worked.
     * @see #look
     */
    public boolean setPan(int pan) {
        return headMove(true, pan, false, 0);
    }
    
    /**
     * This command is just like look, but only sets the tilt.
     * @param tilt The desired tilt angle.
     * @return true if the command worked.
     * @see #look
     */
    public boolean setTilt(int tilt) {
        return headMove(false, 0, true, tilt);
    }
    
    /**
     * This command refreshes the state of the robot.  By calling this function,
     * you will get the most up to date values for the battery voltage, range, etc.
     * @return true if the command worked.
     */
    public boolean refresh() {
        return headMove(false, 0, false, 0);
    }
    
    /**
     * This function is just like the look command, but you can specify whether
     * you want to move ther servo or not.
     */
    public boolean headMove(boolean doPan, int pan, boolean doTilt, int tilt) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.headMove(doPan, pan, doTilt, tilt);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
   /*  The following functions are in the process of being removed.
    
    public boolean SleepServos(boolean pan, boolean tilt) {
      // fixed by rgockley 1-6-03 (had been SetParam)
      return true;//DirectSetParameters(false, 0, false, 0, false, 0, pan, 0, tilt, 0);
   }
    
   public boolean MoveHeadTo(int pan, int tilt) {return look(pan, tilt);}
   public boolean PanHeadTo(int pan) {return setPan(pan);}
   public boolean TiltHeadTo(int tilt) {return setTilt(tilt);}
   public boolean RefreshState() {return refresh();}*/
    
    /**
     * Allows you to directly set the positions of the motors and servos.  The
     * motor speeds must be in the range of [-255, 255] and the servos must be in
     * the range of [0, 255] with 0 meaning that the servos are off.  The values
     * for the servos are the raw servo values that the cerebellum understands.
     * <p>
     * The <code>mask</code> parameter is an 8-bit mask specifying which of the
     * motors and servos should be set.
     * <p><ul>
     * <li>1 = rightMotor 
     * <li>2 = leftMotor 
     * <li>4 = frontLeftServo 
     * <li>8 = frontRightServo 
     * <li>16 = backRightServo
     * <li>32 = backLeftServo
     * <li>64 = pan
     * <li>128 = tilt
     * </ul>
     * <p>
     * As an example, if you only want to turn the motors off, but not move the
     * steering servos, call <code>setAll(3, 0, 0, 0, 0, 0, 0, 0, 0)</code>. 
     * To set just the four steering servos, set the mask to 60 (i.e. 
     * 4 + 8 + 16 + 32).
     *
     *
     * <p>
     * This function will always update the information in <code>state</code>.
     * @param mask An 8-bit mask specifying which of these parameters you want to
     *             set.  1 is the mask for the rightMotor, 2 for the leftMotor,
     * . . . 128 for the tilt servo.
     * @param rightMotor The motor speed for the right motor.
     * @param leftMotor The motor speed for the left motor.
     * @param frontLeftServo The servo position for the front left servo.
     * @param frontRightServo The servo position for the front right servo.
     * @param backRightServo The servo position for the back right servo.
     * @param backLeftServo The servo position for the back left servo.
     * @param pan The servo postition for the pan servo.
     * @param tilt The servo postition for the tilt servo.
     */
    public boolean setAll(int mask, int rightMotor, int leftMotor, int frontLeftServo,
    int frontRightServo, int backRightServo, int backLeftServo, int pan, int tilt) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.setAll(mask, rightMotor, leftMotor, frontLeftServo,
            frontRightServo, backRightServo, backLeftServo, pan, tilt);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    /**
     * Turns the rover about its center point.  A positive speed turns the rover
     * left and a negative speed turns the rover right.
     * This function will always update the information in <code>state</code>
     * @param speed How fast to go - see comments at top of file for more explaination.
     * @return true if the command worked
     */
    public boolean spin(int speed) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.spin(speed);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    /**
     * Moves the rover in a straight line at the specified angle.  For example, at
     * an angle of 0, it will drive straight.  At an angle of 45, it will move
     * forwards and left.  An angle of -90 and a positive speed will move the rover
     * to the right.
     * This function will always update the information in <code>state</code>
     * @param speed How fast to go - see comments at top of file for more explaination.
     * @param angle The angle the steering servos are set to.  Angles outside of
     *              the valid range of [-90, 90] are capped.
     * @return true if the command worked
     */
    public boolean crab(int speed, int angle) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.crab(speed, angle);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    /**
     * This function has the rover move and rotate about the point (0, radius) in
     * the rover's reference frame.  Imaging that the rover is on a grid at (0, 0)
     * and is facing down the x-axis.  This function will cause the rover to rotate
     * about the point (0, radius).  The special case is that if the radius is 0,
     * the rover will drive straight.  You should call spin in that case.
     * This function will always update the information in <code>state</code>
     * @param speed How fast to go - see comments at top of file for more explaination.
     * @param radius The radius to turn around.
     * @return true if the command worked
     */
    public boolean quadTurn(int speed, int radius) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.quadTurn(speed, radius);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    /**
     * This function allows you to turn the UV light on the rover on or off.
     * This function will always update the information in <code>state</code>
     * @param on If true, the light will turn on, if false, the light will turn off.
     * @return true if the command worked
     */
    public boolean setLight(boolean on) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.setLight(on);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    /**
     * Gets the version of the code running on the Stargate as a String.  More
     * recent versions will return a string with two decimal points, for example
     * "2.0.0"
     * @return The version number as a String.
     */
    public String getVersion() {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return null; }
        int seqNum;
        synchronized (command) {
            command.getVersion();
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        Datapack dpack = null;
        if(seqNum > 0)
            dpack = reliagram.receive(seqNum, READ_TIMEOUT);
        if(dpack == null || seqNum <= 0) {
            state.parsePacket(null);
            return null;
        }
        String version = new String(dpack.getData(), 0, dpack.getLength());
        if(version.lastIndexOf('.') < 2) //old versions returned "5.0"
            return "1.0.0"; //this makes sure that old versions are labeled as such
        return version;
    }
    
    /**
     * Compares two version strings.  It expects the strings to be in a format like
     * the one returned by the stargate, which is "x.x.x"  Passing null or improper
     * strings will result in an exception being thrown.
     * @param version1 The first string to compare
     * @param version2 The second string to compare
     * @return 1 if version1 is more recent than version2, 0 if the versions are equal,
     *         and -1 if version2 is more recent than version1.
     * @throws java.lang.NumberFormatException
     */
    public static int compareVersion(String version1, String version2) throws NumberFormatException{
        final String delim = ". \t\n\r\f";
        StringTokenizer st1 = new StringTokenizer(version1, delim);
        StringTokenizer st2 = new StringTokenizer(version2, delim);
        for(int i=0; i<3; i++) {
            if(!st1.hasMoreTokens() || !st2.hasMoreTokens())
                throw new NumberFormatException("Improperly formatted version string");
            int v1 = Integer.parseInt(st1.nextToken());
            int v2 = Integer.parseInt(st2.nextToken());
            if(v1 < v2)
                return -1;
            else if(v1 > v2)
                return 1;
        }
        return 0;
    }
    
    /**
     * Starts the rover tracking an object.  The  information the rover sends back
     * is stored in <code>receive</code>.
     * By default, it tracks the largest blob, moving only pan and tilt, and doesn't drive.
     * @param minY The minimum Y value to track.
     * @param maxY The maximum Y value to track.
     * @param minU The minimum U value to track.
     * @param maxU The maximum U value to track.
     * @param minV The minimum V value to track.
     * @param maxV The maximum V value to track.
     * @return true if starting tracking was successful
     */
    public boolean startTrack(int minY, int maxY, int minU, int maxU,
    int minV, int maxV) {
        return startTrack(minY, maxY, minU, maxU, minV, maxV, 0, true, true, 0);
    }
    
    /**
     * Stops the rover's camera commands that stream which include getMean and tracking.
     * This function will always update the information in <code>state</code>
     *
     * @return A return value of true imples that that the rover is not currently
     *         streaming anything and is ready for new commands.  If the rover is
     *         currently taking pictures, or a TurnTo or DriveTo needs the head,
     *         false will be returned and the rover status will be RESOURCE_CONFLICT.
     */
    public boolean stopStreaming() {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.stopStreaming();
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    /**
     * Starts the rover tracking an object.  The  information the rover sends back
     * is stored in <code>receive</code>.
     * This function will always update the information in <code>state</code>
     * @param minY The minimum Y value to track.
     * @param maxY The maximum Y value to track.
     * @param minU The minimum U value to track.
     * @param maxU The maximum U value to track.
     * @param minV The minimum V value to track.
     * @param maxV The maximum V value to track.
     * @param trackMethod Controls how the object is tracked. 0=biggest blob
     * @param movePan If set to true, it will move the pan angle to try and center the object tracked.
     * @param moveTilt If set to true, it will move the tilt angle to try and center the object tracked.
     * @param driveMethod Currently not supported.
     * @return true if starting tracking was successful
     */
    public boolean startTrack(int minY, int maxY, int minU, int maxU,
    int minV, int maxV, int trackMethod, boolean movePan, boolean moveTilt, int driveMethod) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.startTrack(minY, maxY, minU, maxU, minV, maxV, trackMethod, movePan, moveTilt, driveMethod);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    //only brightness changes 0-65536, above 30,000 is dim 16384=min=very bright
    /**
     * Gets information on the state of the camera.  Because the camera is set in
     * autogain mode, the brightness will change during use.  The other values do
     * not change.  With the camera in the rover, a brightness value of 16384
     * indicates that it is looking at something really bright.  If it is looking
     * at something dark or is in a dimly lit room, the brightness will be in the
     * range of 30000-35000.
     * <p>
     * The function works by calling VIDIOCSPICT and returns the contents of the
     * struct video_picture.
     * This function will only update the status in <code>state</code> in case of error.
     * @return Upon success an integer array of size 7 is returned containing
     *         [brightness, hue, colour, contrast, whiteness, depth, palette].
     *         If a failure occurs, null is returned.
     */
    public int [] getCameraProperties() {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return null; }
        int seqNum;
        synchronized (command) {
            command.getProperties();
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        Datapack dpack = null;
        if(seqNum > 0)
            dpack = reliagram.receive(seqNum, READ_TIMEOUT);
        if(dpack == null || seqNum <= 0) {
            state.parsePacket(null);
            return null;
        }
        if(dpack.getLength() == RoverState.FULL_PACKET_LENGTH) {
            state.parsePacket(dpack.getData());
            return null;
        }
        if(dpack.getLength() != 28) {
            state.setStatus(RoverState.INVALID_PACKET_LENGTH);
            return null;
        }
        int [] properties = new int[7];
        properties[0] = ByteUtil.networkLongToInt(dpack.getData(), 0);
        properties[1] = ByteUtil.networkLongToInt(dpack.getData(), 4);
        properties[2] = ByteUtil.networkLongToInt(dpack.getData(), 8);
        properties[3] = ByteUtil.networkLongToInt(dpack.getData(), 12);
        properties[4] = ByteUtil.networkLongToInt(dpack.getData(), 16);
        properties[5] = ByteUtil.networkLongToInt(dpack.getData(), 20);
        properties[6] = ByteUtil.networkLongToInt(dpack.getData(), 24);
        return properties;
    }
    
    /**
     * Gets the mean for the red, green and blue channels.
     * The  information the rover sends back is stored in <code>receive</code>.
     * This function will always update the information in <code>state</code>
     *
     * @param stream If set to true, the rover will stream the data back until
     * stopStreaming() is called.  If set to false, the rover will send back
     * the current mean values and then quit.
     *
     * @return true if the command worked
     */
    public boolean getMean(boolean stream) {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        long sendTime = System.currentTimeMillis();
        synchronized (command) {
            command.getMean(stream);
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        boolean worked = doReceive(seqNum);
        //if putting rover into streaming mode or it failed, return now.
        if(stream || !worked)
            return worked;
        //wait up to 2.5 seconds for the sent track packet to arrive
        for(int i=0; i<50; i++){
            if(receive.getMeanUpdateTime() > sendTime)
                return true;
            try {Thread.sleep(50);}catch (Exception e) {}
        }
        return false;
    }
    
    /**
     * Starts the rover detection motion.  The  information the rover sends back
     * is stored in <code>receive</code>.
     * This function will always update the information in <code>state</code>
     * @return true if starting to detect motion was successful
     */
    public boolean startMotionDetection() {
        if(reliagram == null) {state.setStatus(RoverState.NOT_CONNECTED); return false; }
        int seqNum;
        synchronized (command) {
            command.startMotion();
            seqNum = reliagram.send(command.getData(), command.getLength());
        }
        return doReceive(seqNum);
    }
    
    private boolean doReceive(int seqNum) {
        if(seqNum < 0) {
            state.parsePacket(null);
            return false;
        }
        Datapack dpack = reliagram.receive(seqNum);
        if(dpack == null)
            return state.parsePacket(null);
        else
            return state.parsePacket(dpack.getData());
    }
    
    private boolean doHighLevelReceive(int seqNum) {
        if(seqNum < 0) {
            highLevelState.parsePacket(null);
            return false;
        }
        Datapack dpack = reliagram.receive(seqNum);
        if(dpack == null)
            return highLevelState.parsePacket(null);
        else
            return highLevelState.parsePacket(dpack.getData());
    }
}