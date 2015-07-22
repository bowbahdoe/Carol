package PER.rover;

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;


/** Filter is used to specify the type of FileFilter for a JFileChooser.
 */
public class Filter extends FileFilter {
    /** Filter for gif, jpg, jpeg, tif, tiff, or png files. */
    public static final int IMAGE_FILTER = 0;
    /** Filter for txt files. */
    public static final int TEXT_FILTER = 1;
    /** Filter for cal files. */
    public static final int CALIBRATION_FILTER = 2;
    /** Filter for scan files. */
    public static final int SCAN_FILTER = 3;
    /** Filter for jpg or jpeg files. */
    public static final int JPG_FILTER = 4;
    
    private int type; //the type of this filter. image, text, scan etc.
    
    /** Creates of new filter of type <code>type</code>.
     */
    public Filter(int type){
        super();
        this.type = type;
    }
    
    /** Accepts all directories, and accepts files based on the filter type.
     */
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        
        String extension = getExtension(f);
        
        switch (type){
            case IMAGE_FILTER:
                if (extension != null) {
                    if (extension.equals("tiff") ||
                    extension.equals("tif") ||
                    extension.equals("gif") ||
                    extension.equals("jpeg") ||
                    extension.equals("jpg") ||
                    extension.equals("png"))
                        return true;
                    else
                        return false;
                }
                break;
            case TEXT_FILTER:
                if (extension != null) {
                    if (extension.equals("txt"))
                        return true;
                    else
                        return false;
                }
                break;
            case CALIBRATION_FILTER:
                if (extension != null) {
                    if (extension.equals("cal"))
                        return true;
                    else
                        return false;
                }
                break;
            case SCAN_FILTER:
                if (extension != null) {
                    if (extension.equals("scan"))
                        return true;
                    else
                        return false;
                }
                break;
            case JPG_FILTER:
                if (extension != null) {
                    if (extension.equals("jpeg") ||
                    extension.equals("jpg"))
                        return true;
                    else
                        return false;
                }
                break;
        }
        return false;
    }
    
    /** Returns the description of this filter.
     */
    public String getDescription() {
        switch (type){
            case IMAGE_FILTER:
                return "Images";
            case TEXT_FILTER:
                return "Text Files";
            case CALIBRATION_FILTER:
                return "Calibration Files";
            case SCAN_FILTER:
                return "Scan Files";
            case JPG_FILTER:
                return "jpg or jpeg";
            default:
                return  "";
        }
    }
    
    /**
     * Gets the extension of a file.
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        
        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
}
