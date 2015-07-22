/*
 * Action.java
 *
 * Created on June 4, 2002, 3:26 PM
 */

package PER.rover;
import PER.rover.control.*;

/**
 * Interface for all Rover "actions" -- turning, moving, etc. All actions are
 * assumed to be "safe"; they should always avoid collisions independently (and
 * return false if they do)!
 * <p>
 * This used to be interface Motion, but was changed to allow for non-motion-related
 * Rover commands, such as looking for a landmark or sending a message.
 *
 * @author  Rachel Gockley
 * @version 2.0
 */
public interface Action extends java.io.Serializable {
    /**
     * Tries to start the action. Returns whether the action started.
     */
    boolean doAction(Rover r);  // should be blocking
    
    /**
     * Emergency stop - end the action immediately, if it's running.
     */
    void kill();
    
    /** Whether the action completed successfully. Undefined until isCompleted() returns
     * true.
     */
    boolean isSuccess();
    
    /** Whether the action has completed. Undefined until doAction has been called.
     */
    boolean isCompleted();
    
    /** How long the action will take, in milliseconds. */
    int getTime();
    
    /** How much time until the action finishes (in milliseconds), if it has already
     * started. Undefined behavior if the action has not yet begun, or has finished.
     */
    int getTimeRemaining();
    
    /** The return value of the Action. Zero is a success. Negative implies one
     * of this class's constants. Other values should be interpreted as
     * appropriate.
     *
     *@see PER.rover.control.RoverState
     *@see PER.rover.ActionConstants
     */
    int getReturnValue();
    
    /** Provides a textual explanation of the Action, such as "turn 90 degrees" */
    String getSummary();
    
    /** Provides a shortened version of the summary returned by getSummary. For
     * example, getSummary may return something like "Turn left and drive about
     * 39 inches toward the red landmark," whereas getShortSummary might just
     * return "Drive toward a landmark."
     */
    String getShortSummary();
    
    /** Returns the most recent image taken by this Action. The image will be null if
     * the Action does not take pictures or the first picture has not yet been taken.
     *
     * @return the most recent picture taken by the Action.
     */
    public java.awt.image.BufferedImage getRecentImage();
    
    /** Lets you know when the last image was taken by this action. If 
     * the Action does not take pictures or the first picture has not yet
     * been taken, will return 0.
     *
     * @return the system time in milliseconds when the last image was taken
     * or 0 if no images have been taken
     */
    public long getImageUpdateTime();
    
}
