/*
 * Examples.java
 *
 * Created on March 30, 2004, 9:25 AM
 */

package PER.basicGUI;

import PER.rover.*;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.border.TitledBorder;


/** Examples is a graphical user interface for people wishing to program
 * the Personal Exploration Rover (PER). Examples provides some basic
 * functionality, allowing programmers to design their own PER programs
 * without the need to design a graphical interface.
 *<p>
 * The Examples interface already provides a way for people to specify an IP address,
 * connect to a rover, take and display pictures and panoramas, save images,
 * and view text output from the rover.
 *<p>
 * Examples also contains ten command buttons. The programmer can specify
 * the commands associated with these buttons, as well as the button text,
 * and an associated command description. There are also six input fields
 * which can be used to specify variables used by the commands.
 *<p>
 * Each command button is already linked to a mouseListener, so pressing
 * the button will launch the command. The programmer only needs to fill
 * in the <code>command</code> method corresponding to the button they wish to use.
 * For example, to program <code>button1</code> to drive 50 cm, you could
 * do something like the following:
 *  <p><code>
 * <pre>
 *   private void command1() {
 *       //create a DriveToAction
 *       Action currAction = new DriveToAction(50);
 *       //execute the Action and print the results
 *       if(executeAction(currAction)){
 *           outputln("Drive successful");
 *       }else{
 *           outputln("Drive failed: "+ActionConstants.getErrorText(currAction.getReturnValue()));
 *       }
 *   }
 * </pre>
 * </code>
 *<p>
 * The above code sample illustrates two other useful functions in Examples:
 * <code>executeAction</code> and <code>outputln</code>. <code>executeAction</code>
 * is a blocking method for executing an Action. <code>outputln</code> will
 * print a message to the output text area in the interface. Another useful
 * function to be aware of is <code>displayImage</code> which will display
 * an image in the interface and allow the image to be viewed at full resolution
 * or scaled.
 *<p>
 * To program <code>button1</code> to drive a variable distance based on user
 * input, you could change the above example slightly:
 * <code>
 * <pre>
 *   private void command1() {
 *       //get the distance entered by the user in the X variable field
 *       int dist = intInput(variableX);
 *       //check that the input is valid
 *       if(inputIsValid()){
 *           Action currAction = new DriveToAction(dist);
 *           ...
 * </pre>
 *</code>
 *<p>
 * The sample code above uses one of the variable input fields to determine
 * the driving distance. Although text entered into a JTextField is always
 * a string, Examples provides convenience methods to allow you to
 * convert the input into other data types and check if the input is valid.
 *<p>
 * When you have programmed a button for a certain task, you will also want
 * to change the text displayed on that button and provide a description of
 * the task. To do this, modify the lines for the button in <code>setButtonText
 * </code> and <code>setDescriptions</code>. For our example that drives
 * a variable distance based on input variable X, we might do something like this:
 *<p>
 *<code><pre>
 *  private void setButtonText(){
 *      button1.setText("Drive");
 *      ...
 *
 *  private void setDescriptions(){
 *      description1.setText("Drive X cm");
 *      ...
 *</pre></code>
 *<p>
 * One final feature of Examples is the Stop button. If you use <code>executeAction</code>
 * to execute Actions, the Stop button will work to kill these actions. If you use
 * other Rover functions or the <code>Action.doAction</code> method to execute Actions,
 * you will need to make sure that the Stop button still has the ability to kill
 * these actions. For example, you may want to print a message while the rover
 * is driving that tells how far it has traveled.
 * <code>
 * <pre>
 *   private void command1() {
 *       Action currAction = new DriveToAction(100);
 *       //start driving
 *       currAction.doAction(rov);
 *       //wait for the rover to finish driving
 *       while(!currAction.isCompleted()){
 *           outputln("Drove " + rov.highLevelState.getDist() + " cm.");
 *           try{
 *               Thread.sleep(100);
 *           }catch(java.lang.InterruptedException e){
 *               //stop driving if someone hits the stop button
 *               currAction.kill();
 *           }
 *       }
 *   }
 * </pre>
 *</code>
 * The Stop button sets the <code>stop</code> variable to <code>true</code> so if
 * you want the rover to do a dance until the Stop button is hit you could use
 * a simple while loop such as:
 * <code>
 * <pre>
 *   private void command1() {
 *       while(!stop){
 *           //do a dance...
 *       }
 *   }
 * </pre>
 *</code>
 *
 *
 *
 * @author  Emily Hamner
 */
public class Examples extends JFrame {
    
    private Rover rov;
    /** Thread of the currently executing command. */
    private Thread currThread;
    /** The full resolution version of the current image. */
    private BufferedImage fullImage;
    /** Version of the current image that is scaled to fit the display area. */
    private BufferedImage scaledImage;
    /** Set to true when the Stop button is hit. */
    private boolean stop;
    /** File chooser for saving images. */
    private final JFileChooser imageFileChooser;
    /** Indicates if the last input value checked was valid or not. */
    private boolean valid;
    /** Vertical scroll bar for the rover output area. */
    private javax.swing.JScrollBar outputScrollBar;
    
