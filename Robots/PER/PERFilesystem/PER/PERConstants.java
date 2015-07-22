/*
 * PERConstants.java
 *
 * Created on February 17, 2004, 3:37 PM
 */

package PER;

/**
 * Contains constants relevant to the PER software as a whole, such as the
 * current version number of the software and the file sytem path.
 *
 * @author  Emily Hamner
 */
public final class PERConstants {
    
    /** The current version number of the PER software. */
    //public static final double VERSION = 5.1;
    public static final String VERSION = "5.2";
    /** The path of the PERFilesystem */
    public static final String filesystemPath = "c:/PERFilesystem/";
    /** The path to the PER package */
    public static final String perPath = filesystemPath + "PER/";
    
    
    /** Creates a new instance of Version */
    public PERConstants() {
    }
    
}
