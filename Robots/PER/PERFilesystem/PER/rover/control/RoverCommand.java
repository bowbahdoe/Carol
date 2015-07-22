/*
 * RoverCommand.java
 *
 * Created on June 30, 2003, 9:33 AM
 */

package PER.rover.control;


/** Low level rover commands - they mainly interface from RoverController
 * methods to the stayton.
 *
 *@author Eric Porter
 */

public class RoverCommand {
    
    //simple stuff
    private final static byte INIT_ROVER                 = (byte) 'i';
    private final static byte HEAD_MOVE                  = (byte) 'h';
    private final static byte SET_ALL                    = (byte) 'l';
    private final static byte SPIN                       = (byte) 'p';
    private final static byte CRAB                       = (byte) 'c';
    private final static byte QUAD_TURN                  = (byte) 'q';
    private final static byte SCAN                       = (byte) 'n';
    private final static byte SET_LIGHT                  = (byte) 'x';
    private final static byte GET_VERSION                = (byte) 'v';
    //cmaera commands
    private final static byte TAKE_PICTURE               = (byte) 'a';
    private final static byte GET_PROPERTIES             = (byte) 'b';
    private final static byte START_TRACK                = (byte) 'r';
    private final static byte STOP_STREAMING             = (byte) 'o';
    private final static byte GET_MEAN                   = (byte) 13;
    private final static byte START_MOTION               = (byte) 14;
    
    //advanced commands
    private final static byte GOTO                       = (byte) 'g';
    private final static byte TURNTO                     = (byte) 't';
    private final static byte STOP_AT_MARK               = (byte) 's';
    //private final static byte ALIGN_TO_MARK              = (byte) 'a';
    private final static byte GET_UPDATE                 = (byte) 'u';
    private final static byte KILL                       = (byte) 'k';
    private final static byte SET_CALIBRATION            = (byte) 'y';
    private final static byte GET_CALIBRATION            = (byte) 'z';
    private final static byte SET_SCAN_LIST              = (byte) 11;
    private final static byte GET_SCAN_LIST              = (byte) 12;
    
    private byte [] buf; //the internal buffer for the packet
    private int BUFFER_LENGTH = 1000;
    
    private int commandLength;
    
    public RoverCommand() {
        buf = new byte[BUFFER_LENGTH];
    }
    
    public byte [] getData() {
        return buf;
    }
    
    public int getLength() {
        return commandLength;
    }
    
    private void setType(byte type) {
        buf[0] = type;
    }
    
    public RoverCommand initRover() {
        commandLength = 1;
        setType(INIT_ROVER);
        return this;
    }
    
    public RoverCommand takePicture(int pan, int tilt, int width, int height, boolean lightUV) {
        commandLength = 20;
        setType(TAKE_PICTURE);
        buf[1] = 0;
        buf[2] = (byte) (lightUV ? 1 : 0);
        ByteUtil.intToNetworkLong(buf, pan, 4);
        ByteUtil.intToNetworkLong(buf, tilt, 8);
        ByteUtil.intToNetworkLong(buf, width, 12);
        ByteUtil.intToNetworkLong(buf, height, 16);
        return this;
    }
    
    public RoverCommand takeRecentPicture() {
        commandLength = 20;
        setType(TAKE_PICTURE);
        buf[1] = 1;
        buf[2] = 0;
        return this;
    }
    
    public RoverCommand takeRawPicture(int pan, int tilt, int width, int height, boolean lightUV) {
        commandLength = 20;
        setType(TAKE_PICTURE);
        buf[1] = 2;
        buf[2] = (byte) (lightUV ? 1 : 0);
        ByteUtil.intToNetworkLong(buf, pan, 4);
        ByteUtil.intToNetworkLong(buf, tilt, 8);
        ByteUtil.intToNetworkLong(buf, width, 12);
        ByteUtil.intToNetworkLong(buf, height, 16);
        return this;
    }
    
    public RoverCommand scan(int tilt, int minPan, int maxPan, int step) {
        commandLength = 20;
        setType(SCAN);
        ByteUtil.intToNetworkLong(buf, tilt, 4);
        ByteUtil.intToNetworkLong(buf, minPan, 8);
        ByteUtil.intToNetworkLong(buf, maxPan, 12);
        ByteUtil.intToNetworkLong(buf, step, 16);
        return this;
    }
    
    public RoverCommand spin(int speed) {
        commandLength = 8;
        setType(SPIN);
        ByteUtil.intToNetworkLong(buf, speed, 4);
        return this;
    }
    
    public RoverCommand crab(int speed, int angle) {
        commandLength = 12;
        setType(CRAB);
        ByteUtil.intToNetworkLong(buf, speed, 4);
        ByteUtil.intToNetworkLong(buf, angle, 8);
        return this;
    }
    
    public RoverCommand quadTurn(int speed, int radius) {
        commandLength = 12;
        setType(QUAD_TURN);
        ByteUtil.intToNetworkLong(buf, speed, 4);
        ByteUtil.intToNetworkLong(buf, radius, 8);
        return this;
    }
    
