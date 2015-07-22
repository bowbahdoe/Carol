/*
 * MissionCentral.java
 *
 * Created on June 24, 2003, 2:31 PM
 */

package PER.exhibit.GUI;

import PER.exhibit.Exhibit;
import PER.rover.Rover;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;

/**
 * Allows exhibit users to view the panorama received from the rover,
 * select a target rock, and specify the angle and distance to that target. 
 *
 * @author  Emily Hamner
 */
public class MissionCentral extends javax.swing.JPanel implements Screen {
    
    /** Creates new form MissionCentral */
    public MissionCentral() {
        initComponents();
        
        //set sizes and bounds
        mapWidth = 300;
        mapHeight = 300;
        panoramaWidth = 935;
        mapLayeredPane.setPreferredSize(new java.awt.Dimension(mapWidth,mapHeight));
        panorama.setSize(panoramaWidth, 0);
        bgLabel.setBounds(0,0,1024,768);
        layeredPane.setPreferredSize(new java.awt.Dimension(1024,768));
        clearButton.setBounds(0,0,clearButton.getPreferredSize().width,clearButton.getPreferredSize().height);
        clearButton.setLocation(224,514);
        dirPanel.setBounds(0,0,dirPanel.getPreferredSize().width,dirPanel.getPreferredSize().height);
        dirPanel.setLocation(850,500); //panorama
        distPanel.setBounds(0,0,distPanel.getPreferredSize().width,distPanel.getPreferredSize().height);
        distPanel.setLocation(850, 548); //satellite map
        goButton.setBounds(0,0, goButton.getPreferredSize().width, goButton.getPreferredSize().height);
        goButton.setLocation(780,672);
        
        //initialize mapLabel
        mapLabel = new MLabel(mapWidth,mapHeight, roverLabel, targetLabel);
        mapLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mapLabel.setBounds(0, 0, 0, 0);
        mapLabel.setToolTipText("distance"); //turn on tool tip
        mapLayeredPane.add(mapLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
        
        //initialize clock
        clock = new PER.exhibit.GUI.ClockPane(false);
        layeredPane.add(clock, javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        
        
        //adjust tool tip behavior
        javax.swing.ToolTipManager tipManager = javax.swing.ToolTipManager.sharedInstance();
        tipManager.setInitialDelay(0);
        tipManager.setDismissDelay(1000000);
        
        //set component layers
        panLayeredPane.setLayer(panBorderLabel,javax.swing.JLayeredPane.DEFAULT_LAYER.intValue());
        panLayeredPane.setLayer(panorama,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        panLayeredPane.setLayer(degreeLabel,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        panLayeredPane.setLayer(siteLabel,javax.swing.JLayeredPane.MODAL_LAYER.intValue());
        
        mapLayeredPane.setLayer(mapLabel,javax.swing.JLayeredPane.DEFAULT_LAYER.intValue());
        mapLayeredPane.setLayer(roverLabel,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        mapLayeredPane.setLayer(targetLabel,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        
        sunLayeredPane.setLayer(satBorderLabel,javax.swing.JLayeredPane.DEFAULT_LAYER.intValue());
        sunLayeredPane.setLayer(mapLayeredPane,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        sunLayeredPane.setLayer(sun,javax.swing.JLayeredPane.MODAL_LAYER.intValue());
        sunLayeredPane.setLayer(sunLabel,javax.swing.JLayeredPane.MODAL_LAYER.intValue());
        
        layeredPane.setLayer(bgLabel,javax.swing.JLayeredPane.DEFAULT_LAYER.intValue());
        layeredPane.setLayer(panLayeredPane,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        layeredPane.setLayer(sunLayeredPane,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        layeredPane.setLayer(directionBoxLabel,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        layeredPane.setLayer(distanceBoxLabel,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        layeredPane.setLayer(goBoxLabel,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        layeredPane.setLayer(dirPanel,javax.swing.JLayeredPane.MODAL_LAYER.intValue());
        layeredPane.setLayer(distPanel,javax.swing.JLayeredPane.MODAL_LAYER.intValue());
        layeredPane.setLayer(goButton,javax.swing.JLayeredPane.MODAL_LAYER.intValue());
        layeredPane.setLayer(instructLabel,javax.swing.JLayeredPane.MODAL_LAYER.intValue());
        layeredPane.setLayer(clearButton,javax.swing.JLayeredPane.POPUP_LAYER.intValue());
        
        //initialize other images that need to be read from files
        initImages();
        
        // start with centered view
        degrees = 0;
        siteX = panoramaWidth/2;
        drawSite(siteX); //?
        
        //initialize distance and direction labels
        setDistance();
        setDirection();
        
        //initialize and set custom cursors
        initCursors();
        
        //** mouse listeners **//
        
        // add mouselisteners to the panorama
        javax.swing.event.MouseInputAdapter panoramaListener = new javax.swing.event.MouseInputAdapter(){
            //snap to click
            public void mouseClicked(MouseEvent evt){
                mouseX = evt.getX();
                siteX = mouseX;
                //repaint site
                drawSite(siteX);
                //set all the direction lables to the correct reading
                setDirection();
                //dim panorama instructions and highlight sat instructions
                setInstructions(2);
                //make the satellite map visible and highlight instructions if map was previously invisible
                showMap(true);
                redispatch(evt);
            }
            //rollover
            public void mouseEntered(MouseEvent evt){
                //highlight border
                panBorderLabel.setVisible(true);
                //highlight directions box
                highlightLabel(directionBoxLabel);
                redispatch(evt);
            }
            public void mouseExited(MouseEvent evt){
                //dim border
                panBorderLabel.setVisible(false);
                //dim directions box
                dimLabel(directionBoxLabel);
                redispatch(evt);
            }
            //these events are redispatched for completeness and consistent timeout behavior
            public void mouseMoved(MouseEvent evt){
                redispatch(evt);
            }
            public void mouseDragged(MouseEvent evt){
                redispatch(evt);
            }
        };
        panorama.addMouseListener(panoramaListener);
        panorama.addMouseMotionListener(panoramaListener);
        
        // add mouselisteners to the mapLabel
        javax.swing.event.MouseInputAdapter mapListener = new javax.swing.event.MouseInputAdapter(){
            public void mousePressed(MouseEvent evt) {
                mapLabelMouseMovement(evt);
                redispatch(evt);
            }
            public void mouseDragged(MouseEvent evt){
                mapLabelMouseMovement(evt);
                redispatch(evt);
            }
            public void mouseMoved(MouseEvent evt){
                mapLabelMouseMovement(evt);
                redispatch(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                mapLabelMouseReleased(evt);
                redispatch(evt);
            }
            //rollover
            public void mouseEntered(MouseEvent evt){
                if(mapPoly.contains(evt.getPoint())){
                    //highlight border
                    satBorderLabel.setVisible(true);
                    //highlight distance box
                    highlightLabel(distanceBoxLabel);
                }
                redispatch(evt);
            }
            public void mouseExited(MouseEvent evt){
                //dim border
                satBorderLabel.setVisible(false);
                //dim distance box
                dimLabel(distanceBoxLabel);
                //if both points are placed on the map, turn off the tool tip
                if(targetLabel.isVisible())
                    mapLabel.setToolTipText(null);
                redispatch(evt);
            }
        };
        mapLabel.addMouseListener(mapListener);
        mapLabel.addMouseMotionListener(mapListener);
        
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        popupMenu = new javax.swing.JPopupMenu();
        navPlusMenuItem = new javax.swing.JMenuItem();
        navOnlyMenuItem = new javax.swing.JMenuItem();
        turnMenuItem = new javax.swing.JMenuItem();
        newPanMenuItem = new javax.swing.JMenuItem();
        attractLoopMenuItem = new javax.swing.JMenuItem();
        layeredPane = new javax.swing.JLayeredPane();
        bgLabel = new javax.swing.JLabel(){
            /*public void paint(java.awt.Graphics g){
                if(!angleScrollTimer.isRunning()){
                    super.paint(g);
                }else{
                    java.awt.Graphics gg = g.create();
                    gg.setClip(panLayeredPane.getBounds());
                    super.paint(gg);
                    gg.setClip(dirPanel.getBounds());
                    super.paint(gg);
                }
            }
            */
        };

        panLayeredPane = new javax.swing.JLayeredPane();
        panBorderLabel = new javax.swing.JLabel();
        panorama = new javax.swing.JLabel();
        siteLabel = new javax.swing.JLabel();
        degreeLabel = new javax.swing.JLabel();
        instructLabel = new javax.swing.JLabel();
        clearButton = new javax.swing.JButton();
        sunLayeredPane = new javax.swing.JLayeredPane();
        satBorderLabel = new javax.swing.JLabel();
        sunLabel = new javax.swing.JLabel();
        sun = new javax.swing.JLabel();
        mapLayeredPane = new javax.swing.JLayeredPane();
        roverLabel = new javax.swing.JLabel();
        targetLabel = new javax.swing.JLabel();
        directionBoxLabel = new javax.swing.JLabel();
        dirPanel = new javax.swing.JPanel();
        directionLabel = new javax.swing.JLabel();
        rightLeftLabel = new javax.swing.JLabel();
        distanceBoxLabel = new javax.swing.JLabel();
        distPanel = new javax.swing.JPanel();
        distanceLabel = new javax.swing.JLabel();
        cmLabel = new javax.swing.JLabel();
        goBoxLabel = new javax.swing.JLabel();
        goButton = new javax.swing.JButton();

        popupMenu.setFont(new java.awt.Font("Verdana", 0, 12));
        popupMenu.setForeground(new java.awt.Color(0, 0, 102));
        navPlusMenuItem.setFont(new java.awt.Font("Verdana", 0, 12));
        navPlusMenuItem.setForeground(new java.awt.Color(0, 0, 102));
        navPlusMenuItem.setText("Navigate and find rock");
        popupMenu.add(navPlusMenuItem);

        navOnlyMenuItem.setFont(new java.awt.Font("Verdana", 0, 12));
        navOnlyMenuItem.setForeground(new java.awt.Color(0, 0, 102));
        navOnlyMenuItem.setText("Navigate only");
        popupMenu.add(navOnlyMenuItem);

        turnMenuItem.setFont(new java.awt.Font("Verdana", 0, 12));
        turnMenuItem.setForeground(new java.awt.Color(0, 0, 102));
        turnMenuItem.setText("Turn only");
        popupMenu.add(turnMenuItem);

        newPanMenuItem.setFont(new java.awt.Font("Verdana", 0, 12));
        newPanMenuItem.setForeground(new java.awt.Color(0, 0, 102));
        newPanMenuItem.setText("Take new panorama");
        popupMenu.add(newPanMenuItem);

        attractLoopMenuItem.setFont(new java.awt.Font("Verdana", 0, 12));
        attractLoopMenuItem.setForeground(new java.awt.Color(0, 0, 102));
        attractLoopMenuItem.setText("Return to Attract Loop");
        popupMenu.add(attractLoopMenuItem);

        setLayout(new java.awt.GridBagLayout());

        setBackground(new java.awt.Color(0, 102, 102));
        layeredPane.setBackground(new java.awt.Color(255, 255, 102));
        layeredPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                layeredPaneMouseClicked(evt);
            }
        });

        bgLabel.setBounds(0, 0, -1, -1);
        layeredPane.add(bgLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        panLayeredPane.setBackground(new java.awt.Color(204, 0, 51));
        panBorderLabel.setBounds(0, 0, -1, -1);
        panLayeredPane.add(panBorderLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        panorama.setBounds(0, 0, -1, -1);
        panLayeredPane.add(panorama, javax.swing.JLayeredPane.DEFAULT_LAYER);

        siteLabel.setBounds(0, 0, -1, -1);
        panLayeredPane.add(siteLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        degreeLabel.setForeground(new java.awt.Color(255, 255, 255));
        degreeLabel.setText("0");
        degreeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        degreeLabel.setBounds(0, 0, -1, -1);
        panLayeredPane.add(degreeLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        panLayeredPane.setBounds(0, 0, -1, -1);
        layeredPane.add(panLayeredPane, javax.swing.JLayeredPane.DEFAULT_LAYER);

        instructLabel.setBounds(0, 0, -1, -1);
        layeredPane.add(instructLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        clearButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/clear.png")));
        clearButton.setBorder(null);
        clearButton.setBorderPainted(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/clear-pressed.png")));
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        clearButton.setBounds(0, 0, -1, -1);
        layeredPane.add(clearButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

        satBorderLabel.setBounds(0, 0, -1, -1);
        sunLayeredPane.add(satBorderLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        sunLabel.setFont(new java.awt.Font("Verdana", 0, 10));
        sunLabel.setForeground(java.awt.Color.white);
        sunLabel.setText("Sun");
        sunLabel.setIconTextGap(2);
        sunLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        sunLabel.setBounds(0, 0, -1, -1);
        sunLayeredPane.add(sunLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        sun.setBounds(0, 0, -1, -1);
        sunLayeredPane.add(sun, javax.swing.JLayeredPane.DEFAULT_LAYER);

        roverLabel.setBounds(0, 0, -1, -1);
        mapLayeredPane.add(roverLabel, javax.swing.JLayeredPane.PALETTE_LAYER);

        targetLabel.setBounds(0, 0, -1, -1);
        mapLayeredPane.add(targetLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        mapLayeredPane.setBounds(0, 0, -1, -1);
        sunLayeredPane.add(mapLayeredPane, javax.swing.JLayeredPane.DEFAULT_LAYER);

        sunLayeredPane.setBounds(0, 0, -1, -1);
        layeredPane.add(sunLayeredPane, javax.swing.JLayeredPane.DEFAULT_LAYER);

        directionBoxLabel.setBounds(0, 0, -1, -1);
        layeredPane.add(directionBoxLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        dirPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dirPanel.setOpaque(false);
        directionLabel.setFont(new java.awt.Font("Verdana", 1, 18));
        directionLabel.setForeground(new java.awt.Color(255, 255, 255));
        directionLabel.setText("555 ");
        dirPanel.add(directionLabel);

        rightLeftLabel.setFont(new java.awt.Font("Verdana", 1, 18));
        rightLeftLabel.setForeground(new java.awt.Color(255, 255, 255));
        rightLeftLabel.setText("right");
        dirPanel.add(rightLeftLabel);

        dirPanel.setBounds(0, 0, -1, -1);
        layeredPane.add(dirPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        distanceBoxLabel.setBounds(0, 0, -1, -1);
        layeredPane.add(distanceBoxLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        distPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        distPanel.setOpaque(false);
        distanceLabel.setFont(new java.awt.Font("Verdana", 1, 18));
        distanceLabel.setForeground(new java.awt.Color(255, 255, 255));
        distanceLabel.setText("555-555");
        distanceLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        distanceLabel.setAlignmentY(1.0F);
        distPanel.add(distanceLabel);

        cmLabel.setFont(new java.awt.Font("Verdana", 1, 18));
        cmLabel.setForeground(new java.awt.Color(255, 255, 255));
        cmLabel.setText("cm");
        cmLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        cmLabel.setAlignmentY(1.0F);
        distPanel.add(cmLabel);

        distPanel.setBounds(0, 0, -1, -1);
        layeredPane.add(distPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        goBoxLabel.setBounds(0, 0, -1, -1);
        layeredPane.add(goBoxLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        goButton.setBackground(new java.awt.Color(42, 241, 22));
        goButton.setFont(new java.awt.Font("Dialog", 1, 18));
        goButton.setForeground(new java.awt.Color(255, 255, 255));
        goButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/gobutton-up.gif")));
        goButton.setBorder(null);
        goButton.setBorderPainted(false);
        goButton.setContentAreaFilled(false);
        goButton.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/gobutton-down.gif")));
        goButton.setBounds(0, 0, -1, -1);
        layeredPane.add(goButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

        add(layeredPane, new java.awt.GridBagConstraints());

    }//GEN-END:initComponents
    
    /** Dispatches a mouse_moved event to the layered pane. We only care
     * when the event happened so we don't bother to convert coordinates.
     *
     * A mouseMotionListener is added to layeredPane in FlowController. This function
     * allows mouse events from the mapLabel and panorama to be registered by the
     * listener in FlowController.
     */
    private void redispatch(java.awt.event.MouseEvent evt){
        layeredPane.dispatchEvent(new MouseEvent(layeredPane,
        MouseEvent.MOUSE_MOVED,
        evt.getWhen(),
        evt.getModifiers(),
        evt.getX(),
        evt.getY(),
        evt.getClickCount(),
        evt.isPopupTrigger()));
    }
    
    private void layeredPaneMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_layeredPaneMouseClicked
        //right click
        if(evt.getButton() == MouseEvent.BUTTON3){
            showPopupMenu(layeredPane, evt.getX(), evt.getY());
        }
        redispatch(evt);
        //changeme - change this to use ispopuptrigger so that code will be more portable
    }//GEN-LAST:event_layeredPaneMouseClicked
    
    /** Initializes all the custom cursors and sets default cursors for
     * clickable objects.
     */
    private void initCursors(){
        java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
        java.awt.Dimension dim;
        
        //crosshairs with endpoint
        try{
            Image cursor = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/cursor_with_endpoint.gif"));
            dim = tk.getBestCursorSize(cursor.getWidth(this),cursor.getHeight(this));
            fullCrosshairs = tk.createCustomCursor(cursor,
            new java.awt.Point((int)(dim.getWidth()/2),(int)(dim.getHeight()/2)),
            "crosshairsWithPoint");
        }catch(Exception e){
            PER.rover.Log.println("Error creating custom cursor (crosshairs with endpoint): "+e,true);
            //default to crosshair cursor
            fullCrosshairs = new Cursor(Cursor.CROSSHAIR_CURSOR);
        }
        
        //empty crosshairs
        try{
            Image cursor = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/crosshairs-empty.gif"));
            dim = tk.getBestCursorSize(cursor.getWidth(this),cursor.getHeight(this));
            emptyCrosshairs = tk.createCustomCursor(cursor,
            new java.awt.Point((int)(dim.getWidth()/2),(int)(dim.getHeight()/2)),
            "emptyCrosshairs");
        }catch(Exception e){
            PER.rover.Log.println("Error creating custom cursor (empty crosshairs): "+e,true);
            //default to crosshair cursor
            emptyCrosshairs = new Cursor(Cursor.CROSSHAIR_CURSOR);
        }
        
        //grabber hand
        hand = new Cursor(Cursor.HAND_CURSOR);
        
        //set cursors
        panorama.setCursor(emptyCrosshairs);
        siteLabel.setCursor(emptyCrosshairs);
        goButton.setCursor(hand);
        clearButton.setCursor(hand);
        mapLabel.setCursor(fullCrosshairs);
    }
    
    /** Initializes MissionCentral images that need to be read from a file. //Should
     * be called after panorama image is initialized.
     */
    private void initImages(){
        //background image
        try{
            bgImg = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/MissionCentral-Background.gif"));
            bgLabel.setIcon(new ImageIcon(bgImg));
        }catch(IOException e){
            PER.rover.Log.println("Error creating background image: "+e,true);
            //ugly but won't crash program
        }
        
        //rover image for satellite map - (now the same gif as targetImg)
        try{
            roverImg = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/endpoint.gif"));
            roverLabel.setIcon(new ImageIcon(roverImg));
            //start with label invisible
            roverLabel.setVisible(false);
        }catch(IOException e){
            PER.rover.Log.println("Error creating rover image: "+e,true);
            //error - use a blank image
            roverImg = new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB);
        }
        
        //target image for satellite map - (now the same gif as roverImg)
        try{
            targetImg = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/endpoint.gif"));
            targetLabel.setIcon(new ImageIcon(targetImg));
            //start with label invisible
            targetLabel.setVisible(false);
        }catch(IOException e){
            PER.rover.Log.println("Error creating target image: "+e,true);
            //error - use a blank image
            targetImg = new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB);
        }
        
        //site image
        try{
            siteImg = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/site.gif"));
        }catch(IOException e){
            PER.rover.Log.println("Error creating site image: "+e,true);
            //error - use a blank image
            siteImg = new BufferedImage(3,128,BufferedImage.TYPE_INT_RGB);
        }
        
        //sun image
        try{
            sunImg = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/sun.gif"));
            sun.setIcon(new ImageIcon(sunImg));
        }catch(IOException e){
            PER.rover.Log.println("Error creating sun image: "+e,true);
            //error - use a blank image
            sunImg = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
            sun.setIcon(new ImageIcon(sunImg));
        }
        
        //sun text image
        try{
            sunLabel.setText("");
            Image sunTextImg = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/SunText.png"));
            sunLabel.setIcon(new ImageIcon(sunTextImg));
        }catch(IOException e){
            PER.rover.Log.println("Error creating sun text image: "+e,true);
            //error - use java text instead
            sunLabel.setText("Sun");
        }
        
        //left pan arrow image and icon
        try{
            panArrowLeftImg = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/arrow-left.gif"));
            panArrowLeftIcon = new ImageIcon(panArrowLeftImg);
        }catch(IOException e){
            PER.rover.Log.println("Error creating left pan arrow image: "+e,true);
            //error - use a blank image
            //ugly but ensures that numbers are always readable
            panArrowLeftImg = new BufferedImage(46,34,BufferedImage.TYPE_INT_RGB);
            panArrowLeftIcon = new ImageIcon(panArrowLeftImg);
        }
        
        //right pan arrow image and icon
        try{
            panArrowRightImg = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/arrow-right.gif"));
            panArrowRightIcon = new ImageIcon(panArrowRightImg);
        }catch(IOException e){
            PER.rover.Log.println("Error creating right pan arrow image: "+e,true);
            //error - use a blank image
            //ugly but ensures that numbers are always readable
            panArrowRightImg = new BufferedImage(46,34,BufferedImage.TYPE_INT_RGB);
            panArrowRightIcon = new ImageIcon(panArrowRightImg);
        }
        
        //panorama highlight border
        panBorderLabel.setIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/pan-rollover.gif")));
        //instruction icons
        instruct1Icon = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/instructions1.gif"));
        instruct2Icon = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/instructions2.gif"));
        instruct3Icon = new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/instructions3.gif"));
        instructLabel.setIcon(instruct1Icon);
        //mission builder icons
        distanceBoxLabel.setIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/distancebox.gif")));
        distanceBoxLabel.setDisabledIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/distancebox-highlight.gif")));
        directionBoxLabel.setIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/directionbox.gif")));
        directionBoxLabel.setDisabledIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/directionbox-highlight.gif")));
        goBoxLabel.setIcon(new ImageIcon(getClass().getResource("/PER/exhibit/GUI/images/goBox.gif")));
        
        //set bounds
        instructLabel.setIcon(instruct1Icon);
        instructLabel.setBounds(41,255,instructLabel.getPreferredSize().width,instructLabel.getPreferredSize().height);
        directionBoxLabel.setBounds(699,493,directionBoxLabel.getPreferredSize().width,directionBoxLabel.getPreferredSize().height);
        distanceBoxLabel.setBounds(699,541,distanceBoxLabel.getPreferredSize().width,distanceBoxLabel.getPreferredSize().height);
        goBoxLabel.setBounds(0,0,goBoxLabel.getPreferredSize().width,goBoxLabel.getPreferredSize().height+2); //initial icon image is smaller than others - but the image should be changed to match
        goBoxLabel.setLocation(699,589);
        
    }
    
    /** Positions the sun with its center at (sunX, sunY) and adjusts the sunLayeredPane
     * accordingly.
     */
    private void orientSun(){
        int sunW = sunImg.getWidth(this); //width of the sun
        int sunH = sunImg.getHeight(this); //height of the sun
        int hBuffer; //horizontal buffer between edge of mapLayeredPane and sunLayeredPane
        int topBuffer; //buffer between top edge of mapLayeredPane and sunLayeredPane
        int bottomBuffer; //buffer between bottom edge of mapLayeredPane and sunLayeredPane
        int centerX;
        int centerY;
        int space = 2; //extra space between sun image and sun label
        
        //orient sun label
        if(sunX == 0 && sunY == 0){
            //top left corner
            topBuffer = sunH/2 + sunLabel.getPreferredSize().height + space;
            bottomBuffer = 0;
            hBuffer = java.lang.Math.max(sunW/2, sunLabel.getPreferredSize().width/2);
            //shift sunX and sunY to be w.r.t. sunLayeredPane
            centerX = sunX + hBuffer; //center of the sun wrt sunLayeredPane
            centerY = sunY + topBuffer; //center of the sun wrt sunLayeredPane
            sunLabel.setBounds(centerX-(sunLabel.getPreferredSize().width/2),centerY-sunH/2-space-sunLabel.getPreferredSize().height,sunLabel.getPreferredSize().width,sunLabel.getPreferredSize().height);
        }else if(sunX == mapWidth && sunY == 0){
            //top right corner
            topBuffer = java.lang.Math.max(sunH/2, sunLabel.getPreferredSize().height/2);
            bottomBuffer = 0;
            hBuffer = sunW/2 + sunLabel.getPreferredSize().width + space;
            //shift sunX and sunY to be w.r.t. sunLayeredPane
            centerX = sunX + hBuffer; //center of the sun wrt sunLayeredPane
            centerY = sunY + topBuffer; //center of the sun wrt sunLayeredPane
            //right
            sunLabel.setBounds(centerX+sunW/2+space,centerY-(sunLabel.getPreferredSize().height/2),sunLabel.getPreferredSize().width,sunLabel.getPreferredSize().height);
        }else if(sunX == mapWidth && sunY == mapHeight){
            //bottom right corner
            topBuffer = 0;
            bottomBuffer = java.lang.Math.max(sunH/2, sunLabel.getPreferredSize().height/2);
            hBuffer = sunW/2 + sunLabel.getPreferredSize().width + space;
            //shift sunX and sunY to be w.r.t. sunLayeredPane
            centerX = sunX + hBuffer; //center of the sun wrt sunLayeredPane
            centerY = sunY + topBuffer; //center of the sun wrt sunLayeredPane
            //right
            sunLabel.setBounds(centerX+sunW/2+space,centerY-(sunLabel.getPreferredSize().height/2),sunLabel.getPreferredSize().width,sunLabel.getPreferredSize().height);
        }else if(sunX == 0 && sunY == mapHeight){
            //bottom left corner
            topBuffer = 0;
            bottomBuffer = java.lang.Math.max(sunH/2, sunLabel.getPreferredSize().height/2);
            hBuffer = sunW/2 + sunLabel.getPreferredSize().width + space;
            //shift sunX and sunY to be w.r.t. sunLayeredPane
            centerX = sunX + hBuffer; //center of the sun wrt sunLayeredPane
            centerY = sunY + topBuffer; //center of the sun wrt sunLayeredPane
            //left
            sunLabel.setBounds(centerX-sunW/2-space-sunLabel.getPreferredSize().width,centerY-(sunLabel.getPreferredSize().height/2),sunLabel.getPreferredSize().width,sunLabel.getPreferredSize().height);
        }else if((sunY == 0 || sunY == mapHeight) && (sunX != 0 && sunX != mapWidth)){
            //vertical
            hBuffer = sunW/2 + sunLabel.getPreferredSize().width/2;
            if(sunY == 0){
                //top
                topBuffer = sunH/2 + sunLabel.getPreferredSize().height + space;
                bottomBuffer = 0;
                //shift sunX and sunY to be w.r.t. sunLayeredPane
                centerX = sunX + hBuffer; //center of the sun wrt sunLayeredPane
                centerY = sunY + topBuffer; //center of the sun wrt sunLayeredPane
                sunLabel.setBounds(centerX-(sunLabel.getPreferredSize().width/2),centerY-sunH/2-space-sunLabel.getPreferredSize().height,sunLabel.getPreferredSize().width,sunLabel.getPreferredSize().height);
            }else{
                //bottom
                topBuffer = 0;
                bottomBuffer = sunH/2 + sunLabel.getPreferredSize().height + space;
                //shift sunX and sunY to be w.r.t. sunLayeredPane
                centerX = sunX + hBuffer; //center of the sun wrt sunLayeredPane
                centerY = sunY + topBuffer; //center of the sun wrt sunLayeredPane
                sunLabel.setBounds(centerX-(sunLabel.getPreferredSize().width/2),centerY+sunH/2+space,sunLabel.getPreferredSize().width,sunLabel.getPreferredSize().height);
            }
        }else{
            //sideways
            //set buffers
            topBuffer = 0;
            bottomBuffer = 0;
            hBuffer = sunW/2 + sunLabel.getPreferredSize().width + space;
            //shift sunX and sunY to be w.r.t. sunLayeredPane
            centerX = sunX + hBuffer; //center of the sun wrt sunLayeredPane
            centerY = sunY + topBuffer; //center of the sun wrt sunLayeredPane
            
            if(sunX <= mapWidth/2){
                //left
                sunLabel.setBounds(centerX-sunW/2-space-sunLabel.getPreferredSize().width,centerY-(sunLabel.getPreferredSize().height/2),sunLabel.getPreferredSize().width,sunLabel.getPreferredSize().height);
            }else{
                //right
                sunLabel.setBounds(centerX+sunW/2+space,centerY-(sunLabel.getPreferredSize().height/2),sunLabel.getPreferredSize().width,sunLabel.getPreferredSize().height);
            }
        }
        //set size of sunLayeredPane
        sunLayeredPane.setPreferredSize(new java.awt.Dimension(hBuffer*2+mapWidth,topBuffer+mapHeight+bottomBuffer));
        sunLayeredPane.setBounds(311-hBuffer,386-topBuffer, sunLayeredPane.getPreferredSize().width,sunLayeredPane.getPreferredSize().height);
        //set bounds of mapLayeredPane now that buffer is set
        mapLayeredPane.setBounds(hBuffer, topBuffer, mapWidth, mapHeight);
        satBorderLabel.setBounds(hBuffer+1, topBuffer+1, satBorderLabel.getPreferredSize().width, satBorderLabel.getPreferredSize().height);
        
        //place sun
        sun.setBounds(centerX-(sunW/2),centerY-(sunH/2),sunW,sunH);
    }
    
    /** Resets the satellite map. Resets cursor to fullCrosshairs, makes roverLabel
     * and targetLabel invisible, resets the instruction text, and clears the distance
     * from the mission builder.
     */
    private void resetMap(){
        //reset map
        mapLabel.setCursor(fullCrosshairs);
        roverLabel.setVisible(false);
        targetLabel.setVisible(false);
        //change instructions text
        if(sunLayeredPane.isVisible())
            setInstructions(2);
        //reset distance
        distanceLabel.setVisible(false);
        cmLabel.setVisible(false);
    }
    
    /** Resets the map and hides the go button.
     */
    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        resetMap();
        showGo(false);
    }//GEN-LAST:event_clearButtonActionPerformed
    
    /** Sets the distance and direction instructions. Makes sure clearButton
     * is only visible for the fourth set of instructions.
     *
     *@param i The instructions to use. Possible instructions are 1, 2, or 3.
     * The function does nothing if <code>i</code> is not 1, 2, or 3.
     */
    private void setInstructions(int i){
        switch (i){
            case 1:
                //panorama instructions highlighted, others invisible
                instructLabel.setIcon(instruct1Icon);
                clearButton.setVisible(false);
                break;
            case 2:
                if(!targetLabel.isVisible()){
                    //satellite map instructions highlighted
                    instructLabel.setIcon(instruct2Icon);
                    clearButton.setVisible(false);
                }
                break;
            case 3:
                //satellite instructions dimmed, showing clear button
                instructLabel.setIcon(instruct3Icon);
                clearButton.setVisible(true);
                break;
        }
        //have to set bounds each time because the icons are different sizes
        instructLabel.setBounds(41,255,instructLabel.getPreferredSize().width,instructLabel.getPreferredSize().height);
    }
    
    /** Scales the site image to the given height; sets the
     * site image as the icon for the siteLabel and sets the bounds for the
     * site label.
     */
    private void scaleAndBoundSite(int height){
        try{
            //scale site to be the same height as panorama
            siteImg = siteImg.getScaledInstance(
            (int)(height * siteImg.getWidth(this) / siteImg.getHeight(this)),
            height,
            Image.SCALE_SMOOTH);
        }catch(java.lang.IllegalArgumentException e){
            PER.rover.Log.println("illegalArgumentException in MissionCentral.scaleAndBoundSite(): "+e); //temp?
        }
        
        siteLabel.setIcon(new ImageIcon(siteImg));
        siteLabel.setBounds((int)panorama.getBounds().getCenterX(),3,siteLabel.getPreferredSize().width,siteLabel.getPreferredSize().height);
    }
    
    /** Sets the panorama image to the given image. */
    public void setImage(Image original) {
        int width = panoramaWidth; //panorama width
        int height = original.getHeight(this) * width / original.getWidth(this);
        Image img = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        panorama.setIcon(new ImageIcon(img));
        
        //panorama.setBounds((1024-panoramaWidth)/2, 0, width, height);
        panorama.setBounds((1024-panoramaWidth)/2, 3, width, height);
        panBorderLabel.setBounds(42,0, panBorderLabel.getPreferredSize().width,panBorderLabel.getPreferredSize().height);
        degreeLabel.setBounds(siteX-degreeLabel.getPreferredSize().width/2 + 5,
        height,
        degreeLabel.getPreferredSize().width,
        degreeLabel.getPreferredSize().height);
        panLayeredPane.setPreferredSize(new java.awt.Dimension(1024,height+degreeLabel.getPreferredSize().height));
        panLayeredPane.setBounds(0, 120, panLayeredPane.getPreferredSize().width, panLayeredPane.getPreferredSize().height);
        
        //make sure the site is scaled to match the panorama and set bounds correctly
        scaleAndBoundSite(panorama.getPreferredSize().height);
        
        //center site
        siteX = (int)panorama.getBounds().getCenterX();
        drawSite(siteX);
        
        revalidate();
    }
    
    /** Sets the map image to <code>mimg</code> and the rollover border image to
     * <code>rolloverImg</code>.
     */
    public void setMapImage(Image mimg, Image rolloverImg){
        //map image
        Image mapImg = mimg.getScaledInstance(mapWidth, mapHeight, Image.SCALE_SMOOTH);
        mapLabel.setMapImg(mapImg);
        mapLabel.setIcon(new ImageIcon(mapImg));
        //satellite map highlight border
        //rolloverImg = ImageIO.read(new File(Exhibit.exhibitPath,"GUI/images/sat-rollover.gif"));
        //border image is 1 pixel smaller than map on all sides
        rolloverImg = rolloverImg.getScaledInstance(mapWidth-2, mapHeight-2, Image.SCALE_SMOOTH);
        satBorderLabel.setIcon(new ImageIcon(rolloverImg));
        //set sizes and bounds
        mapLabel.setBounds(0, 0, mapWidth, mapHeight);
        
        mapLayeredPane.revalidate();
        sunLayeredPane.revalidate();
    }
    
    /** Places the rover icon or target icon on the map and draws the line between
     * them. Sets the cursor to the default after placing the target icon.
     */
    private void mapLabelMouseReleased(MouseEvent evt) {
        if(mapPoly.contains(evt.getPoint())){
            if(!roverLabel.isVisible()){
                //place rover icon
                roverLabel.setVisible(true);
                roverLabel.setBounds((int)(evt.getX()-roverImg.getWidth(this)/2),(int)(evt.getY()-roverImg.getHeight(this)/2),roverImg.getWidth(this),roverImg.getHeight(this));
                //set cursor to fullCrosshairs
                //mapLabel.setCursor(fullCrosshairs);
                //roverLabel.setCursor(fullCrosshairs);
                //draw line
                drawLine(evt.getX(),evt.getY());
            } else if(!targetLabel.isVisible()){
                //place target icon
                targetLabel.setBounds((int)(evt.getX()-targetImg.getWidth(this)/2),(int)(evt.getY()-targetImg.getHeight(this)/2),targetImg.getWidth(this),targetImg.getHeight(this));
                targetLabel.setVisible(true);
                //set cursor to default cursor
                mapLabel.setCursor(Cursor.getDefaultCursor());
                roverLabel.setCursor(Cursor.getDefaultCursor());
                targetLabel.setCursor(Cursor.getDefaultCursor());
                //draw line
                drawLine(evt.getX(),evt.getY());
                //change instructions text
                setInstructions(3);
                //make go button visible
                showGo(true);
            }
        }
    }
    
    /** Draws the line from roverLabel to mouse point.
     */
    private void mapLabelMouseMovement(MouseEvent evt) {
        if(mapPoly.contains(evt.getPoint())){
            if(roverLabel.isVisible() && !targetLabel.isVisible()){
                //draw line
                drawLine(evt.getX(),evt.getY());
            }
            //highlight border
            satBorderLabel.setVisible(true);
            //highlight distance box
            highlightLabel(distanceBoxLabel);
        }else{
            //dim border
            satBorderLabel.setVisible(false);
            //dim distance box
            dimLabel(distanceBoxLabel);
            //if both points are placed on the map, turn off the tool tip
            if(targetLabel.isVisible())
                mapLabel.setToolTipText(null);
        }
    }
    
    /** Disables the label there by displaying the disabled icon, which we are using
     * as the highlighted icon.
     */
    private void highlightLabel(javax.swing.JLabel label){
        label.setEnabled(false);
    }
    
    /** Enables the label there by displaying the regular icon, which we are using
     * as the dim icon.
     */
    private void dimLabel(javax.swing.JLabel label){
        label.setEnabled(true);
    }
    
    public int getDegrees() {
        return degrees;
    }
    
    public int getDist() {
        return dist;
    }
    
    /** Sets the degreeLabel, directionLabel, rightLeftLabel, and degrees variable
     * to the correct value based on the position of the site.
     */
    private void setDirection() {
        //set degrees variable
        degrees = siteX * 360 / panoramaWidth * -1;
        degrees = (degrees + 180) % 360;
        if (degrees > 180)
            degrees -= 360;
        
        //set text of degreeLabel, directionLabel, and rightLeftLabel
        if (degrees == 0){
            degreeLabel.setText("0" + '\u00b0');
            directionLabel.setText("0" + '\u00b0');
            rightLeftLabel.setText("");
            degreeLabel.setIcon(panArrowLeftIcon); //default to left
        }
        else if (degrees > 0){
            degreeLabel.setText("" + degrees + '\u00b0');
            directionLabel.setText("" + degrees + '\u00b0');
            rightLeftLabel.setText("left");
            degreeLabel.setIcon(panArrowLeftIcon);
        }
        else{
            degreeLabel.setText("" + (-1 * degrees) + '\u00b0');
            directionLabel.setText("" + (-1 * degrees) + '\u00b0');
            rightLeftLabel.setText("right");
            degreeLabel.setIcon(panArrowRightIcon);
        }
        directionLabel.setVisible(true);
        rightLeftLabel.setVisible(true);
    }
    
    /** Sets the distance label and dist variable to the correct values. */
    private void setDistance(){
        distanceLabel.setText(""+dist+"-"+(dist+10));
        //don't need this as long as original sizes of distanceLabel and cmLabel are the largest sizes (same for dist, dir, and time)
        //distPanel.setBounds(distPanel.getX(),distPanel.getY(),distPanel.getPreferredSize().width,distPanel.getPreferredSize().height);
        
        distanceLabel.setVisible(true);
        cmLabel.setVisible(true);
    }
    
    private void setDistance(int d){
        dist = d;
        setDistance();
    }
    
    /** Displays the popup menu at the position x,y in the coordinate space
     * of the component invoker <code>c</code>.
     */
    public void showPopupMenu(java.awt.Component c, int x, int y){
        popupMenu.show(c, x, y);
    }
    
    /** Paints the site with the center of the site
     * at <code>center</code>; positions the degreeLabel under the site.
     *
     *@param center The x position to be the center of the site with respect to the panorama.
     */
    private void drawSite(int center){
        //set position of site
        siteLabel.setLocation(center-siteLabel.getPreferredSize().width/2+(int)panorama.getBounds().getX(), 3);
        //set position of degreelabel
        //setLocation rather than setBounds works as long as degreeLabel icon is always wider than the degreeLabel text (otherwise a change in text could change width)
        degreeLabel.setLocation(center-degreeLabel.getPreferredSize().width/2 + (int)panorama.getBounds().getX() + 5, panorama.getPreferredSize().height);
        //don't need this as long as degreeLabel height is not changing
        // panLayeredPane.setPreferredSize(new java.awt.Dimension(1024,panorama.getPreferredSize().height+degreeLabel.getPreferredSize().height));
    }
    
    /** Draws the dotted line from the roverLabel to the point x,y.
     *
     *@param x The x location for the line endpoint with resecpt to mapLabel.
     *@param y The y location for the line endpoint with respect to mapLabel.
     */
    private void drawLine(int x, int y){
        mapLabel.paintLineTo(mapLabel.getGraphics(),x,y);
    }
    
    public void start() {
        PER.rover.StatsLog.print(PER.rover.StatsLog.START_MISSION_CENTRAL);
        
        //show only panorama...
        showPanorama(true);
        showMap(false);
        showGo(false);
        
        // ... resetPanorama();
        //draw centered site and display default direction
        //siteX = panoramaWidth/2;
        siteX = panoramaWidth/2 + 2;
        drawSite(siteX);
        setDirection();
        //clear distance and direction
        directionLabel.setVisible(false);
        rightLeftLabel.setVisible(false);
        
        //reset map
        resetMap();
        
        //dimhighlight borders
        panBorderLabel.setVisible(false);
        satBorderLabel.setVisible(false);
    }
    
    public void stop() {
        mapLabel.setToolTipText(null); //make sure tool tip is off
        PER.rover.StatsLog.print(PER.rover.StatsLog.STOP_MISSION_CENTRAL);
    }
    
    /** Sets the instructions to instructions1.
     */
    private void showPanorama(boolean b){
        setInstructions(1);
    }
    
    /** Shows the satellite map, highlights the map instructions, and displays
     * the distance portion of the mission builder. Also enables the turn only
     * right click option.
     */
    private void showMap(boolean b){
        if(b)
            setInstructions(2);
        else
            setInstructions(1);
        
        sunLayeredPane.setVisible(b);
        turnMenuItem.setEnabled(b);
        
        if(b && roverLabel.isVisible()){
            distanceLabel.setVisible(true);
            cmLabel.setVisible(true);
        }else{
            distanceLabel.setVisible(false);
            cmLabel.setVisible(false);
        }
        
    }
    
    /** Shows the go button and the final portion of the mission builder.
     * Also enables the two navigation right click options.
     */
    private void showGo(boolean b){
        goBoxLabel.setVisible(b);
        goButton.setVisible(b);
        navPlusMenuItem.setEnabled(b);
        navOnlyMenuItem.setEnabled(b);
    }
    
    
    /** Sets the mapPolygon and sun center point after scaling them to match
     * the map image.
     */
    public void setMapCoordinates(java.awt.Polygon mapPolygon,
    int sunxposition, int sunyposition, double yardWidthInCM,
    int originalImageWidth, int originalImageHeight){
        
        //scale and set map polygon
        for(int i =0; i < mapPolygon.npoints; i++){
            mapPolygon.xpoints[i] = mapPolygon.xpoints[i] * mapWidth / originalImageWidth;
            mapPolygon.ypoints[i] = mapPolygon.ypoints[i] * mapHeight / originalImageHeight;
        }
        mapPoly = mapPolygon;
        
        //scale and set sun position
        if(sunxposition == -1 || sunyposition == -1){
            //don't display sun
            sunLabel.setVisible(false);
            sun.setVisible(false);
        }
        sunX = sunxposition * mapWidth / originalImageWidth;
        sunY = sunyposition * mapHeight / originalImageHeight;
        orientSun();
        
        
        //scale and set cmPerPixel (map scale)
        cmPerPixel = yardWidthInCM / originalImageWidth; //in original image
        cmPerPixel = cmPerPixel * mapWidth / originalImageWidth; //in scaled image
    }
    
    class MLabel extends javax.swing.JLabel{
        private BufferedImage bimg;
        java.awt.Graphics2D g2;
        Image mapImg; //the map image to display on the label
        int width; //image width
        int height; //image height
        javax.swing.JLabel rlabel; //the first label (rover label)
        javax.swing.JLabel tlabel; //the second label (target label)
        Cursor cursor;
        int x1 = 0; //the x position of the first line endpoint
        int y1 = 0; //the y position of the first line endpoint
        int x2 = 0; //the x position of the second line endpoint
        int y2 = 0; //the y position of the second line endpoint
        int d = 0; //the distance represented by the line
        
        public MLabel(int width, int height, javax.swing.JLabel rlabel, javax.swing.JLabel tlabel){
            this.width = width;
            this.height = height;
            this.rlabel = rlabel;
            this.tlabel = tlabel;
            
            /** Displays different cursors for the map and the mask area. */
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                public void mouseMoved(MouseEvent evt) {
                    if(mapPoly.contains(evt.getPoint())){
                        setNormalCursor();
                    }else{
                        setMaskCursor();
                    }
                }
            });
            
            bimg = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_RGB);
        }
        
        /** Returns a customized tool tip. */
        public javax.swing.JToolTip createToolTip(){
            javax.swing.JToolTip tip = new javax.swing.JToolTip(){
                public String getTipText(){
                    try{
                        if(rlabel != null && rlabel.isVisible()){
                            return ""+d+"-"+(d+10)+"cm";
                        }else {
                            //no labels visible - tool tip off
                            return null;
                        }
                    }catch (java.lang.NullPointerException e){
                        return null;
                    }
                }
            };
            tip.setBorder(new javax.swing.border.LineBorder(java.awt.Color.white,1));
            tip.setForeground(new java.awt.Color(0,0,102));
            tip.setFont(new java.awt.Font("Verdana",java.awt.Font.BOLD,11));
            tip.setBackground(new java.awt.Color(255,255,204,153));
            
            return tip;
        }
        
        public void setCursor(Cursor c){
            super.setCursor(c);
            cursor = c;
        }
        private void setNormalCursor(){
            super.setCursor(cursor);
        }
        private void setMaskCursor(){
            Cursor temp = cursor;
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            cursor = temp;
        }
        
        public void paintLineTo(java.awt.Graphics g, int x, int y){
            if(mapPoly.contains(x,y)){
                x2 = x;
                y2 = y;
                paint(g);
            }
        }
        
        protected void paintComponent(java.awt.Graphics g){
            g2 = (java.awt.Graphics2D)g;
            super.paintComponent(g2);
            
            if(rlabel != null && rlabel.isVisible()){
                //rlabel is visible so draw the line to the target
                
                //draw line
                g2.setColor(new java.awt.Color(255,255,255));
                java.awt.Stroke oldStroke = g2.getStroke();
                float[] dashArray = {5};
                g2.setStroke(new java.awt.BasicStroke((float)1.0,java.awt.BasicStroke.CAP_SQUARE,java.awt.BasicStroke.JOIN_MITER,(float)10.0,dashArray,(float)0.0));
                
                x1 = rlabel.getX()+rlabel.getPreferredSize().width/2;
                y1 = rlabel.getY()+rlabel.getPreferredSize().height/2;
                g2.drawLine(x1,y1,x2,y2);
                g2.setStroke(oldStroke);
                
                d = (int)(new java.awt.Point(x1,y1)).distance(x2,y2);
                //scale
                d = (int)(d*cmPerPixel);
                if(tlabel != null){
                    if(!tlabel.isVisible()){
                        //only rover label visible - turn tool tip on
                        this.setToolTipText("dist "+d);
                    }
                }
                setDistance(d);
            }
            rlabel.repaint();
        }
        
        public void setMapImg(Image img){
            mapImg = img;
        }
        
    }
    
    public static void main(String args[]) {
        javax.swing.JFrame f = new javax.swing.JFrame();
        f.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                System.exit(0);
            }
        });
        
        java.awt.GraphicsDevice device = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean isFullScreen = device.isFullScreenSupported();
        
        try {
            Image img = ImageIO.read(new File("c:/RoverDev/0.jpg"));
            //Image mapImg = ImageIO.read(new File(Exhibit.exhibitPath,"SatelliteMaps/satellite.JPG"));
            //Image mapImg = ImageIO.read(new File(Exhibit.exhibitPath,"SatelliteMaps/polyYard.gif"));
            //Image mapImg = ImageIO.read(new File(Exhibit.exhibitPath,"SatelliteMaps/topo3.jpg"));
            //Image mapImg = ImageIO.read(new File(Exhibit.exhibitPath,"SatelliteMaps/satmap.gif"));
            /*Image mapImg = ImageIO.read(new File(Exhibit.exhibitPath,"SatelliteMaps/satimage.gif"));
            Image rolloverImg = ImageIO.read(new File(Exhibit.exhibitPath,"SatelliteMaps/satimage-rollover.gif"));
             */
            Image mapImg = ImageIO.read(new File(Exhibit.exhibitPath,"SatelliteMaps/justdistance.gif"));
            Image rolloverImg = ImageIO.read(new File(Exhibit.exhibitPath,"SatelliteMaps/justdistance-rollover.gif"));
            
            MissionCentral panel = new MissionCentral();
            panel.setImage(img);
            panel.setMapImage(mapImg,rolloverImg);
            
            
            //just distance
            int w = 326;
            int h = 80;
            int[] xs = {0,w,w,0};
            int[] ys = {0,0,h,h};
            int numMapPoints = 4;
            
           /* //user test yard
            int w = 331;
            int h = 331;
            int[] xs = {0,w,h,0};
            int[] ys = {0,0,w,h};
            int numMapPoints = 4;
            */
            /*//polyyard
             int[] xs = {100,300,300,0,0};
            int[] ys = {0,0,300,300,100};
            int numMapPoints = 5;
             */
            //int sunX = 200; int sunY = 0;//test top
            //int sunX = 331; int sunY = 200;//test right
            //int sunX = 0; int sunY = 250;//test left
            //int sunX = 200; int sunY = 331;//test bottom
            //int sunX = 0; int sunY = 0; //test corner
            //int sunX = 331; int sunY = 0;//test corner
            //int sunX = 331; int sunY = 331;//test corner
            //int sunX = 0; int sunY = 331;//test corner
            //int sunX = 150; int sunY = 150;//test other
            int sunX = 331; int sunY = 75;//user test - right side 3 feet down
            
           /* panel.setMapCoordinates(new java.awt.Polygon(xs,ys,numMapPoints),
            sunX,sunY, 12 * 30.48,mapImg.getWidth(panel),mapImg.getHeight(panel));
            */
            panel.setMapCoordinates(new java.awt.Polygon(xs,ys,numMapPoints),
            -1,-1, 480,mapImg.getWidth(panel),mapImg.getHeight(panel));
            
            f.getContentPane().add(panel);
            f.setResizable(false);
            f.setUndecorated(true);
            f.pack();
            panel.start();
            if (isFullScreen) {
                // Full-screen mode
                device.setFullScreenWindow(f);
                f.validate();
            } else {
                // Windowed mode
                f.show();
                panel.stop();
            }
        } catch (IOException e) {
            System.err.println("Error in reading image: " + e);
        }
    }
    
    public void setClockTime(String s) {
        clock.setText(s);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JMenuItem attractLoopMenuItem;
    private javax.swing.JLabel bgLabel;
    private javax.swing.JButton clearButton;
    private javax.swing.JLabel cmLabel;
    private javax.swing.JLabel degreeLabel;
    private javax.swing.JPanel dirPanel;
    private javax.swing.JLabel directionBoxLabel;
    private javax.swing.JLabel directionLabel;
    private javax.swing.JPanel distPanel;
    private javax.swing.JLabel distanceBoxLabel;
    private javax.swing.JLabel distanceLabel;
    private javax.swing.JLabel goBoxLabel;
    public javax.swing.JButton goButton;
    private javax.swing.JLabel instructLabel;
    public javax.swing.JLayeredPane layeredPane;
    private javax.swing.JLayeredPane mapLayeredPane;
    public javax.swing.JMenuItem navOnlyMenuItem;
    public javax.swing.JMenuItem navPlusMenuItem;
    public javax.swing.JMenuItem newPanMenuItem;
    private javax.swing.JLabel panBorderLabel;
    private javax.swing.JLayeredPane panLayeredPane;
    private javax.swing.JLabel panorama;
    private javax.swing.JPopupMenu popupMenu;
    private javax.swing.JLabel rightLeftLabel;
    private javax.swing.JLabel roverLabel;
    private javax.swing.JLabel satBorderLabel;
    private javax.swing.JLabel siteLabel;
    private javax.swing.JLabel sun;
    private javax.swing.JLabel sunLabel;
    private javax.swing.JLayeredPane sunLayeredPane;
    private javax.swing.JLabel targetLabel;
    public javax.swing.JMenuItem turnMenuItem;
    // End of variables declaration//GEN-END:variables
    
    private int siteX;  //the current x location of the scroll site with respect to the panorama
    private int mouseX; //the current x location of the mouse when over the panorama with respect to the panorama
    
    private int degrees;
    private int dist;
    
    //timers
    //    private javax.swing.Timer mbAnimationTimer; //timer for mission builder animation
    
    //sizes
    private int mapWidth;
    private int mapHeight;
    private int panoramaWidth;
    
    //custom cursors
    private Cursor hand;
    private Cursor fullCrosshairs;
    private Cursor emptyCrosshairs;
    
    //images
    private Image bgImg; //large background image for the entire screen
    //roverImg and targetImg are now the same
    private Image roverImg; //rover image that gets placed on the map
    private Image targetImg; //target image that gets placed on the map
    private Image siteImg; //site for the panorama
    private Image sunImg; //the sun image
    private Image panArrowLeftImg; //the arrow icon displayed under the panorama
    private Image panArrowRightImg; //the arrow icon displayed under the panorama
    
    //icons
    private ImageIcon panArrowLeftIcon;
    private ImageIcon panArrowRightIcon;
    private ImageIcon instruct1Icon;
    private ImageIcon instruct2Icon;
    private ImageIcon instruct3Icon;
    
    private ClockPane clock;
    
    private MLabel mapLabel;
    //private int cmPerPixel = 2 ; //scale //adjust based on actual scale.
    private double cmPerPixel = 1.2192 ; //scale //adjust based on actual scale.
    //private double cmPerPixel = 1.0 ; //scale //adjust based on actual scale.
    
    
    //all these points are with respect 0,0 at the top left corner of the map image
    //map bounding points
    /*private int[] mapXs = {100,300,300,0,0};
    private int[] mapYs = {0,0,300,300,100};
    private int numMapPoints = 5;
     */
    /*
    private int[] mapXs = {0,300,300,0};
    private int[] mapYs = {0,0,300,300};
    private int numMapPoints = 4;
     */
    //mask bounding points
    /*private int[] maskXs = {0,100,0};
    private int[] maskYs = {0,0,100};
    private int numMaskPoints = 3;
     */
    /*private int[] maskXs = {0,0,0};
    private int[] maskYs = {0,0,0};
    private int numMaskPoints = 3;
     */
    //java.awt.Polygon maskp = new java.awt.Polygon(maskXs,maskYs,numMaskPoints);
    //java.awt.Polygon mapPoly = new java.awt.Polygon(mapXs,mapYs,numMapPoints);
    java.awt.Polygon mapPoly; //polygon defining the shape of the satellite map
    
    //sun center point
    private int sunX;
    private int sunY;
    
    private int buffer; //the buffer between the edge of the mapLayeredPane and the sunLayeredPane
    
}
