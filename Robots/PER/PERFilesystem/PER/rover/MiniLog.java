/*
 * MiniLog.java
 *
 * Created on April 17, 2003, 3:22 PM
 */

package PER.rover;

import java.io.*;
import java.text.DateFormat;
import java.util.Date;

/**
 * A comma delimited log file that records the time, outcome, and length of
 * rover missions. This log file was primarily designed to aid in statistical
 * analysis of the PER Exhibits.
 *
 * @author  Rachel Gockley
 */
public class MiniLog {
    private static PrintStream log;
    private static String defaultname = "miniLog.txt";
    //date format used to format date
    private static java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMMM d yyyy h:mm:ss a z");
    
    /** Creates a new instance of MiniLog */
    public MiniLog() {
        //sdf = new java.text.SimpleDateFormat("MMMMM d yyyy h:mm:ss a z");
    }
    
    /** Initializes the miniLog file with the given name. If the log has not
     * been initalized, all strings will be printed to System.out.
     */
    public static void initLogFile(String name) {
        File file = null;
        if (name == null)
            file = new File(Rover.getTopLevelDir(), defaultname);
        else
            file = new File(Rover.getTopLevelDir(), name);
        
        try {
            file.createNewFile();
        } catch (IOException e) {
            try {
                file = new File(defaultname);
                file.createNewFile();
            } catch (IOException ee) {
                log = null;
                return;
            }
        }
        
        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            log = new PrintStream(fos, true);
            log.println("");
            //log.println("****");
            log.println("**** PER version "+PER.PERConstants.VERSION+" ****");
            //log.println("Log initialized, " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()));
            log.println(sdf.format(new Date())+", Log initialized");
        } catch (Exception e) {
            log = null;
        }
    }
    
    /** Print a line to the logfile */
    public static synchronized void println(String str) {
        if (log == null)
            System.out.println(str);
        else
            //log.println(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()) + ",  " + str);
            log.println(sdf.format(new Date()) + ",  " + str);
    }
    
    /** Print a line to the logfile. If printToConsole is true, then echo the
     * line to System.out.
     */
    public static synchronized void println(String str, boolean printToConsole) {
        if (log == null) {
            System.out.println(str);
        } else {
            //log.println(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()) + ",  " + str);
            log.println(sdf.format(new Date()) + ",  " + str);
            
            if (printToConsole)
                System.out.println(str);
        }
    }
    
    /** Print a string to the logfile. */
    public static synchronized void print(String str) {
        if (log == null)
            System.out.print(str);
        else
            //log.print(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()) + ",  " + str);
            log.print(sdf.format(new Date()) + ",  " + str);
    }
    
    /** Print a string to the logfile. If printToConsole is true, echos the
     * string to System.out.
     */
    public static synchronized void print(String str, boolean printToConsole) {
        if (log == null) {
            System.out.print(str);
        } else {
            //log.print(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()) + ",  " + str);
            log.print(sdf.format(new Date()) + ",  " + str);
            log.flush();
            
            if (printToConsole)
                System.out.print(str);
        }
    }
}
