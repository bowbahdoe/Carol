/*
 * ScanAction.java
 *
 * Created on August 14, 2003, 12:14 PM
 */

package PER.rover;
import PER.rover.control.*;

/**
 * ScanAction.java uses the IR rangefinder to search for "rocks" near
 * the rover. If multiple rocks are found, preference is given to the
 * one closest to the front of the rover.
 *
 * @author  Eric Porter
 */
public class ScanAction implements PER.rover.Action {
    /** An object needs to be at least this close to be called a rock. */
    public static final int DISTANCE_THRESHOLD = 100;
    /** A rock can be at most this wide, otherwise I call it a wall. */
    public static final int MAX_ROCK_WIDTH = 45;
    /** The height of the IR range finger, in cm. */
    public static final int IR_HEIGHT = 30;
    
    private boolean inputIsValid;
    private int tilt, minPan, maxPan, step;
    private int time;
    //by default it won't search border rocks - that is rocks on the edge of the scan area
    private boolean allowBorderRocks = false;
    
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success = false, completed = false;
    transient private int ret = 0;
    transient private boolean quit = false;
    transient private long starttime = 0;
    transient private boolean foundRock = false, detectedWall = false;
    transient private int rockAngle = 0, rockDist = 0;
    transient private int [] rawScanVals, scanDists;
    transient private int minIndex, maxIndex; //the minimum and maximum indecies of the rock
    
    /** Creates a new instance of ScanAction with default scan parameters of
     * tilt -10, minimum pan angle -135, maximum pan angle 135, and step 5.
     */
    public ScanAction() {
        this(-10, -135, 135, 5);
    }
    
    /**
     * Scans the area around where the rover is.  The scan is done starting with
     * the minPan and increments to maxPan.  Scans are done at positions from
     * minPan to maxPan inclusive.
     * @param tilt The tilt at which the scan is done.
     * @param minPan The pan angle at which the scan starts
     * @param maxPan The pan angle at which the scan ends.
     * @param step The angle difference between successive scan points.
     */
    public ScanAction(int tilt, int minPan, int maxPan, int step) {
        this.tilt = tilt;
        this.minPan = minPan;
        this.maxPan = maxPan;
        this.step = step;
        inputIsValid = !(tilt > 90 || tilt < -90 || minPan < -180 || maxPan > 180
        || minPan > maxPan || step <= 0);
        if(inputIsValid)
            time = 200+(maxPan-minPan)*60/step;
        else
            time = 0;
    }
    
