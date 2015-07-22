/*
 * ActionConstants.java
 *
 * Created on February 13, 2003, 2:30 PM
 */

package PER.rover;

import PER.rover.control.RoverState;

/**
 * Allows the constants representing rover status to be translated
 * into more descriptive strings. Also contains some of the constants
 * used as Action return values and allows errors to be logged in the
 * StatsLog.
 * 
 *@see Action#getReturnValue()
 *@see RoverState
 * 
 * @author  Rachel Gockley
 */

public class ActionConstants extends RoverState{
   /* some return values */
   /** Can't begin action because it can't see landmark */
   //public final static int LANDMARK_NOTSEEN = -1;
   /** The version of code running on the Stayton is out of date */
   public final static int WRONG_STAYTON_VERSION = -2;
   /** Couldn't talk to the SMTP server */
   public final static int SMTP_FAILED = -3;
   /** I don't know what happened */
   public final static int UNKNOWN_ERROR = -6;
   /** Can't find a rock that should be there */
   public final static int NO_ROCK = -7;
   /** Did not detect any motion */
   public final static int NO_MOTION_DETECTED = -8;
   
    /**
     * Gives a description of an error code.
     * @param err The error code
     * @return A message describing the error.
     */
    public static String getErrorText(int err) {
        switch (err) {
            case(OBSTACLE_DETECTED):
                return "Stopped early because IR rangefinder detected an obstacle.";
            case(HIT_WALL):
                return "Came too close to a wall.";
                
            case (CAMERA_TIMEOUT):
                return "Robot error: webCam stopped responding";
            case CEREB_TIMEOUT:
                return "Robot error: The cerebellum failed to respond";
           case STAYTON_IO_ERROR:
               return "Robot error: Error on Stayton opening/closing configuration file";
           case BATTERY_LOW:
               return "Robot error: Battery voltage low";
                
            case (COMM_DEAD):
                return "Stayton error: Wireless communication not working";
            case WRONG_STAYTON_VERSION:
                return "Stayton error: The Stayton code is out of date";
            case RESOURCE_CONFLICT:
                return "Stayton error: Resource already in use!";
                
            case (KILLED):
                return "Stopped: Manually terminated";
                
            case UNKNOWN_PACKET_TYPE:
                return "Internal error: send unknown packet type to Stayton";
            case INVALID_PACKET_LENGTH:
                return "Internal error: response from Stayton is the wrong length";
            case STAYTON_INVALID_LENGTH:
                return "Internal error: command to Stayton is the wrong length";
            case BAD_INPUT:
                return "Internal error: Function call to the Stayton with bad input arguments";
            case NOT_CONNECTED:
                return "Internal error: You must connect to the robot first!";
                
            case SMTP_FAILED:
                return "Non-fatal error: Sending the email failed";
            case NO_ROCK:
                return "Rock can no longer be located";
            case NO_MOTION_DETECTED:
                return "No motion was detected.";
                
            case STAYTON_UNKNOWN_TYPE:
                return "STYATON_UNKNOWN_TYPE";
            case HL_CONTINUE:
                return "HL_CONTINUE";
                
            case SUCCESS:
                return "Success";
            default:
                return "Unknown error ("+err+")";
        }
    }
    
    /** Notes the error in the StatsLog.
     *
     *@see StatsLog
     */
    public static void logErrorStats(int err) {
        switch (err) {
            case(OBSTACLE_DETECTED):
                StatsLog.print(StatsLog.OBSTACLE_DETECTED_ERROR);
                break;
            case(HIT_WALL):
                StatsLog.print(StatsLog.HIT_WALL_ERROR);
                break;
            case (CAMERA_TIMEOUT):
                StatsLog.print(StatsLog.CAMERA_TIMEOUT_ERROR);
                break;
            case CEREB_TIMEOUT:
                StatsLog.print(StatsLog.CEREB_TIMEOUT_ERROR);
                break;
            case STAYTON_IO_ERROR:
                StatsLog.print(StatsLog.STAYTON_IO_ERROR);
                break;
            case BATTERY_LOW:
                StatsLog.print(StatsLog.BATTERY_LOW_ERROR);
                break;
            case (COMM_DEAD):
                StatsLog.print(StatsLog.COMM_DEAD_ERROR);
                break;
            case WRONG_STAYTON_VERSION:
                StatsLog.print(StatsLog.WRONG_STAYTON_VERSION_ERROR);
                break;
            case (KILLED):
                StatsLog.print(StatsLog.KILLED_ERROR);
                break;
            case UNKNOWN_PACKET_TYPE:
                StatsLog.print(StatsLog.UNKNOWN_PACKET_TYPE_ERROR);
                break;
            case INVALID_PACKET_LENGTH:
                StatsLog.print(StatsLog.INVALID_PACKET_LENGTH_ERROR);
                break;
            case SMTP_FAILED:
                StatsLog.print(StatsLog.SMTP_FAILED_ERROR);
                break;
            case BAD_INPUT:
                StatsLog.print(StatsLog.BAD_INPUT_ERROR);
                break;
            case STAYTON_INVALID_LENGTH:
                StatsLog.print(StatsLog.STAYTON_INVALID_LENGTH_ERROR);
                break;
            case STAYTON_UNKNOWN_TYPE:
                StatsLog.print(StatsLog.STAYTON_UNKNOWN_TYPE_ERROR);
                break;
            case HL_CONTINUE:
                StatsLog.print(StatsLog.HL_CONTINUE_ERROR);
                break;
            case SUCCESS:
                StatsLog.print(StatsLog.ERROR_SUCCESS);
                break;
            default:
                StatsLog.print(StatsLog.UNKNOWN_ERROR);
        }
    }
    
    /** Returns true if the error is fatal to rover operations, false
     * if it is not. Currently only SMTP_FAILED is a non-fatal error.
     */
    public static boolean isFatalError(int error) {
        switch (error) {
            case SMTP_FAILED:
                return false;
            default:
                return true;
        }
    }
}
