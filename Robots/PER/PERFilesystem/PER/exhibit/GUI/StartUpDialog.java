/*
 * StartUpDialog.java
 *
 * Created on July 22, 2003, 4:10 PM
 */

package PER.exhibit.GUI;

import java.awt.Image;
import java.io.*;
import PER.rover.Filter;
import PER.rover.ImagePreview;

/**
 * Allows museum staff to specify variables specific to the rover and yard
 * currently being used. The rover's IP address, yard map files, timeouts,
 * and the angle to use for the panorama are all specified here. The
 * StartUpDialog also allows one to test the panorama directly.
 *
 * @author  Rachel Gockley
 * @author  Emily Hamner
 */

public class StartUpDialog extends javax.swing.JDialog {
    public final static int BEGIN_BUTTON = 1;
    public final static int CANCEL_BUTTON = 2;
    
    public final static Integer DEFAULT_PANORAMA_ANGLE = new Integer(-40);
    
    /** Creates new form StartUpDialog */
    public StartUpDialog(java.awt.Frame parent, PER.rover.Rover r) {
        super(parent,"PER Exhibit - version "+PER.PERConstants.VERSION,true);
        
        rov = r;
        
        initComponents();
        
        //set input verifiers for text fields
        missionTextField.setInputVerifier(new SecsVerifier()); //must be an int. no minimum value
        finalTextField.setInputVerifier(new SecsVerifier(1)); //must be an int >= 1
        
        //load the timeouts from the saved file and initialize text fields
        loadExhibitSettings();
        
        //show default ip address
        ipTextField.setText(rov.getDefaultIP());
        
        //set sizes and bounds
        previewWidth = 300;
        previewHeight = 300;
        previewLabel.setPreferredSize(new java.awt.Dimension(previewWidth, previewHeight));
        previewLabel.setBounds(0,0,previewLabel.getPreferredSize().width, previewLabel.getPreferredSize().height);
        rolloverLabel.setPreferredSize(new java.awt.Dimension(previewWidth-2,previewHeight-2));
        rolloverLabel.setBounds(1,1,rolloverLabel.getPreferredSize().width, rolloverLabel.getPreferredSize().height);
        previewLayeredPane.setPreferredSize(new java.awt.Dimension(previewWidth, previewHeight));
        
        //set layers
        previewLayeredPane.setLayer(rolloverLabel,javax.swing.JLayeredPane.DEFAULT_LAYER.intValue());
        previewLayeredPane.setLayer(previewLabel,javax.swing.JLayeredPane.PALETTE_LAYER.intValue());
        
        //start with unhighlighted
        rolloverLabel.setVisible(false);
        
        mapImage = null;
        rolloverImage = null;
        mapFile = null;
        fileNameLabel.setText("Please select a file.");
        rollFileNameLabel.setText("Please select a file.");
        fileNameLabel1.setText("Please select a file.");
        //try the last satellite file
        try {
            loadMapFiles();
            if(mapFile != null){
                mapImage = javax.imageio.ImageIO.read(mapFile);
                previewLabel.setIcon(new javax.swing.ImageIcon(mapImage.getScaledInstance(previewWidth,previewHeight,Image.SCALE_SMOOTH)));
                fileNameLabel.setText(mapFile.getName());
            }
            if(rolloverFile != null){
                rolloverImage = javax.imageio.ImageIO.read(rolloverFile);
                rolloverLabel.setIcon(new javax.swing.ImageIcon(rolloverImage.getScaledInstance(previewWidth-2,previewHeight-2,Image.SCALE_SMOOTH)));
                rollFileNameLabel.setText(rolloverFile.getName());
            }
            if(coordinatesFile != null){
                fileNameLabel1.setText(coordinatesFile.getName());
            }
        } catch (java.io.IOException e) {
            PER.rover.Log.println("Error loading satellite map files: " + e);
        }
        
        //create file chooser for satellite map picture and rollover border
        imageFileChooser = new javax.swing.JFileChooser(PER.exhibit.Exhibit.exhibitPath+"/SatelliteMaps/");
        //filter for only image files
        imageFileChooser.setAcceptAllFileFilterUsed(false);
        imageFileChooser.setFileFilter(new Filter(Filter.IMAGE_FILTER));
        imageFileChooser.setAccessory(new ImagePreview(imageFileChooser));
        
        //create file chooser for satellite map coordinates
        coordinatesFileChooser = new javax.swing.JFileChooser(PER.exhibit.Exhibit.exhibitPath+"/SatelliteMaps/");
        //filter for only text files
        coordinatesFileChooser.setAcceptAllFileFilterUsed(false);
        coordinatesFileChooser.setFileFilter(new Filter(Filter.TEXT_FILTER));
        
        //show rollover border when rollover preview
        previewLayeredPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                //highlight
                rolloverLabel.setVisible(true);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                //dim
                rolloverLabel.setVisible(false);
            }
        });
        
        //initialize panorama combo box values
        for(int i = upperPanLimit; i >= lowerPanLimit; i--){
            panComboBox.addItem(new Integer(i));
        }
        //load panorama angle from the saved file and set pan combo box selection
        loadPanoramaAngle();
        //initialize takePanoramaAction
        tpa = new PER.rover.TakePanoramaAction(getPanoramaAngle(), getPanoramaAngle()+50, 320, 240);
            
        pack();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jTabbedPane1 = new javax.swing.JTabbedPane();
        contentPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        ipTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        advancedPanel = new javax.swing.JPanel();
        missionTOLabel = new javax.swing.JLabel();
        missionTextField = new javax.swing.JTextField();
        secsLabel2 = new javax.swing.JLabel();
        timeoutClarificationLabel = new javax.swing.JLabel();
        finalTOLabel = new javax.swing.JLabel();
        finalTextField = new javax.swing.JTextField();
        secsLabel3 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        restoreDefaultsButton = new javax.swing.JButton();
        mapPanel = new javax.swing.JPanel();
        mapLabelLabel = new javax.swing.JLabel();
        fileNameLabel = new javax.swing.JLabel();
        browseButton = new javax.swing.JButton();
        mapLabelLabel2 = new javax.swing.JLabel();
        rollFileNameLabel = new javax.swing.JLabel();
        rollBrowseButton = new javax.swing.JButton();
        previewLayeredPane = new javax.swing.JLayeredPane();
        rolloverLabel = new javax.swing.JLabel();
        previewLabel = new javax.swing.JLabel();
        mapLabelLabel1 = new javax.swing.JLabel();
        fileNameLabel1 = new javax.swing.JLabel();
        browseButton1 = new javax.swing.JButton();
        panoramaPanel = new javax.swing.JPanel();
        panAngleLabel = new javax.swing.JLabel();
        panComboBox = new javax.swing.JComboBox();
        testPanButton = new javax.swing.JButton();
        restoreDefaultAngleButton = new javax.swing.JButton();
        buttonPanel = new javax.swing.JPanel();
        beginButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        contentPanel.setLayout(new java.awt.GridBagLayout());

        contentPanel.setBackground(java.awt.Color.white);
        contentPanel.setBorder(new javax.swing.border.EtchedBorder());
        jLabel1.setText("Enter the IP address of the Rover to connect to:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        contentPanel.add(jLabel1, gridBagConstraints);

        ipTextField.setText("192.168.2.0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contentPanel.add(ipTextField, gridBagConstraints);

        jLabel2.setText("Make sure the Rover is on and ready, and then press BEGIN.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        contentPanel.add(jLabel2, gridBagConstraints);

        jTabbedPane1.addTab("Connection", contentPanel);

        advancedPanel.setLayout(new java.awt.GridBagLayout());

        advancedPanel.setBackground(new java.awt.Color(255, 255, 255));
        missionTOLabel.setText("Mission Central Timeout:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 5);
        advancedPanel.add(missionTOLabel, gridBagConstraints);

        missionTextField.setColumns(3);
        missionTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        missionTextField.setText("jTextField2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        advancedPanel.add(missionTextField, gridBagConstraints);

        secsLabel2.setText("seconds");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(10, 3, 0, 0);
        advancedPanel.add(secsLabel2, gridBagConstraints);

        timeoutClarificationLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        timeoutClarificationLabel.setText("(A value of 0 will turn this timeout off.)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 3, 0, 10);
        advancedPanel.add(timeoutClarificationLabel, gridBagConstraints);

        finalTOLabel.setText("Final Screen Timeout:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 5);
        advancedPanel.add(finalTOLabel, gridBagConstraints);

        finalTextField.setColumns(3);
        finalTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        finalTextField.setText("jTextField3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        advancedPanel.add(finalTextField, gridBagConstraints);

        secsLabel3.setText("seconds");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        advancedPanel.add(secsLabel3, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("(This timeout must be at least 1 second.)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 10);
        advancedPanel.add(jLabel3, gridBagConstraints);

        restoreDefaultsButton.setText("Restore Default Timeouts");
        restoreDefaultsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restoreDefaultsButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        advancedPanel.add(restoreDefaultsButton, gridBagConstraints);

        mapPanel.setLayout(new java.awt.GridBagLayout());

        mapPanel.setBackground(new java.awt.Color(255, 255, 255));
        mapLabelLabel.setText("Satellite map:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        mapPanel.add(mapLabelLabel, gridBagConstraints);

        fileNameLabel.setText("filename");
        fileNameLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 2, 5);
        mapPanel.add(fileNameLabel, gridBagConstraints);

        browseButton.setText("Browse");
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        mapPanel.add(browseButton, gridBagConstraints);

        mapLabelLabel2.setText("Satellite map rollover border:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        mapPanel.add(mapLabelLabel2, gridBagConstraints);

        rollFileNameLabel.setText("filename");
        rollFileNameLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 2, 5);
        mapPanel.add(rollFileNameLabel, gridBagConstraints);

        rollBrowseButton.setText("Browse");
        rollBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rollBrowseButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        mapPanel.add(rollBrowseButton, gridBagConstraints);

        previewLayeredPane.setBackground(java.awt.Color.lightGray);
        previewLayeredPane.setOpaque(true);
        rolloverLabel.setBounds(0, 0, -1, -1);
        previewLayeredPane.add(rolloverLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        previewLabel.setBounds(0, 0, -1, -1);
        previewLayeredPane.add(previewLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 3, 3);
        mapPanel.add(previewLayeredPane, gridBagConstraints);

        mapLabelLabel1.setText("Satellite map coordinates file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        mapPanel.add(mapLabelLabel1, gridBagConstraints);

        fileNameLabel1.setText("filename");
        fileNameLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 0, 5);
        mapPanel.add(fileNameLabel1, gridBagConstraints);

        browseButton1.setText("Browse");
        browseButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButton1ActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        mapPanel.add(browseButton1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        advancedPanel.add(mapPanel, gridBagConstraints);

        panoramaPanel.setLayout(new java.awt.GridBagLayout());

        panoramaPanel.setBackground(new java.awt.Color(255, 255, 255));
        panAngleLabel.setText("Panorama Angle");
        panoramaPanel.add(panAngleLabel, new java.awt.GridBagConstraints());

        panComboBox.setBackground(new java.awt.Color(255, 255, 255));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panoramaPanel.add(panComboBox, gridBagConstraints);

        testPanButton.setText("Test Panorama");
        testPanButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testPanButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panoramaPanel.add(testPanButton, gridBagConstraints);

        restoreDefaultAngleButton.setText("Restore Default Angle");
        restoreDefaultAngleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restoreDefaultAngleButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panoramaPanel.add(restoreDefaultAngleButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        advancedPanel.add(panoramaPanel, gridBagConstraints);

        jTabbedPane1.addTab("Advanced", advancedPanel);

        getContentPane().add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        beginButton.setText("BEGIN");
        beginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                beginButtonMouseClicked(evt);
            }
        });

        buttonPanel.add(beginButton);

        cancelButton.setText("CANCEL");
        cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                cancelButtonMouseClicked(evt);
            }
        });

        buttonPanel.add(cancelButton);

        getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);

    }//GEN-END:initComponents
    
    private void restoreDefaultAngleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restoreDefaultAngleButtonActionPerformed
        panComboBox.setSelectedItem(DEFAULT_PANORAMA_ANGLE);
    }//GEN-LAST:event_restoreDefaultAngleButtonActionPerformed
    
    private void testPanButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testPanButtonActionPerformed
        PER.rover.Log.println("Test Panorama Button clicked. (Angle = "+getPanoramaAngle()+".)");
        //connect to rover
        if(initCommunications()){
            System.gc();
            //take panorama
            //PER.rover.TakePanoramaAction tpa = new PER.rover.TakePanoramaAction(getPanoramaAngle(), getPanoramaAngle()+50, 320, 240);
            //tpa = new PER.rover.TakePanoramaAction(getPanoramaAngle(), getPanoramaAngle()+50, 320, 240);
            tpa.setAngles(getPanoramaAngle(), getPanoramaAngle()+50);
            if(tpa.doAction(rov)){
                while(!tpa.isCompleted()){
                    try{Thread.sleep(10);}catch(Exception e) {}
                }
                //    if(tpa.isSuccess()){
                //scale panorama
                Image panImage = tpa.getImage();
                int width = 900;
                int height = width * panImage.getHeight(this) / panImage.getWidth(this);
                panImage = panImage.getScaledInstance(width,height,Image.SCALE_SMOOTH);
                //display panorama
                javax.swing.JDialog panDialog = new javax.swing.JDialog((java.awt.Frame)null,"Test Panorama: Angle "+getPanoramaAngle(),false);
                panDialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
                panDialog.getContentPane().add(new javax.swing.JLabel(new javax.swing.ImageIcon(panImage)));
                panDialog.setSize(width+5,height+30);
                panDialog.show();
                //  }
            }else{
                javax.swing.JOptionPane.showMessageDialog(this,
                "Error: could not take panorama!",
                "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
            
        }
    }//GEN-LAST:event_testPanButtonActionPerformed
    
    private void rollBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rollBrowseButtonActionPerformed
        int returnVal = imageFileChooser.showOpenDialog(null);
        
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
            try{
                rolloverFile = imageFileChooser.getSelectedFile();
                //open the file.
                rolloverImage = javax.imageio.ImageIO.read(rolloverFile);
                rolloverLabel.setIcon(new javax.swing.ImageIcon(rolloverImage.getScaledInstance(previewWidth-2,previewHeight-2,Image.SCALE_SMOOTH)));
                rollFileNameLabel.setText(rolloverFile.getName());
                
            }catch(java.io.IOException e){
                javax.swing.JOptionPane.showMessageDialog(this,
                "Error reading the satellite map rollover border file.\n"
                + "Please try again.",
                "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                PER.rover.Log.println("Error reading the satellite map rollover border file: "+e,true);
            }
        } else {
            //log.append("Open command cancelled by user." + newline);
        }
    }//GEN-LAST:event_rollBrowseButtonActionPerformed
    
    private void browseButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButton1ActionPerformed
        int returnVal = coordinatesFileChooser.showOpenDialog(null);
        
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
            coordinatesFile = coordinatesFileChooser.getSelectedFile();
            fileNameLabel1.setText(coordinatesFile.getName());
        } else {
            //log.append("Open command cancelled by user." + newline);
        }
        
    }//GEN-LAST:event_browseButton1ActionPerformed
    
    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        int returnVal = imageFileChooser.showOpenDialog(null);
        
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
            try{
                mapFile = imageFileChooser.getSelectedFile();
                //open the file.
                mapImage = javax.imageio.ImageIO.read(mapFile);
                previewLabel.setIcon(new javax.swing.ImageIcon(mapImage.getScaledInstance(previewWidth,previewHeight,Image.SCALE_SMOOTH)));
                fileNameLabel.setText(mapFile.getName());
                
            }catch(java.io.IOException e){
                javax.swing.JOptionPane.showMessageDialog(this,
                "Error reading the satellite map file.\n"
                + "Please try again.",
                "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                PER.rover.Log.println("Error reading the satellite map file: "+e,true);
            }
        } else {
            //log.append("Open command cancelled by user." + newline);
        }
    }//GEN-LAST:event_browseButtonActionPerformed
    
    private void restoreDefaultsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restoreDefaultsButtonActionPerformed
        //timeouts
        setMissionTimeout(FlowController.DEFAULT_MISSION_TIMEOUT/1000);
        setFinalTimeout(FlowController.DEFAULT_FINAL_TIMEOUT/1000);
    }//GEN-LAST:event_restoreDefaultsButtonActionPerformed
    
    private void cancelButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_cancelButtonMouseClicked
        buttonPressed = CANCEL_BUTTON;
        setVisible(false);
    }//GEN-LAST:event_cancelButtonMouseClicked
    
    private void beginButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_beginButtonMouseClicked
        //make sure timeouts and satellite map are valid
        if(!verifyInputs())
            return;
        else{
            //set timeouts
            FlowController.setTimeouts(getMissionTimeout()*1000, getFinalTimeout()*1000);
        }
        
        if(initCommunications()){
            buttonPressed = BEGIN_BUTTON;
            setVisible(false);
            //save IP
            rov.saveIP(ipTextField.getText());
            //save timeout settings and panorama angle
            saveExhibitSettings();
            //save satellite map files and coordinates file
            //saveMapFiles(mapFile, rolloverFile, coordinatesFile);
            saveMapFiles();
            //save panorama angle
            savePanoramaAngle();
        }
    }//GEN-LAST:event_beginButtonMouseClicked
    
    /** Initializes communications and initializes the robot. Displays an error
     * message if there is an error.
     *
     *@returns true if successful, false if no connection can be established
     * or the rover does not respond.
     */
    private boolean initCommunications(){
        boolean success;
        String IP;
        
        //initialize communications
        IP = ipTextField.getText();
        success = rov.initComm(IP);
        rov.setCurrentIP(IP); //saves the current ip in case communication needs to be reestablished
        
        if (success) {
            success = rov.initRobot();
            success = rov.refresh();
            if (success) {
                return true;
            } else {
                javax.swing.JOptionPane.showMessageDialog(this,
                "Error: rover is not responding! Please check that\n"
                + "the rover is on and ready, and then try again.",
                "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        } else {
            javax.swing.JOptionPane.showMessageDialog(this,
            "Error: could not connect to the rover! Check that\n"
            + "you have entered the IP address correctly. Also,\n"
            + "make sure the rover is on and ready.",
            "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
    
    public int getButtonPressed() {
        return buttonPressed;
    }
    
    public Image getMapImage(){
        return mapImage;
    }
    
    public Image getRolloverImage(){
        return rolloverImage;
    }
    
    public File getMapCoordinatesFile(){
        return coordinatesFile;
    }
    
    public int getPanoramaAngle(){
        return ((Integer)panComboBox.getSelectedItem()).intValue();
    }
    
    /** Returns the mission timeout in seconds. */
    private int getMissionTimeout(){
        return Integer.parseInt(missionTextField.getText());
    }
    
    /** Returns the final timeout in seconds. */
    private int getFinalTimeout(){
        return Integer.parseInt(finalTextField.getText());
    }
    
    public void setMapFile(File file){
        mapFile = file;
    }
    
    public void setRolloverFile(File file){
        rolloverFile = file;
    }
    
    public void setCoordinatesFile(File file){
        coordinatesFile = file;
    }
    
    public void setPanoramaAngle(int i){
        panComboBox.setSelectedItem(new Integer(i));
    }
    
    /** Given a timeout value in seconds, displays the value in the mission timeout
     * text field.
     */
    private void setMissionTimeout(int t){
        missionTextField.setText(""+t);
    }
    
    /** Given a timeout value in seconds, displays the value in the final timeout
     * text field.
     */
    private void setFinalTimeout(int t){
        finalTextField.setText(""+t);
    }
    
    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        buttonPressed = CANCEL_BUTTON;
        setVisible(false);
        dispose();
    }//GEN-LAST:event_closeDialog
    
    /** Saves the panorama angle to a file.
     */
    public void savePanoramaAngle() {
        try {
            FileOutputStream ostream = new FileOutputStream(new File(rov.topLevelDir, "PanoramaAngle"));
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            
            p.writeObject(new Integer(getPanoramaAngle()));
            
            p.flush();
            ostream.close();
        } catch (Exception e) {
            PER.rover.Log.println("Saving the panorama angle failed! " + e);
        }
    }
    
    /** Loads the panorama angle from the saved file and selects the angle
     * in the panComboBox. If loading fails, sets the combo box selection
     * to DEFAULT_PANORAMA_ANGLE.
     */
    public void loadPanoramaAngle() {
        Integer value = DEFAULT_PANORAMA_ANGLE;
        try {
            FileInputStream istream = new FileInputStream(new File(rov.topLevelDir, "PanoramaAngle"));
            ObjectInputStream p = new ObjectInputStream(istream);
            
            value = ((Integer)p.readObject());
            
            istream.close();
            
        } catch (Exception e) {
            //use default
            PER.rover.Log.println("Loading panorama angle failed; using default: " + e);
        }
        panComboBox.setSelectedItem(value);
    }
    
    /** Loads the timeouts and transmission delay from the saved file
     * and displays them in the appropriate text fields. If loading fails,
     * uses the default values.
     */
    private void loadExhibitSettings() {
        int mt;
        int ft;
        
        try {
            FileInputStream istream = new FileInputStream(new File(PER.rover.Rover.topLevelDir, "ExhibitSettings"));
            ObjectInputStream p = new ObjectInputStream(istream);
            
            //timeouts
            //FlowController.MISSION_TIMEOUT = ((Integer)p.readObject()).intValue();
            //FlowController.FINAL_TIMEOUT = ((Integer)p.readObject()).intValue();
            mt = ((Integer)p.readObject()).intValue();
            ft = ((Integer)p.readObject()).intValue();
            
            istream.close();
            
        } catch (Exception e) {
            //use defaults
            PER.rover.Log.println("Loading exhibit settings failed; using defaults: " + e);
            //timeouts
            //FlowController.MISSION_TIMEOUT = FlowController.DEFAULT_MISSION_TIMEOUT;
            //FlowController.FINAL_TIMEOUT = FlowController.DEFAULT_FINAL_TIMEOUT;
            mt = FlowController.DEFAULT_MISSION_TIMEOUT;
            ft = FlowController.DEFAULT_FINAL_TIMEOUT;
        }
        
        //initialize text field timeout lengths
        setMissionTimeout(mt/1000);
        setFinalTimeout(ft/1000);
    }
    
    /** Saves the two TIMEOUTs to a file.
     */
    private void saveExhibitSettings() {
        try {
            FileOutputStream ostream = new FileOutputStream(new File(PER.rover.Rover.topLevelDir, "ExhibitSettings"));
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            
            //timeouts
            //p.writeObject(new Integer(FlowController.MISSION_TIMEOUT));
            //p.writeObject(new Integer(FlowController.FINAL_TIMEOUT));
            p.writeObject(new Integer(getMissionTimeout()*1000));
            p.writeObject(new Integer(getFinalTimeout()*1000));
            
            p.flush();
            ostream.close();
        } catch (Exception e) {
            PER.rover.Log.println("Saving the exhibit settings failed! " + e);
        }
    }
    
    /** Loads the previous satellite map file and previous coordinates file;
     * sets the map file and coordinates file in <code>dialog</code>.
     */
    private void loadMapFiles() {
        try {
            FileInputStream istream = new FileInputStream(new File(PER.rover.Rover.topLevelDir, "prevMapAndCoords"));
            ObjectInputStream p = new ObjectInputStream(istream);
            
            setMapFile((File)p.readObject());
            setRolloverFile((File)p.readObject());
            setCoordinatesFile((File)p.readObject());
            
            istream.close();
        } catch (Exception e) {
            PER.rover.Log.println("Loading satellite map file, rollover border file, and coordinates file failed: " + e);
        }
    }
    
    /** Saves the satellite map file, rollover border file, and coordinates file to a file.
     */
    private void saveMapFiles() {
        try {
            FileOutputStream ostream = new FileOutputStream(new File(PER.rover.Rover.topLevelDir, "prevMapAndCoords"));
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            
            p.writeObject(mapFile);
            p.writeObject(rolloverFile);
            p.writeObject(coordinatesFile);
            
            p.flush();
            ostream.close();
        } catch (Exception e) {
            PER.rover.Log.println("Saving the satellite map file, rollover border file, and coordinates file failed! " + e);
        }
    }
    
    /** Verifies the text field inputs and satellite map selection
     * and displays a message if any input
     * is invalid. Use this function to force the input to be verified in
     * cases (such as pressing the BEGIN button) where it is not verified
     * automatically.
     */
    private boolean verifyInputs(){
        if(!(missionTextField.getInputVerifier().shouldYieldFocus(missionTextField) &&
        finalTextField.getInputVerifier().shouldYieldFocus(finalTextField))){
            //invalid timeout
            javax.swing.JOptionPane.showMessageDialog(this,
            /*"Error: Please make sure all timeouts are\n"
            + "valid integers greater than zero.",
             */"Error: Please make sure all\n"
             + "timeouts are valid integers.",
             "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }else if(mapImage == null){
            //invalid image
            javax.swing.JOptionPane.showMessageDialog(this,
            "Error: Please select a satellite map image\n"
            + "under the Advanced settings.",
            "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }else if(rolloverImage == null){
            //invalid image
            javax.swing.JOptionPane.showMessageDialog(this,
            "Error: Please select a satellite map rollover border image\n"
            + "under the Advanced settings.",
            "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }else if(coordinatesFile == null){
            //invalid map coordinates file
            javax.swing.JOptionPane.showMessageDialog(this,
            "Error: Please select a satellite map coordinates\n"
            + "file under the Advanced settings.",
            "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }else return true;
        
        return false;
    }
    
    /** Verifies that the input is an integer greater than or equal to the given
     * minimum value.
     */
    class SecsVerifier extends javax.swing.InputVerifier {
        int min; //the minimum valid value
        public SecsVerifier(int minimum){
            super();
            min = minimum;
        }
        public SecsVerifier(){
            super();
            min = -1; //no minimum value
        }
        public boolean verify(javax.swing.JComponent input) {
            try{
                javax.swing.JTextField tf = (javax.swing.JTextField) input;
                try{
                    int i = java.lang.Integer.parseInt(tf.getText());
                    if(min == -1)
                        return true;
                    else
                        return i >= min;
                }catch(java.lang.NumberFormatException e){
                    return false;
                }
            }catch(java.lang.ClassCastException e){
                PER.rover.Log.println("Can not verify input. Component is not a JTextField. "+e,true);
                return false;
            }
        }
    }
    
    private int buttonPressed;
    private PER.rover.Rover rov;
    private final javax.swing.JFileChooser imageFileChooser;
    private final javax.swing.JFileChooser coordinatesFileChooser;
    private int previewWidth;
    private int previewHeight;
    private Image mapImage;
    private Image rolloverImage;
    private File mapFile;
    private File rolloverFile;
    private File coordinatesFile;
    
    private int upperPanLimit = -20;
    private int lowerPanLimit = -55;
    
    PER.rover.TakePanoramaAction tpa; //for testing the panorama
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel advancedPanel;
    private javax.swing.JButton beginButton;
    private javax.swing.JButton browseButton;
    private javax.swing.JButton browseButton1;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel contentPanel;
    private javax.swing.JLabel fileNameLabel;
    private javax.swing.JLabel fileNameLabel1;
    private javax.swing.JLabel finalTOLabel;
    private javax.swing.JTextField finalTextField;
    private javax.swing.JTextField ipTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel mapLabelLabel;
    private javax.swing.JLabel mapLabelLabel1;
    private javax.swing.JLabel mapLabelLabel2;
    private javax.swing.JPanel mapPanel;
    private javax.swing.JLabel missionTOLabel;
    private javax.swing.JTextField missionTextField;
    private javax.swing.JLabel panAngleLabel;
    private javax.swing.JComboBox panComboBox;
    private javax.swing.JPanel panoramaPanel;
    private javax.swing.JLabel previewLabel;
    private javax.swing.JLayeredPane previewLayeredPane;
    private javax.swing.JButton restoreDefaultAngleButton;
    private javax.swing.JButton restoreDefaultsButton;
    private javax.swing.JButton rollBrowseButton;
    private javax.swing.JLabel rollFileNameLabel;
    private javax.swing.JLabel rolloverLabel;
    private javax.swing.JLabel secsLabel2;
    private javax.swing.JLabel secsLabel3;
    private javax.swing.JButton testPanButton;
    private javax.swing.JLabel timeoutClarificationLabel;
    // End of variables declaration//GEN-END:variables
    
}
