/*
 * RoverPOVPanel.java
 *
 * Created on June 26, 2003, 9:02 AM
 */

package PER.exhibit.GUI;

import PER.exhibit.Sequencer;
import PER.rover.Rover;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Shows images from the rover and the rover's current status as it executes
 * a mission. Also displays the final image taken at the end of the mission
 * which the Mission Scientist analyses for signs of life.
 *
 * @author  Emily Hamner
 */


public class RoverPOVScreen extends javax.swing.JPanel implements Screen {
    private static final int ERROR_FIXED = 999;
    private static final int ERROR_FATAL = 998;
    
    /** Creates new form RoverPOVPanel */
    public RoverPOVScreen(Rover r, MissionProgressPanel mpanel, Sequencer s) {
        rov = r;
        mpp = mpanel;
        seq = s;
        degrees = 0;
        distance = 0;
        showCountdown(true);
        doScan(true); 
        
        initComponents();
        initTracks();
        initOtherComponents();
        initImages();
        
        
        update = new Timer(10, new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setFeedbackLabel(mpp.getStatus());
                
                //if(mpp.getStatus().equals(new String("Turning.")) && (mpp.getPercentDone() >= 0.0 && mpp.getPercentDone() <= 1.0 )){
                if(mpp.getStatus().equals("Turning.")){
                    int deg = degrees - (int)((double)degrees * mpp.getPercentDone());
                    //System.out.println(mpp.getStatus() + " -- " + mpp.getPercentDone());
                    //System.out.println("deg="+deg + " degrees=" + degrees);
                    missionBoxRover.rotate((int)((double)degrees*mpp.getPercentDone()));
                    /*if (deg == 0)
                        currentDirLabel.setText("0" + '\u00b0');
                    else if (deg > 0)
                        currentDirLabel.setText("" + deg + '\u00b0'+" left");
                    else
                        currentDirLabel.setText("" + (-1 * deg) + '\u00b0'+" right");*/
                    if(degrees >= 0)
                        currentDirLabel.setText("" + rov.highLevelState.getDist() + '\u00b0'+" left");
                    else
                        currentDirLabel.setText("" + rov.highLevelState.getDist() + '\u00b0'+" right");
                }
                if(mpp.getStatus().equals("Driving.") && (mpp.getPercentDone() >= 0.0 && mpp.getPercentDone() <= 1.0 )){
                    int dist = distance - (int)((double)distance * mpp.getPercentDone());
                    
                    //System.out.println(mpp.getStatus() + " -- " + mpp.getPercentDone());
                    //System.out.println("dist="+dist + " distance=" + distance);
                    //currentDirLabel.setText(""+dist+" cm");
                    currentDirLabel.setText(""+rov.highLevelState.getDist()+" cm");
                }
                if(mpp.isCompleted()){
                    //mission is complete. stop updating status and video
                    //i think we want to do this now no matter what the status of the mission
                    update.stop();
                    video.stop();
                    //check mission status
                    //switch (rov.mySequencer.getStatus()) {
                    switch (seq.getStatus()) {
                        case Sequencer.SUCCESS:
                            //mission successful
                            if(doScan){
                                //show analyze scan
                                if(!analyzeTimer.isRunning()){
                                    //java.awt.Image img = rov.mySequencer.getLastMissionNoUVImage();
                                    java.awt.Image img = seq.getLastMissionNoUVImage();
                                    if(img != null)
                                        roverPOV.setIcon(new ImageIcon(img.getScaledInstance(1024, 768, java.awt.Image.SCALE_SMOOTH)));
                                    //img = rov.mySequencer.getLastMissionImage();
                                    img = seq.getLastMissionImage();
                                    if(img != null)
                                        roverAnalyzePOV.setIcon(new ImageIcon(img.getScaledInstance(1024, 768, java.awt.Image.SCALE_SMOOTH)));
                                    //update.stop();
                                    //video.stop();
                                    analyzeTimer.start();
                                }
                            }else{
                                //skip the scan
                                //analyzeTimer.stop(); //finish after final delay
                                done = true; //finish directly, no final delay
                            }
                            PER.rover.MiniLog.println("SUCCESS, "+clock.getClockTime());
                            PER.rover.Log.println("Mission result: SUCCESS");
                            break;
                        case Sequencer.ERROR:
                            //mission ended due to rover error - try to recover
                            /////String errorMsg = ActionConstants.getErrorText(rov.mySequencer.getCurrentAction().getReturnValue());
                            ///String errorMsg = ActionConstants.getErrorText(seq.getCurrentAction().getReturnValue());
                            if(fixError()){
                                //rover recovered from error
                                showErrorMsg(ERROR_FIXED);
                                errorCaseTimeoutTimer.start();
                                //countdownTimer.start();
                            }else {
                                //error could not be fixed - notify museum staff error message
                                showErrorMsg(ERROR_FATAL);
                                //don't start the countdown
                            }
                            break;
                        default:
                            //showErrorMsg(rov.mySequencer.getStatus());
                            showErrorMsg(seq.getStatus());
                            errorCaseTimeoutTimer.start();
                            //countdownTimer.start();
                            break;
                    }
                }
            }
        });
        update.setInitialDelay(0);
        
        video = new Timer(300, new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if(!mpp.getStatus().equals(new String("Driving.")))
                    missionBoxRover_head.rotate(-rov.state.getPan());
                if(mpp.getStatus().equals(new String("Driving."))){
                    trackAnimation++;
                    if(trackAnimation == 23)
                        trackAnimation = 19;
                    missionBoxRover_tracks.setIcon(rovertracks[trackAnimation]);
                    //missionBoxRover_PER.rover.setIcon(roverIcon);
                }
                //System.out.println(mpp.getStatus());
                //System.out.println("last update = " + last_update + " updateTime = " + rov.receive.getImageUpdateTime());
                if( last_update != rov.receive.getImageUpdateTime()){
                    last_update = rov.receive.getImageUpdateTime();
                    java.awt.image.BufferedImage img = rov.receive.getRecentImage();
                    //java.awt.image.BufferedImage img = rov.takeRecentPicture();
                    if(img != null)
                        roverPOV.setIcon(new ImageIcon(img.getScaledInstance(1024, 768, java.awt.Image.SCALE_SMOOTH)));
                    //use mpp to get status...
                    repaint();
                }
            }
        });
        video.setInitialDelay(0);
        
        analyzeTimer = new Timer(300, new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scan+=64;
                if(scan > 768){
                    //done = true;
                    analyzeTimer.stop();
                    return;
                }
                roverPOV.setBounds(0,scan + 5,1024, 768 - scan + 5);
                roverAnalyzePOV.setBounds(0,0,1024,scan);
                roverAnalyzeScanLine.setBounds(0,scan,1024, 5);
                repaint();
            }
        }){
            public void stop(){
                super.stop();
                //wait for finalDelay then begin the countdown
                //the final screen delay will occur as the countdown timer's inital delay
                countdownTimer.start();
            }
        };
        //analyzeTimer.setInitialDelay(775); //initial delay so that non-uv image will be displayed for a time before UV image is displayed
        analyzeTimer.setInitialDelay(800); //initial delay so that non-uv image will be displayed for a time before UV image is displayed
        
        countdownTimer = new Timer(1000, new ActionListener() {
            //countdownTimer = new javax.swing.Timer(775, new java.awt.event.ActionListener() {
            int i = 16; //image number
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                //only show the countdown if doCountdown is true. stop() immediately if false.
                if(!doCountdown)
                    countdownTimer.stop();
                //display the next countdown image
                displayCountdownNumber(i--);
                //stop the timer after the last image
                if(i < -1){
                    countdownTimer.stop();
                    i = 16; //reset image number for next time
                }
            }
        }){
            public void start(){
                tryAgainButton.setVisible(false);
                quitButton.setVisible(false);
                super.start();
            }
            public void stop(){
                //when the countdown is done, the screen is completed
                done = true;
                super.stop();
            }
        };
        //countdownTimer.setCoalesce(false);
        
        /** time out if no one hits try again or quit */
        errorCaseTimeoutTimer = new Timer(1000, new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                //log error case timeout
                PER.rover.Log.println("Rover POV Screen timeout (error case timeout); returning to attract loop!");
                PER.rover.StatsLog.print(PER.rover.StatsLog.POV_TIMEOUT);
                //return to attract loop
                done = true;
                errorCaseTimeoutTimer.stop();
            }
        });
        errorCaseTimeoutTimer.setInitialDelay(60000); //delay before returning to attract loop
        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        myLayeredPane = new javax.swing.JLayeredPane();
        quitButton = new javax.swing.JButton();
        tryAgainButton = new javax.swing.JButton();

        quitButton.setBorder(null);
        quitButton.setBorderPainted(false);
        quitButton.setContentAreaFilled(false);
        quitButton.setBounds(0, 0, -1, -1);
        myLayeredPane.add(quitButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

        tryAgainButton.setBorder(null);
        tryAgainButton.setBorderPainted(false);
        tryAgainButton.setContentAreaFilled(false);
        tryAgainButton.setBounds(0, 0, -1, -1);
        myLayeredPane.add(tryAgainButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

        add(myLayeredPane);

    }//GEN-END:initComponents
    
    private void initOtherComponents() {
        
        roverPOV = new javax.swing.JLabel();
        roverAnalyzePOV = new javax.swing.JLabel();
        roverAnalyzeScanLine = new javax.swing.JLabel();
        missionBox = new javax.swing.JLabel();
        missionBoxRover = new RLayeredPane();
        missionBoxRover_rover = new javax.swing.JLabel();
        missionBoxRover_head = new RLabel();
        missionBoxRover_tracks = new javax.swing.JLabel();
        missionBoxMessage = new javax.swing.JLabel();
        totalDirLabel = new javax.swing.JLabel();
        totalDistLabel = new javax.swing.JLabel();
        currentDirLabel = new javax.swing.JLabel();
        countDownScreen = new javax.swing.JLabel();
        
        setLayout(null);
        
        myLayeredPane.setBounds(0,0,1024,768);
        
        setBackground(new java.awt.Color(0, 0, 51));
        roverPOV.setBounds(0,0,1024, 768);
        roverPOV.setVerticalAlignment(SwingConstants.BOTTOM);
        roverPOV.setPreferredSize(new java.awt.Dimension(1024, 768));
        myLayeredPane.add(roverPOV, javax.swing.JLayeredPane.DEFAULT_LAYER);
        
        //why are there two sections for roverAnalyzePOV??
        setBackground(new java.awt.Color(0, 0, 51));
        roverAnalyzePOV.setBounds(0,0,0, 0);
        roverAnalyzePOV.setVerticalAlignment(SwingConstants.TOP);
        roverAnalyzePOV.setPreferredSize(new java.awt.Dimension(1024, 768));
        myLayeredPane.add(roverAnalyzePOV, javax.swing.JLayeredPane.DEFAULT_LAYER);
        
        setBackground(new java.awt.Color(0, 0, 0));
        roverAnalyzePOV.setBounds(0,0,0,0);
        roverAnalyzePOV.setPreferredSize(new java.awt.Dimension(1024, 5));
        myLayeredPane.add(roverAnalyzeScanLine, javax.swing.JLayeredPane.DEFAULT_LAYER);
        
        missionBox.setBounds(675,283,339,333); //small size
        missionBox.setIcon(smallMissionBoxIcon);
        missionBox.setVerticalAlignment(SwingConstants.TOP); //so that cropping will work correctly
        myLayeredPane.add(missionBox, javax.swing.JLayeredPane.PALETTE_LAYER);
        
        missionBoxRover.setBounds(845,360,139,116);
        
        missionBoxRover_tracks.setBounds(41,91,63,22);
        missionBoxRover_tracks.setIcon(rovertracks[0]);
        missionBoxRover.add(missionBoxRover_tracks, javax.swing.JLayeredPane.DEFAULT_LAYER);
        
        missionBoxRover_rover.setBounds(36,17,73,75);
        missionBoxRover_rover.setIcon(roverIcon_noHead);
        missionBoxRover.add(missionBoxRover_rover, javax.swing.JLayeredPane.MODAL_LAYER);
        
        missionBoxRover_head.setBounds(50,7,47,58);
        missionBoxRover_head.setVerticalAlignment(SwingConstants.CENTER);
        missionBoxRover_head.setHorizontalAlignment(SwingConstants.CENTER);
        //missionBoxRover_head.setVisible(false);
        missionBoxRover_head.setIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/RoverHead.gif")));
        missionBoxRover.add(missionBoxRover_head, javax.swing.JLayeredPane.PALETTE_LAYER);
        
        myLayeredPane.add(missionBoxRover, javax.swing.JLayeredPane.MODAL_LAYER);
        
        missionBoxMessage.setBounds(721,394,163,97); //small size
        missionBoxMessage.setVerticalAlignment(SwingConstants.TOP); //so that cropping will work correctly
        missionBoxMessage.setIcon(null);//new javax.swing.ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/status-turning.png")));
        myLayeredPane.add(missionBoxMessage, javax.swing.JLayeredPane.MODAL_LAYER);
        
        currentDirLabel.setText("555"+'\u00b0'+" right"); //for size
        currentDirLabel.setFont(new java.awt.Font("Verdana", 1, 18));
        currentDirLabel.setForeground(new java.awt.Color(255, 255, 255));
        currentDirLabel.setBounds(730, 435, 105, 50); //?
        //currentDirLabel.setText(null);
        //myLayeredPane.add(currentDirLabel, javax.swing.JLayeredPane.POPUP_LAYER);
        myLayeredPane.add(currentDirLabel, javax.swing.JLayeredPane.MODAL_LAYER);
        
        totalDirLabel.setText("555"+'\u00b0'+" right"); //for size
        totalDirLabel.setFont(new java.awt.Font("Verdana", 1, 18));
        totalDirLabel.setForeground(new java.awt.Color(255, 255, 255));
        totalDirLabel.setBounds(850, 505, totalDirLabel.getPreferredSize().width, totalDirLabel.getPreferredSize().height); //?
        myLayeredPane.add(totalDirLabel, javax.swing.JLayeredPane.MODAL_LAYER);
        
        totalDistLabel.setText("555-555 cm"); //for size
        totalDistLabel.setFont(new java.awt.Font("Verdana", 1, 18));
        totalDistLabel.setForeground(new java.awt.Color(255, 255, 255));
        totalDistLabel.setBounds(850, 552, totalDistLabel.getPreferredSize().width, totalDistLabel.getPreferredSize().height); //?
        myLayeredPane.add(totalDistLabel, javax.swing.JLayeredPane.MODAL_LAYER);
        
        clock = new PER.exhibit.GUI.ClockPane(true);
        myLayeredPane.add(clock, javax.swing.JLayeredPane.PALETTE_LAYER);
        
        countDownScreen.setBounds(0,0,0,0);
        myLayeredPane.add(countDownScreen, javax.swing.JLayeredPane.POPUP_LAYER);
        
        tryAgainButton.setIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/try_again_up.png")));
        tryAgainButton.setPressedIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/try_again_pressed.png")));
        tryAgainButton.setBounds(150,650,210,53);
        myLayeredPane.setLayer(tryAgainButton, javax.swing.JLayeredPane.DRAG_LAYER.intValue());
        
        quitButton.setIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/quit_up.png")));
        quitButton.setPressedIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/quit_pressed.png")));
        quitButton.setBounds(400,650,130,53);
        myLayeredPane.setLayer(quitButton, javax.swing.JLayeredPane.DRAG_LAYER.intValue());
        
        add(myLayeredPane);
        
    }
    
    /** Tries to fix any errors with the rover be reinitializing communication
     * and reinitializing the PER.rover.
     *
     *@returns True if communication and the rover were successfully reinitialized,
     * false if they could not be initialized.
     */
    private boolean fixError(){
        //try to recover from error
        boolean success = rov.initComm(rov.getCurrentIP());
        if (success) {
            success = rov.initRobot();
            success = rov.refresh();
        }
        //log status of error recovery effort
        if(success){
            System.out.println("error fixed");
            PER.rover.StatsLog.print(PER.rover.StatsLog.ERROR_FIXED);
        }
        else {
            PER.rover.StatsLog.print(PER.rover.StatsLog.ERROR_FATAL);
            System.out.println("error fatal");
        }
        
        return success;
    }
    
    private void showErrorMsg(int errorCode){
        countDownScreen.setBounds(0,0, 1024, 768);
        //show buttons
        tryAgainButton.setVisible(true);
        quitButton.setVisible(true);
        
        if(whichMissionBoxMessage == 5){
            //crop mission box so that the "purple glow..." section will not show
            missionBox.setBounds(675,283,339,211); //extra small cropped size
            missionBoxMessage.setBounds(721,394,163,97); //small size
            //totalDirLabel.setVisible(false);
            //totalDistLabel.setVisible(false);
            //missionBoxRover.setVisible(false);
            //clock.setVisible(false);
            //currentDirLabel.setVisible(false);
        }
        
        //show error message
        switch (errorCode){
            case Sequencer.NO_ROCK:
                //no rock found
                PER.rover.MiniLog.println("NO_ROCK, "+clock.getClockTime());
                PER.rover.Log.println("Mission result: NO_ROCK");
                countDownScreen.setIcon(errorFoundNothing);
                break;
            case Sequencer.INCOMPLETE:
                //mission incomplete - out of range error
                PER.rover.MiniLog.println("OUT_OF_RANGE, "+clock.getClockTime());
                PER.rover.Log.println("Mission result: OUT_OF_RANGE");
                countDownScreen.setIcon(errorOutOfRange);
                break;
            case Sequencer.INTERRUPTED:
                //mission interrupted by unexpected obstacle
                PER.rover.MiniLog.println("OBSTACLE, "+clock.getClockTime());
                PER.rover.Log.println("Mission result: OBSTACLE");
                countDownScreen.setIcon(errorObstructed);
                break;
            case ERROR_FIXED:
                //rover recovered from error
                PER.rover.MiniLog.println("FIXED_ERROR, "+clock.getClockTime());
                PER.rover.Log.println("Mission result: FIXED_ERROR");
                countDownScreen.setIcon(errorTryAgain);
                break;
            case ERROR_FATAL:
                //rover could not recover from error
                PER.rover.MiniLog.println("FATAL_ERROR, "+clock.getClockTime());
                PER.rover.Log.println("Mission result: FATAL_ERROR");
                countDownScreen.setIcon(errorNotifyStaff);
                //don't show buttons
                tryAgainButton.setVisible(false);
                quitButton.setVisible(false);
                //return;
                break;
        }
    }
    
    /** Displays the countdown image with the number <code>i</code>.
     */
    private void displayCountdownNumber(int i){
        switch(i){
            case 1:
                countDownScreen.setIcon(countDown1);
                break;
            case 2:
                countDownScreen.setIcon(countDown2);
                break;
            case 3:
                countDownScreen.setIcon(countDown3);
                break;
            case 4:
                countDownScreen.setIcon(countDown4);
                break;
            case 5:
                countDownScreen.setIcon(countDown5);
                break;
            case 6:
                countDownScreen.setIcon(countDown6);
                break;
            case 7:
                countDownScreen.setIcon(countDown7);
                break;
            case 8:
                countDownScreen.setIcon(countDown8);
                break;
            case 9:
                countDownScreen.setIcon(countDown9);
                break;
            case 10:
                countDownScreen.setIcon(countDown10);
                //System.out.println("10!");
                break;
            case 11:
                break;
            case 12:
                missionBox.setVisible(false);
                missionBoxRover.setVisible(false);
                missionBoxMessage.setVisible(false);
                totalDirLabel.setVisible(false);
                totalDistLabel.setVisible(false);
                clock.setVisible(false);
                currentDirLabel.setVisible(false);
                countDownScreen.setIcon(buckleUp);
                break;
            case 13:
                break;
            case 14:
                break;
            case 15:
                break;
            case 16:
                countDownScreen.setIcon(excellentWork);
                countDownScreen.setBounds(0,0, 1024, 768);
                break;
            default:
                break;
                
        }
    }
    
    /* sets feedback message in Mission Box*/
    private void setFeedbackLabel(String state){
        if(state.equals(new String("Driving.")) && whichMissionBoxMessage != 0){
            missionBoxRover.rotate(0);
            //missionBoxRover_PER.rover.setIcon(roverIcon_noHead);
            //missionBoxRover_head.setVisible(true);
            missionBoxMessage.setIcon(feedbackDriving);
            whichMissionBoxMessage = 0;
        }else if(state.equals(new String("Turning.")) && whichMissionBoxMessage != 1){
            missionBoxMessage.setIcon(feedbackTurning);
            whichMissionBoxMessage = 1;
        }else if(state.equals(new String("Scanning for nearest rock...")) && whichMissionBoxMessage != 2){
            currentDirLabel.setText(null);
            missionBoxRover_tracks.setIcon(rovertracks[0]);
            missionBoxRover.rotate(0);
            missionBoxRover_head.rotate(0);
            missionBoxMessage.setIcon(feedbackScanning);
            whichMissionBoxMessage = 2;
        }else if(state.equals(new String("Approaching rock...")) && whichMissionBoxMessage != 3){
            currentDirLabel.setText(null);
            missionBoxMessage.setIcon(feedbackApproaching);
            whichMissionBoxMessage = 3;
        }else if(state.equals(new String("Analyzing rock...")) && whichMissionBoxMessage != 4){
            currentDirLabel.setText(null);
            missionBoxMessage.setIcon(feedbackAnalyzing);
            whichMissionBoxMessage = 4;
        }else if(state.equals(new String("Mission has ended. Preparing report...")) && whichMissionBoxMessage != 5){
            currentDirLabel.setText(null);
            missionBox.setIcon(bigMissionBoxIcon);
            missionBox.setBounds(675,283,339,478); //large size
            missionBoxMessage.setBounds(721,394,163,290); //large size
            totalDirLabel.setVisible(false);
            totalDistLabel.setVisible(false);
            missionBoxMessage.setIcon(feedbackMissionComplete);
            whichMissionBoxMessage = 5;
        }
    }
    
    /** Sets and displays the total degrees to turn and total distance to drive.
     */
    public void setDegreesAndDistance(int deg, int dist){
        degrees = deg;
        distance = dist;
        
        if (degrees == 0)
            totalDirLabel.setText("0" + '\u00b0');
        else if (degrees > 0)
            totalDirLabel.setText("" + degrees + '\u00b0'+" left");
        else
            totalDirLabel.setText("" + (-1 * degrees) + '\u00b0'+" right");
        
        totalDistLabel.setText(""+distance+"-"+(distance+10)+" cm");
        
        missionBoxRover.rotate(degrees);
        missionBoxRover.setVisible(true);
    }
    
    public boolean isCompleted() {
        return done;//mpp.isCompleted();
    }
    
    /** Sets the delay between displaying the final mission image and starting the
     * countdown.
     */
    public void setFinalDelay(int delay){
        finalDelay = delay;
        countdownTimer.setInitialDelay(finalDelay);
    }
    
    public void setClockTime(String s) {
        //while the mission is running, update the clock
        if(!mpp.isCompleted()){
            clock.setText(s);
        }
    }
    
    /** Sets missionProgressPanel to completed and sequencer status to ERROR
     * so that showing the POV screen will cause the error messages to be
     * displayed.
     */
    public void setErrorCase(){
        mpp.setCompleted();
        //rov.mySequencer.setStatus(Sequencer.ERROR);
    seq.setStatus(Sequencer.ERROR);
    }
    
    /** Setting show Countdown to false before calling start() will turn
     * off the countdown. By default the countdown will be on (turned on
     * by the last call to stop()).
     */
    public void showCountdown(boolean b){
        doCountdown = b;
    }
    
    /** Setting doScan to false before calling start() will cause the RoverPOVScreen
     * to be completed as soon as the mission is completed, in other words, no scan
     * line will display and the final delay will be skipped. By default
     * doScan will be true (set to true by the last call to stop()).
     */
    public void doScan(boolean b){
        doScan = b;
    }
    
    public void start() {
        PER.rover.StatsLog.print(PER.rover.StatsLog.START_POV);
        java.awt.Image img = rov.takePicture(0, -20, 320,  240);
        if(img!= null)
            roverPOV.setIcon(new ImageIcon(img.getScaledInstance(1024, 768, java.awt.Image.SCALE_SMOOTH)));
        scan = 0;
        trackAnimation = 7;
        missionBoxRover_tracks.setIcon(rovertracks[0]);
        //missionBoxRover_PER.rover.setIcon(roverIcon);
        whichMissionBoxMessage = -1;
        currentDirLabel.setText(null);
        roverPOV.setBounds(0,0,1024,768);
        roverAnalyzePOV.setBounds(0,0,0,0);
        roverAnalyzeScanLine.setBounds(0,0,0,0);
        countDownScreen.setBounds(0,0,0,0);
        missionBox.setIcon(smallMissionBoxIcon);
        missionBox.setBounds(675,283,339,333); //small size
        missionBox.setVisible(true);
        missionBoxMessage.setIcon(null);
        missionBoxMessage.setBounds(721,394,163,97); //small size
        missionBoxMessage.setVisible(true);
        totalDirLabel.setVisible(true);
        totalDistLabel.setVisible(true);
        clock.setVisible(true);
        currentDirLabel.setVisible(true);
        
        //missionBoxRover_head.setVisible(false);
        tryAgainButton.setVisible(false);
        quitButton.setVisible(false);
        done = false;
        video.start();
        update.start();
        repaint();
    }
    
    public void stop() {
        video.stop();
        errorCaseTimeoutTimer.stop();
        showCountdown(true); //reset doCountdown to true by default
        doScan(true); //reset doScan to true by default
        PER.rover.StatsLog.print(PER.rover.StatsLog.STOP_POV);
    }
    
    /** Rotatable JLabel. */
    class RLabel extends javax.swing.JLabel{
        long r = 0;
        
        public RLabel(){
            
        }
        
        /** Repaints the label rotated <code>degrees</code> degrees from the original
         * orientation.
         */
        public void rotate(long degrees){
            r = degrees;
            this.repaint();
        }
        
        protected void paintComponent(java.awt.Graphics g){
            java.awt.Graphics2D g2 = (java.awt.Graphics2D)g;
            g2.rotate(java.lang.Math.toRadians(r),this.getWidth()/2,this.getHeight()/2);
            super.paintComponent(g2);
        }
    }
    
    /** Rotatable JLayeredPane. */
    class RLayeredPane extends javax.swing.JLayeredPane{
        long r = 0;
        public RLayeredPane(){
        }
        public void rotate(long degrees){
            r = degrees;
            this.repaint();
        }
        protected void paintComponent(java.awt.Graphics g){
            java.awt.Graphics2D g2 = (java.awt.Graphics2D)g;
            g2.rotate(java.lang.Math.toRadians(r),this.getWidth()/2,this.getHeight()/2);
            super.paintComponent(g2);
        }
    }
    
    public static void main(String[] args) {
        PER.rover.Rover rov = new PER.rover.Rover();
        PER.exhibit.Sequencer seq = new PER.exhibit.Sequencer(rov);
        final RoverPOVScreen POVScreen = new RoverPOVScreen(rov, new MissionProgressPanel(seq),seq);
        javax.swing.JFrame f = new javax.swing.JFrame("Rover Point of View");
        f.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });
        f.getContentPane().add(POVScreen);
        POVScreen.setDegreesAndDistance(45,150);
        f.setSize(1024, 768);
        f.setVisible(true);
    }
    
    void initImages(){
        countDown1 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/1.png"));
        countDown2 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/2.png"));
        countDown3 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/3.png"));
        countDown4 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/4.png"));
        countDown5 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/5.png"));
        countDown6 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/6.png"));
        countDown7 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/7.png"));
        countDown8 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/8.png"));
        countDown9 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/9.png"));
        countDown10 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/10.png"));
        excellentWork =  new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/ExcellentWorkMask65.png"));
        buckleUp =  new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/BuckleUpMask65.png"));
        purpleblank = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/purpleblank.gif"));
        introIcon = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/CountDownScreens/1_IntroNoTitle.jpg"));
        feedbackDriving = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/Feedback/driving.png"));
        feedbackTurning = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/Feedback/turning.png"));
        feedbackAnalyzing = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/Feedback/analyzing_target_rock.png"));
        feedbackApproaching = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/Feedback/positioning_to_collect.png"));
        feedbackMissionComplete = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/Feedback/mission_completed.png"));
        feedbackScanning = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/Feedback/scanning_terrain_to_find.png"));
        errorFoundNothing = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/error_found_nothing.png"));
        errorOutOfRange = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/error_out_of_range.png"));
        errorObstructed = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/error_obstructed.png"));
        errorTryAgain = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/error_try_again.png"));
        errorNotifyStaff = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/error_notify_staff.png"));
    }
    
    void initTracks(){
        rovertracks = new ImageIcon [23];
        rovertracks[0] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks01.gif"));
        rovertracks[1] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks02.gif"));
        rovertracks[2] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks03.gif"));
        rovertracks[3] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks04.gif"));
        rovertracks[4] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks05.gif"));
        rovertracks[5] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks06.gif"));
        rovertracks[6] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks07.gif"));
        rovertracks[7] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks08.gif"));
        rovertracks[8] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks09.gif"));
        rovertracks[9] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks10.gif"));
        rovertracks[10] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks11.gif"));
        rovertracks[11] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks12.gif"));
        rovertracks[12] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks13.gif"));
        rovertracks[13] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks14.gif"));
        rovertracks[14] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks15.gif"));
        rovertracks[15] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks16.gif"));
        rovertracks[16] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks17.gif"));
        rovertracks[17] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks18.gif"));
        rovertracks[18] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks19.gif"));
        rovertracks[19] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks20.gif"));
        rovertracks[20] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks21.gif"));
        rovertracks[21] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks22.gif"));
        rovertracks[22] = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/rovertracks/rovertracks23.gif"));
        //roverIcon = new javax.swing.ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/Rover-withHead.gif"));
        roverIcon_noHead = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/RoverAniImages/Rover-withoutHead.gif"));
        smallMissionBoxIcon = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/missionbox.png"));
        bigMissionBoxIcon = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/missionboxLarge.png"));
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLayeredPane myLayeredPane;
    public javax.swing.JButton quitButton;
    public javax.swing.JButton tryAgainButton;
    // End of variables declaration//GEN-END:variables
    
    private PER.rover.Rover rov;
    private Sequencer seq;
    private long last_update = 0;
    private int degrees;
    private int distance;
    private int scan;
    private int whichMissionBoxMessage;
    private boolean done;
    private int finalDelay; //the delay between displaying the final mission image and starting the countdown
    private boolean doCountdown;
    private boolean doScan;
    
    private Timer video;
    private Timer update;
    private Timer analyzeTimer;
    private Timer countdownTimer;
    private Timer errorCaseTimeoutTimer;
    
    private MissionProgressPanel mpp;
    private ClockPane clock;
    
    private javax.swing.JLabel roverPOV;
    private javax.swing.JLabel roverAnalyzePOV;
    private javax.swing.JLabel roverAnalyzeScanLine;
    
    private javax.swing.JLabel missionBox;
    private RLayeredPane missionBoxRover;
    private javax.swing.JLabel missionBoxRover_rover;
    private RLabel missionBoxRover_head;
    private javax.swing.JLabel missionBoxRover_tracks;
    private javax.swing.JLabel missionBoxMessage;
    private javax.swing.JLabel currentDirLabel;
    private javax.swing.JLabel totalDirLabel;
    private javax.swing.JLabel totalDistLabel;
    
    private ImageIcon countDown1;
    private ImageIcon countDown2;
    private ImageIcon countDown3;
    private ImageIcon countDown4;
    private ImageIcon countDown5;
    private ImageIcon countDown6;
    private ImageIcon countDown7;
    private ImageIcon countDown8;
    private ImageIcon countDown9;
    private ImageIcon countDown10;
    private ImageIcon excellentWork;
    private ImageIcon buckleUp;
    private ImageIcon purpleblank;
    private ImageIcon introIcon;
    
    private ImageIcon feedbackDriving;
    private ImageIcon feedbackTurning;
    private ImageIcon feedbackAnalyzing;
    private ImageIcon feedbackApproaching;
    private ImageIcon feedbackScanning;
    private ImageIcon feedbackMissionComplete;
    private ImageIcon errorFoundNothing;
    private ImageIcon errorOutOfRange;
    private ImageIcon errorObstructed;
    private ImageIcon errorTryAgain;
    private ImageIcon errorNotifyStaff;
    
    private javax.swing.JLabel countDownScreen;
    private ImageIcon[] rovertracks;
    //private javax.swing.ImageIcon roverIcon;
    private ImageIcon smallMissionBoxIcon;
    private ImageIcon bigMissionBoxIcon;
    private ImageIcon roverIcon_noHead;
    private int trackAnimation;
}




