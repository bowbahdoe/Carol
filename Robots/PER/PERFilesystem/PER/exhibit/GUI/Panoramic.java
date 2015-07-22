/*
 * Panoramic.java
 *
 * Created on October 25, 2003, 5:44 PM
 */

package PER.exhibit.GUI;

import PER.rover.Rover;
import PER.rover.TakePanoramaAction;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Displays an animation of the rover taking a panorama and displays the panorama
 * image by image as new images become available.
 *
 * @author mblain
 */
public class Panoramic extends javax.swing.JPanel implements Screen{
    
    /** Creates new form Panoramic */
    public Panoramic(Rover r, TakePanoramaAction tpa) {
        initComponents();
        initImages();
        rov = r;
        bg.setBounds(0,0,1042, 768);
        panoramaWidth = 935;
        panorama.setSize(panoramaWidth, 0);
        robotPane.setBounds(0, 274, 1024, 494);
        layeredPane.setBounds(0,0,1042, 768);
        
        //initialize clock
        clock = new PER.exhibit.GUI.ClockPane(false);
        layeredPane.add(clock, javax.swing.JLayeredPane.PALETTE_LAYER);
        
        layeredPane.setLayer(bg, javax.swing.JLayeredPane.DEFAULT_LAYER.intValue());
        layeredPane.setLayer(panorama, javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        layeredPane.setLayer(robotPane, javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        
        pictureTimer = new javax.swing.Timer(200, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updatePicture();
            }
        });
        pictureTimer.setInitialDelay(0);
        pictureTimer.setCoalesce(true);
        
        scanTimer = new javax.swing.Timer(200, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scanPicture();
            }
        });
        scanTimer.setInitialDelay(0);
        scanTimer.setCoalesce(true);
        
        action = tpa;
        lastPicUpdate = 0;
        image_on = 0;
        
        //set cursor to wait cursor for panoramic screen
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        layeredPane = new javax.swing.JLayeredPane();
        bg = new javax.swing.JLabel();
        panorama = new javax.swing.JLabel();
        robotPane = new javax.swing.JLabel();

        setLayout(null);

        setBackground(new java.awt.Color(204, 204, 254));
        bg.setIcon(new javax.swing.ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/DownloadPano-Background.gif")));
        bg.setBounds(0, 0, -1, -1);
        layeredPane.add(bg, javax.swing.JLayeredPane.DEFAULT_LAYER);

        panorama.setBounds(0, 0, -1, -1);
        layeredPane.add(panorama, javax.swing.JLayeredPane.DEFAULT_LAYER);

        robotPane.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        robotPane.setIcon(new javax.swing.ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/5.gif")));
        robotPane.setBounds(0, 0, -1, -1);
        layeredPane.add(robotPane, javax.swing.JLayeredPane.DEFAULT_LAYER);

        add(layeredPane);
        layeredPane.setBounds(0, 0, 0, 0);

    }//GEN-END:initComponents
    
    public void start() {
        PER.rover.StatsLog.print(PER.rover.StatsLog.START_RECEIVING_PANORAMA);
        scan = 0; //reset scan!
        lastPicUpdate = 0;
        image_on = 0;
        oldValueOfPan = 0;
        animation = 0; //reset initial animation
        panorama.setBounds(45,123, 0,128);
        
        if (!pictureTimer.isRunning()) {
            pictureTimer.start();
        }
        if (!scanTimer.isRunning()) {
            scanTimer.start();
        }
        robotPane.setIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/1.gif")));
        panorama.setIcon(null);
    }
    
    public void stop() {
        if (pictureTimer.isRunning()) {
            pictureTimer.stop();
        }
        if (!scanTimer.isRunning()) {
            scanTimer.stop();
        }
        PER.rover.StatsLog.print(PER.rover.StatsLog.STOP_RECEIVING_PANORAMA);
    }
    
    private void updatePicture() {
        // update the image
        if (lastPicUpdate != action.ImagesDone()) {
            lastPicUpdate = action.ImagesDone();
            BufferedImage img = action.getImage();
            int width = 935; //panorama width
            int height = img.getHeight(this) * width / img.getWidth(this);
            if (img != null)
                panorama.setIcon(new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_FAST)));
        }
        /*if (lastPicUpdate>1 && !scanTimer.isRunning()) {
            scanTimer.start();
        }*/
        rov.refresh();
        int pan = rov.state.getPan();
        if(pan != oldValueOfPan && animation == -1){
            oldValueOfPan = pan;
            if(pan <= -150 || action.isCompleted()){
                robotPane.setIcon(pan13);
                animation = 5;
            }
            else if(pan <= -100)
                robotPane.setIcon(pan12);
            else if(pan <= -50)
                robotPane.setIcon(pan11);
            else if(pan <= 0)
                robotPane.setIcon(pan10);
            else if(pan <= 50)
                robotPane.setIcon(pan9);
            else if(pan <= 100)
                robotPane.setIcon(pan8);
            else if(pan <= 150)
                robotPane.setIcon(pan7);
            else{
                robotPane.setIcon(pan6);
                scan -=117;
            }
            /* increment one pane of the panorama image */
            scan += 117;
            if(scan > 935)
                scan = 935;
            panorama.setBounds(45,123, scan,128);
        }
    }
    
    public void setImage(Image original) {
        int width = 935;//panoramaWidth; //panorama width
        int height = original.getHeight(this) * width / original.getWidth(this);
        Image img = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        panorama.setIcon(new ImageIcon(img));
        panorama.setBounds(45, 123, width, height);
        revalidate();
    }
    private void scanPicture(){
        if(animation>= 0){
            rov.refresh();
            if(animation==6 && rov.state.getPan() <= -150)
                return;
            switch(animation++){
                case 0:
                    robotPane.setIcon(pan1);
                    break;
                case 1:
                    robotPane.setIcon(pan2);
                    break;
                case 2:
                    robotPane.setIcon(pan3);
                    break;
                case 3:
                    robotPane.setIcon(pan4);
                    break;
                case 4:
                    robotPane.setIcon(pan5);
                    animation = -1;
                    break;
                case 5:
                    break;
                case 6:
                    panorama.setBounds(45,123,935,128);
                    robotPane.setIcon(pan14);
                    break;
                case 7:
                    robotPane.setIcon(pan15);
                    break;
                case 8:
                    robotPane.setIcon(pan16);
                    break;
                case 9:
                    robotPane.setIcon(pan17);
                    break;
                case 10:
                    robotPane.setIcon(pan18);
                    break;
                case 11:
                case 12:
                    animation = 12;
            }
            revalidate();
        }
    }
    
    public boolean isCompleted(){
        //return (animation == 12 && action.isCompleted());
        //if the action is completed successfully return wether or not the animation is complete
        if(action.isCompleted() && action.isSuccess())
            return (animation == 12);
        //the action is completed with an error
        if(action.isCompleted() && !action.isSuccess())
            return true; //panoramic screen is complete but this is an error case...
        //the action is not complete
        return false;
        
    }
    
    /** Whether the panorama completed successfully. Undefined until panoramaAction isCompleted() returns
     * true.
     */
    public boolean isSuccess(){
        return action.isSuccess();
    }
    
    public static void main(String args[]) {
        javax.swing.JFrame f = new javax.swing.JFrame();
        f.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                System.exit(0);
            }
        });
        
        java.awt.GraphicsDevice device = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean isFullScreen = device.isFullScreenSupported();
        
        try {
            Image img = ImageIO.read(new File(PER.PERConstants.perPath+"0.jpg"));
            Panoramic panel = new Panoramic(new Rover(),new TakePanoramaAction());
            panel.setImage(img);
            f.getContentPane().add(panel);
            f.setResizable(false);
            f.setUndecorated(true);
            f.pack();
            //panel.start();
            panel.scanTimer.start();//starter();
            //f.setVisible(true);
            if (isFullScreen) {
                // Full-screen mode
                device.setFullScreenWindow(f);
                f.validate();
            } else {
                // Windowed mode
                f.show();
                panel.stop();
            }
        } catch (java.io.IOException e) {
            System.err.println("Error in reading image: " + e);
        }
        
    }
    
    public void setClockTime(String s) {
        clock.setText(s);
    }
    
    private void initImages(){
        pan1 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/1.gif"));
        pan2 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/2.gif"));
        pan3 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/3.gif"));
        pan4 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/4.gif"));
        pan5 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/5.gif"));
        pan6 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/6.gif"));
        pan7 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/7.gif"));
        pan8 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/8.gif"));
        pan9 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/9.gif"));
        pan10 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/10.gif"));
        pan11 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/11.gif"));
        pan12 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/12.gif"));
        pan13 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/13.gif"));
        pan14 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/14.gif"));
        pan15 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/15.gif"));
        pan16 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/16.gif"));
        pan17 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/17.gif"));
        pan18 = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan/18.gif"));
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bg;
    private javax.swing.JLayeredPane layeredPane;
    private javax.swing.JLabel panorama;
    private javax.swing.JLabel robotPane;
    // End of variables declaration//GEN-END:variables
    
    private javax.swing.Timer pictureTimer;
    private int panoramaWidth;
    long startTime;
    private long lastPicUpdate;
    private TakePanoramaAction action;
    private int image_on;
    private Rover rov;
    private TeleopPanPanel panPanel;
    private javax.swing.Timer scanTimer;
    private int scan = 0;
    private int oldValueOfPan;
    private int animation;
    private ClockPane clock;
    
    /* images*/
    private ImageIcon pan1;
    private ImageIcon pan2;
    private ImageIcon pan3;
    private ImageIcon pan4;
    private ImageIcon pan5;
    private ImageIcon pan6;
    private ImageIcon pan7;
    private ImageIcon pan8;
    private ImageIcon pan9;
    private ImageIcon pan10;
    private ImageIcon pan11;
    private ImageIcon pan12;
    private ImageIcon pan13;
    private ImageIcon pan14;
    private ImageIcon pan15;
    private ImageIcon pan16;
    private ImageIcon pan17;
    private ImageIcon pan18;
    
}