    /** Creates new form Examples */
    public Examples(Rover r) {
        rov = r;
        currThread = null;
        stop = false;
        valid = false;
        
        //initialize GUI components
        initComponents();
        imageScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        imageScrollPane.getHorizontalScrollBar().setUnitIncrement(10);
        outputScrollBar = outputScrollPane.getVerticalScrollBar();
        //set color scheme
        setBackgroundColor(new Color(130,130,182));
        setDescriptionColor(new Color(186,188,214));
        setButtonColor(new Color(186,188,214));
        setTextColor(new Color(0,0,101));
        
        //set the text to be displayed on the command buttons
        setButtonText();
        //set the command descriptions
        setDescriptions();
        
        //display the last IP address used or the default address
        ipTextField.setText(Rover.getDefaultIP());
        
        //disable most options until connected to a rover
        setEnabledPanel(commandPanel, false);
        setEnabledPanel(inputPanel, false);
        setEnabledPanel(presetPanel, false);
        setEnabledPanel(scalePanel, false);
        
        //create file chooser for saving images
        imageFileChooser = new JFileChooser(rov.topLevelDir);
        //filter for only jpg or jpeg image files
        imageFileChooser.setAcceptAllFileFilterUsed(false);
        imageFileChooser.setFileFilter(new Filter(Filter.JPG_FILTER));
        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        scalebuttonGroup = new javax.swing.ButtonGroup();
        mainPanel = new javax.swing.JPanel();
        ipPanel = new javax.swing.JPanel();
        ipLabel = new javax.swing.JLabel();
        ipTextField = new javax.swing.JTextField();
        connectButton = new javax.swing.JButton();
        commandPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        description1 = new javax.swing.JTextArea();
        button1 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        description2 = new javax.swing.JTextArea();
        button2 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        description3 = new javax.swing.JTextArea();
        button3 = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        description4 = new javax.swing.JTextArea();
        button4 = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        description5 = new javax.swing.JTextArea();
        button5 = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        description6 = new javax.swing.JTextArea();
        button6 = new javax.swing.JButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        description7 = new javax.swing.JTextArea();
        button7 = new javax.swing.JButton();
        jScrollPane8 = new javax.swing.JScrollPane();
        description8 = new javax.swing.JTextArea();
        button8 = new javax.swing.JButton();
        jScrollPane9 = new javax.swing.JScrollPane();
        description9 = new javax.swing.JTextArea();
        button9 = new javax.swing.JButton();
        jScrollPane10 = new javax.swing.JScrollPane();
        description10 = new javax.swing.JTextArea();
        button10 = new javax.swing.JButton();
        inputPanel = new javax.swing.JPanel();
        aLabel = new javax.swing.JLabel();
        variableA = new javax.swing.JTextField();
        bLabel = new javax.swing.JLabel();
        variableB = new javax.swing.JTextField();
        cLabel = new javax.swing.JLabel();
        variableC = new javax.swing.JTextField();
        xLabel = new javax.swing.JLabel();
        variableX = new javax.swing.JTextField();
        yLabel = new javax.swing.JLabel();
        variableY = new javax.swing.JTextField();
        zLabel = new javax.swing.JLabel();
        variableZ = new javax.swing.JTextField();
        presetPanel = new javax.swing.JPanel();
        takePictureButton = new javax.swing.JButton();
        takePanoramaButton = new javax.swing.JButton();
        saveImageButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        imageScrollPane = new javax.swing.JScrollPane();
        imageLabel = new javax.swing.JLabel();
        scalePanel = new javax.swing.JPanel();
        fullRadioButton = new javax.swing.JRadioButton();
        scaledRadioButton = new javax.swing.JRadioButton();
        outputPanel = new javax.swing.JPanel();
        outputScrollPane = new javax.swing.JScrollPane();
        outputTextArea = new javax.swing.JTextArea(){
            /** Override JTextArea.append so that special scrolling behavior will
            * occur even if append is called from PER.rover.Log
            */
            public void append(String str){
                if(outputScrollBar.getMaximum()
                    -outputScrollBar.getVisibleAmount()
                    == outputScrollBar.getValue()){
                    super.append(str);
                    //scroll
                    outputScrollPane.validate();
                    outputScrollPane.update(outputTextArea.getGraphics());
                    try{Thread.sleep(5);}catch(Exception e){}
                    outputScrollBar.setValue(
                        outputScrollBar.getMaximum()
                        -outputScrollBar.getVisibleAmount());
                }else{
                    //don't scroll
                    super.append(str);
                }
            }
        };

        setTitle("Personal Exploration Rover Interface");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        mainPanel.setLayout(new java.awt.GridBagLayout());

        ipLabel.setText("Rover IP Address:");
        ipPanel.add(ipLabel);

        ipTextField.setColumns(20);
        ipPanel.add(ipTextField);

        connectButton.setText("Connect");
        connectButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                connectButtonMouseClicked(evt);
            }
        });

        ipPanel.add(connectButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        mainPanel.add(ipPanel, gridBagConstraints);

        commandPanel.setLayout(new java.awt.GridBagLayout());

        commandPanel.setBorder(new javax.swing.border.TitledBorder(null, "Commands", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        description1.setBackground(new java.awt.Color(204, 204, 204));
        description1.setColumns(25);
        description1.setEditable(false);
        description1.setLineWrap(true);
        description1.setRows(2);
        description1.setWrapStyleWord(true);
        jScrollPane1.setViewportView(description1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        commandPanel.add(jScrollPane1, gridBagConstraints);

        button1.setText("jButton1");
        button1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button1MouseClicked(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 10);
        commandPanel.add(button1, gridBagConstraints);

        description2.setBackground(new java.awt.Color(204, 204, 204));
        description2.setColumns(25);
        description2.setEditable(false);
        description2.setLineWrap(true);
        description2.setRows(2);
        description2.setWrapStyleWord(true);
        jScrollPane2.setViewportView(description2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        commandPanel.add(jScrollPane2, gridBagConstraints);

        button2.setText("jButton2");
        button2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button2MouseClicked(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 10);
        commandPanel.add(button2, gridBagConstraints);

        description3.setBackground(new java.awt.Color(204, 204, 204));
        description3.setColumns(25);
        description3.setEditable(false);
        description3.setLineWrap(true);
        description3.setRows(2);
        description3.setWrapStyleWord(true);
        jScrollPane3.setViewportView(description3);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        commandPanel.add(jScrollPane3, gridBagConstraints);

        button3.setText("jButton3");
        button3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button3MouseClicked(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 10);
        commandPanel.add(button3, gridBagConstraints);

        description4.setBackground(new java.awt.Color(204, 204, 204));
        description4.setColumns(25);
        description4.setEditable(false);
        description4.setLineWrap(true);
        description4.setRows(2);
        description4.setWrapStyleWord(true);
        jScrollPane4.setViewportView(description4);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        commandPanel.add(jScrollPane4, gridBagConstraints);

        button4.setText("jButton4");
        button4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button4MouseClicked(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 10);
        commandPanel.add(button4, gridBagConstraints);

        description5.setBackground(new java.awt.Color(204, 204, 204));
        description5.setColumns(25);
        description5.setEditable(false);
        description5.setLineWrap(true);
        description5.setRows(2);
        description5.setWrapStyleWord(true);
        jScrollPane5.setViewportView(description5);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        commandPanel.add(jScrollPane5, gridBagConstraints);

        button5.setText("jButton5");
        button5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button5MouseClicked(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 10);
        commandPanel.add(button5, gridBagConstraints);

        description6.setBackground(new java.awt.Color(204, 204, 204));
        description6.setColumns(25);
        description6.setEditable(false);
        description6.setLineWrap(true);
        description6.setRows(2);
        description6.setWrapStyleWord(true);
        jScrollPane6.setViewportView(description6);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        commandPanel.add(jScrollPane6, gridBagConstraints);

        button6.setText("jButton6");
        button6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button6MouseClicked(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        commandPanel.add(button6, gridBagConstraints);

        description7.setBackground(new java.awt.Color(204, 204, 204));
        description7.setColumns(25);
        description7.setEditable(false);
        description7.setLineWrap(true);
        description7.setRows(2);
        description7.setWrapStyleWord(true);
        jScrollPane7.setViewportView(description7);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        commandPanel.add(jScrollPane7, gridBagConstraints);

        button7.setText("jButton7");
        button7.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button7MouseClicked(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        commandPanel.add(button7, gridBagConstraints);

        description8.setBackground(new java.awt.Color(204, 204, 204));
        description8.setColumns(25);
        description8.setEditable(false);
        description8.setLineWrap(true);
        description8.setRows(2);
        description8.setWrapStyleWord(true);
        jScrollPane8.setViewportView(description8);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        commandPanel.add(jScrollPane8, gridBagConstraints);

        button8.setText("jButton8");
        button8.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button8MouseClicked(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        commandPanel.add(button8, gridBagConstraints);

        description9.setBackground(new java.awt.Color(204, 204, 204));
        description9.setColumns(25);
        description9.setEditable(false);
        description9.setLineWrap(true);
        description9.setRows(2);
        description9.setWrapStyleWord(true);
        jScrollPane9.setViewportView(description9);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        commandPanel.add(jScrollPane9, gridBagConstraints);

        button9.setText("jButton9");
        button9.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button9MouseClicked(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        commandPanel.add(button9, gridBagConstraints);

        description10.setBackground(new java.awt.Color(204, 204, 204));
        description10.setColumns(25);
        description10.setEditable(false);
        description10.setLineWrap(true);
        description10.setRows(2);
        description10.setWrapStyleWord(true);
        jScrollPane10.setViewportView(description10);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        commandPanel.add(jScrollPane10, gridBagConstraints);

        button10.setText("jButton10");
        button10.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button10MouseClicked(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        commandPanel.add(button10, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 5, 5);
        mainPanel.add(commandPanel, gridBagConstraints);

        inputPanel.setLayout(new java.awt.GridBagLayout());

        inputPanel.setBorder(new javax.swing.border.TitledBorder(null, "Variables", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        aLabel.setText("A:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        inputPanel.add(aLabel, gridBagConstraints);

        variableA.setColumns(10);
        variableA.setName("Variable A");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        inputPanel.add(variableA, gridBagConstraints);

        bLabel.setText("B:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        inputPanel.add(bLabel, gridBagConstraints);

        variableB.setColumns(10);
        variableB.setName("Variable B");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        inputPanel.add(variableB, gridBagConstraints);

        cLabel.setText("C:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        inputPanel.add(cLabel, gridBagConstraints);

        variableC.setColumns(10);
        variableC.setName("Variable C");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        inputPanel.add(variableC, gridBagConstraints);

        xLabel.setText("X:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        inputPanel.add(xLabel, gridBagConstraints);

        variableX.setColumns(10);
        variableX.setName("Variable X");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        inputPanel.add(variableX, gridBagConstraints);

        yLabel.setText("Y:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        inputPanel.add(yLabel, gridBagConstraints);

        variableY.setColumns(10);
        variableY.setName("Variable Y");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        inputPanel.add(variableY, gridBagConstraints);

        zLabel.setText("Z:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        inputPanel.add(zLabel, gridBagConstraints);

        variableZ.setColumns(10);
        variableZ.setName("Variable Z");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        inputPanel.add(variableZ, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 5, 5);
        mainPanel.add(inputPanel, gridBagConstraints);

        takePictureButton.setText("Take Picture");
        takePictureButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                takePictureButtonMouseClicked(evt);
            }
        });

        presetPanel.add(takePictureButton);

        takePanoramaButton.setText("Take Panorama");
        takePanoramaButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                takePanoramaButtonMouseClicked(evt);
            }
        });

        presetPanel.add(takePanoramaButton);

        saveImageButton.setText("Save Image");
        saveImageButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                saveImageButtonMouseClicked(evt);
            }
        });

        presetPanel.add(saveImageButton);

        stopButton.setBackground(new java.awt.Color(252, 14, 14));
        stopButton.setText("Stop!");
        stopButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                stopButtonMouseClicked(evt);
            }
        });

        presetPanel.add(stopButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        mainPanel.add(presetPanel, gridBagConstraints);

        imageScrollPane.setPreferredSize(new java.awt.Dimension(324, 244));
        imageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageScrollPane.setViewportView(imageLabel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
        mainPanel.add(imageScrollPane, gridBagConstraints);

        scalePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 0));

        fullRadioButton.setText("Full resolution image");
        scalebuttonGroup.add(fullRadioButton);
        fullRadioButton.setContentAreaFilled(false);
        fullRadioButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fullRadioButtonMouseClicked(evt);
            }
        });

        scalePanel.add(fullRadioButton);

        scaledRadioButton.setSelected(true);
        scaledRadioButton.setText("Scaled image");
        scalebuttonGroup.add(scaledRadioButton);
        scaledRadioButton.setContentAreaFilled(false);
        scaledRadioButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                scaledRadioButtonMouseClicked(evt);
            }
        });

        scalePanel.add(scaledRadioButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
        mainPanel.add(scalePanel, gridBagConstraints);

        outputPanel.setBorder(new javax.swing.border.TitledBorder(null, "Rover Output", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        outputTextArea.setBackground(new java.awt.Color(204, 204, 204));
        outputTextArea.setColumns(75);
        outputTextArea.setEditable(false);
        outputTextArea.setLineWrap(true);
        outputTextArea.setRows(4);
        outputTextArea.setWrapStyleWord(true);
        outputScrollPane.setViewportView(outputTextArea);

        outputPanel.add(outputScrollPane);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        mainPanel.add(outputPanel, gridBagConstraints);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }//GEN-END:initComponents
    
    /** Enables or disables all the components in the given panel.
     *
     *@param b True to enable the panel and its components, false to
     * disable.
     */
    private void setEnabledPanel(javax.swing.JPanel panel, boolean b){
        int count = panel.getComponentCount();
        for (int i = 0; i < count; i++){
            panel.getComponent(i).setEnabled(b);
            try{
                setEnabledPanel((javax.swing.JPanel)panel.getComponent(i),b);
            }catch (Exception e){}
        }
    }
    
    /** Sets the background color for the GUI.
     */
    private void setBackgroundColor(Color c){
        mainPanel.setBackground(c);
        ipPanel.setBackground(c);
        presetPanel.setBackground(c);
        scalePanel.setBackground(c);
        
        commandPanel.setBackground(c);
        inputPanel.setBackground(c);
        outputPanel.setBackground(c);
    }
    
    /** Sets the background color for the description text fields.
     */
    private void setDescriptionColor(Color c){
        description1.setBackground(c);
        description2.setBackground(c);
        description3.setBackground(c);
        description4.setBackground(c);
        description5.setBackground(c);
        description6.setBackground(c);
        description7.setBackground(c);
        description8.setBackground(c);
        description9.setBackground(c);
        description10.setBackground(c);
        
        imageScrollPane.getViewport().setBackground(c);
        outputTextArea.setBackground(c);
    }
    
    /** Sets the background color of most of the buttons. The stop button's
     * color is not set.
     */
    private void setButtonColor(Color c){
        button1.setBackground(c);
        button2.setBackground(c);
        button3.setBackground(c);
        button4.setBackground(c);
        button5.setBackground(c);
        button6.setBackground(c);
        button7.setBackground(c);
        button8.setBackground(c);
        button9.setBackground(c);
        button10.setBackground(c);
        takePictureButton.setBackground(c);
        takePanoramaButton.setBackground(c);
        saveImageButton.setBackground(c);
        connectButton.setBackground(c);
        fullRadioButton.setBackground(c);
        scaledRadioButton.setBackground(c);
    }
    
    /** Sets the text color for those components that display text. Does not
     * set the text color for the IP or variable input fields.
     */
    private void setTextColor(Color c){
        button1.setForeground(c);
        button2.setForeground(c);
        button3.setForeground(c);
        button4.setForeground(c);
        button5.setForeground(c);
        button6.setForeground(c);
        button7.setForeground(c);
        button8.setForeground(c);
        button9.setForeground(c);
        button10.setForeground(c);
        takePictureButton.setForeground(c);
        takePanoramaButton.setForeground(c);
        saveImageButton.setForeground(c);
        connectButton.setForeground(c);
        fullRadioButton.setForeground(c);
        scaledRadioButton.setForeground(c);
        
        description1.setForeground(c);
        description2.setForeground(c);
        description3.setForeground(c);
        description4.setForeground(c);
        description5.setForeground(c);
        description6.setForeground(c);
        description7.setForeground(c);
        description8.setForeground(c);
        description9.setForeground(c);
        description10.setForeground(c);
        
        outputTextArea.setForeground(c);
        
        ipLabel.setForeground(c);
        aLabel.setForeground(c);
        bLabel.setForeground(c);
        cLabel.setForeground(c);
        xLabel.setForeground(c);
        yLabel.setForeground(c);
        zLabel.setForeground(c);
        
        ((TitledBorder)commandPanel.getBorder()).setTitleColor(c);
        ((TitledBorder)inputPanel.getBorder()).setTitleColor(c);
        ((TitledBorder)outputPanel.getBorder()).setTitleColor(c);
    }
    
    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
        stop();
        System.exit(0);
    }//GEN-LAST:event_exitForm
    
    /** If the current image is too large for the display area, displays
     * a scaled down version of the image. If the full resolution image
     * fits within the display area, the image will not be scaled.
     */
    private void scaledRadioButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_scaledRadioButtonMouseClicked
        if(!scaledRadioButton.isEnabled()) return;
        if(scaledImage != null)
            imageLabel.setIcon(new ImageIcon(scaledImage));
    }//GEN-LAST:event_scaledRadioButtonMouseClicked
    
    /** Displays the full high resolution version of the current image.
     */
    private void fullRadioButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fullRadioButtonMouseClicked
        if(!fullRadioButton.isEnabled()) return;
        if(fullImage != null)
            imageLabel.setIcon(new ImageIcon(fullImage));
    }//GEN-LAST:event_fullRadioButtonMouseClicked
    
    /** Takes a picture at the rovers current head position and displays it.
     * The UV light is off for this picture.
     */
    private void takePictureButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_takePictureButtonMouseClicked
        if(!takePictureButton.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                
                BufferedImage image = rov.takePicture(rov.state.getPan(), rov.state.getTilt(), 640, 480);
                
                if(image == null)
                    outputln("Error taking picture: "+ActionConstants.getErrorText(rov.state.getStatus()));
                else{
                    outputln("Picture complete");
                    displayImage(image);
                }
            }
        };
        
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
        
    }//GEN-LAST:event_takePictureButtonMouseClicked
    
    /** Takes a 360 degree panorama and displays it.
     */
    private void takePanoramaButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_takePanoramaButtonMouseClicked
        if(!takePanoramaButton.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                int angle = -43;
                TakePanoramaAction a = new TakePanoramaAction(angle,angle+50, 320, 240);
                
                if(!executeAction(a))
                    outputln("Error taking panorama: "+ActionConstants.getErrorText(a.getReturnValue()));
                else
                    outputln("Panorama complete");
            }
        };
        
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
        
    }//GEN-LAST:event_takePanoramaButtonMouseClicked
    
    /** Saves the current full resolution image to a file.
     */
    private void saveImageButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_saveImageButtonMouseClicked
        if(!saveImageButton.isEnabled()) return;
        if (fullImage == null){
            outputln("No image to save.");
            return;
        }
        
        //open a file chooser
        int retval = imageFileChooser.showSaveDialog(this);
        if(retval == JFileChooser.APPROVE_OPTION) {
            File f = imageFileChooser.getSelectedFile();
            //save the image
            if(rov.saveImageToDisk(f, fullImage))
                outputln("Image saved.");
        }
        
    }//GEN-LAST:event_saveImageButtonMouseClicked
    
    /** Kills any command that is currently executing.
     */
    private void stopButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_stopButtonMouseClicked
        if(!stopButton.isEnabled()) return;
        stop();
    }//GEN-LAST:event_stopButtonMouseClicked
    
    /** Kills any command that is currently executing.
     */
    private void stop(){
        if(currThread != null && currThread.isAlive()){
            outputln("Stopping command.");
            stop = true;
            currThread.interrupt();
        }else
            outputln("Nothing to stop.");
    }
    
    /** Attempts to connect to and initialize a rover with the IP address
     * entered by the user.
     */
    private void connectButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_connectButtonMouseClicked
        if(!connectButton.isEnabled()) return;
        if(initCommunications()){
            //enable other components on successful connect
            setEnabledPanel(commandPanel, true);
            setEnabledPanel(inputPanel, true);
            setEnabledPanel(presetPanel, true);
            setEnabledPanel(scalePanel, true);
        }
    }//GEN-LAST:event_connectButtonMouseClicked
    
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
        rov.saveIP(IP); //save the IP address so it can be loaded the next time the program is run
        
        if (success) {
            success = rov.initRobot();
            success = rov.refresh();
            if (success) {
                outputln("Connected");
                return true;
            } else
                outputln("Error: rover is not responding! Please check that the rover is on and ready, and then try again.");
        } else
            outputln("Error: could not connect to the rover! Check that you have entered the IP address correctly. Also, make sure the rover is on and ready.");
        return false;
    }
    
    /** Scales the given image so that it can be displayed in the imageScrollPane
     * without the need to scroll.
     *
     *@returns a scaled version of the image
     */
    private BufferedImage scaleImage(BufferedImage image){
        if(image == null)
            return null;
        //calculate scaling factors
        double scalex;
        double scaley;
        java.awt.Dimension panesize = imageScrollPane.getSize();
        if(image.getWidth() > panesize.width-4){
            scalex = (panesize.width-4)/(double)image.getWidth();
        }else
            scalex = 1.0;
        if(image.getHeight() > panesize.height-4){
            scaley = (panesize.height-4)/(double)image.getHeight();
        }else
            scaley = 1.0;
        scalex = scaley = Math.min(scalex,scaley);
        
        //scale image
        AffineTransform tx = new AffineTransform();
        tx.scale(scalex, scaley);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        return op.filter(image, null);
    }
    
    /** Displays the given image in full or scaled mode based on the current
     * radio button selection. <code>img</code> should be a full resolution
     * image. A scaled image will be created from <code>img</code>, and
     * the <code>fullImage</code> and <code>scaledImage</code> variables
     * will be set.
     *
     *@param img A full resolution image to be displayed
     */
    private void displayImage(BufferedImage img){
        if(img == null)
            return;
        //set variables and scale the image
        fullImage = img;
        scaledImage = scaleImage(fullImage);
        //display image
        if(fullRadioButton.isSelected())
            imageLabel.setIcon(new ImageIcon(fullImage));
        else{
            if(scaledImage == null)
                return;
            imageLabel.setIcon(new ImageIcon(scaledImage));
        }
    }
    
    /** Executes the given Action. If the Action is one that takes pictures,
     * the pictures will be displayed as the Action executes. This method
     * is blocking and does not return until the action is complete.
     * Returns true if and only if the action completes successfully.
     *
     *@returns true if the action completes successfully, false if there is
     * an error
     */
    private boolean executeAction(Action a){
        long last_update = 0;
        if(a == null)
            return false;
        
        Log.println("About to execute action: " + a.getSummary(), true);
        
        if (stop)
            return false;
        
        //start the action
        if (!a.doAction(rov))
            return false;
        
        //wait for the action to finish
        while (!stop && !a.isCompleted()){
            //if the action takes pictures, display the latest image
            if( last_update != a.getImageUpdateTime()){
                last_update = a.getImageUpdateTime();
                displayImage(a.getRecentImage());
            }
            try {
                Thread.sleep(10);
            } catch (java.lang.InterruptedException e) {
                outputln("executeAction interrupted");
            }
        }
        
        if (stop) {
            Log.println("Stopping the current action.");
            a.kill();
        }
        
        Log.println("Action completed (" + ActionConstants.getErrorText(a.getReturnValue()) + ").", true);
        return (!stop && a.isSuccess());
    }
    
    /** Returns the value of input <code>field</code> as an int or returns
     * Integer.MAX_VALUE if the value is empty or is not a valid integer.
     *
     *@see #inputIsValid
     */
    private int intInput(javax.swing.JTextField field){
        if(field == null){
            outputln("Error: not a valid text field.");
            valid = false;
            return Integer.MAX_VALUE;
        }
        try{
            valid = true;
            return Integer.parseInt(stringInput(field));
        }catch (java.lang.NumberFormatException e){
            //not a valid int
            valid = false;
            outputln("Error: "+field.getName() + " is not a valid int.");
            return Integer.MAX_VALUE;
        }
    }
    
    /** Returns the value of input <code>field</code> as a double or returns
     * Double.NaN if the value is empty or is not a valid double.
     *
     *@see #inputIsValid
     */
    private double doubleInput(javax.swing.JTextField field){
        if(field == null){
            outputln("Error: not a valid text field.");
            valid = false;
            return Double.NaN;
        }
        try{
            valid = true;
            return Double.parseDouble(stringInput(field));
        }catch (java.lang.NumberFormatException e){
            //not a valid double
            outputln("Error: "+field.getName() + " is not a valid double.");
            valid = false;
            return Double.NaN;
        }
    }
    
    /** Returns the value of input <code>field</code> as a long or returns
     * Long.MAX_VALUE if the value is empty or is not a valid long.
     *
     *@see #inputIsValid
     */
    private long longInput(javax.swing.JTextField field){
        if(field == null){
            outputln("Error: not a valid text field.");
            valid = false;
            return Long.MAX_VALUE;
        }
        try{
            valid = true;
            return Long.parseLong(stringInput(field));
        }catch (java.lang.NumberFormatException e){
            //not a valid long
            outputln("Error: "+field.getName() + " is not a valid long.");
            valid = false;
            return Long.MAX_VALUE;
        }
    }
    
    /** Returns the value of input <code>field</code> as a float or returns
     * Float.NaN if the value is empty or is not a valid float.
     *
     *@see #inputIsValid
     */
    private float floatInput(javax.swing.JTextField field){
        if(field == null){
            outputln("Error: not a valid text field.");
            valid = false;
            return Float.NaN;
        }
        try{
            valid = true;
            return Float.parseFloat(stringInput(field));
        }catch (java.lang.NumberFormatException e){
            //not a valid float
            outputln("Error: "+field.getName() + " is not a valid float.");
            valid = false;
            return Float.NaN;
        }
    }
    
    
    /** Returns the value of input <code>field</code> as a boolean or returns
     * false if the value is empty or is not a valid boolean.
     *
     *@see #inputIsValid
     */
    private boolean booleanInput(javax.swing.JTextField field){
        if(field == null){
            outputln("Error: not a valid text field.");
            valid = false;
            return false;
        }
        valid = true;
        String s = field.getText();
        if(s.equalsIgnoreCase("true"))
            return true;
        else if(s.equalsIgnoreCase("false"))
            return false;
        else{
            //not a valid boolean
            outputln("Error: "+field.getName() + " is not a valid boolean.");
            valid = false;
            return false;
        }
    }
    
    /** Returns the value of input <code>field</code> as a String. Returns
     * null if <code>field</code> is not a valid input text field. The string
     * can be the empty string.
     *
     *@see #inputIsValid
     */
    private String stringInput(javax.swing.JTextField field){
        if(field != null){
            valid = true;
            return field.getText();
        }else{
            outputln("Error: not a valid text field.");
            valid = false;
            return null;
        }
    }
    
    /** Returns whether or not the last input value checked by
     * <code>stringInput</code>, <code>intInput</code>, <code>booleanInput</code>
     * etc. was valid. This is a convenience method so that the validity
     * of an input value can be checked in the same way for each type of input.
     *
     *@return whether or not the last input value checked was valid
     */
    private boolean inputIsValid(){
        return valid;
    }
    
    /** Prints a line to the output text area. If the last line of
     * the output text field is displayed before printing,
     * the last line will be displayed after printing. If the
     * field has been scrolled to a position other than the bottom
     * of the field, the position will remain the same.
     * <p>
     * Another option is to print to the Rover.log file using
     * <code>Log.println</code>. Lines printed to Rover.log from Examples
     * will also be printed to the output text area.
     *
     *@param s The string to print
     *@see PER.rover.Log
     *@bug The automatic scrolling does not always work correctly.
     */
    private void outputln(String s){
        //add the new
        outputTextArea.append(s + "\n");
    }
    
    private void button1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button1MouseClicked
        if(!button1.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                command1();
            }
        };
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
    }//GEN-LAST:event_button1MouseClicked
    
    private void button2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button2MouseClicked
        if(!button2.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                command2();
            }
        };
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
    }//GEN-LAST:event_button2MouseClicked
    
    private void button3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button3MouseClicked
        if(!button3.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                command3();
            }
        };
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
    }//GEN-LAST:event_button3MouseClicked
    
    private void button4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button4MouseClicked
        if(!button4.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                command4();
            }
        };
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
    }//GEN-LAST:event_button4MouseClicked
    
    private void button5MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button5MouseClicked
        if(!button5.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                command5();
            }
        };
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
    }//GEN-LAST:event_button5MouseClicked
    
    private void button6MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button6MouseClicked
        if(!button6.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                command6();
            }
        };
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
    }//GEN-LAST:event_button6MouseClicked
    
    private void button7MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button7MouseClicked
        if(!button7.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                command7();
            }
        };
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
    }//GEN-LAST:event_button7MouseClicked
    
    private void button8MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button8MouseClicked
        if(!button8.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                command8();
            }
        };
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
    }//GEN-LAST:event_button8MouseClicked
    
    private void button9MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button9MouseClicked
        if(!button9.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                command9();
            }
        };
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
    }//GEN-LAST:event_button9MouseClicked
    
    private void button10MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button10MouseClicked
        if(!button10.isEnabled()) return;
        //check if another command is running
        if(currThread != null && currThread.isAlive()){
            outputln("Can not execute command. Another command is currently running.");
            return;
        }
        //create the thread that will run the command
        currThread = new Thread(){
            public void run(){
                stop = false;
                command10();
            }
        };
        //run the command
        try{
            currThread.start();
            outputln("Command started");
        }catch (java.lang.IllegalThreadStateException e){}
    }//GEN-LAST:event_button10MouseClicked
    
    /** Ensures that the command buttons are not overly large. If the buttons
     * were allowed to be too long, the GUI would not display properly.
     */
    private void limitButtonSizes(){
        int w = 130;
        int h = 26;
        pack();
        if(button1.getPreferredSize().getWidth() > w)
            button1.setPreferredSize(new java.awt.Dimension(w,h));
        if(button2.getPreferredSize().getWidth() > w)
            button2.setPreferredSize(new java.awt.Dimension(w,h));
        if(button3.getPreferredSize().getWidth() > w)
            button3.setPreferredSize(new java.awt.Dimension(w,h));
        if(button4.getPreferredSize().getWidth() > w)
            button4.setPreferredSize(new java.awt.Dimension(w,h));
        if(button5.getPreferredSize().getWidth() > w)
            button5.setPreferredSize(new java.awt.Dimension(w,h));
        if(button6.getPreferredSize().getWidth() > w)
            button6.setPreferredSize(new java.awt.Dimension(w,h));
        if(button7.getPreferredSize().getWidth() > w)
            button8.setPreferredSize(new java.awt.Dimension(w,h));
        if(button9.getPreferredSize().getWidth() > w)
            button9.setPreferredSize(new java.awt.Dimension(w,h));
        if(button10.getPreferredSize().getWidth() > w)
            button10.setPreferredSize(new java.awt.Dimension(w,h));
        if(button10.getPreferredSize().getWidth() > w)
            button10.setPreferredSize(new java.awt.Dimension(w,h));
        pack();
    }
    
    /** Sets the text of the command buttons one through ten. This is the text
     * that appears on the button so text should not be extremely long or
     * more than one line.
     */
    private void setButtonText(){
        //Customize the button text by editing the strings below:
        button1.setText("Dance");
        button2.setText("Leash");
        button3.setText("Red Light Green Light");
        button4.setText("Wall Follow");
        button5.setText("Drive and turn head");
        button6.setText("Time Lapse");
        button7.setText("Button7");
        button8.setText("Button8");
        button9.setText("Button9");
        button10.setText("Smart Wander");
        limitButtonSizes();
    }
    
    /** Sets the commands' description text. This is the text that appears
     * beside each command button. This is a good place to not only
     * describe the command, but also say which variables it uses.
     * <p>
     * Text will wrap automatically so you do not need to worry about line
     * length. The description text areas are contained within jScrollPanes
     * so there is not a size limit for the descriptions, although, only
     * two lines will be visible at a time.
     */
    private void setDescriptions(){
        //Customize the command descriptions by editing the strings below:
        description1.setText("Put # of seconds to dance in A");
        description2.setText("The rover will try to stay 30cm away from an object held in front of it. Hit Stop to end.");
        description3.setText("Needs Stargate code version 2.0.0. Moves forward when looking at a green object. Stops when looking at a red object.");
        description4.setText("Needs Stargate code version 2.0.0. Put # cm to drive in A.");
        description5.setText("Drive 50cm and simultaneously turn the head to pan -90, til 45. Example of doing two actions at once.");
        description6.setText("Take time lapse pictures until the Stop button is hit.");
        description7.setText("");
        description8.setText("");
        description9.setText("");
        description10.setText("Wander for C seconds. (Put the number of seconds to drive in C.)");
    }
    
    private void command1(){
        //get the distance entered by the user in the A variable field
        int seconds = intInput(variableA);
        DanceAction da = new DanceAction(seconds);
        
        //check that the input is valid
        if(inputIsValid()){
            da.doAction(rov);
            while(!da.isCompleted()){
                try{
                    Thread.sleep(10);
                }catch(java.lang.InterruptedException e){
                    da.kill();
                }
            }
        } else
            System.err.println("Input is invalid");
    }
    
    /** A simple leash. */
    private void command2(){
        rov.look(0, 0);
        int cm = 0;
        
        while(!stop){
            rov.refresh();
            cm = rov.state.getRangeCM();
            //System.err.println("range is " + cm + " cm");
            //outputln("range is " + cm + " cm");
            
            if(cm > 35 && cm < 100){
                //move forward
                rov.quadTurn(255,0);
            }else if(cm < 30){
                //move backwards
                rov.quadTurn(-255,0);
            }else{
                //stop
                rov.quadTurn(0,0);
            }
            
            try{
                Thread.sleep(10);
            }catch(java.lang.InterruptedException e){}
        }
        //stop when the stop button is pushed
        rov.quadTurn(0,0);
    }
    
    /** red light green light using getMean and a rgb transform */
    private void command3(){
        int[] rgbcolors = new int[3];
        
        if(!rov.stopStreaming())
            System.err.println("stopStream failed");
        rov.getMean(true);
        rov.look(0, -20);
        
        while(!stop){
            rov.state.getStatus();
            rgbcolors = YUVToRGB(rov.receive.meanY, rov.receive.meanU, rov.receive.meanV);
            System.err.println("red" + rgbcolors[0] + " green " + rgbcolors[1] + " blue " + rgbcolors[2]);
            if((rgbcolors[0] + 25) > rgbcolors[1]){
                System.err.println("redder");
                rov.crab(0, 0);
            }
            else{
                System.err.println("greener");
                rov.crab(200, 0);
            }
        }
    }
    
    private static int[] YUVToRGB(int Y, int U, int V){
        int[] rgb = new int[3];
        rgb[0] = (int)(1.164 * (Y - 16) + 1.596 * (V - 128));
        rgb[1] = (int)(1.164 * (Y - 16) - .813 * (V - 128) - .391 * (U - 128));
        rgb[2] = (int)(1.164 * (Y - 16) + 2.018 * (U - 128));
        return rgb;
    }
    
    /** The task associated with button4. */
    private void command4(){
        
        //get the distance entered by the user in the X variable field
        int dist = intInput(variableA);
        //check that the input is valid
        if(inputIsValid()){
            WallFollowAction wfa = new WallFollowAction(dist);
            wfa.doAction(rov);
            
            while(!wfa.isCompleted()){
                try{
                    Thread.sleep(10);
                }catch(java.lang.InterruptedException e){
                    wfa.kill();
                }
            }
        } else
            System.err.println("Input is invalid");
        
    }
    
    /** The task associated with button5. */
    private void command5(){
        //Add your code here:
        
        //sample - do two actions at once
        Action driveAct = new DriveToAction(50,0,false);
        Action turnAct = new TurnHeadAction(-90,45);
        
        driveAct.doAction(rov);
        turnAct.doAction(rov);
        
        while(!driveAct.isCompleted() || !turnAct.isCompleted()){
            try{
                Thread.sleep(10);
            }catch(java.lang.InterruptedException e){
                //stop the actions if the Stop button was hit
                driveAct.kill();
                turnAct.kill();
            }
        }
        
        if(!driveAct.isSuccess())
            outputln("Drive failed: "+ActionConstants.getErrorText(driveAct.getReturnValue()));
        else outputln("Drive success");
        if(!turnAct.isSuccess())
            outputln("Turn failed: "+ActionConstants.getErrorText(turnAct.getReturnValue()));
        else outputln("Turn success");
        //end sample
        
    }
    
    /** The task associated with button6. */
    private void command6(){
        
        int fwd = 0;
        int ang = 0;
        boolean safety = true; //NO STOP
        
        System.out.println(" is called");
        System.out.println("MAAPER:maaperDriveToActionWithWait : " + " fwd=" + fwd + "ang=" + ang);
        
        DriveToAction m_oDriveAction;
        
        m_oDriveAction = new DriveToAction(fwd, ang, safety);
        
        m_oDriveAction.doAction(rov);
        
        while (!m_oDriveAction.isCompleted()) {
            try {
                Thread.sleep(20);
            } catch (Exception e) {}
        }
        
        //-##############--- POSSIBLE BUG-------
        // if you move -XXX cm, it is fine
        // if you move +XXX cm,  it returns not success but status is "Success";most of the time
        //-#################-------------------------------------------
        if (!m_oDriveAction.isSuccess()) {
            System.out.println("MAAPER:maaperDriveToActionWithWait " + ActionConstants.getErrorText(rov.state.getStatus()));
        }
        //----end bug
        
        double[] pos = rov.highLevelState.getPosition();
        System.out.println("*****POSITION  x= " + pos[0] + "   y= " + pos[1] + "   t= " + pos[2]);
        System.out.println("MAAPER:maaperDriveToActionWithWait " + ActionConstants.getErrorText(rov.state.getStatus()));
        
        
    }
    
    /** The task associated with button7. */
    private void command7(){
        Action driveAct;
        Action turnAct;
        double position [] = new double[3];
        
        driveAct = new DriveToAction(0);
        turnAct = new TurnToAction(5, false);
        
        for(int i = 0; i < 3; i++){
            driveAct.doAction(rov);
            
            while(!driveAct.isCompleted()){
                try{
                    Thread.sleep(10);
                }catch(java.lang.InterruptedException e){
                    driveAct.kill();
                }
            }
            
            if(!driveAct.isSuccess())
                outputln("Drive failed: "+ActionConstants.getErrorText(driveAct.getReturnValue()));
            else outputln("Drive success");
            
            position = rov.highLevelState.getPosition();
            System.err.println("After drive, position is x:" + position[0] + " y: " + position[1] + " theta: " + position[2]);
            
            turnAct.doAction(rov);
            while(!turnAct.isCompleted()){
                try{
                    Thread.sleep(10);
                }catch(java.lang.InterruptedException e){
                    driveAct.kill();
                }
            }
            if(!turnAct.isSuccess())
                outputln("Turn failed: "+ActionConstants.getErrorText(turnAct.getReturnValue()));
            else outputln("Turn success");
            
            position = rov.highLevelState.getPosition();
            System.err.println("After turn, position is x:" + position[0] + " y: " + position[1] + " theta: " + position[2]);
        }
    }
    
    /** The task associated with button8. */
    private void command8(){
        int fwd = 100;
        int ang = 30;
        boolean safety = true;
        
        DriveToAction m_oDriveAction;
        
        m_oDriveAction = new DriveToAction(fwd, ang, safety);
        m_oDriveAction.doAction(rov);
        
        while (!m_oDriveAction.isCompleted()) {
            try {
                Thread.sleep(20);
                if (false) {
                    System.out.println("Move terminated");
                    m_oDriveAction.kill();
                    break;
                }
                
            } catch (Exception e) {}
            
            //---------DISPLAY X Y--
            double[] pos = rov.highLevelState.getPosition();
            System.out.println("position  x= " + pos[0] + "   y= " + pos[1] + "   t= " + pos[2]);
            
        }//end while
        System.out.println("MAAPER:maaperDriveToActionWithWait " + ActionConstants.getErrorText(rov.state.getStatus()));
    }
    
    /** The task associated with button9. */
    private void command9(){
        MoveToAction moveTest1 = new MoveToAction(60, 60, 0);
        MoveToAction moveTest2 = new MoveToAction(30, 0, 0);
        MoveToAction moveTest3 = new MoveToAction(120, 0, 0);
        MoveToAction moveTest4 = new MoveToAction(0, 0, 0);
        
        moveTest1.doAction(rov);
        
        while (!moveTest1.isCompleted()) {
            try {
                Thread.sleep(20);
                if (false) {
                    System.out.println("Move terminated");
                    moveTest1.kill();
                    break;
                }
                
            } catch (Exception e) {}
        }
        //---------DISPLAY X Y--
        if(moveTest1.isSuccess()){
            double[] pos = rov.highLevelState.getPosition();
            System.out.println("First move completed, position  x= " + pos[0] + "   y= " + pos[1] + "   t= " + pos[2]);
        }else System.out.println("movetest failed.  how peculiar");
        
        moveTest2.doAction(rov);
        
        while (!moveTest2.isCompleted()) {
            try {
                Thread.sleep(20);
                if (false) {
                    System.out.println("Move terminated");
                    moveTest1.kill();
                    break;
                }
                
            } catch (Exception e) {}
        }
        //---------DISPLAY X Y--
        if(moveTest2.isSuccess()){
            double[] pos2 = rov.highLevelState.getPosition();
            System.out.println("Second move completed? position  x= " + pos2[0] + "   y= " + pos2[1] + "   t= " + pos2[2]);
        }else System.out.println("movetest failed.  how peculiar");
        
        moveTest3.doAction(rov);
        
        while (!moveTest3.isCompleted()) {
            try {
                Thread.sleep(20);
                if (false) {
                    System.out.println("Move terminated");
                    moveTest1.kill();
                    break;
                }
                
            } catch (Exception e) {}
        }
        //---------DISPLAY X Y--
        if(moveTest3.isSuccess()){
            double[] pos3 = rov.highLevelState.getPosition();
            System.out.println("Third move completed? position  x= " + pos3[0] + "   y= " + pos3[1] + "   t= " + pos3[2]);
        }else System.out.println("movetest failed.  how peculiar");
        
        //       moveTest4.doAction(rov);
        
        while (!moveTest4.isCompleted()) {
            try {
                Thread.sleep(20);
                if (false) {
                    System.out.println("Move terminated");
                    moveTest1.kill();
                    break;
                }
                
            } catch (Exception e) {}
        }
        //---------DISPLAY X Y--
        if(moveTest4.isSuccess()){
            double[] pos4 = rov.highLevelState.getPosition();
            System.out.println("Fourth move completed? position  x= " + pos4[0] + "   y= " + pos4[1] + "   t= " + pos4[2]);
        }else System.out.println("movetest failed.  how peculiar");
        
        
    }
    
    /** The task associated with button10. */
    private void command10(){
        //Add your code here:
        
        //sample - smart wander displaying pictures
        int secs = intInput(variableC);
        if(!inputIsValid())
            return;
        
        SmartWanderAction a = new SmartWanderAction(secs);
        executeAction(a);
        //end sample
        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new Examples(new Rover()).show();
        //make sure topLevelDir exists
        File f = new File(Rover.topLevelDir);
        if(!f.exists())
            f.mkdirs();
        //init log file
        Log.initLogFile("Rover.Log",outputTextArea);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel aLabel;
    private javax.swing.JLabel bLabel;
    private javax.swing.JButton button1;
    private javax.swing.JButton button10;
    private javax.swing.JButton button2;
    private javax.swing.JButton button3;
    private javax.swing.JButton button4;
    private javax.swing.JButton button5;
    private javax.swing.JButton button6;
    private javax.swing.JButton button7;
    private javax.swing.JButton button8;
    private javax.swing.JButton button9;
    private javax.swing.JLabel cLabel;
    private javax.swing.JPanel commandPanel;
    private javax.swing.JButton connectButton;
    private javax.swing.JTextArea description1;
    private javax.swing.JTextArea description10;
    private javax.swing.JTextArea description2;
    private javax.swing.JTextArea description3;
    private javax.swing.JTextArea description4;
    private javax.swing.JTextArea description5;
    private javax.swing.JTextArea description6;
    private javax.swing.JTextArea description7;
    private javax.swing.JTextArea description8;
    private javax.swing.JTextArea description9;
    private javax.swing.JRadioButton fullRadioButton;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JScrollPane imageScrollPane;
    private javax.swing.JPanel inputPanel;
    private javax.swing.JLabel ipLabel;
    private javax.swing.JPanel ipPanel;
    private javax.swing.JTextField ipTextField;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel outputPanel;
    private javax.swing.JScrollPane outputScrollPane;
    private static javax.swing.JTextArea outputTextArea;
    private javax.swing.JPanel presetPanel;
    private javax.swing.JButton saveImageButton;
    private javax.swing.JPanel scalePanel;
    private javax.swing.ButtonGroup scalebuttonGroup;
    private javax.swing.JRadioButton scaledRadioButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JButton takePanoramaButton;
    private javax.swing.JButton takePictureButton;
    private javax.swing.JTextField variableA;
    private javax.swing.JTextField variableB;
    private javax.swing.JTextField variableC;
    private javax.swing.JTextField variableX;
    private javax.swing.JTextField variableY;
    private javax.swing.JTextField variableZ;
    private javax.swing.JLabel xLabel;
    private javax.swing.JLabel yLabel;
    private javax.swing.JLabel zLabel;
    // End of variables declaration//GEN-END:variables
    
}
