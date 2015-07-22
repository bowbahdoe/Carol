/*
 * Screen.java
 *
 * Created on July 14, 2003, 4:24 PM
 */

package PER.exhibit.GUI;

/**
 * Interface for all screens displayed by the kiosk software.
 *
 * @author  Rachel Gockley
 */
public interface Screen {
    
    /** Initializes the screen and prepares it to be displayed. 
     */
    public void start();
    
    /** Performs any cleanup, such as stoping timers, when the screen
     * stops being displayed.
     */
    public void stop();

    /** If the screen has a clock, sets the clock text to the given string, 
     * otherwise does nothing.
     */
    public void setClockTime(String s);
}
