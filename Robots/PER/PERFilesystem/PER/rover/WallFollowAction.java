/*
 * WallFollowAction.java
 *
 * Created on April 7, 2004, 2:45 PM
 */

package PER.rover;

import PER.rover.control.RoverState;

/** WallFollowAction will follow a flat surface for a specified
 * centimeter distance. The action is considered successfull
 * if the wall is followed for the complete distance. The action is
 * not successful if the rover encounters an obstacle or reaches
 * the end of the wall before traversing the entire distance specified.
 *
 *
 * @author Brian Dunlavey
 */


public class WallFollowAction implements Action {
    
    private int dist;
    private int time;
    
    transient private Rover rov = null;
    transient private Thread myThread = null;
    transient private boolean success;
    transient private int ret;
    transient private boolean quit = false;
    transient private long starttime;
    
    /** Creates a new instance of WallFollowAction which drives forever
     */
    public WallFollowAction(){
        this(-1);
    }
    
    /** Creates a new instance of WallFollowAction with the saftey on.
     *
     * @param dist the distance to travel along the wall in centimeters
     */
    public WallFollowAction(int dist) {
        this.dist = dist;
    }
    
    public boolean doAction(Rover r) {
        //PER.rover.StatsLog.println(PER.rover.StatsLog.DRIVE,dist);
        rov = r;
        myThread = new Thread() {
            public void run() {
                starttime = System.currentTimeMillis();
                
                quit = false;
                if(dist == -1)
                    follow();
                else
                    success = follow(dist);
                
                long endtime = System.currentTimeMillis();
                if (success)
                    time = (int)((endtime - starttime) );
            }
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
        return "Wall follow.";
    }
    
    public String getSummary() {
        return "Wall follow for "+dist+" cm";
    }
    
    public int getTime() {
        return time;
    }
    
    public int getTimeRemaining() {
        return time - (int)((System.currentTimeMillis() - starttime) );
    }
    
    public boolean isCompleted() {
        return myThread != null && !myThread.isAlive();
    }
    
    /** Returns true if the rover traversed the entire specified distance.
     * Returns false if an obstacle was detected, the end of the wall
     * was reached before completing the entire distance, or some other
     * error occurred.
     */
    public boolean isSuccess() {
        return success;
    }
    
    public void kill() {
        quit = true;
    }
    
        public long getImageUpdateTime() {
        return 0;
    }
    
    public java.awt.image.BufferedImage getRecentImage() {
        return null;
    }
    
    /** Translates the values in a nine item array from range data into CM */
    private int[] translateScanArray(int[] rangeData){
        int[] rangeInCM = new int[9];
        for(int i = 0; i < 9; i++)
            rangeInCM[i] = ScanAction.translateScan(rangeData[i]);
        return rangeInCM;
    }
    
    /** This scans 9 points and defines the 2 edge points as the left and right
     *  sides, and the rest as it's front.  It thresholds the returns after
     *  converting them to CM and then returns the results
     */
    private boolean[] nineScan(){
        boolean[] bWalls = new boolean[3]; //left center right
        int[] scanResults = new int[9];
        int[] CMResults = new int[9];
        int minDist = 40;
        
        if((scanResults = rov.scan(-25, -120, 120, 30)) == null)
            System.err.println("rov.scan null");
        
        CMResults = translateScanArray(scanResults);
        
        if(CMResults[6] > minDist && CMResults[7] > minDist && CMResults[8] > minDist)
            bWalls[2] = false;
        else
            bWalls[2] = true;
        if(CMResults[0] > minDist && CMResults[1] > minDist && CMResults[2] > minDist)
            bWalls[0] = false;
        else
            bWalls[0] = true;
        if(CMResults[3] > minDist && CMResults[4] > minDist && CMResults[5] > minDist)
            bWalls[1] = false;
        else
            bWalls[1] = true;
        
//        System.out.println("left side  " + CMResults[0] + " " + CMResults[1] + " " + bWalls[0]);
//        System.out.println("center     " + CMResults[3] + " " + CMResults[4] + " " + CMResults[8] + " " + CMResults[2] + " " + CMResults[5] + " " + bWalls[1]);
//        System.out.println("right side " + CMResults[6] + " " + CMResults[7] + " " + bWalls[2]);
        return bWalls;
    }
    
    /** A stateless wallfollwer which makes right turns and left arcs
     *    It thresholds the returns and if
     *  turns right if something is in the way, otherwise it goes straight if
     *  nothing is nearby or arcs if something is near.
     *
     *@param totalDist distance which will be traveled
     */
    private boolean follow(int totalDist){
        
        boolean[] walls = new boolean[3]; //left, center, right        
        time = (200+3200+Math.abs(totalDist)*311);   // time to drive, plus 5cm/s
        long accumulatedTime = 0;
        long startDriveTime = System.currentTimeMillis();
        boolean moving = false;
        
        //while(accumulatedTime < time){
        while(!quit && accumulatedTime < time){
            walls = nineScan();
            if(walls[0] == false && walls[2] == false && walls[1] == false){
                accumulatedTime = accumulatedTime + System.currentTimeMillis() - startDriveTime;
                startDriveTime = System.currentTimeMillis();
                moving = true;
                System.err.println("nothing near me, going straight");
                rov.crab(200, 0);
            } else if(walls[2] == false || walls[1] == true) {
                if(moving)
                    accumulatedTime = accumulatedTime + System.currentTimeMillis() - startDriveTime;
                else
                    startDriveTime = System.currentTimeMillis();
                System.err.println("nothing to the right or something ahead.  turning right");
                rov.spin(-200);
                moving = false;
            } else {
                accumulatedTime = accumulatedTime + System.currentTimeMillis() - startDriveTime;
                startDriveTime = System.currentTimeMillis();
                System.err.println("nothing straight.  curving left");
                rov.quadTurn(200, 50);
                moving = true;                
            }
            System.err.println("I'm moved " + accumulatedTime + " ms");
        }
        rov.crab(0,0);
        System.err.println("All done.  Moved for " + accumulatedTime + "ms");
        return true;
    }
    
    /** same thing only with no timechecks, so it runs forever
     */
    private void follow(){
        boolean[] walls = new boolean[3]; //left, center, right
        
        //while(true){
        while(!quit){
            walls = nineScan();
            if(walls[0] == false && walls[2] == false && walls[1] == false){
                System.err.println("nothing near me, going straight");
                rov.crab(200, 0);
            } else if(walls[2] == false || walls[1] == true) {
                System.err.println("nothing to the right or something ahead.  turning right");
                rov.spin(-200);
            }else {
                System.err.println("nothing straight.  arcing left");
                rov.quadTurn(200, 50);
            }
        }
    }
}
