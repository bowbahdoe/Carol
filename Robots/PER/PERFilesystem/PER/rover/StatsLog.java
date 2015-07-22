/*
 * StatsLog.java
 *
 * Created on July 29, 2003, 4:20 PM
 */

package PER.rover;

import java.io.*;
import java.text.DateFormat;
import java.util.Date;

/** 
 * Allows for the logging of program events in a format easily read into Microsoft Excel
 * or other statistics software.
 *
 *@author Emily Hamner
 */

public class StatsLog {
    /** IMPORTANT: Change the version number whenever the logged events change
     * or the column header text changes. Version number is checked
     * and a new log file created if the version has changed,
     * otherwise the column headers will be incorrect.
     */
    private static final double VERSION = 1.6;
    private static Double oldVersion;
    
    private static PrintStream log;
    private static String defaultname = "statsLog.txt";
    
    //NOTE: if angles are reported same as events, angle (possibly dist) could be -1
    //so this might not work
    //private static String NE = "-1"; //non-event or no event
    private static String NE = ""; //non-event or no event
    //private static String delimeter = "\t";
    private static String delimeter = ",";
    
    //events and data values
    //start and stop screens
    public static final int INIT_LOG                    = 1;
    public static final int START_ATTRACT               = 2;
    public static final int STOP_ATTRACT                = 3;
    public static final int START_RECEIVING_PANORAMA    = 4;
    public static final int STOP_RECEIVING_PANORAMA     = 5;
    public static final int START_MISSION_CENTRAL       = 6;
    public static final int STOP_MISSION_CENTRAL        = 7;
    public static final int START_POV                   = 8;
    public static final int STOP_POV                    = 9;
    public static final int TRY_AGAIN_BUTTON            = 10;
    public static final int QUIT_BUTTON                 = 11;
    public static final int MIS_TIMEOUT                 = 12;
    public static final int POV_TIMEOUT                 = 13;
    public static final int TURN                        = 14; //use with angle value as well
    public static final int DRIVE                       = 15; //use with distance value as well
    public static final int FIND_ROCK                   = 16;
    public static final int APPROACH_ROCK               = 17;
    public static final int ANALYZE_ROCK                = 18;
    public static final int SCAN                        = 19;
    public static final int SEND_EMAIL                  = 20;
    public static final int PANORAMA                    = 21;
    public static final int TURN_HEAD                   = 22;
    public static final int OBSTACLE_DETECTED_ERROR     = 26;
    public static final int HIT_WALL_ERROR              = 27;
    public static final int CAMERA_TIMEOUT_ERROR        = 28;
    public static final int CEREB_TIMEOUT_ERROR         = 29;
    public static final int STAYTON_IO_ERROR            = 30;
    public static final int BATTERY_LOW_ERROR           = 31;
    public static final int COMM_DEAD_ERROR             = 32;
    public static final int WRONG_STAYTON_VERSION_ERROR = 33;
    public static final int KILLED_ERROR                = 35;
    public static final int CANT_START_ERROR            = 36;
    public static final int UNKNOWN_PACKET_TYPE_ERROR   = 37;
    public static final int INVALID_PACKET_LENGTH_ERROR = 38;
    public static final int SMTP_FAILED_ERROR           = 39;
    public static final int BAD_INPUT_ERROR             = 40;
    public static final int STAYTON_INVALID_LENGTH_ERROR = 41;
    public static final int STAYTON_UNKNOWN_TYPE_ERROR  = 42;
    public static final int HL_CONTINUE_ERROR           = 43;
    public static final int ERROR_SUCCESS               = 44;
    public static final int UNKNOWN_ERROR               = 45; //include error code??
    public static final int ERROR_FIXED                 = 46;
    public static final int ERROR_FATAL                 = 47;
    public static final int PROGRAM_CLOSED              = 48;
    
    private static int numEvents = 48;
    
    /** Creates a new instance of StatsLog */
    public StatsLog() {
    }
    
