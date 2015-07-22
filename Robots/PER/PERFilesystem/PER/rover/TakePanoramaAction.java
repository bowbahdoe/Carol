/*
 * TakePanoramaAction.java
 *
 * Created on July 21, 2003, 9:20 AM
 */

package PER.rover;

import PER.rover.control.Datapack;
import PER.rover.control.Reliagram;
import PER.rover.control.RoverCommand;
import PER.rover.control.RoverState;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

/**
 * Takes panoramic images.
 *
 * @author  Eric Porter
 * @version 1.0
 */


public class TakePanoramaAction implements Action {
    public static final int FOV_WIDTH = 45;
    public static final int FOV_HEIGHT = 34;
    
    private int time;
    private BufferedImage img = null;
    private Graphics graphics = null;
    private int imgWidth, imgHeight, width, height, lowAngle, highAngle;
    
    transient private boolean quit = false;
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean completed = false;
    transient private int ret;
    transient private long starttime, imageUpdateTime, imagesDone;
    
    /** Creates new TakePanoramaAction from angles -50 to 0 at 320x240 */
    public TakePanoramaAction() {
        this(-50, 0, 320, 240);
    }
    
    /**
     * Creates a new TakePanoramaAction with options for the tilt angle and image size.
     * @param lowAng The minimum tilt angle that the picture includes.
     * @param highAng The maximum tilt angle that the picture includes.
     * @param width The width in pixels of the target image.
     * @param height The height in pixels of the target image.
     */
    public TakePanoramaAction(int lowAng, int highAng, int width, int height) {
        completed = false;
        this.width = width;
        this.height = height;
        lowAngle = lowAng;
        highAngle = highAng;
        if(lowAng > highAng || width > 480 || width <= 0 || height > 640 || height <= 0)
            return;
        imgWidth = 360*width/FOV_WIDTH;
        imgHeight = (highAngle - lowAngle) * height / FOV_HEIGHT;
        //img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_3BYTE_BGR);
        graphics = img.getGraphics();
        imageUpdateTime = System.currentTimeMillis();
        
        //calculate the estimated time in seconds - this should actually work
        time = (int)(.75*Math.ceil(360./FOV_WIDTH)*Math.ceil((double)(highAngle-lowAngle)/FOV_HEIGHT));
        
        clearImage();
    }
    
    /** Sets the low and high angles to be used for the panorama.
     *
     *@return False if the low angle is not less that the high angle.
     */
    public boolean setAngles(int lowAng, int highAng){
        if(lowAng > highAng)
            return false;
        //update angles
        lowAngle = lowAng;
        highAngle = highAng;
        //update affected variables
        imgHeight = (highAngle - lowAngle) * height / FOV_HEIGHT;
        img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_3BYTE_BGR);
        graphics = img.getGraphics();
        time = (int)(.75*Math.ceil(360./FOV_WIDTH)*Math.ceil((double)(highAngle-lowAngle)/FOV_HEIGHT));
        