    public RoverCommand headMove(boolean movePan, int panAngle, boolean moveTilt, int tiltAngle) {
        commandLength = 12;
        setType(HEAD_MOVE);
        buf[1] = (byte)(movePan ? 1 : 0);
        buf[2] = (byte)(moveTilt ? 1 : 0);
        ByteUtil.intToNetworkLong(buf, panAngle, 4);
        ByteUtil.intToNetworkLong(buf, tiltAngle, 8);
        return this;
    }
    
    public RoverCommand setAll(int mask, int motor0, int motor1, int servo0, int servo1,
    int servo2, int servo3, int pan, int tilt) {
        commandLength = 40;
        setType(SET_ALL);
        buf[1] = buf[2] = buf[3] = 0;
        ByteUtil.intToNetworkLong(buf, mask, 4);
        ByteUtil.intToNetworkLong(buf, motor0, 8);
        ByteUtil.intToNetworkLong(buf, motor1, 12);
        ByteUtil.intToNetworkLong(buf, servo0, 16);
        ByteUtil.intToNetworkLong(buf, servo1, 20);
        ByteUtil.intToNetworkLong(buf, servo2, 24);
        ByteUtil.intToNetworkLong(buf, servo3, 28);
        ByteUtil.intToNetworkLong(buf, pan, 32);
        ByteUtil.intToNetworkLong(buf, tilt, 36);
        return this;
    }
    
    public RoverCommand goTo(int dist, int angle, byte safetyLevel, boolean takePics) {
        commandLength = 12;
        setType(GOTO);
        buf[1] = safetyLevel;
        buf[2] = (byte)(takePics ? 0 : 1);
        buf[3] = 0;
        ByteUtil.intToNetworkLong(buf, dist, 4);
        ByteUtil.intToNetworkLong(buf, angle, 8);
        return this;
    }
    
    public RoverCommand turnTo(int degrees, boolean takePics) {
        commandLength = 8;
        setType(TURNTO);
        buf[1] = (byte)(takePics ? 0 : 1);
        buf[2] = buf[3] = 0;
        ByteUtil.intToNetworkLong(buf, degrees, 4);
        return this;
    }
    
    public RoverCommand killHL() {
        commandLength = 1;
        setType(KILL);
        return this;
    }
    
    public RoverCommand getUpdate() {
        commandLength = 1;
        setType(GET_UPDATE);
        return this;
    }
    
    public RoverCommand getCalibration() {
        commandLength = 1;
        setType(GET_CALIBRATION);
        return this;
    }
    
    public RoverCommand setCalibration(String cal) {
        commandLength = 1+cal.length();
        //make sure buffer is large enough
        if(cal.length() + 1 >= BUFFER_LENGTH) {
            BUFFER_LENGTH = cal.length() + 1;
            buf = new byte[BUFFER_LENGTH];
        }
        setType(SET_CALIBRATION);
        //copy into buffer
        byte [] bytes = cal.getBytes();
        for(int i=0; i<bytes.length; i++)
            buf[i+1] = bytes[i];
        return this;
    }
    
    public RoverCommand getScanList() {
        commandLength = 1;
        setType(GET_SCAN_LIST);
        return this;
    }
    
    public RoverCommand setScanList(String cal) {
        commandLength = 1+cal.length();
        //make sure buffer is large enough
        if(cal.length() + 1 >= BUFFER_LENGTH) {
            BUFFER_LENGTH = cal.length() + 1;
            buf = new byte[BUFFER_LENGTH];
        }
        setType(SET_SCAN_LIST);
        //copy into buffer
        byte [] bytes = cal.getBytes();
        for(int i=0; i<bytes.length; i++)
            buf[i+1] = bytes[i];
        return this;
    }
    
    public RoverCommand setLight(boolean on) {
        commandLength = 2;
        setType(SET_LIGHT);
        buf[1] = (byte) (on ? 1 : 0);
        return this;
    }
    
    public RoverCommand getVersion() {
        commandLength = 1;
        setType(GET_VERSION);
        return this;
    }
    
    public RoverCommand startTrack(int minY, int maxY, int minU, int maxU,
    int minV, int maxV, int trackMethod, boolean movePan, boolean moveTilt, int driveMethod) {
        commandLength = 10;
        setType(START_TRACK);
        buf[1] = (byte) minY;
        buf[2] = (byte) maxY;
        buf[3] = (byte) minU;
        buf[4] = (byte) maxU;
        buf[5] = (byte) minV;
        buf[6] = (byte) maxV;
        buf[7] = (byte) trackMethod;
        buf[8] = (byte) ((movePan ? 2 : 0) + (moveTilt ? 1 : 0));
        buf[9] = (byte) driveMethod;
        return this;
    }
    
    public RoverCommand stopStreaming() {
        commandLength = 1;
        setType(STOP_STREAMING);
        return this;
    }
    
    public RoverCommand getMean(boolean stream) {
        commandLength = 2;
        setType(GET_MEAN);
        buf[1] = (byte) (stream ? 1 : 0);
        return this;
    }
    
    public RoverCommand startMotion() {
        commandLength = 1;
        setType(START_MOTION);
        return this;
    }
    
    public RoverCommand getProperties() {
        commandLength = 1;
        setType(GET_PROPERTIES);
        return this;
    }
}
