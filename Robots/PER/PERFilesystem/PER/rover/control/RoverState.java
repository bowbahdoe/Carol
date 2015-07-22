/*
 * RoverState.java
 *
 * Created on June 30, 2003, 10:48 AM
 */

package PER.rover.control;

import java.math.*;

/**
 * Allows one to determine the current status of the rover. The state is updated
 * when commands are sent to the rover or by calling refresh in RoverController.
 * The constants in this class are used to represent the rovers current status as well
 * as used as Action return values.
 *
 *@see PER.rover.control.RoverController#refresh()
 *@see PER.rover.Action#getReturnValue()
 *
 * @author  Eric Porter
 */

public class RoverState {
    private int status;
    private int range;
    private int voltage;
    private int pan;
    private int tilt;
    private int dist;
    private int lastDist;
    private int distSign;
    private int crabAngle;
    private double[] position; //cartesian coordinate position [x, y, theta]
    
    //locks, cerebThreadState, and webCamThreadState are new variables
    //in version 2 of the Stargate code
    
    /** A bitmask telling you what parts of the rover are in use. 
     * <p>
     * 1 = LEGS_LOCK - the legs are in use  <br>
     * 2 = PAN_LOCK - the pan is in use   <br>
     * 4 = TILT_LOCK - the tilt is in use  <br>
     * 8 = UV_ON - the light is on    
     */
    public int locks;
    
    /** The state of the cerebellum thread. 
     * <p>
     * 0 = CEREB_IDLE - no requests have been received <br>
     * 1 = CEREB_STARTING - a request has been received, but it hasn't started <br>
     * 2 = CEREB_SCAN - doing a scan <br>
     * 3 = CEREB_TURNTO - doing a turn to <br>
     * 4 = CEREB_DRIVETO - doing a drive to
     */
    public int cerebThreadState;
    
    /** The state of the webcam thread.
     * <p>
     * 0  =  WEBCAM_IDLE - thread doing nothing <br>
     * 1  =  WEBCAM_STOP - thread doing nothing <br>
     * 2  =  WEBCAM_GRAB - grabbing a new picture <br>
     * 3  =  WEBCAM_PROPERTIES - getting camera properties <br>
     * 50 =  WEBCAM_CYCLE_PIC - cycling through safety positions with pictures being taken <br>
     * 51 =  WEBCAM_CYCLE_NOPIC - cycling through safety positions without pictures being taken <br>
     * 52 =  WEBCAM_PIC - taking constant pictures <br>
     * 53 =  WEBCAM_TRACK - doing tracking     <br>
     * 54 =  WEBCAM_MEAN - getting mean      <br>
     * 55 =  WEBCAM_MOTION - getting motion    <br>
     * <p>
     * Note that the above values are consistent with Stargate software version 2.1
     * and later and are not guaranteed to be compatible with earlier Stargate software
     * versions.
     */
    public int webCamThreadState;
    
    //possible values in the locks bitmask
    public final static int LEGS_LOCK = 1;
    public final static int PAN_LOCK = 2;
    public final static int TILT_LOCK = 4;
    public final static int UV_ON = 8;
    
    //All of the states the cerebellum thread can be in.
    public final static int CEREB_IDLE = 0;
    public final static int CEREB_STARTING = 1;
    public final static int CEREB_SCAN = 2;
    public final static int CEREB_TURNTO = 3;
    public final static int CEREB_DRIVETO = 4;
    
    //All of the states the webCam thread can be in.
    public final static int WEBCAM_IDLE = 0;
    public final static int WEBCAM_STOP = 1;
    
    public final static int WEBCAM_CYCLE_PIC = 50 ; //was 2;
    /**
     * @deprecated  As of PER 5.1.2, replaced by {@link #WEBCAM_CYCLE_NOPIC}
     */
    public final static int WEBCAM_CYCLY_NOPIC = 51; //was 3;
    //note, if we move to J2SE 5.0 replace the line above with the line below (or remove WEBCAM_CYCLY_NOPIC completely at that point)
    //@Deprecated public final static int WEBCAM_CYCLY_NOPIC = 51; //was 3;
    public final static int WEBCAM_CYCLE_NOPIC = 51; //was 3;
    public final static int WEBCAM_PIC = 52; //was 4;
    public final static int WEBCAM_GRAB = 2; //was 5;
    public final static int WEBCAM_PROPERTIES = 3; //was 6;
    public final static int WEBCAM_TRACK = 53; //was 50;
    public final static int WEBCAM_MEAN = 54; //was 51;
    public final static int WEBCAM_MOTION = 55; //was 52;
    
    /** Length of a packet from the rover. */
    public final static int FULL_PACKET_LENGTH = 16;
    
