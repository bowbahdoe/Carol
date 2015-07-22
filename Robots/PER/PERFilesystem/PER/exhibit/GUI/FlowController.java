/*
 * FlowController.java
 *
 * Created on July 10, 2003, 4:34 PM
 */

package PER.exhibit.GUI;

import java.awt.event.*;
import PER.rover.Rover;
import PER.rover.TakePanoramaAction;
import PER.rover.Log;
import java.io.*;
import java.lang.reflect.*;

/**
 * Controlls the order that screens are displayed.
 *
 * @author  Rachel Gockley
 */

public class FlowController extends javax.swing.JPanel {
    // these are all so that flipping around the card panels is easier
    private static final String ATTRACT            = "Attract loop";
    private static final String RECEIVING_PANORAMA = "Receiving Panorama";
    private static final String MISSION_CENTRAL    = "Mission Central";
    private static final String ROVER_POV          = "Rover POV";
    
    //default screen timeouts in milliseconds
    public static final int DEFAULT_MISSION_TIMEOUT = 300000; //5 minutes
    public static final int DEFAULT_FINAL_TIMEOUT = 5000; //5 seconds
    
    //screen timeouts in milliseconds
    public static int MISSION_TIMEOUT = DEFAULT_MISSION_TIMEOUT;
    public static int FINAL_TIMEOUT = DEFAULT_FINAL_TIMEOUT;
    
    private boolean isMAC; //is this running on a MAC?
    
    /** Creates a new instance of FlowController */
    public FlowController(PER.rover.Rover r, MissionProgressPanel mpp, PER.exhibit.Sequencer s) {
        super();
        
        rov = r;
        seq = s;
        
        //check if running on a MAC
        /*try{
            if(System.getProperty("mrj.version") != null)
                isMAC = true;
            else isMAC = false;
        }catch(java.lang.SecurityException e){
            rover.Log.println("Error getting system property: "+e,true);
            isMAC = true; //?
        }*/
        isMAC = false;
        
        
        initComponents(r, mpp);
        registerListeners();
        
        skipAttractLoop = false;
        currScreen = attractLoop;
        
        time = new java.util.GregorianCalendar();
        time.set(java.util.Calendar.MINUTE,0);
        time.set(java.util.Calendar.SECOND,0);
        sdf = new java.text.SimpleDateFormat("m:ss");
        
        timer = new javax.swing.Timer(10, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkScreen();
            }
        });
        
        //timer for the clock that is displayed throughout the different screens
        clockTimer = new javax.swing.Timer(1000, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                incrementTime();
            }
        }){
            public void start(){
                //reset clock time and format
                time.set(java.util.Calendar.MINUTE,0);
                time.set(java.util.Calendar.SECOND,0);
                sdf.applyPattern("m:ss");
                currScreen.setClockTime(sdf.format(time.getTime()));
                super.start();
            }
            //currently stop() is never called...
            public void stop(){
                currScreen.setClockTime(sdf.format(time.getTime()));
                super.stop();
            }
        };
        //clockTimer.setCoalesce(false);
    }
    
    /** Adds one second to time and displays the new value on
     * the current screen's clock timer label.
     */
    private void incrementTime(){
        time.add(java.util.Calendar.SECOND, 1);
        if(time.get(java.util.Calendar.MINUTE) >= 10)
            sdf.applyPattern("mm:ss");
        currScreen.setClockTime(sdf.format(time.getTime()));
    }
    
    /** Returns a string representation of the current clock time. */
