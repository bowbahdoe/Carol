package PER.rover;

/*
 * SendEmailAction.java
 *
 * Created on December 3, 2002, 4:28 PM
 */

import java.io.*;
import sun.net.smtp.SmtpClient;

/**
 * Allows the rover to send email messages.
 *
 * @author  Rachel Gockley
 */
public class SendEmailAction implements Action {
    transient private SmtpClient smtp;
    private String mailto;
    private String subject;
    private String message;
    
    private String mailfrom = "donotreply@cmu.edu";
    private String mailfromName = "Rover";
    private String serverName = "smtp.andrew.cmu.edu";
    
    private boolean messageSent;
    
    /** Creates a new instance of SendEmailAction with an empty message and
     * the subject "Message from Rover!". The recipient is empty and should
     * be set using setRecipient. Uses the default sender name of "Rover",
     * default from address of donotreply@cmu.edu, and default server of
     * smtp.andrew.cmu.edu.
     *
     *@see #setRecipient
     */
    public SendEmailAction() {
        mailto = "";
        subject = "Message from " + mailfromName + "!";
        message = "";
        messageSent = false;
    }
    
    /** Creates a new instance of SendEmailAction with the subject "Message
     * from Rover!". Uses the default sender name of "Rover", default from
     * address of donotreply@cmu.edu, and default server of smtp.andrew.cmu.edu.
     */
    public SendEmailAction(String recip, String msg) {
        super();
        mailto = recip;
        message = msg;
    }
    
    /** Creates a new instance of SendEmailAction. Uses the default sender
     * name of "Rover", default from address of donotreply@cmu.edu,
     * and default server of smtp.andrew.cmu.edu.
     */
    public SendEmailAction(String recip, String msg, String subj) {
        super();
        mailto = recip;
        message = msg;
        subject = subj;
    }
    
    /** Sets the message recipient to <code> recip </code>.
     */
    public void setRecipient(String recip) {
        mailto = recip;
    }
    
    /** Sets the email's message to <code> msg </code>.
     */
    public void setMessage(String msg) {
        message = msg;
    }
    
    /** Sets the email's subject to <code> subj </code>.
     */
    public void setSubject(String subj) {
        subject = subj;
    }
    
    /** Sets the SMTP server to be used when sending the email message.
     */
    public void setServer(String SMTP) {
        serverName = SMTP;
    }
    
    /** Sets the name of the email sender.
     */
    public void setFrom(String roverName) {
        mailfromName = roverName;
    }
    
    public boolean doAction(Rover r) {
        PER.rover.StatsLog.println(PER.rover.StatsLog.SEND_EMAIL);
        try {
            smtp = new SmtpClient(serverName);
            
            // Sets the originating e-mail address
            smtp.from(mailfrom);
            
            // Sets the recipients' e-mail address
            smtp.to(mailto);
            
            // Create an output stream to the connection
            PrintStream msg = smtp.startMessage();
            
            msg.println("To: " + mailto); // so mailers will display the recipient's e-mail address
            msg.println("From: " + mailfromName + " <" + mailfrom + ">"); // so that mailers will display the sender's e-mail address
            msg.println("Subject: " + subject);
            msg.println(message);
            
            // Close the connection to the SMTP server and send the message out to the recipient
            smtp.closeServer();
            
            messageSent = true;
            return true;
        } catch (IOException e) {
            messageSent = false;
            return false;
        }
    }
    
    public int getReturnValue() {
        if (messageSent)
            return 0;
        else{
            return ActionConstants.SMTP_FAILED;
        }
    }
    
    public String getShortSummary() {
        return "Send an email.";
    }
    
    public String getSummary() {
        return "Send an email to <" + mailto + ">, with subject \"" + subject + "\".";
    }
    
    /** How long the action will take, in milliseconds. This action always returns
     * a time of zero.
     */
    public int getTime() {
        return 0;
    }
    
    public boolean isSuccess() {
        return messageSent;
    }
    
    public boolean isCompleted() {
        return true;
    }
    
    /** Emergency stop - end the action immediately, if it's running. Not currently
     * implemented for this action.
     */
    public void kill() {
    }
    
    /** How much time until the action finishes (in milliseconds), if it has already
     * started. This action always returns a time remaining of zero.
     */
    public int getTimeRemaining() {
        return 0;
    }
    
    public long getImageUpdateTime() {
        return 0;
    }
    
    public java.awt.image.BufferedImage getRecentImage() {
        return null;
    }
    
}
