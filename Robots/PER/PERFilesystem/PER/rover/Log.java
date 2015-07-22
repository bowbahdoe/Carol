/*
 * Log.java
 *
 * Created on April 17, 2003, 3:22 PM
 */

package PER.rover;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;


/**
 * A human-readable output log for the rover. Any string printed to the log file will
 * be prefaced by a time stamp. Other than the time stamp, strings do not have
 * any special format requirements. 
 *
 * @author  Rachel Gockley
 */
public class Log {
    private static PrintStream log;
    private static String defaultname = "rover.Log";
    private static javax.swing.JTextArea linkedTextArea = null;
    
    /** Creates a new instance of Log */
    public Log() {
    }
    
    /** Initializes the log file with the given name. If the log has not been
     * initalized, all strings will be printed to System.out.
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
            log.println("Log initialized: " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()));
        } catch (Exception e) {
            log = null;
        }
    }

    /** Initializes the log file with the given name and associated JTextArea.
     * If a non-null textArea is given, strings printed to the log and console
     * will also be printed to the textArea. If the log has not been initalized,
     * all strings will be printed to System.out.
     */
    public static void initLogFile(String name, javax.swing.JTextArea textArea) {
        linkedTextArea = textArea;
        initLogFile(name);
    }
    
    /** Print a line to the logfile */
    public static synchronized void println(String str) {
        if (log == null)
            System.out.println(str);
        else
            log.println(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()) + ":  " + str);
    }
    
    /** Print a line to the logfile. If printToConsole is true, then echo the
     * line to System.out.
     */
    public static synchronized void println(String str, boolean printToConsole) {
        if (log == null) {
            System.out.println(str);
        } else {
            log.println(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()) + ":  " + str);
            
            if (printToConsole){
                System.out.println(str);
                
                if(linkedTextArea != null)
                    linkedTextArea.append(str + "\n");
            }
        }
    }
    
    /** Print a string to the logfile. */
    public static synchronized void print(String str) {
        if (log == null)
            System.out.print(str);
        else
            log.print(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()) + ":  " + str);
    }
    
    /** Print a string to the logfile. If printToConsole is true, echos the
     * string to System.out.
     */
    public static synchronized void print(String str, boolean printToConsole) {
        if (log == null) {
            System.out.print(str);
        } else {
            log.print(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()) + ":  " + str);
            log.flush();
            
            if (printToConsole){
                System.out.print(str);

                if(linkedTextArea != null)
                    linkedTextArea.append(str);
            }
        }
    }
}
