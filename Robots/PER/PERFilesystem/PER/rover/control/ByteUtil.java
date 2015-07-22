/*
 * ByteUtil.java
 *
 * Created on June 30, 2003, 11:15 AM
 */

package PER.rover.control;

/** Byte conversions for network
 *
 * @author  Eric Porter
 */

public class ByteUtil extends java.lang.Object {
   // Converts a signed byte to an unsigned value as an integer
   public static int unsign(byte b) {
      return (b + 256) % 256;
   }
   
   public static int networkLongToInt(byte [] buf, int offset) {
      return (unsign(buf[offset]) << 24) |  (unsign(buf[offset+1]) << 16) |
      (unsign(buf[offset+2]) << 8) |  unsign(buf[offset+3]);
   }
   
   public static int networkShortToUnsignedInt(byte [] buf, int offset) {
      return (unsign(buf[offset]) << 8) |  unsign(buf[offset+1]);
   }
   
   public static int networkShortToInt(byte [] buf, int offset) {
      int built = (unsign(buf[offset]) << 8) |  unsign(buf[offset+1]);
      if(built <= 32767)
         return built;
      return built - 65536;
   }
   
   //convert the int into a 32 byte int in network byte order
   public static void intToNetworkLong(byte [] buf, int num, int place) {
      buf[place+3] = (byte) (num & 255);
      buf[place+2] = (byte) ((num >> 8) & 255);
      buf[place+1] = (byte) ((num >> 16) & 255);
      buf[place]   = (byte) ((num >> 24) & 255);
   }
   
   public static void intToNetworkShort(byte [] buf, int num, int place) {
      buf[place+1] = (byte) (num & 255);
      buf[place]   = (byte) ((num >> 8) & 255);
   }
}