    //rover states...
    
    /** Command successful. */
    public final static int SUCCESS                    = 0;
    /** This code indicates that the rover is currently doing a DriveTo or TurnTo */
    public final static int HL_CONTINUE                = 5;
    
    //errors detected locally
    
    /** Response from Stayton is the wrong length. */
    public final static int INVALID_PACKET_LENGTH      = 10;
    /** Sent unknown packet type to Stayton. */
    public final static int UNKNOWN_PACKET_TYPE        = 11;
    /** Wireless communication not working. */
    public final static int COMM_DEAD                  = 12;
    /** Not connected to the robot. */
    public final static int NOT_CONNECTED              = 13;
    
    //nonfatal errors detected on the Stayton
    
    /** Command to the Stayton is the wrong length. */
    public final static int STAYTON_INVALID_LENGTH     = 30;
    /** The Stayton does not recognize the command sent to it. */
    public final static int STAYTON_UNKNOWN_TYPE       = 31;
    /** Function call to the Stayton with bad input arguments. */
    public final static int BAD_INPUT                  = 32;
    
    //simple errors detected on Stayton that are fatal to a high level command
    
    /** The cerebellum failed to respond. */
    public final static int CEREB_TIMEOUT              = 50;
    /** The cameara failed to respond. */
    public final static int CAMERA_TIMEOUT             = 51;
    /** Error on Stayton. */
    public final static int STAYTON_IO_ERROR           = 60;
    /** Battery voltage low. */
    public final static int BATTERY_LOW                = 70;
    
    //fatal errors from the Stayton for high level commands only
    
    /** Manually terminated. */
    public final static int KILLED                     = 100;
    /** IR Rangefinder detected an obstacle. */
    public final static int OBSTACLE_DETECTED          = 103;
    /** Came too close to a wall. */
    public final static int HIT_WALL                   = 104;
    
    /** What you're trying to do can't be done because the resource is already in use.
     * Look at the locks, cerebThreadState, and webCamThreadState variables. */
    public final static int RESOURCE_CONFLICT          = 120;
    
    /** Creates new RoverState */
    public RoverState() {
        dist = 0;
        position = new double[3];
    }
    
    /**
     * Parses a packet from the rover.
     * Computes coordinate changes.
     * @param packet The packet, which should be FULL_PACKET_LENGTH long.
     * @return true if it is the right length and the status is SUCCESS
     */
    public boolean parsePacket(byte [] packet) {
        if (packet == null || packet.length == 0) {
            status = COMM_DEAD;
            PER.rover.Log.println("No response, expected "+FULL_PACKET_LENGTH+" bytes!");
            return false;
        }
        if (packet.length != FULL_PACKET_LENGTH) {
            status = INVALID_PACKET_LENGTH;
            PER.rover.Log.println("Invalid packet; got "+packet.length+" bytes, expected "+FULL_PACKET_LENGTH+"!");
            return false;
        }
        
        status = ByteUtil.unsign(packet[0]);
        range = ByteUtil.unsign(packet[1]);
        pan = ByteUtil.networkShortToInt(packet, 2);
        tilt = ByteUtil.networkShortToInt(packet, 4);
        
        voltage = ByteUtil.unsign(packet[8]);
        //System.out.println("raw voltage: "+voltage+" read voltage: "+getRealVoltage());
        locks = ByteUtil.unsign(packet[9]);
        cerebThreadState = ByteUtil.unsign(packet[10]);
        webCamThreadState = ByteUtil.unsign(packet[11]);
        
        lastDist = dist;
        if(cerebThreadState == CEREB_TURNTO){
            dist = ByteUtil.networkShortToInt(packet, 6);
        }
        if(cerebThreadState == 4){
            dist = ByteUtil.networkShortToInt(packet, 6);
        }
        
        //       System.err.println("cereb thread reads " + cerebThreadState + " roverstate reads crabangle:  " + crabAngle);
        
        if(dist > lastDist){
            if(cerebThreadState == CEREB_TURNTO){
                position[2] = position[2] + distSign * (dist - lastDist);
                //                System.out.println("position 2 in state (theta) is " + position[2] + " direction is " + distSign);
                if(position[2] > 180)
                    position[2] -= 360;
                if(position[2] < -180)
                    position[2] += 360;
                
            }else if(cerebThreadState == CEREB_DRIVETO){
                position[0] = position[0] + (distSign * (dist - lastDist) * Math.cos(Math.toRadians(position[2] + crabAngle)));
                position[1] = position[1] + (distSign * (dist - lastDist) * Math.sin(Math.toRadians(position[2] + crabAngle)));
            }
        }else if(lastDist > dist){
            lastDist = 0;
        }
        return status == SUCCESS;
    }
    
