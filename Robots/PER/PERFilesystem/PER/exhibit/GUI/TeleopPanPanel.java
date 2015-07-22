package PER.exhibit.GUI;

import java.awt.Color;
import java.awt.Graphics;

/*
 * TeleopPanPanel.java
 *
 * Created on July 1, 2002, 1:45 PM
 */

/**
 * Displays a shadow of the Rover's head, top view. Calls to setAngle change
 * the angle of the head.
 *
 * This panel really needs to be displayed at 150x150 pixels. It might resize
 * properly, but it probably won't.
 *
 * Used in Panoramic.java.
 *
 * @see Panoramic
 *
 * @author  Rachel Gockley
 * @version 1.0
 */
public class TeleopPanPanel extends javax.swing.JPanel {
    int angle;
    int size;
    int headw; // width of head
    int headl; // length of head
    int lensw;
    int lensl;
    
    /** Creates new TeleopPanPanel */
    public TeleopPanPanel() {
        angle = 0;
        headw = 20;
        headl = 30;
        lensw = 10;
        lensl = 5;
        size = 150;  // doesn't currently scale to size.
        
        setBackground(Color.white);
    }
    
    public void setAngle(int a) {
/*        if (a < CalibrationConstants.PANMIN_ANGLE)
            a = CalibrationConstants.PANMIN_ANGLE;
        else if (a > CalibrationConstants.PANMAX_ANGLE)
            a = CalibrationConstants.PANMAX_ANGLE;
*/        
        angle = a;
        repaint();
    }
    
    public void paintComponent(Graphics g) {
        double a = Math.toRadians(angle + 90);
        double cosAngle = Math.cos(a);
        double sinAngle = Math.sin(a);
        
        int x1 = (int)(Math.sqrt(headl*headl/9 + headw*headw/4) * Math.cos(a+3*Math.PI/4)) + size/2;
        int y1 = (int)(-Math.sqrt(headl*headl/9 + headw*headw/4) * Math.sin(a+3*Math.PI/4)) + size/2;
        int xvals[] = {x1,
                       x1 + (int)(headl * cosAngle),
                       x1 + (int)(headl * cosAngle + (headw/2-lensw/2) * sinAngle),
                       x1 + (int)((headl + lensl)*cosAngle + (headw/2-lensw/2)*sinAngle),
                       x1 + (int)((headl + lensl)*cosAngle + (headw/2+lensw/2)*sinAngle),
                       x1 + (int)(headl*cosAngle + (headw/2+lensw/2)*sinAngle),
                       x1 + (int)(headl*cosAngle + headw*sinAngle),
                       x1 + (int)(headw*sinAngle)};
        int yvals[] = {y1,
                       y1 + (int)(-headl * sinAngle),
                       y1 + (int)((headw/2-lensw/2)*cosAngle - headl*sinAngle),
                       y1 + (int)((headw/2-lensw/2)*cosAngle - (headl + lensl)*sinAngle),
                       y1 + (int)((headw/2+lensw/2)*cosAngle - (headl + lensl)*sinAngle),
                       y1 + (int)((headw/2+lensw/2)*cosAngle - headl*sinAngle),
                       y1 + (int)(headw*cosAngle - headl*sinAngle),
                       y1 + (int)(headw*cosAngle)};
        
        super.paintComponent(g);
        
        // the rover head
        g.setColor(Color.black);
        g.fillPolygon(xvals, yvals, xvals.length);
        
        // decoration
        g.setColor(Color.blue);
        g.drawOval(10, 10, size-20, size-20);
        g.drawLine(size/2, 5, size/2, size-5);
        g.drawLine(5, size/2, size-5, size/2);
	g.fillRect(size/2-1, 0, 3, size/2);
        
        // line indicating angle
        g.setColor(Color.red);
        g.drawLine((int)(1.0*size*cosAngle/2) + size/2, (int)(-1.0*size*sinAngle/2) + size/2,
            (int)(-1.0*size*cosAngle/2) + size/2, (int)(1.0*size*sinAngle/2) + size/2);
    }
}