        return true;
    }

    /** Returns the panoramic image taken by the action. May be null if the image
     * has not yet been taken.
     * <p>
     * This method is included for backwards compatibility. <code>getRecentImage</code>
     * performs the same function.
     *@see #getRecentImage
     */
    public BufferedImage getImage() {
        return img;
    }
    
    public boolean isSuccess() {
        return completed;
    }
    
    /** Takes a panorama.
     */
    public boolean doAction(Rover r) {
        PER.rover.StatsLog.println(PER.rover.StatsLog.PANORAMA);
        rov = r;
        
        clearImage();
        
        myThread = new Thread() {
            public void run() {
                starttime = System.currentTimeMillis();
                
                quit = false;
                ret = doit();
                completed = (ret == RoverState.SUCCESS);
                
                long endtime = System.currentTimeMillis();
                if (completed)
                    time = (int)((endtime - starttime) );
            }
        };
        
        myThread.start();
        return true;
    }
    
    private void clearImage() {
        imageUpdateTime = System.currentTimeMillis();
        graphics.setColor(java.awt.Color.gray);
        graphics.fillRect(0, 0, imgWidth, imgHeight);
    }
    
    private int doit(){
        Reliagram rgram = rov.reliagram;
        RoverCommand cmd = new RoverCommand();
        int i=0, j=0; //helpers
        int imagesWide = (360-1)/FOV_WIDTH+1;
        int imagesHigh = (highAngle-lowAngle-1)/FOV_HEIGHT+1;
        int positions [][][] = new int[imagesWide][imagesHigh][5];
        int grabPan, grabTilt; //where to take picture from
        int placeX, placeY; //where to place it in the large image
        int placeWidth, placeHeight; //how much of the just taken image to use
        //int panAngles [] = {170, 106, 62, 23, -21, -67, -124, -175}, whichPan = 0;
        
        //I need to move the head far left so that head is always moving left.
        //the head goes to different positons depending on whether it moved left or right.
        int sleepTime = Math.abs(rov.state.getPan()-180)*8; //sleep for 8 ms for each degree off
        //rov.look(180, lowAngle + FOV_HEIGHT/2);
        rov.look(180, lowAngle + FOV_HEIGHT/2);
        try{Thread.sleep(sleepTime);}catch(Exception e) {}
        
        int waitTimeForReceipt = 7000, timeWaitedForReceipt=0;
        for(int pan=180; pan>-180; pan-=FOV_WIDTH) {
            if(pan - FOV_WIDTH >= -180) { //enough room to draw whole image
                grabPan = pan - FOV_WIDTH/2;
                placeX = width*i;
                placeWidth = width;
            }else {
                grabPan = -180 + FOV_WIDTH/2;
                placeX = imgWidth-width; //fix this, maybe
                placeWidth = width;
            }
            //uses special pan angles if using the real mars rover
            for(int tilt=lowAngle; tilt<highAngle; tilt+=FOV_HEIGHT) {
                if(tilt + FOV_HEIGHT <= highAngle) { //enough room to draw whole image
                    grabTilt = tilt + FOV_HEIGHT/2;
                    placeY = imgHeight - height*(j+1);
                    placeHeight = height;
                }else {
                    grabTilt = highAngle - FOV_HEIGHT/2;
                    placeY = 0;
                    placeHeight = height/2+(height/2)*(highAngle-tilt)/FOV_HEIGHT;
                }
                cmd.takePicture(grabPan, grabTilt, width, height, false);
                positions[i][j][0] = rgram.send(cmd.getData(), cmd.getLength());
                positions[i][j][1] = placeX;
                positions[i][j][2] = placeY;
                positions[i][j][3] = placeWidth;
                positions[i][j][4] = placeHeight;
                //wait until the Stayton receives the request
                while(!rgram.wasReceived(positions[i][j][0])) {
                    try{Thread.sleep(5);}catch(Exception e) {}
                    timeWaitedForReceipt += 5;
                    if(timeWaitedForReceipt > waitTimeForReceipt)
                        return RoverState.COMM_DEAD;
                }
                j++;
            }
            i++;
            j=0;
        }
        int imagesTaken = imagesWide * imagesHigh;
        int imagesReceived = 0, errorImages = 0;
        imagesDone = 0;
        long lastRcvTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - lastRcvTime < 7000 && imagesReceived + errorImages < imagesTaken){
            for(i=0; i<imagesWide; i++)
                for(j=0; j<imagesHigh; j++) {
                    if(positions[i][j][0] > 0) { //if there is no result for it yet. . .
                        Datapack dpack = rgram.receive(positions[i][j][0], -1); //no wait for image
                        if(dpack != null) {
                            if(dpack.getLength() == RoverState.FULL_PACKET_LENGTH) {
                                positions[i][j][0] = -1; //indicate an error while grabbing image
                                errorImages++;
                            }else
                                try {
                                    ByteArrayInputStream bais = new ByteArrayInputStream(dpack.getData(), 0, dpack.getLength());
                                    BufferedImage image = ImageIO.read(bais);
                                    bais.close();
                                    if(image == null) {
                                        //System.out.println("the image was null");
                                        throw new Exception();
                                    }
                                    placeX = positions[i][j][1];
                                    placeY = positions[i][j][2];
                                    placeWidth = positions[i][j][3];
                                    placeHeight = positions[i][j][4];
                                    
                                    if(placeWidth != width || placeHeight != height) //only crop if necessary
                                        image = image.getSubimage(0, 0, placeWidth, placeHeight);
                                    //if the one just on top of this image has already been placed
                                    if(j == imagesHigh-2 && positions[i][j+1][0] == 0) {
                                        int intendedEnding = placeY+placeHeight;
                                        int upperImHeight = positions[i][imagesHigh-1][4];
                                        int myHeight = intendedEnding-upperImHeight;
                                        image = image.getSubimage(0, placeHeight-myHeight, placeWidth, myHeight);
                                        placeY += placeHeight-myHeight;
                                        placeHeight = myHeight;
                                        //System.out.println("i: "+i+" j: "+j+" myHeight: "+myHeight+" PlaceHeight: "+placeHeight);
                                    }
                                    //System.out.println("drawing "+placeX+" "+placeY+" "+placeWidth+" "+placeHeight);
                                    graphics.drawImage(image, placeX, placeY, placeWidth, placeHeight, null);
                                    imageUpdateTime = System.currentTimeMillis();
                                    positions[i][j][0] = 0;
                                    imagesReceived++;
                                    imagesDone++;
                                    
                                } catch(Exception e) {
                                    positions[i][j][0] = -1; //indicate an error while grabbing image
                                    e.printStackTrace();
                                    errorImages++;
                                }
                            lastRcvTime = System.currentTimeMillis();
                        }
                    }
                }
            
            try{Thread.sleep(20);}catch(Exception e) {}
        }
        
        Log.println("Successful images: " + imagesReceived + ", Total: " + imagesTaken + ", Errors: " + errorImages);
        
        if(imagesReceived == imagesTaken) {
            rov.look(0, 0);
            return RoverState.SUCCESS;
        }else
            return RoverState.COMM_DEAD;
    }
    
    public String getSummary() {
        return getShortSummary();
    }
    
    public int getReturnValue() {
        return ret;
    }
    
    public int getTime() {
        return time;
    }
    
    public String getShortSummary() {
        return "Take a panorama.";
    }
    
    public boolean isCompleted() {
        if (myThread == null)
            return false;
        return !myThread.isAlive();
    }
    
    public void kill() {
        quit = true;
    }
    
    public int getTimeRemaining() {
        return time - (int)((System.currentTimeMillis() - starttime) );
    }
    
    /** Returns the time (in ms) when the panoramic image was last updated with a 
     * new picture from the rover.
     * <p>
     * This method is here for backwards compatibility. New programs should use
     * <code>getImageUpdateTime</code>.
     *@see #getImageUpdateTime
     *
     */
    public long lastTimeImageUpdated() {
        return imageUpdateTime;
    }
    
    
    public long ImagesDone() {
        return imagesDone;
    }
    
    public long getImageUpdateTime() {
        return imageUpdateTime;
    }
    
    public BufferedImage getRecentImage() {
    return img;
    }
    
}