    /**
     * Gets the last status code that the rover sent back.
     * @return the status code
     * @see #getStatusMessage
     */
    public int getStatus() {
        return status;
    }
    
    /**
     * Gives a description of the status of the last command sent.
     * @return the message
     */
    public String getStatusMessage() {
        return PER.rover.ActionConstants.getErrorText(status);
    }
    
    /**
     * Sets the current status; This should only be done in RoverController.
     */
    public void setStatus(int status) { this.status = status; }
    
    /**
     * Sets the current status; This should only be done in ReceiveThread.
     */
    public void setPan(int pan) { this.pan = pan; }
    
    /**
     * Sets the current status; This should only be done in ReceiveThread.
     */
    public void setTilt(int tilt) { this.tilt = tilt; }
    
    /** Set the current sign representing the direction of motion
     *  of the robot.  Used in calculating the coordinates, this
     *  should only be called by RoverController.
     */
    
    public void setSign(int sign){ this.distSign = sign; }
    
    /** Set the current angle representing the direction of motion
     *  of the robot regardless of it's heading.
     *  Used in calculating the coordinates, this
     *  should only be called by RoverController.
     */
    
    public void setCrabAngle(int cA){ this.crabAngle = cA;}
    
    /**
     * Sets the current coordinate position.
     * Call on the highLevelState, as that is where the coordinates
     * are stored.    The coordinates are centered on the robot
     * with the X axis running parallel to the rover.
     * @param x The new X coordinate in centimeters
     * @param y The new Y coordinate in centimeters
     * @param theta The new orientation, in integer degrees
     */
    
    public void setPosition(double x, double y, double theta){
        position[0] = x;
        position[1] = y;
        position[2] = theta;
    }
    
    /**
     * Returns the raw IR rangefinder reading from the cerebellum.  This is an
     * 8-bit value and typical values range from 0 to 140.  A value of 140 means
     * that an object is about 20cm away and the value falls off as the distance
     * increases.  The minimum useful reading is about 20 and that corresponds to
     * an object being about 150cm away.  For more information, see the data
     * sheet for the sensor at http://www.acroname.com/robotics/parts/gp2y0a02_e.pdf
     * @return The raw reading from the cerebellum.
     */
    public int getRange() {
        return range;
    }
    
    /**
     * This function attempts to translate the range from the range sensing output
     * of the sensor to centimeters.  I'm assuming that the object is more than
     * 15cm away for this converstion.  Raw values greater than 135 get mapped
     * to 20cm and raw values less than 25 get mapped to Integer.MAX_VALUE.
     * Other than that, the values returned by this function are very close to
     * the actual distance.
     * @return The distance in centimeters.
     */
    public int getRangeCM() {
        return PER.rover.ScanAction.translateScan(range);
    }
    
    /**
     * Gets the pan angle.  Positive angles are to the left of center.
     * @return The pan angles in degrees.
     */
    public int getPan() {
        return pan;
    }
    
    /**
     * Gets the tilt angle.  Positive angles are above level.
     * @return The tilt angle in degrees.
     */
    public int getTilt() {
        return tilt;
    }
    
    /**
     * Gets the raw voltage reading from the cerebellum.
     * @return the raw voltage reading, between 0 and 255.
     */
    public int getRawVoltage() {
        return voltage;
    }
    
    /**
     * Gets a translated voltage reading on the robot.  Fully charged batteries
     * should be at about 32V.  If the battery voltage falls below about 25V,
     * the rover is programmed to stop accepting commands.
     * As long as the voltage is above 29V, everything's fine.
     * @return the voltage of the batteries in volts
     */
    public double getRealVoltage() {
        return .204*voltage;
    }
    
    /** Returns the absolute value of how far the rover has traveled.
     * This is in cm if the last command was a goTo and degrees if
     * the last command was a turnTo.
     *
     * @return the absolute value of how far the rover has traveled
     *  (cm for goTo or degrees for turnTo)
     */
    public int getDist() {
        return dist;
    }
    
    /** Returns the current coordinates of the robot.
     *  Call on the highLevelState, as that is where the coordinates
     *  are stored.
     *  @return The current coordinates as doubles in a 3 element
     *    array. (x, y, theta in centimeters and degrees).  The coordinates
     *    are centered on the robot with the X axis running parallel to the rover.
     */
    
    public double[] getPosition(){
        return position;
    }
    
    /**
     * Function to help with high level commands.
     * @return true if the status returned caused the high level command to terminate.
     */
    public boolean isTerminationCondition() {
        return status == SUCCESS || status >= CEREB_TIMEOUT;
    }
}
