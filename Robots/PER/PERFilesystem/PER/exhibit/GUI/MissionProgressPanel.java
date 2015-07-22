/*
 * MissionProgressPanel.java
 *
 * Created on July 9, 2003, 9:32 AM
 */

package PER.exhibit.GUI;

import PER.rover.Action;
import javax.swing.Timer;

/**
 * Acts as a liason between Sequencer and RoverPOVScreen so that the mission
 * status being displayed on screen matches the status of the the mission as
 * it is executed by the rover.
 *
 * @author  Rachel Gockley
 */
public class MissionProgressPanel extends javax.swing.JPanel {
    
    /** Creates new form MissionProgressPanel */
    public MissionProgressPanel(PER.exhibit.Sequencer s) {
        seq = s;

        // initialize timers
        turnTimer = new Timer(10, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timerActionPerformed(turnTimer, turnAction);
            }
        });
        
        driveTimer = new Timer(10, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timerActionPerformed(driveTimer, driveAction);
            }
        });
        
        findTimer = new Timer(10, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timerActionPerformed(findTimer, findAction);
            }
        });
        
        approachTimer = new Timer(10, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timerActionPerformed(approachTimer, approachAction);
            }
        });
        
        analyzeTimer = new Timer(10, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timerActionPerformed(analyzeTimer, analyzeAction);
            }
        });
        
        turnTimer.setInitialDelay(0);
        driveTimer.setInitialDelay(0);
        findTimer.setInitialDelay(0);
        approachTimer.setInitialDelay(0);
        analyzeTimer.setInitialDelay(0);
    }
    
    private void timerActionPerformed(Timer t, Action a) {
        if (a.isCompleted()) {
            current = max;
            t.stop();
            if(!a.isSuccess()){
                //if(rov.mySequencer.getStatus() == rov.mySequencer.ERROR){
                if(seq.getStatus() == seq.ERROR){
                    //System.out.println("mpp - action completed with error");
                    status = "Error.";
                    done = true;
                }
            }
        } else {
            current = max - a.getTimeRemaining();
        }
    }
    
    private void setRunning_safe(Action act, Timer timer) {
        int time = act.getTime();
        if (time < 1)
            time = 1;
        max=time;
        timer.start();
    }
    
    //public void resetPanel(int deg, int cm) {
    public void resetPanel() {
        done = false;
        turnTimer.stop();
        driveTimer.stop();
        findTimer.stop();
        approachTimer.stop();
        analyzeTimer.stop();
        turnAction = driveAction = findAction = approachAction = analyzeAction = null;
    }
    
    public synchronized void startTurn(Action tta) {
        if (tta == null)
            return;
        
        status = "Turning.";
        
        turnAction = tta;
        setRunning_safe(turnAction, turnTimer);
    }
    
    public synchronized void startDrive(Action dta) {
        if (dta == null)
            return;
        
        status = "Driving.";
        
        driveAction = dta;
        setRunning_safe(driveAction, driveTimer);
    }
    
    public synchronized void startFind(Action fra) {
        if (fra == null)
            return;
        
        status = "Scanning for nearest rock...";
        
        findAction = fra;
        setRunning_safe(findAction, findTimer);
    }
    
    public synchronized void startApproach(Action a) {
        if (a == null)
            return;
        
        status = "Approaching rock...";
        
        approachAction = a;
        setRunning_safe(approachAction, approachTimer);
    }
    
    public synchronized void startAnalyze(Action ara) {
        if (ara == null)
            return;
        
        status = "Analyzing rock...";
        
        analyzeAction = ara;
        setRunning_safe(analyzeAction, analyzeTimer);
    }
    
    public synchronized void setMissionCompleted() {
        status = "Mission has ended. Preparing report...";
        done = true;
        timeDone = System.currentTimeMillis();
    }
    
    public synchronized String getStatus() {
        return status;
    }
    
    /* range of 0.0 to 1.0 
      This function really returns 1 - the fraction done -EP */
    public double getPercentDone(){
        return ((double)(max - current)) / ((double)max);
    }
    
    public synchronized boolean isCompleted() {
        return done;
    }
    
    //hack for displaying error message when error in panorama
    public synchronized void setCompleted() {
        done = true;
    }
    
    private PER.exhibit.Sequencer seq;
    
    //private String status = null;
    private String status = "";
    private boolean done = false;
    private long timeDone;
    
    private Timer turnTimer;
    private Timer driveTimer;
    private Timer findTimer;
    private Timer approachTimer;
    private Timer analyzeTimer;
    
    private Action turnAction;
    private Action driveAction;
    private Action findAction;
    private Action approachAction;
    private Action analyzeAction;
    
    private int max;
    private int current;
}
