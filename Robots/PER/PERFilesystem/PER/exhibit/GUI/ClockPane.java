/*
 * ClockPane.java
 *
 * Created on November 20, 2003, 8:38 PM
 */

package PER.exhibit.GUI;

/**
 * A pane for displaying a stylized clock used on multiple screens of the kiosk
 * interface.
 *
 * @author  Emily Hamner
 */
public class ClockPane extends javax.swing.JLayeredPane {
    private javax.swing.JLabel bgLabel;
    private javax.swing.JLabel shadowLabel;
    private javax.swing.JLabel timeLabel;
    
    /** Creates a new instance of ClockPane */
    public ClockPane(boolean showBG) {
        initComponents();
        bgLabel.setVisible(showBG); 
        setText("0:00");
    }
    
    private void initComponents() {
        bgLabel = new javax.swing.JLabel();
        shadowLabel = new javax.swing.JLabel();
        timeLabel = new javax.swing.JLabel();
        /*{
            public void setText(String s){
                super.setText(s);
                shadowLabel.setText(s);
            }
        };*/
        
        bgLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/TimeCounterBackground.png")));
        bgLabel.setBounds(0, 0, 114, 47);
        add(bgLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
        
        shadowLabel.setFont(new java.awt.Font("Verdana", 1, 28));
        shadowLabel.setForeground(new java.awt.Color(0, 0, 0, 51));
        shadowLabel.setText("55:55");
        shadowLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        shadowLabel.setBounds(12, 4, 91, 35);
        add(shadowLabel, javax.swing.JLayeredPane.PALETTE_LAYER);
        
        timeLabel.setFont(new java.awt.Font("Verdana", 1, 28));
        timeLabel.setForeground(new java.awt.Color(255, 255, 204));
        timeLabel.setText("55:55");
        timeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        timeLabel.setBounds(14, 2, 91, 35);
        add(timeLabel, javax.swing.JLayeredPane.MODAL_LAYER);
        
        setPreferredSize(new java.awt.Dimension(bgLabel.getPreferredSize().width,bgLabel.getPreferredSize().height));
        this.setBounds(909,25,getPreferredSize().width,getPreferredSize().height);
        
    }
    
    public void setClockTime(String s){
        timeLabel.setText(s);
        shadowLabel.setText(s);
    }

    public void setText(String s){
        setClockTime(s);
    }
    
    public String getClockTime(){
        return getText();
    }
    
    public String getText(){
        return timeLabel.getText();
    }
}