    /** Initializes the log file and prints the column headers to the file. If the
     * log is not initalized, strings will be printed to System.out.
     */
    public static void initLogFile(String name) {
        File file = null;
        if (name == null)
            file = new File(Rover.getTopLevelDir(), defaultname);
        else
            file = new File(Rover.getTopLevelDir(), name);
        
        //check version number
        if(!versionIsCurrent()){
            PER.rover.Log.println("StatsLog is out of date.");
            //try to rename the old file
            if(file.renameTo(new File(file.getParent(),"version"+oldVersion.toString()+file.getName()))){
                PER.rover.Log.println("Old version of statsLog renamed to version"+oldVersion.toString()+file.getName());
            }else{
                //file could not be renamed.
                PER.rover.Log.println("Old version of statsLog could not be renamed. Old version will be deleted.");
            }
            //delete the out of date file
            file.delete();
            //create a new file
            if (name == null)
                file = new File(Rover.getTopLevelDir(), defaultname);
            else
                file = new File(Rover.getTopLevelDir(), name);
            //save the new version number
            saveVersion();
            PER.rover.Log.println("New version of statsLog created.");
        }
        
        boolean fileCreated = false;
        try {
            fileCreated = file.createNewFile();
        } catch (IOException e) {
            try {
                file = new File(defaultname);
                fileCreated = file.createNewFile();
            } catch (IOException ee) {
                log = null;
                return;
            }
        }
        
        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            log = new PrintStream(fos, true);
            
            if(fileCreated){
                PER.rover.Log.println("New statsLog file created.");
                //record StatsLog version number
                log.println("StatsLogVersion"+VERSION);
                //print colum headers
                String str = "";
                for(int i = 1; i<=numEvents; i++){
                    str = str.concat(columnHeader(i)+delimeter);
                }
                log.println(str);
            }else{
                PER.rover.Log.println("Found existing statsLog file.");
            }
        } catch (Exception e) {
            log = null;
        }
        //first event recorded each time program is executed
        PER.rover.StatsLog.print(INIT_LOG);
    }
    
    /** Checks if the stats log file on the machine is of the current version.
     *
     *@returns True if the version is current, false if the file is an old version.
     */
    private static boolean versionIsCurrent(){
        try {
            //open the stored version file
            FileInputStream istream = new FileInputStream(new File(Rover.getTopLevelDir(), "statsVersion"));
            ObjectInputStream p = new ObjectInputStream(istream);
            oldVersion = (Double)p.readObject();
            istream.close();
            //check if the stored version matches the current version
            if (oldVersion.doubleValue() == VERSION)
                return true;
            else return false;
        } catch (Exception e) {
            //if no version file is found, return false
            oldVersion = new Double(0);
            Log.println("Loading the stats log version failed: " + e);
            return false;
        }
    }
    
    /** Saves the current version to a file. */
    private static void saveVersion(){
        try {
            FileOutputStream ostream = new FileOutputStream(new File(Rover.getTopLevelDir(), "statsVersion"));
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            
            p.writeObject(new Double(VERSION));
            
            p.flush();
            ostream.close();
        } catch (Exception e) {
            Log.println("Saving the stats log version failed: "+e);
        }
    }
    
    /** Returns a string formatted with a timestamp for the given event
     * and the non-event code for all other events.
     */
    private static String formatString(int event){
        String str = "";
        for(int c = 1; c <= numEvents; c++){
            if(c == TURN || c == DRIVE)
                str = str.concat(NE+delimeter+NE+delimeter);
            else if(c != event)
                str = str.concat(NE+delimeter);
            else
                str = str.concat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date())+delimeter);
        }
        return str;
    }
    
    private static String formatString(int event, int value){
        String str = "";
        for(int c = 1; c <= numEvents; c++){
            if(c == event){
                str = str.concat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date())+delimeter);
                str = str.concat(value+delimeter);
            }
            else if(c == TURN || c == DRIVE)
                str = str.concat(NE+delimeter+NE+delimeter);
            else if(c != event)
                str = str.concat(NE+delimeter);
        }
        return str;
    }
    
    /** Returns the string describing the given event.
     */
    private static String columnHeader(int event){
        switch (event){
            case INIT_LOG:                  return "Log Initialized";
            case START_ATTRACT:             return "Start Attract";
            case STOP_ATTRACT:              return "Stop Attract";
            case START_RECEIVING_PANORAMA:  return "Start Receiving Panorama";
            case STOP_RECEIVING_PANORAMA:   return "Stop Receiving Panorama";
            case START_MISSION_CENTRAL:     return "Start Mission Central";
            case STOP_MISSION_CENTRAL:      return "Stop Mission Central";
            case START_POV:                 return "Start Rover POV";
            case STOP_POV:                  return "Stop Rover POV";
            case QUIT_BUTTON:               return "Quit Button Clicked";
            case TRY_AGAIN_BUTTON:          return "Try Again Button Clicked";
            case MIS_TIMEOUT:               return "Mission Central Timeout";
            case POV_TIMEOUT:               return "Rover POV Timeout";
            case TURN:                      return "Turn"+delimeter+"Angle";
            case DRIVE:                     return "Drive"+delimeter+"Distance";
            case FIND_ROCK:                 return "Find Rock";
            case APPROACH_ROCK:             return "Approach Rock";
            case ANALYZE_ROCK:              return "Analyze Rock";
            case SCAN:                      return "Scan For Rock";
            case SEND_EMAIL:                return "Send Email";
            case PANORAMA:                  return "Take Panorama";
            case TURN_HEAD:                 return "Turn Head";
            case OBSTACLE_DETECTED_ERROR:   return "Obstacle Detected";
            case HIT_WALL_ERROR:            return "Wall";
            case CAMERA_TIMEOUT_ERROR:      return "CAMERA_TIMEOUT";
            case CEREB_TIMEOUT_ERROR:       return "CEREB_TIMEOUT";
            case STAYTON_IO_ERROR:          return "STAYTON_IO_ERROR";
            case BATTERY_LOW_ERROR:         return "Low Battery";
            case COMM_DEAD_ERROR:           return "COMM_DEAD";
            case WRONG_STAYTON_VERSION_ERROR:return "Wrong Stayton Version";
            case KILLED_ERROR:              return "Manually Terminated";
            case CANT_START_ERROR:          return "Tracking Not On Error";
            case UNKNOWN_PACKET_TYPE_ERROR: return "Unknown Packet Type";
            case INVALID_PACKET_LENGTH_ERROR:return "Invalid Packet Length";
            case SMTP_FAILED_ERROR:         return "SMTP_FAILED";
            case BAD_INPUT_ERROR:           return "BAD_INPUT";
            case STAYTON_INVALID_LENGTH_ERROR: return "STAYTON_INVALID_LENGTH";
            case STAYTON_UNKNOWN_TYPE_ERROR:return "STAYTON_UNKNOWN_TYPE";
            case HL_CONTINUE_ERROR:         return "HL_CONTINUE";
            case ERROR_SUCCESS:             return "Error Success";
            case UNKNOWN_ERROR:             return "Unknown Error"; //include error code??
            case ERROR_FIXED:               return "Recovered From Error";
            case ERROR_FATAL:               return "Error Was Fatal";
            case PROGRAM_CLOSED:            return "Program Closed";
            
            default:                        return "No such event or value";
        }
    }
    
    
    /** Prints a line to the logfile with a timestamp for the given event.
     */
    public static synchronized void println(int event) {
        if (log == null)
            System.out.println(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date())+":  "+columnHeader(event));
        else{
            log.println(formatString(event));
        }
    }
    
    /** Prints a line to the logfile with a timestamp for the given event
     * and prints the data value.
     */
    public static synchronized void println(int event, int value) {
        if (log == null)
            System.out.println(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date())+":  "+columnHeader(event)+" "+value);
        else{
            log.println(formatString(event,value));
        }
    }
    
    /** Print a line to the logfile with a timestamp for the given event.
     * If printToConsole is true, also print a timestamp and column header to System.out.
     */
    public static synchronized void println(int event, boolean printToConsole) {
        println(event);
        if(log != null && printToConsole)
            System.out.println(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date())+":  "+columnHeader(event));
    }
    
    /** Print a line to the logfile with a timestamp for the given event
     * and print the data value.
     * If printToConsole is true, also print a timestamp and column header to System.out.
     */
    public static synchronized void println(int event, int value, boolean printToConsole) {
        println(event,value);
        if(log != null && printToConsole)
            System.out.println(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date())+":  "+columnHeader(event)+" "+value);
    }
    
    /** Print a string to the logfile with a timestamp for the given event.
     */
    public static synchronized void print(int event) {
        println(event);
    }
    
    /** Print a string to the logfile with a timestamp for the given event
     * and print the data value.
     */
    public static synchronized void print(int event, int value) {
        println(event,value);
    }
    
    /** Print a string to the logfile with a timestamp for the given event.
     * If printToConsole is true, also print a timestamp and column header to System.out.
     */
    public static synchronized void print(int event, boolean printToConsole) {
        println(event,printToConsole);
    }
    
    /** Print a string to the logfile with a timestamp for the given event
     * and print the data value.
     * If printToConsole is true, also print a timestamp and column header to System.out.
     */
    public static synchronized void print(int event, int value, boolean printToConsole) {
        println(event,value,printToConsole);
    }
    
}
