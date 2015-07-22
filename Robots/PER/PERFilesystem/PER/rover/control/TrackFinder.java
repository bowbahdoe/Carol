/*
 * TrackFinder.java
 *
 * Created on October 27, 2003, 4:04 PM
 */

package PER.rover.control;
import java.util.LinkedList;

/**
 * This is a class that, given an image and a measure of how much of a deviation
 * from mean is allowed, creates good track color parameters.
 * @author  Eric Porter
 */
public class TrackFinder {
   
   public int yr, ur, vr; //the ranges
   private int yuv [][][]; //storage for yuv data - 0=v, 1=y, 2=u
   public boolean currMask[][], workMask[][]; //the tracked pixels
   private int width=0, height=0;
   //int wpix, wtoty, wtotu, wtotv;
   public int cminy, cmaxy, cminu, cmaxu, cminv, cmaxv, cpix; //vars for current track
   private int wminy, wmaxy, wminu, wmaxu, wminv, wmaxv, wpix; //vars for work (tmp)
   
   /** Creates a new instance of TrackFinder
    * @param yRange maximum deviation allowed from mean Y
    * @param uRange maximum deviation allowed from mean U
    * @param vRange maximum deviation allowed from mean V
    */
   public TrackFinder(int yRange, int uRange, int vRange) {
      yr = yRange;
      ur = uRange;
      vr = vRange;
   }
   
   /**
    * You should call this whenever the source pixels have changed
    */
   public void setYUV(int yuv_in[], int width, int height) {
      if(this.width != width || this.height != height) {
         this.width = width;
         this.height = height;
         currMask = new boolean[height][width];
         workMask = new boolean[height][width];
         yuv = new int[height][width][3];
      }
      int offset = 0;
      for(int y=0; y<height; y++)
         for(int x=0; x<width; x++) {
            currMask[y][x] = false;
            yuv[y][x][0] =  ((yuv_in[offset] >> 16) & 255);
            yuv[y][x][1] =  ((yuv_in[offset] >> 8) & 255);
            yuv[y][x][2] =  (yuv_in[offset] & 255);
            offset++;
         }
      cminy = cminu = cminv = 255;
      cmaxy = cmaxu = cmaxv = cpix = 0;
   }
   public boolean [][] getTrackMask() {
      return currMask;
   }
   
   /**
    * Adds a point to what is being tracked.  Returns true if it is the first
    * point added or if as a second point, the filled in region overlaps with
    * the region already tracked.
    */
   public boolean addPoint(int x, int y) {
      return expand(x, y);
   }
   
   /** return true if the pixel is in the range of the track params */
   public boolean pixelInRange(int x, int y) {
      return yuv[y][x][0] >= cminv && yuv[y][x][0] <= cmaxv && yuv[y][x][1] >= cminy &&
      yuv[y][x][1] <= cmaxy && yuv[y][x][2] >= cminu && yuv[y][x][2] <= cmaxu;
   }
   
   private boolean expand(int xp, int yp) {
      int xs, xe, ys, ye, xi;
      for(int y=0; y<height; y++)
         for(int x=0; x<width; x++)
            workMask[y][x] = false;
      wminy = wminu = wminv = 255;
      wmaxy = wmaxu = wmaxv = wpix = 0;
      //find the ranges of what I will track;
      int miny = yuv[yp][xp][1] - yr;
      int maxy = yuv[yp][xp][1] + yr;
      int minu = yuv[yp][xp][2] - ur;
      int maxu = yuv[yp][xp][2] + ur;
      int minv = yuv[yp][xp][0] - vr;
      int maxv = yuv[yp][xp][0] + vr;
      
      LinkedList list = new LinkedList();
      list.add(new Integer(yp*width+xp));
      while(list.size() > 0) {
         yp = ((Integer)list.removeFirst()).intValue();
         xp = yp % width;
         yp /= width;
         ys = Math.max(yp-1, 0);
         ye = Math.min(yp+1, height-1);
         xs = Math.max(xp-1, 0);
         xe = Math.min(xp+1, width-1);
         for(;ys <= ye; ys++)
            for(xi=xs; xi <= xe; xi++)
               if(!workMask[ys][xi] && yuv[ys][xi][1] >= miny && yuv[ys][xi][1] <= maxy && yuv[ys][xi][2] >= minu &&
               yuv[ys][xi][2] <= maxu && yuv[ys][xi][0] >= minv && yuv[ys][xi][0] <= maxv) {
                  workMask[ys][xi] = true;
                  wpix++;
                  list.add(new Integer(ys*width+xi));
                  wminy = Math.min(wminy, yuv[ys][xi][1]);
                  wminu = Math.min(wminu, yuv[ys][xi][2]);
                  wminv = Math.min(wminv, yuv[ys][xi][0]);
                  wmaxy = Math.max(wmaxy, yuv[ys][xi][1]);
                  wmaxu = Math.max(wmaxu, yuv[ys][xi][2]);
                  wmaxv = Math.max(wmaxv, yuv[ys][xi][0]);
               }
      }
      //System.out.println("found "+wpix+" pixels");
      if(cpix == 0 || intersect()) {
         //relax the range
         cminy = Math.min(cminy, wminy);
         cmaxy = Math.max(cmaxy, wmaxy);
         cminu = Math.min(cminu, wminu);
         cmaxu = Math.max(cmaxu, wmaxu);
         cminv = Math.min(cminv, wminv);
         cmaxv = Math.max(cmaxv, wmaxv);
         
         //add all of the points found so far
         for(int y=0; y<height; y++)
            for(int x=0; x<width; x++)
               if(currMask[y][x] || workMask[y][x])
                  list.add(new Integer(y*width+x));
         
         //find all pixels that are tracked now
         while(list.size() > 0) {
            yp = ((Integer)list.removeFirst()).intValue();
            xp = yp % width;
            yp /= width;
            ys = Math.max(yp-1, 0);
            ye = Math.min(yp+1, height-1);
            xs = Math.max(xp-1, 0);
            xe = Math.min(xp+1, width-1);
            for(;ys <= ye; ys++)
               for(xi=xs; xi <= xe; xi++)
                  if(!currMask[ys][xi] && yuv[ys][xi][1] >= cminy && yuv[ys][xi][1] <= cmaxy && yuv[ys][xi][2] >= cminu &&
                  yuv[ys][xi][2] <= cmaxu && yuv[ys][xi][0] >= cminv && yuv[ys][xi][0] <= cmaxv) {
                     currMask[ys][xi] = true;
                     cpix++;
                     list.add(new Integer(ys*width+xi));
                  }
         }
         
         return true;
      }else {
         for(int y=0; y<height; y++)
            for(int x=0; x<width; x++)
               currMask[y][x] = workMask[y][x];
         cpix = wpix;
         cminy = wminy;
         cmaxy = wmaxy;
         cminu = wminu;
         cmaxu = wmaxu;
         cminv = wminv;
         cmaxv = wmaxv;
         return false;
      }
   }
   
   private boolean intersect() {
      for(int y=0; y<height; y++)
         for(int x=0; x<width; x++)
            if(currMask[y][x] && workMask[y][x])
               return true;
      return false;
   }
   
}
