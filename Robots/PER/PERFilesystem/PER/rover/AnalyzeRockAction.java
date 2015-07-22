/*
 * AnalyzeRockAction.java
 *
 * Created on August 20, 2003, 1:49 PM
 */

package PER.rover;
import PER.rover.control.*;
import java.awt.image.BufferedImage;


/**
 * Causes the rover to drive up to a rock, take a picture, and back away.
 * This action expects to be called when the rover is facing a rock.
 *
 * @author  Eric Porter
 */
public class AnalyzeRockAction implements Action {
    /** how far from rock to take picture from (in cm) */
    private final static int PICTURE_DIST = 5;
    /** how far it is from the IR to the front of the rover */
    private final static int IR_TO_FRONT_PROTO = 11;
    private final static int IR_TO_FRONT_ROVER = 2;
    
    private int time;
    private boolean lightUV;
    private int width, height;
    
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success = false, completed = false;
    transient private int ret = 0;
    transient private boolean quit = false;
    transient private long starttime = 0;
    transient private BufferedImage image;
    transient private BufferedImage noUVImage;
    private long nouvUpdateTime, uvUpdateTime;
    
    /** Creates a new instance of AnalyzeRockAction
     * This action expects to be called when the rover is facing a rock.  It will
     * drive up to the rock, take a picture, then back away.
     * @param lightUV Whether or not to have the UV light on while the picture
     *        is being taken.
     * @param width The width of the picture to be taken.
     * @param height The height of the picture to be taken.
     */
    public AnalyzeRockAction(boolean lightUV, int width, int height) {
        this.lightUV = lightUV;
        this.width = width;
        this.height = height;
        time = 10;
    }
    
    public boolean doAction(Rover r) {
        PER.rover.StatsLog.println(PER.rover.StatsLog.ANALYZE_ROCK);
        completed = false;
        rov = r;
        myThread = new Thread() {
            public void run() {
                success = false;
                image = null;
                uvUpdateTime = 0;
                noUVImage = null;
                nouvUpdateTime = 0;
                starttime = System.currentTimeMillis();
                
                quit = false;
                ret = analyzeRock();
                success = (ret == RoverState.SUCCESS);
                
                if (success) {
                    long endtime = System.currentTimeMillis();
                    time = (int)((endtime - starttime) );
                }
                completed = true;
            };
        };
        myThread.start();
        for(int i=0; i<10 && !myThread.isAlive(); i++)
            Thread.yield();
        return true;
    }
    
    public int getReturnValue() {
        return ret;
    }
    
    public String getShortSummary() {
        return "Drive to rock and take a picture.";
    }
    
    public String getSummary() {
        return getShortSummary();
    }
    
    public int getTime() {
        return time;
    }
    
    public int getTimeRemaining() {
        return time - (int)((System.currentTimeMillis() - starttime) );
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void kill() {
        quit = true;
    }
    
    /** Returns the second image taken by this Action. This image has the
     * UV light turned on.
     */
    public BufferedImage getImage() {
        return image;
    }
    
    /** Returns the first image taken by this Action. This image has the
     * UV light turned off.
     */
    public BufferedImage getNoUVImage() {
        return noUVImage;
    }
    
    public int analyzeRock() {
        ScanAction highSA = new ScanAction(PER.rover.Rover.USING_MARS_ROVER ? -15 : -10, -24, 24, 3);
        highSA.setBorderRocks(true);
        highSA.doAction(rov);
        while(!highSA.isCompleted())
            try{Thread.sleep(20);}catch(Exception e) {}
        if(quit == true)
            return ActionConstants.KILLED;
        if(!highSA.isSuccess())
            return highSA.getReturnValue();
        
        ScanAction lowSA = new ScanAction(PER.rover.Rover.USING_MARS_ROVER ? -25 : -20, -24, 24, 3);
        lowSA.setBorderRocks(true);
        lowSA.doAction(rov);
        while(!lowSA.isCompleted())
            try{Thread.sleep(20);}catch(Exception e) {}
        if(quit == true)
            return ActionConstants.KILLED;
        if(!lowSA.isSuccess())
            return lowSA.getReturnValue();
        
        int minDist, rockAngle;
        if(highSA.hasFoundRock() && !lowSA.hasFoundRock()) {
            minDist = highSA.closestReading();
            rockAngle = highSA.getRockAngle();
        }else if(!highSA.hasFoundRock() && lowSA.hasFoundRock()) {
            minDist = lowSA.closestReading();
            rockAngle = lowSA.getRockAngle();
        }else if(highSA.hasFoundRock() && lowSA.hasFoundRock()) {
            minDist = Math.min(highSA.closestReading(), lowSA.closestReading());
            rockAngle = lowSA.getRockAngle();
        }else //one of the scans should have found the rock
            return ActionConstants.NO_ROCK;
        
        if(Math.abs(rockAngle) >= 5) {
            //create a new TurnToAction to get near rock
            TurnToAction tta = new TurnToAction(rockAngle);
            tta.doAction(rov);
            while(!tta.isCompleted()) {
                if(quit)
                    tta.kill();
                try{Thread.sleep(20);}catch(Exception e) {}
            }
            if(!tta.isSuccess())
                return tta.getReturnValue();
        }
        
        //create a new DriveToAction to get near rock
        DriveToAction dta = new DriveToAction(minDist - PICTURE_DIST -
        (PER.rover.Rover.USING_MARS_ROVER ? IR_TO_FRONT_ROVER : IR_TO_FRONT_PROTO), 0,
        DriveToAction.STATIC_SAFETY, true);
        dta.doAction(rov);
        while(!dta.isCompleted()) {
            if(quit)
                dta.kill();
            try{Thread.sleep(20);}catch(Exception e) {}
        }
        if(!dta.isSuccess() && dta.getReturnValue() != RoverState.OBSTACLE_DETECTED)
            return dta.getReturnValue();
        
        noUVImage = rov.takePicture(0, PER.rover.Rover.USING_MARS_ROVER ? -45 : -20, width, height);
        nouvUpdateTime = System.currentTimeMillis();
        if(noUVImage == null)
            System.out.println("noUVImage is null - "+PER.rover.ActionConstants.getErrorText(rov.state.getStatus()));
        image = rov.takePicture(0, PER.rover.Rover.USING_MARS_ROVER ? -45 : -20, width, height, lightUV);
        uvUpdateTime = System.currentTimeMillis();
        if(image == null)
            System.out.println("image is null - "+PER.rover.ActionConstants.getErrorText(rov.state.getStatus()));
        
        //create a new DriveToAction to get away from rock
        dta = new DriveToAction(-12, 0, DriveToAction.NO_SAFETY, false);
        dta.doAction(rov);
        while(!dta.isCompleted()) {
            if(quit)
                dta.kill();
            try{Thread.sleep(20);}catch(Exception e) {}
        }
        
        if(image == null)
            return rov.state.getStatus();
        
        if(!dta.isSuccess())
            return dta.getReturnValue();
        
        return RoverState.SUCCESS;
    }
    
    public long getImageUpdateTime() {
        if(uvUpdateTime != 0)
            return uvUpdateTime;
        else return nouvUpdateTime;
    }
    
    public java.awt.image.BufferedImage getRecentImage() {
        if(image != null)
            return image;
        else return noUVImage;
        //return null;
    }
    
}