/*    public String getTime(){
        if(time.get(java.util.Calendar.MINUTE) < 10)
            return (new java.text.SimpleDateFormat("m:ss")).format(time.getTime());
        else
            return (new java.text.SimpleDateFormat("mm:ss")).format(time.getTime());
    }
 */
    
    public void startTimer() {
        currScreen = missionCentral; //if the current screen is attract, the attract loop will be stopped in showScreen
        showScreen(attractLoop, ATTRACT);
        timer.start();
    }
    
    // check automatic screen completion and timeouts
    private void checkScreen() {
        long timeOutMillis = System.currentTimeMillis() - lastMotion;
        
        if (currScreen == receivingPanoramaScreen && receivingPanoramaScreen.isCompleted()) {
            Log.println("Receiving Panorama complete.");
            //check if there was an error or not....
            if(receivingPanoramaScreen.isSuccess()){
                missionCentral.setImage(tpa.getImage());
                showScreen(missionCentral, MISSION_CENTRAL);
            }else{
                //log error
                PER.rover.ActionConstants.logErrorStats(tpa.getReturnValue());
                //show error message
                roverPOVScreen.setDegreesAndDistance(0, 0);//not needed if this part of POV is invisible in error case
                roverPOVScreen.setErrorCase();
                showScreen(roverPOVScreen, ROVER_POV);
            }
        } else if (currScreen == roverPOVScreen && roverPOVScreen.isCompleted()){
            if(skipAttractLoop){
                //skip the attract loop and go directly to new panorama
                Log.println("Rover POV screen countdown complete; going directly to panorama.");
                tpa.doAction(rov);
                showScreen(receivingPanoramaScreen, RECEIVING_PANORAMA);
                //clockTimer.start(); //reset clock? ?
                //reset skipAttractLoop to false by default
                skipAttractLoop = false;
            }else{
                Log.println("Rover POV screen countdown complete; returning to attract loop!");
                showScreen(attractLoop, ATTRACT);
            }
        } else if (currScreen == missionCentral && timeOutMillis > MISSION_TIMEOUT && MISSION_TIMEOUT > 0){
            Log.println("Mission Central Timeout; returning to attract loop!");
            PER.rover.StatsLog.print(PER.rover.StatsLog.MIS_TIMEOUT);
            showScreen(attractLoop, ATTRACT);
        }
    }
    
    private void showScreen(Screen screen, String screenName) {
        //on a MAC, never show the attract loop. go directly to panorama
        if(isMAC && screenName == ATTRACT){
            //if(screenName == ATTRACT){
            Log.println("Running on a MAC. Skipping the attract loop.");
            tpa.doAction(rov);
            showScreen(receivingPanoramaScreen, RECEIVING_PANORAMA);
            clockTimer.start();
            return;
        }
        
        Screen oldScreen = currScreen;
        
        Log.println("About to show the \"" + screenName + "\" screen.");
        
        currScreen = screen;
        //set the clock on the new screen before displaying the screen
        currScreen.setClockTime(sdf.format(time.getTime()));
        // show the new screen before stopping the old!
        currScreen.start();
        layout.show(this, screenName);
        oldScreen.stop();
        
        if(currScreen == roverPOVScreen){
            /* Wait for at most about one second to make sure that the picture
             * taken in RoverPOVScreen.start() is completely finished and
             * the resources freed before starting the mission.
             */
            for(int i=0; i<20 && rov.state.webCamThreadState == rov.state.WEBCAM_GRAB; i++){
                rov.refresh();
                try { Thread.sleep(50); } catch (Exception e) {}
            }
        }
        
        // pretend that the mouse moves every time the screen changes
        lastMotion = System.currentTimeMillis();
    }
    
    private void initComponents(PER.rover.Rover r, MissionProgressPanel mpp) {
        layout = new java.awt.CardLayout();
        setLayout(layout);
        
        degreesToTurn = 0;
        distToDrive = 0;
        
        tpa = new PER.rover.TakePanoramaAction(-40, 10, 320, 240);
        //tpa = new rover.TakePanoramaAction(-45, 5, 320, 240); //NSC
        //tpa = new rover.TakePanoramaAction(-43, 7, 320, 240); //DC - downtown
        
        attractLoop = new AttractLoop();
        receivingPanoramaScreen = new Panoramic(r,tpa);
        missionCentral = new MissionCentral();
        roverPOVScreen = new RoverPOVScreen(r, mpp, seq);
        
        add(attractLoop, ATTRACT);
        add(receivingPanoramaScreen, RECEIVING_PANORAMA);
        add(missionCentral, MISSION_CENTRAL);
        add(roverPOVScreen, ROVER_POV);
        
        Log.println("FlowController: all components initialized.");
    }
    
    private void registerListeners() {
        // timeout listener
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                lastMotion = System.currentTimeMillis();
            }
        });
        
        //timeout listener - This second listener is needed for missionCentral because missionCentral has other listeners that consume mouse events.
        missionCentral.layeredPane.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                lastMotion = System.currentTimeMillis();
            }
        });
        
        //**Attract Loop**//
        // mouse click for attract loop
        attractLoop.addMouseListener(new MouseAdapter() {
/*            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Log.println("Exitting attract loop (mouse clicked).");
                tpa.doAction(rov);
                showScreen(receivingPanoramaScreen, RECEIVING_PANORAMA);
                clockTimer.start();
            }
 */
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                Log.println("Exitting attract loop (mouse released).");
                tpa.doAction(rov);
                showScreen(receivingPanoramaScreen, RECEIVING_PANORAMA);
                clockTimer.start();
            }
        });
        
        //**Mission Central**//
        //sendButton
        missionCentral.goButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if(evt.getButton() == java.awt.event.MouseEvent.BUTTON1){
                    //left click
                    Log.println("Clicked \"Go\" in mission central.");
                    degreesToTurn = missionCentral.getDegrees();
                    distToDrive = missionCentral.getDist();
                    roverPOVScreen.setDegreesAndDistance(degreesToTurn, distToDrive);
                    // start the mission
                    Log.println("Began new mission (" + degreesToTurn + ", " + distToDrive + ")!");
                    showScreen(roverPOVScreen, ROVER_POV); 
                    seq.runMission(degreesToTurn, distToDrive);
                    //showScreen(roverPOVScreen, ROVER_POV); //changed order
                }else if(evt.getButton() == java.awt.event.MouseEvent.BUTTON3){
                    //right click
                    missionCentral.showPopupMenu(missionCentral.goButton, evt.getX(), evt.getY());
                }
            }
        });
        
        //right click menu - navigate and find rock
        missionCentral.navPlusMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Log.println("Selected \"Navigate and find rock\" right click menu option in Mission Central.");
                degreesToTurn = missionCentral.getDegrees();
                distToDrive = missionCentral.getDist();
                roverPOVScreen.setDegreesAndDistance(degreesToTurn, distToDrive);
                roverPOVScreen.showCountdown(false); //no countdown
                //no attract loop
                skipAttractLoop = true;
                // start the mission
                Log.println("Began new mission (" + degreesToTurn + ", " + distToDrive + ")!");
                showScreen(roverPOVScreen, ROVER_POV); 
                seq.runMission(degreesToTurn, distToDrive);
                //showScreen(roverPOVScreen, ROVER_POV); //changed order
            }
        });
        
        //right click menu - navigate only
        missionCentral.navOnlyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Log.println("Selected \"Navigate only\" right click menu option in Mission Central.");
                degreesToTurn = missionCentral.getDegrees();
                distToDrive = missionCentral.getDist();
                roverPOVScreen.setDegreesAndDistance(degreesToTurn, distToDrive);
                roverPOVScreen.showCountdown(false); //no countdown
                roverPOVScreen.doScan(false); //no rock scan
                //no attract loop
                skipAttractLoop = true;
                // start the mission
                Log.println("Began new mission (" + degreesToTurn + ", " + distToDrive + ")!");
                showScreen(roverPOVScreen, ROVER_POV); 
                seq.runMission(degreesToTurn, distToDrive, false);
                //showScreen(roverPOVScreen, ROVER_POV); //changed order
            }
        });
        
        //right click menu - turn only
        missionCentral.turnMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Log.println("Selected \"Navigate and find rock\" right click menu option in Mission Central.");
                degreesToTurn = missionCentral.getDegrees();
                distToDrive = 0;
                roverPOVScreen.setDegreesAndDistance(degreesToTurn, distToDrive);
                roverPOVScreen.showCountdown(false); //no countdown
                roverPOVScreen.doScan(false); //no rock scan
                //no attract loop
                skipAttractLoop = true;
                // start the mission
                Log.println("Began new mission (" + degreesToTurn + ", " + distToDrive + ")!");
                showScreen(roverPOVScreen, ROVER_POV); 
                seq.runMission(degreesToTurn, distToDrive, false);
                //showScreen(roverPOVScreen, ROVER_POV); //changed order
            }
        });
        
        //right click menu - take new panorama
        missionCentral.newPanMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Log.println("Selected \"Take new panorama\" right click menu option in Mission Central.");
                tpa.doAction(rov);
                showScreen(receivingPanoramaScreen, RECEIVING_PANORAMA);
                //clockTimer.start(); //restart timer?
            }
        });
        
        //right click menu - return to Attract Loop
        missionCentral.attractLoopMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Log.println("Selected \"Return to Attract Loop\" right click menu option in Mission Central.");
                showScreen(attractLoop, ATTRACT);
            }
        });
        
        //**POV Screen**//
        //quitButton
        roverPOVScreen.quitButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Log.println("Clicked \"Quit\" in Rover POV screen.");
                PER.rover.StatsLog.print(PER.rover.StatsLog.QUIT_BUTTON);
                showScreen(attractLoop, ATTRACT);
            }
        });
        
        //tryAgain
        roverPOVScreen.tryAgainButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Log.println("Clicked \"Try Again\" in Rover POV screen; now taking a panorama.");
                PER.rover.StatsLog.print(PER.rover.StatsLog.TRY_AGAIN_BUTTON);
                tpa.doAction(rov);
                showScreen(receivingPanoramaScreen, RECEIVING_PANORAMA);
                clockTimer.start(); //restart timer?
            }
        });
        
        Log.println("FlowController: all listeners registered.");
    }
    
    public static void setTimeouts(int missiontimeout, int finaltimeout){
        MISSION_TIMEOUT = missiontimeout;
        FINAL_TIMEOUT = finaltimeout;
        Log.println("Timeouts set (mission: " + MISSION_TIMEOUT + ", final: " + FINAL_TIMEOUT + ").");
    }
    
    public void setPOVDelay(){
        roverPOVScreen.setFinalDelay(FINAL_TIMEOUT);
    }
    
    public void setPanoramaAngle(int i){
        tpa.setAngles(i, i+50);
    }
    
    /** Sets the satellite map image and map coordinates in MissionCentral.
     *
     *@param mimg The satellite map image.
     *@param rolloverImg The border image to display when the mouse rolls over the map.
     *@param f The map coordinates file.
     *@return False if there was an IOException or the coordinates file was
     * not formatted correctly, true otherwise.
     */
    public boolean setMapImageAndCoordinates(java.awt.Image mimg, java.awt.Image rolloverImg, File f){
        int originalMapWidth = mimg.getWidth(this);
        int originalMapHeight = mimg.getHeight(this);
        
        missionCentral.setMapImage(mimg,rolloverImg);
        
        return setMapCoordinates(f,originalMapWidth,originalMapHeight);
    }
    
    /** Reads the coordinates file and sets the map polygon and sun location
     * in missionCentral.
     *
     *@return True if the map polygon and sun location were set, false if
     * there was an IOException or the coordinates file was not formatted correctly.
     */
    private boolean setMapCoordinates(File f, int originalMapWidth, int originalMapHeight){
        try{
            File inputFile = f;
            FileReader in = new FileReader(inputFile);
            StreamTokenizer st = new StreamTokenizer(in);
            st.parseNumbers();
            st.eolIsSignificant(false);
            
            int numMapPoints = -1;
            int[] xs = {0};
            int[] ys = {0};
            int sunx = -1;
            int suny = -1;
            double yardWidthInCM = -1;
            
            while(st.nextToken() != st.TT_EOF){
                if(st.ttype == st.TT_WORD){
                    //map polygon points
                    if(st.sval.equals("numMapPoints")){
                        st.nextToken();
                        if(st.ttype == st.TT_NUMBER){
                            numMapPoints = (int)st.nval;
                            xs = (int[])Array.newInstance(xs.getClass().getComponentType(),numMapPoints);
                            ys = (int[])Array.newInstance(ys.getClass().getComponentType(),numMapPoints);
                        }else{
                            invalidFormat(f);
                            return false;
                        }
                        for(int i = 0; i<numMapPoints; i++){
                            if(st.nextToken() != st.TT_EOF){
                                xs[i] = (int)st.nval;
                            }else{
                                invalidFormat(f);
                                return false;
                            }
                            if(st.nextToken() != st.TT_EOF){
                                ys[i] = (int)st.nval;
                            }else{
                                invalidFormat(f);
                                return false;
                            }
                        }
                    }else if(st.sval.equals("sun")){
                        //sun location
                        if(st.nextToken() != st.TT_EOF){
                            sunx = (int)st.nval;
                        }else{
                            invalidFormat(f);
                            return false;
                        }
                        if(st.nextToken() != st.TT_EOF){
                            suny = (int)st.nval;
                        }else{
                            invalidFormat(f);
                            return false;
                        }
                    }else if(st.sval.equals("yardWidthInCentimeters")){
                        //scale
                        if(st.nextToken() != st.TT_EOF){
                            yardWidthInCM = (double)st.nval;
                        }else{
                            invalidFormat(f);
                            return false;
                        }
                    }else if(st.sval.equals("yardWidthInInches")){
                        //scale
                        if(st.nextToken() != st.TT_EOF){
                            //convert from inches to cm
                            yardWidthInCM = (double)st.nval * 2.54;
                        }else{
                            invalidFormat(f);
                            return false;
                        }
                    }else if(st.sval.equals("yardWidthInFeet")){
                        //scale
                        if(st.nextToken() != st.TT_EOF){
                            //convert from feet to cm
                            yardWidthInCM = (double)st.nval * 30.48;
                        }else{
                            invalidFormat(f);
                            return false;
                        }
                    }else{
                        invalidFormat(f);
                        return false;
                    }
                }
            }
            
            //yard coordinates and scale are required, but sun location is not
            if(numMapPoints == -1 || yardWidthInCM == -1){
                invalidFormat(f);
                return false;
            }
            
            missionCentral.setMapCoordinates(new java.awt.Polygon(xs,ys,numMapPoints),
            sunx,suny,yardWidthInCM,originalMapWidth,originalMapHeight);
            
            in.close();
            return true;
        }catch (java.io.IOException e){
            PER.rover.Log.println("Error reading map coordinates: "+e,true);
            javax.swing.JOptionPane.showMessageDialog(this,
            "Error reading map coordinates file "+f.getName()+".\n"
            +e+"\n"
            + "Exhibit program will be closed.",
            "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    private void invalidFormat(File f){
        PER.rover.Log.println("Invalid map coordinates file format.",true);
        javax.swing.JOptionPane.showMessageDialog(this,
        "Invalid map coordinates file format.\n"
        +"Please select another file or check that\n"
        +f.getName()+" is formatted correctly.\n"
        + "Exhibit program will be closed.",
        "Error: Invalid File Format", javax.swing.JOptionPane.ERROR_MESSAGE);
    }
    
    public static void main(String[] args) {
        PER.rover.Rover rov = new PER.rover.Rover();
        PER.exhibit.Sequencer seq = new PER.exhibit.Sequencer(rov);
        final FlowController flowController = new FlowController(rov, new MissionProgressPanel(seq), seq);
        javax.swing.JFrame f = new javax.swing.JFrame("Flow Controller");
        
        
        //test
        /*try{
            flowController.setMapCoordinates(new File(PER.exhibit.Exhibit.exhibitPath+"/SatelliteMaps/test.txt"));
            System.exit(0);
        }catch (Exception e){
            System.out.println("Error reading map coordinates: "+e);
            System.exit(0);
        }*/
        
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        f.getContentPane().add(flowController);
        f.setResizable(false);
        f.setUndecorated(true);
        //f.setSize(1024, 768);
        
        flowController.timer.start();
        
        java.awt.GraphicsDevice device = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean isFullScreen = device.isFullScreenSupported();
        if (isFullScreen) {
            // Full-screen mode
            device.setFullScreenWindow(f);
            f.validate();
        } else {
            // Windowed mode
            f.show();
        }
        
    }
    
    private java.awt.CardLayout     layout;
    private AttractLoop             attractLoop;
    private Panoramic/*ReceivingPanoramaScreen*/ receivingPanoramaScreen;
    private MissionCentral          missionCentral;
    private RoverPOVScreen          roverPOVScreen;
    
    private Rover                   rov;
    private PER.exhibit.Sequencer   seq;
    private TakePanoramaAction      tpa;
    private int                     degreesToTurn;
    private int                     distToDrive;
    private boolean                 skipAttractLoop;
    
    private javax.swing.Timer       timer;
    private javax.swing.Timer       clockTimer;
    java.text.SimpleDateFormat      sdf; //date format used to format time
    private java.util.GregorianCalendar time; //the current time count for the mission in progress
    private Screen                  currScreen;
    private long                    lastMotion;
}
