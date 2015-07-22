/*
 * VisionUtil.java
 *
 * Created on May 13, 2004, 2:45 PM
 */

package PER.rover.control;

/** Utilities for vision.  Primarily conversions of different sorts.
 *
 * @author  Eric Porter
 */
public class VisionUtil {
   
   public static final int TRACK_WIDTH = 176;
   public static final int TRACK_HEIGHT = 144;
   
   public static final int FOV_WIDTH = 45;
   public static final int FOV_HEIGHT = 34;
   
   /** The height of the camera in cm */
   public static final int CAMERA_HEIGHT = 32;
   
   /** Creates a new instance of VisionUtil */
   public VisionUtil() {
   }
   
/*
 * convert a YUV420P to a rgb image
 */
   public static boolean v4l_yuv420p2rgb(byte yuv_in [], int width, int height, int rgb_out []) {
      if(yuv_in.length < 3*width*height/2)
         return false;
      
      int numpix = width * height;
      int h, w, y00, y01, y10, y11, u, v;
      int pY = 0;
      int pU = pY + numpix;
      int pV = pU + numpix / 4;
      int pOut = 0;
      
      for (h = 0; h <= height - 2; h += 2) {
         for (w = 0; w <= width - 2; w += 2) {
            y00 = ByteUtil.unsign(yuv_in[pY]);
            y01 = ByteUtil.unsign(yuv_in[pY + 1]);
            y10 = ByteUtil.unsign(yuv_in[pY + width]);
            y11 = ByteUtil.unsign(yuv_in[pY + width + 1]);
            u = ByteUtil.unsign(yuv_in[pU++]) - 128;
            v = ByteUtil.unsign(yuv_in[pV++]) - 128;
            
            v4l_copy_420_block(y00, y01, y10, y11, u, v, width, pOut, rgb_out);
            pY += 2;
            pOut += 2;
         }
         pY += width;
         pOut += width;
      }
      return true;
   }
   
/*
 * convert a YUV420P to a yuv image in the same format as an rgb one
 */
   public static boolean v4l_yuv420p2yuv(byte yuv_in [], int width, int height, int yuv_out []) {
      if(yuv_in.length < 3*width*height/2)
         return false;
      
      int numpix = width * height;
      int h, w, y00, y01, y10, y11, u, v;
      int pY = 0;
      int pU = pY + numpix;
      int pV = pU + numpix / 4;
      int pOut = 0;
      
      for (h = 0; h <= height - 2; h += 2) {
         for (w = 0; w <= width - 2; w += 2) {
            y00 = ByteUtil.unsign(yuv_in[pY]);
            y01 = ByteUtil.unsign(yuv_in[pY + 1]);
            y10 = ByteUtil.unsign(yuv_in[pY + width]);
            y11 = ByteUtil.unsign(yuv_in[pY + width + 1]);
            u = ByteUtil.unsign(yuv_in[pU++]);
            v = ByteUtil.unsign(yuv_in[pV++]);
            
            yuv_out[pOut] = (255<<24) | (v<<16) | (y00<<8) | u;
            yuv_out[pOut+1] = (255<<24) | (v<<16) | (y01<<8) | u;
            yuv_out[pOut+width] = (255<<24) | (v<<16) | (y10<<8) | u;
            yuv_out[pOut+width+1] = (255<<24) | (v<<16) | (y11<<8) | u;
            
            pY += 2;
            pOut += 2;
         }
         pY += width;
         pOut += width;
      }
      return true;
   }
   
   /** 
    * Converts a trackX value into a pan adjustment.  To find the real location 
    * of an object, take pan + objectPan(trackX)
    * @param trackX The trackX value from color tracking.
    * @return the amount by which you should adjust the pan.
    */
   public static int objectPan(int trackX) {
      return (TRACK_WIDTH/2-trackX)*FOV_WIDTH/TRACK_WIDTH;
   }
   
   /** 
    * Converts a trackY value into a tilt adjustment.  To find the real location 
    * of an object, take tilt + objectTilt(trackY)
    * @param trackY The trackY value from color tracking.
    * @return the amount by which you should adjust the tilt.
    */
   public static int objectTilt(int trackY) {
      return (TRACK_HEIGHT/2-trackY)*FOV_HEIGHT/TRACK_HEIGHT;
   }
   
   /* LIMIT: convert a 16.16 fixed-point value to a byte, with clipping. */
   private static int LIMIT(int x) {
      return ((x)>0xffffff?0xff: ((x)<=0xffff?0:((x)>>16)));
   }
   
/*
 */
   private static void v4l_copy_420_block(int yTL, int yTR, int yBL, int yBR,
   int u, int v, int rowPixels, int offset, int rgb []) {
      final int rvScale = 91881;
      final int guScale = -22553;
      final int gvScale = -46801;
      final int buScale = 116129;
      final int yScale  = 65536;
      int r, g, b;
      
      g = guScale * u + gvScale * v;
      r = rvScale * v;
      b = buScale * u;
      
      yTL *= yScale; yTR *= yScale;
      yBL *= yScale; yBR *= yScale;
      
      rgb[offset] = (255<<24) | (LIMIT(r+yTL)<<16) | (LIMIT(g+yTL)<<8) | LIMIT(b+yTL);
      rgb[offset+1] = (255<<24) | (LIMIT(r+yTR)<<16) | (LIMIT(g+yTR)<<8) | LIMIT(b+yTR);
      
      /* Skip down to next line to write out bottom two pixels */
      offset += rowPixels;
      
      rgb[offset] = (255<<24) | (LIMIT(r+yBL)<<16) | (LIMIT(g+yBL)<<8) | LIMIT(b+yBL);
      rgb[offset+1] = (255<<24) | (LIMIT(r+yBR)<<16) | (LIMIT(g+yBR)<<8) | LIMIT(b+yBR);
      
   }
   
}