    public boolean doAction(Rover r) {
        completed = false;
        PER.rover.StatsLog.println(PER.rover.StatsLog.SCAN);
        
        if(!inputIsValid) {
            completed = true;
            success = false;
            ret = RoverState.BAD_INPUT;
            return false;
        }
        rov = r;
        myThread = new Thread() {
            public void run() {
                success = false;
                starttime = System.currentTimeMillis();
                
                quit = false;
                success = scan();
                
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
        return "Scans the area around the rover.";
    }
    
    public String getSummary() {
        return "Scans around the rover for objects.  At tilt "+tilt+" it scans from "+
        minPan+" to "+maxPan+" taking samples every "+step+" degrees.";
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
    
    /** this action can't currently be killed
     */
    public void kill() {
    }
    
    private boolean scan() {
        rawScanVals = rov.scan(tilt, minPan, maxPan, step);
        if(rawScanVals == null) {
            ret = rov.state.getStatus();
            return false;
        }
        translateScanVals();
        analyzeData();
        if(foundRock)
            rov.look(rockAngle, tilt);
        else
            rov.look(0, 0);
        return true;
    }
    
    /** Getter for property foundRock.
     * @return Whether or not a rock has been detected, only valid once action is complete.
     *
     */
    public boolean hasFoundRock() {
        return foundRock;
    }
    
    /** Getter for property rockAngle.
     *
     * @return The angle to the nearest rock, but only if a rock was found.
     */
    public int getRockAngle() {
        return rockAngle;
    }
    
    /** Getter for property rockDist.
     * @return The cm distance to a detected rock.
     *
     */
    public int getRockDist() {
        return rockDist;
    }
    
    public int getMinRockAngle() {
        return getAngle(minIndex);
    }
    
    public int getMaxRockAngle() {
        return getAngle(maxIndex);
    }
    
    public int closestReading() {
        int minDist = Integer.MAX_VALUE;
        for(int i=0; i<scanDists.length; i++) {
            if(scanDists[i] < ScanAction.DISTANCE_THRESHOLD + 15 && scanDists[i] > 0) {
                //find the dist from the front of the rover
                int dist = (int)(scanDists[i]*Math.cos(Math.toRadians(tilt))*
                Math.cos(Math.toRadians(minPan+i*step)));
                if(dist < minDist)
                    minDist = dist;
            }
        }
        return minDist;
    }
    
    /**
     * Finds and returns the closest point from the recent scan
     * @return an array containing the distance in position 0 and the angle in
     * position 1, or null if nothing was detected.
     */
    public int [] getClosestPoint() {
        int minDist = Integer.MAX_VALUE, angleIndex=0;
        for(int i=0; i<scanDists.length; i++) {
            if(scanDists[i] < minDist) {
                minDist = scanDists[i];
                angleIndex = i;
            }
        }
        if(minDist < Integer.MAX_VALUE)
            return new int [] {minDist, getAngle(angleIndex)};
        else
            return null;
    }
    
    /** Translates the range sensing output from a GP2Y0A02YK to a distance in cm.
     * Values outside of the range are translated to Integer.MAX_VALUE
     */
    private void translateScanVals() {
        if(rawScanVals == null)
            return;
        scanDists = new int [rawScanVals.length];
        for(int i=0; i<rawScanVals.length; i++) {
            scanDists[i] = translateScan(rawScanVals[i]);
         /*if(rawScanVals[i] < 25)
            scanDists[i] = Integer.MAX_VALUE;
         else if(rawScanVals[i] > 135)
            scanDists[i] = Integer.MAX_VALUE;
         else
            scanDists[i] = (int)Math.round(2.9318e-6*Math.pow(rawScanVals[i], 4)-.00111014*Math.pow(rawScanVals[i], 3)+
            .156177*Math.pow(rawScanVals[i], 2)-10.1177*rawScanVals[i]+298.26);*/
            //an estimation using a 4th degree polynomial
            //System.out.println("pan: "+getAngle(i)+" dist: "+scanDists[i]+" raw: "+rawScanVals[i]);
        }
    }
    
    public static int translateScan(int scan) {
        if(scan < 25)
            return Integer.MAX_VALUE;
        else if(scan > 135)
            return 20; //too high to really measure, it shouldn't be this high.
        else
            return (int)Math.round(2.9318e-6*Math.pow(scan, 4)-.00111014*Math.pow(scan, 3)+
            .156177*Math.pow(scan, 2)-10.1177*scan+298.26);
    }
    
    //should be called after translation
    private void analyzeData() {
        //a copy of the translated values for me to work with
        int [] workCopy = new int [scanDists.length];
        for(int i=0; i<scanDists.length; i++)
            workCopy[i] = scanDists[i];
        
        //the distance at which the IR beam would hit the ground - on a flat floor
        double hitGroundDist = IR_HEIGHT/Math.tan(Math.toRadians(Math.abs(tilt)));
        // the maximum distance at which I can consider it a rock
        int maxRockDist = Math.min(DISTANCE_THRESHOLD, (int) (hitGroundDist*.7));
        
        int minDist, cminIndex=0, cmaxIndex;
        do {
            minDist = Integer.MAX_VALUE;
            for(int i=0; i<workCopy.length; i++)
                if(workCopy[i] < minDist) {
                    minDist = workCopy[i];
                    cminIndex = i;
                }
            
            if(minDist <= maxRockDist) {
                cmaxIndex = cminIndex;
                //expand to fill all values within 12 cm of the lowest detected point,
                //also fill points within 6 raw values in case of noise
                while(cminIndex > 0 && (Math.abs(workCopy[cminIndex-1] - workCopy[cminIndex]) < 12 ||
                Math.abs(rawScanVals[cminIndex-1] - rawScanVals[cminIndex]) <= 11))
                    cminIndex--;
                while(cmaxIndex < workCopy.length-1 && (Math.abs(workCopy[cmaxIndex+1] - workCopy[cmaxIndex]) < 12 ||
                Math.abs(rawScanVals[cmaxIndex+1] - rawScanVals[cmaxIndex]) <= 11))
                    cmaxIndex++;
                double width = getDist(cminIndex, cmaxIndex);
                //System.out.println("width: "+width+" dist: "+minDist+" min Angle: "+
                //getAngle(cminIndex)+" max Angle: "+getAngle(cmaxIndex));
                if(width <= MAX_ROCK_WIDTH && cminIndex != cmaxIndex && (allowBorderRocks || cminIndex != 0)
                && (allowBorderRocks || cmaxIndex != (workCopy.length-1))) { //it's a rock
                    if(foundRock) {
                        //I've seen a rock before - only replace it if its
                        //angle is less than the current rock's angle
                        int newRockAngle = (getAngle(cminIndex) + getAngle(cmaxIndex))/2;
                        if(Math.abs(newRockAngle) < Math.abs(rockAngle)) {
                            rockDist = minDist;
                            minIndex = cminIndex;
                            maxIndex = cmaxIndex;
                            rockAngle = newRockAngle;
                        }
                    }else { //this is the first rock found
                        foundRock = true;
                        rockDist = minDist;
                        minIndex = cminIndex;
                        maxIndex = cmaxIndex;
                        rockAngle = (getAngle(cminIndex) + getAngle(cmaxIndex))/2;
                    }
                } else if(width > MAX_ROCK_WIDTH)
                    detectedWall = true;
                //clear these values so I won't use them again
                for(int i=cminIndex; i<=cmaxIndex; i++)
                    workCopy[i] = Integer.MAX_VALUE;
            }
            
        }while(minDist <= maxRockDist);
        
    }
    
    /** Gets the distance between the two indecies in the scanDists array
     */
    private double getDist(int index1, int index2){
        if(scanDists[index1] == Integer.MAX_VALUE || scanDists[index2] == Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        double x1 = scanDists[index1]*Math.cos(getAngleRad(index1));
        double y1 = scanDists[index1]*Math.sin(getAngleRad(index1));
        double x2 = scanDists[index2]*Math.cos(getAngleRad(index2));
        double y2 = scanDists[index2]*Math.sin(getAngleRad(index2));
        return Math.sqrt(Math.pow(x1-x2, 2)+Math.pow(y1-y2, 2));
    }
    
    private int getAngle(int index) {
        return (int) Math.round(Math.toDegrees(getAngleRad(index)));
    }
    
    private double getAngleRad(int index) {
        double ang =  Math.toRadians(minPan + index*step);
        if(PER.rover.Rover.USING_MARS_ROVER)
            ang -= Math.atan(1.5/scanDists[index]);
        return ang;
    }
    
    public void setBorderRocks(boolean allow) {
        allowBorderRocks = allow;
    }
    
    /** Returns whether or not a wall was detected. Only valid once the action
     * is completed.
     */
    public boolean hasDetectedWall() {
        return detectedWall;
    }
    
    /** If the rover is near a rock and will turn into it, this function should
     *  make it so that the rover won't hit the rock
     * @param rov An active instantion of a Rover
     * @param turnAngle the angle that the rover is about to turn
     */
    public int moveFromRock(Rover rov, int turnAngle) {
        boolean needToMove = false;
        int angle=0;
        //if(turnAngle > 0) {
        for(int i=0; i<rawScanVals.length; i++)
            if(getAngle(i) > 40 && rawScanVals[i] > 125) {
                needToMove = true;
                angle = 80;
                break;
            }
        //}else {
        for(int i=0; i<rawScanVals.length; i++)
            if(getAngle(i) < -40 && rawScanVals[i] > 125) {
                needToMove = true;
                angle = -80;
                break;
            }
        //}
        if(needToMove) {
            DriveToAction backOff = new DriveToAction(-5, angle, false);
            backOff.doAction(rov);
            while(!backOff.isCompleted())
                try{Thread.sleep(20);}catch(Exception e) {}
            if(backOff.isSuccess())
                return RoverState.SUCCESS;
        }
        return RoverState.SUCCESS;
    }
    
    public long getImageUpdateTime() {
        return 0;
    }
    
    public java.awt.image.BufferedImage getRecentImage() {
        return null;
    }
    
}
