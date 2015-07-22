// This is Version 3 of cereb 
/* The following code was written by Eric Porter, eporter@andrew.cmu.edu
   I've changed some of the funcitons in cerebExhibit.c, so you need that exact file.
   Here is a summary of the protocol for communication with the Stayton:
   The servos are labeled clockwise, from the front left steering servo.

   Packet format for sending to cerebellum:
   Byte 0:  'p' //for Porter
   Byte 1:  'G' //for Gockley
   Byte 2:  'h' //for Hamner
   Byte 3:  code for motor 0
   Byte 4:  code for motor 1
   Byte 5:  the position of steering servo 0 (Front left)
   Byte 6:  the position of steering servo 1 (Front right)
   Byte 7: the position of steering servo 2  (Back right) 
   Byte 8: the position of steering servo 3  (Back left)
   Byte 9: the position of the pan servo
   Byte 10: the position of the tilt servo
   Byte 11: 1 if turning light on, 0 to turn it off
   Byte 12: checksum of bytes 3-11
   Byte 13: 'N' //for Nourbakhsh

   Packet format from cerebellum to Stargate:
   Byte 0: status
   Byte 1: range
   Byte 2: voltage
   Byte 3: checksum = (status+range+voltage) mod 256

   All of these bytes must be received in less than 5ms, or an error is returned.
   The cerebellum will time out and stop everything after about 120ms.
   If the cerebellum received all 13 bytes and it is a valid packet, the status will be 0.
   If the cerebellum times out on the receive, the status will be 1.
   If the 'pGh' and 'N' are not in the proper place, the status will be 2.
   If the checksum fails, the status will be 3.
   The cerebellum will not respond until it has gotten at least one valid packet.  The reason for this
   is that the Stayton sends out a lot of stuff while booting up and a response from the cerebellum
   interrupts the boot sequence.  Game over.

   The motor positions require a code.  0 means stopped.  0-127 go forward at a speed of 128 plus the code.
   A code of greater than 128 sends the motors backwards at that speed.  Note that you cann't control the motors 
   with a duty cycle in the range of 0-127.  That is because the motors don't move at that speed.

   During normal operation, the greed LED will flash once per second and be lit for half the time.  
   The yellow LED will only light if the cerebellum received a bad packet.
	
************************ WIRING INFORMATION **********************************
	Connect the motors from the right side to motor output '1' with ground to the right of power
	Connect the motors from the left side to motor output '2' with ground to the right of power
	Connect steering servo 0 to digital output 0.
	Connect steering servo 1 to digital output 1.
	Connect steering servo 2 to digital output 2.
	Connect steering servo 3 to digital output 3.
	Connect the pan servo to digital output 4.
	Connect the tilt servo to digital output 5.
	Connect the UV relay to E0.
	Connect the IR to to analog input 1.
	Connect the battery voltage line E1.
*/


#include "cerebExhibit.h" // load declarations
#include "cerebExhibit.c" // load useful cerebellum functions

char place = 0;    //the place in the array to store the next byte I receive
char gotValid = 0; //set to 1 once I get a valid packet; then I will respond to commands
int timeout = 0, sleepTimeout=0;
char range=0, voltage=0;

void sendReply(char status) {
	if(status == 0) {	
		clear_bit(PORTB, YELLOW); //no yellow means last read worked
		gotValid = 1;             //I have a valid packet, now start responding
		sleepTimeout = 0;         //don't sleep the motors for a little while
	}else 
		set_bit(PORTB, YELLOW);   //yellow means last read failed

	if(gotValid) {			
		voltage = readADC(5); //read from ir
		delay_ms(1);
		range = readADC(1); //read battery voltage
		write(status);	
		write(range);
		write(voltage);
		write(status+range+voltage); //checksum
	}
	place = 0;         //reset the array
	timeout = 0;
}

//the main loop that handles communication with the Stayton
//a description of the protocol is at the top of the file.
void roverLoop() { 
	char arr[14];//the array to hold the command send from the Stayton
	char greenOn = 0, check, i;
	int gFlash = 0;
	TRISE = 1; //make it so that I can read the battery voltage and control the uv
	PORTE |= 2; //UV off
	while(1) {
		if(readNB()) { //it failed!  - Nothing new from serial port
			if(place > 0) {
				if(timeout++ > 245) {
					sendReply(1); // sending 1 over serial means timeout.
					place = 0;
				}
			}
		}else { //the read worked
			arr[place++] = byteNB;
			if(place == 14) {
				//check for valid packet
				if(arr[0]!='p' || arr[1]!='G' || arr[2]!='h' || arr[13]!='N') {
					sendReply(2); // sending 2 over serial means invalid.
					continue;
				}
				check = 0;
				for(i=3; i<12; i++)
					check = check + arr[i];
				if(check != arr[12]) {
					sendReply(3); // sending 3 over serial means bad checksum.
					continue;
				}

				sendReply(0); // sending 0 over serial means success.
				
				if(arr[3] == 0)
					setVel(0, 0, 0);
				else if(arr[3] < 128)
					setVel(0, 0, arr[3] + 128);
				else 
					setVel(0, 1, arr[3]);
				if(arr[4] == 0)
					setVel(1, 0, 0);
				else if(arr[4] < 128)
					setVel(1, 0, arr[4] + 128);
				else 
					setVel(1, 1, arr[4]);
				servo[0] = arr[5];
				servo[1] = arr[6];
				servo[2] = arr[7];
				servo[3] = arr[8];
				servo[4] = arr[9];
				servo[5] = arr[10];
				if(arr[11]) //turn light off
					PORTE |= 2;
				else //turn light on
					PORTE &= 253;
			}
		}
		//this loop is just to let me know the cerebellum is working
		if(gFlash++ == 24500) { //flashed the light once per second
			if(greenOn == 0) 
				set_bit(PORTB, GREEN);
			else
				clear_bit(PORTB, GREEN);
			greenOn = 1-greenOn;
			gFlash = 0;
		}
		

		if(sleepTimeout++ == 6000) { //haven't heard anything for 120 ms
			servo[0] = 0;
			servo[1] = 0;
			servo[2] = 0;
			servo[3] = 0;
			servo[4] = 0;
			servo[5] = 0;
			setVel(0, 0, 0); 
			setVel(1, 0, 0);	
		}
	}	
}

void main(void)
{				
	init_cerebellum(); // configure cerebellum
	ser_init(SER_115200); // start up serial port handling
	servo_init(); // start up servo control
	set_bit(INTCON, GIE); // start up interrupts- required for servo control
	servo_mask = 111111b; // set up D0-D5 pins for servo control
	ADCON1 = 0; // initialize analog-digital converter //
	pwm_init(); // initialize pwm motor output
	
	roverLoop();	
}
