/* Version Information:

April 25, 2003: now version 3- this has a change in 
ser_rcv_nonblock so that it deals with overflows and framing errors
correctly-- the old nonblock failed when overflows occurred, freezing up
the cerebellum on blocking serial receives thereafter

This is Version 2, so it's for the new Cerebellum in which Btn1 is connected to B0
and the motor direction is now B1 instead of B0.

I'VE CHANGED MANY OF THE FUNCTION NAMES BECAUSE I HATE TYPING THE UNDERSCORE AND
ALL OF THE LONG AND CONFUSING NAMES. - Eric Porter

*/

/* Cereb.c is the main helper file for Cerebellum Programming. 
   See the API for details of how to use this stuff.
	Much of original code is courtesy of Kwanjee Ng
	Illah Nourbakhsh 2002 ; 2003
*/

/* things to add later -illah- :



*/

// API provided for cerebv2 //////////////////////
//
// call init_cerebellum() first in your main
// turn on an led with set_bit(PORTB, (GREEN|YELLOW)); turn off with clear_bit
// (PORTB & BTNn) is true if button n is depressed and false otherwise
//
// To output on a digital pin on portD, use set_bit(PORTD, n); where n is 0-7 (and clear_bit)
// These are the pins to generally use for digital output.
// But if you're using the Servoes, you will have to set the mask for the servoes
// appropriately so that you can preserve some of the pins for your own digital out
// purposes.  Read the Servoes section below.
// 
// call ser_init(SER_115200) to set up hardware serial port
// write(char c) sends a character out over the serial line
// writeString(const char *text) writes a CONST string out the serial line
// writeChar(char c) writes out a character in decimal (good for debugging!)
// read() is a blocking receive char call that returns a char
//		note that if the input buffer has overflowed it dumps the buffer
//		and continues waiting for a new character to return!
// readNB() is a nonblocking receive char call that returns 0 if data is available
// and 1 if no data is available.  If it returned 0, check the variable byteNB for
// the data that just came in.
// to save memory, if not using the serial port, don't call ser_init() and then
// don't use any of the above functions.
// 
// to use servoes, begin by calling servo_init() in your main.
// then before expecting servoes to get used make servo_state=1;
// servoes use interrupts, so make sure you're starting interrupts:
//		set_bit(INTCON, GIE);
// then command a servo by commanding servo[n]=p. This will drive
// the servo on port Dn to position p.  If p is zero, it deactivates the servo.
// to save memory, if not using servoes don't call servo_init(), do not
// put "set_bit(INTCON, GIE);" in your code, and also
// go to cereb.c and find void interrupt(void) and comment out the line that
// calls do_servo().
//
// to use some servoes and other portD lines for direct digital outputs, note
// the variable servo_mask.  This defaults to 11111111b, which means all pins
// on portD are configured for servo output use.  If you want to have D0,D1
// and D3 be servo pins and the rest controlled directly by you, using set_bit,
// then in Main() you would include the line: "servo_mask = 00000111b;"
// Then the servo controller would ignore pins D3 - D7 and you can set them
// as desired. For example you could set pin3 to be high: "set_bit(PORTD,3);"
// Note the tristate is still configuring ALL of portD for digital output (not input).
//
// to use the analog-digital input lines, first in main you must initialize
// with the command: ADCON1=0;
// to read analog input channel ch, use adc_read(ch) which returns a char
// ch should be 0-7 inclusive. numbering on cerebellum goes from A0 as channel
// zero, left-to-right and top-to-bottom, so that E2 is analog channel 7
//
// to use the motor PWM outputs, first call pwm_init() in your code.
// if both commands are in there, call ser_init() first. The motor command
// is pwm_setvel8(n,dir,duty) where n=0 or 1 (0 means Motor1 on Cerebellum
// and 1 means Motor2 on Cerebellum). dir is direction (0 or 1) and duty is
// a char, with 0 meaning off and 255 meaning full-on.
// To save memory, if you are not using motor PWM then comment out pwm_init() in
// main().
// // 
////////////////////
// 

// Below code is for implementing the hardware serial port //
char ser_tmp;
char SSPCON@0x14;
char PIR1@0x0c;
char PIE1@0x8c;
char TRISC@0x87;
char TRISD@0x88;
char TXSTA@0x98;
char RCSTA@0x18;
char SPBRG@0x99;
char RCREG@0x1a;
char TXREG@0x19;
char PORTD@0x08;
char PORTE@0x09;
char TRISE@0x89;
char myOption_Reg@0x81;

// call ser_init to initial hardware serial as: ser_init(SER_115200) //
void ser_init(char spbrg_val) {
	
	SSPCON = 0;
	PIR1 = 0;  // this ensures also RCIF = TXIF = 0;
	clear_bit(PIE1,5); //RCIE = 0;
	clear_bit(PIE1,4); //TXIE = 0;
	TRISC = TRISC | 0xC0; // setup TRISC for USART use
	SPBRG = spbrg_val;
    	TXSTA = 000100100b; // txen, brgh
	RCSTA = 010010000b; // spen and cren, the rest all off
	ser_tmp = RCREG; // flush the rx buffer
	ser_tmp = RCREG;
	ser_tmp = RCREG;
}

// low-level call to send a character serially.
void write(char c) {

	//wait for txif to go hi
	while (!(PIR1 & 16)) ; //while (!TXIF);
	//disable_interrupt(GIE); //?
	TXREG = c;
	//enable_interrupt(GIE);  //?
}

void writeString(const char* text) {
    char i = 0;
    while( text[i] != 0 )
        write( text[i++] ); 
}

// writes a character out the serial port as a 3 digit number
void writeChar(char data) {
    write( '0' + data/100 );
    write( '0' + (data%100)/10 );
    write( '0' + data%10 );
}

// this is a blocking receive //
char read(void) {
	while (1) {
		if (RCSTA & 2) { // RCSTA bit 1 is overflow error
			// overflow error
			clear_bit(RCSTA,CREN); // CREN is RCStatus bit 4 //
			ser_tmp = RCREG; // flush the rx buffer
			ser_tmp = RCREG;
			ser_tmp = RCREG;
			set_bit(RCSTA,CREN); // CREN = 1;
		}
		else if (RCSTA & 4) { // RCSTA bit 2 is framing error
			// framing error
			ser_tmp = RCREG;
		}
		else if (PIR1 & 32) { // PIR1 bit 5 is RCIF
			ser_tmp = RCREG;
			return ser_tmp;
		}
	}
}

char byteNB;

// nonblocking receive returns 0 for success, 1 if nothing to read.
// if the read worked, byteNB is set to the byte that was read in.
// (yes, this is a stupid way to do it, but pointers can't be passed
// as arguments in this compiler.)  This way, you can read in 0!
// you need something connected to the serial port, or else the cerebellum
// will block.  About 50000 of these commands will run each second
char readNB(void) {
	int problem = 1;
	while (problem == 1) {
		problem = 0;
		if (RCSTA & 2) { // RCSTA bit 1 is overflow error
			// overflow error
			clear_bit(RCSTA,CREN); // CREN is RCStatus bit 4 //
			ser_tmp = RCREG; // flush the rx buffer
			ser_tmp = RCREG;
			ser_tmp = RCREG;
			set_bit(RCSTA,CREN); // CREN = 1;
			problem = 1;
		}
		else if (RCSTA & 4) { // RCSTA bit 2 is framing error
			// framing error
			ser_tmp = RCREG;
			problem = 1;
		}
		else if (PIR1 & 32) {
			ser_tmp = RCREG;
			byteNB = RCREG;
			return 0;
		}
	} // end while (problem == 1) //
	return 1; // no byte to receive and no errors; return zero
}

// this next section is for servo control /////////////

char servo_state = 1;
char servo_curr = 0;
char servoBuf; //buffer the value of the servo so a sudden shutoff won't jerk servo
char servo_mask = 11111111b; // default that all of portD will be
					// used for servo operations rather 
					// than direct digital output twiddling
char servo_switch = 1;
char servo[8] = {0,0,0,0,0,0,0,0}; //holds desired servo positions
char servoLast[8] = {0,0,0,0,0,0,0,0}; //holds the positions that I'm currently commanding the servos to.
char _servo_free = 0;

void servo_init(void) {
    OPTION_REG = 132; // bit 7 = 1 for portB pull-up and bit2 is for prescaler
				// TMR0 @ 1:32, prescaler to timer0 (for servo control)
    TRISD = 0; // port D as outputs	for servo commands!
	// in fact we already do this in init_cerebellum(); we do it again for //
	// redundancy only here //
    set_bit(INTCON, T0IE); //T0IE = 1 ie enable TMR0 overflow bit - T0IE is bit 5
    delay_ms(1);   
}

//limit the servos to only move 4 positions per cycle
char profileServo(char which) {
	char diff;
	if(servoLast[which] == 0)  //no known position
		servoLast[which] = servo[which];
	else if(servo[which] == 0) //make it alseep
		return 0;
	else if(servoLast[which] > servo[which]) {
		diff = servoLast[which] - servo[which];
		if(diff < 3)
			servoLast[which] = servo[which];
		else if(diff > 8)
			servoLast[which] = servoLast[which] - 4;
		else
			servoLast[which] = servoLast[which] - (diff >> 1);
	}else if(servoLast[which] < servo[which]) {
		diff = servo[which] - servoLast[which];
		if(diff < 3)
			servoLast[which] = servo[which];
		else if(diff > 8)
			servoLast[which] = servoLast[which] + 4;
		else
			servoLast[which] = servoLast[which] + (diff >> 1);
	}

	return servoLast[which];
}

//limit the servos to only move 3 positions per cycle
char profileServo3(char which) {
	char diff;
	if(servoLast[which] == 0)  //no known position
		servoLast[which] = servo[which];
	else if(servo[which] == 0) //make it alseep
		return 0;
	else if(servoLast[which] > servo[which]) {
		diff = servoLast[which] - servo[which];
		if(diff < 2)
			servoLast[which] = servo[which];
		else if(diff > 6)
			servoLast[which] = servoLast[which] - 3;
		else
			servoLast[which] = servoLast[which] - (diff >> 1);
	}else if(servoLast[which] < servo[which]) {
		diff = servo[which] - servoLast[which];
		if(diff < 2)
			servoLast[which] = servo[which];
		else if(diff > 6)
			servoLast[which] = servoLast[which] + 3;
		else
			servoLast[which] = servoLast[which] + (diff >> 1);
	}

	return servoLast[which];
}

void do_servo(void) {
	clear_bit(_servo_free,0); // the bit 0 of servo_free = 0;
	if (servo_state == 1) {
		if(servo_curr == 5) { //it's the tilt servo
			servoBuf = profileServo3(servo_curr);
			TMR0 = 140;
		}else if(servo_curr == 4) { //pan servo, extend the pulse so that it won't spin forever
			servoBuf = servo[servo_curr];
			TMR0 = 150;
		}else { //the steering servos - give them extra range	
			servoBuf = profileServo(servo_curr);
			TMR0 = 120 + ((255 - servoBuf) >> 2);
		}

		if ((servoBuf != 0) && ((servo_mask & servo_switch) != 0))
			PORTD |= servo_switch; // output hi to specific servo pin out
		servo_state = 2;
                set_bit(_servo_free,0); // the bit servo_free = 1;
	}
	else if (servo_state == 2) {
		TMR0 = 255 - servoBuf;
		servo_state = 3;
	}
	else if (servo_state == 3) {
		if ((servo_mask & servo_switch) != 0)
			PORTD &= ~servo_switch; // output lo to this servo bit, but
							// preserve all other digital hi's on port D
		TMR0 = servo[servo_curr];
		servo_curr = servo_curr + 1;
		servo_curr = servo_curr & 0x07; // increment to next servo, mod 8 //
		servo_switch = servo_switch << 1;
		if (servo_switch == 0) servo_switch = 1; // increment to next servo port bit //
		servo_state = 1;
	}
	clear_bit(INTCON, 2); // clear T0IF = 0;
}

// end of servo control

// Analog-digital converter code //////////////////

char ADCON0@0x1f;
char ADRESH@0x1e;
char ADCON1@0x9f;

// this is called with a character 0 through 7 and returns 
// an analog-digital reading
char readADC(char ch) {
	ADCON0 = (ch << 3) & 56; // shift ch to correct bit position
	ADCON0 |= 0x89; // Tad = Fosc/32, ADC on.
	delay_us(12); // wait for Tacq
    	ADCON0 |= 4; // acquire go!	
      while (ADCON0 & 4); // wait for AD to complete
      delay_us(4); // wait for 2Tad
      return ADRESH; // return captured value
}

// End of Analog-Digital conversion code //////////////

// PWM Motor code //////////////
//globals for motor PWM control//
char ccpcon;
char CCP1CON@0x17;
char CCP2CON@0x1d;
char TMR2@0x11;
char PR2@0x92;
char CCPR1L@0x15;
char CCPR2L@0x1b;
char T2CON@0x12;
char PORTC@0x07;

void pwm_init(void) {
	// init hardware PWM
	CCP1CON = 0; // CCP off
	CCP2CON = 0;
	TMR2 = 0;
	PR2 = 0xff; // 0x3f: 78kHz, 0xff: 19.5kHz
	CCPR1L = 0; // actually, I don't think the output is inverted! 0 = stop.
	CCPR2L = 0;
	TRISC = TRISC & 11011000b; // set DC motor pins to output
	clear_bit(TRISB,1); // clear for motor direction output lines - chnged 0 to 1 illah 1/16/03
	clear_bit(TRISA,4); // clear for motor direction output lines	
	ccpcon = 00111100b; // setup reg mask
	CCP1CON = ccpcon; // CCP module to PWM mode
	CCP2CON = ccpcon;
	set_bit(T2CON,TMR2ON);
}

// parameters are motor number (0 or 1), direction (0 or 1) and speed //
// presumes that port c, bits 5 and 0 are being used for this.
void setVel(char n, char d, char c) {
	if (n == 0) {
		if (d == 0) { //set_bit(PORTC,5);
			set_bit(PORTC,5);
			clear_bit(PORTB,1); // changed 0 to 1 illah 1/16/2003
		}
		else { //clear_bit(PORTC,5);
			clear_bit(PORTC,5);
			set_bit(PORTB,1); // changed 0 to 1 illah 1/16/2003
		}
		CCPR1L = c;
	}
	else if (n==1) {
		if (d == 0) { //set_bit(PORTC,0);
			set_bit(PORTC,0);
			clear_bit(PORTA,4);
		}
		else { //clear_bit(PORTC,0);
			clear_bit(PORTC,0);
			set_bit(PORTA,4);
		}
		CCPR2L = c;
	}
}
// END of PWM Motor Code ///////////////////////

// interrupt handler code

// variables for saving state during interrupt handling
int save_w;
int save_status;

//the actual interrupt handler function itself.
void interrupt(void) {
      //store current state of processor
	asm {
   	movwf _save_w
   	swapf STATUS,W
   	bcf   STATUS,5
   	bcf   STATUS,6
   	movwf _save_status
	} // end asm

	if (INTCON & 36) do_servo();
      // in other words if (TOIE & TOIF) then do_servo() //
	
	// restore processor state
	asm {
   	swapf _save_status,W
   	movwf STATUS
   	swapf _save_w,F
   	swapf _save_w,W
	} // end asm
}

// this initializes the buttons and leds on cerebellum //
// this also initializes PortD digital as outputs, for servoes and digital outs //
void init_cerebellum(void)
{
	char mask = 255 - (1 << GREEN) - (1 << YELLOW);
	TRISB &= mask; // make port B pins GREEN and YELLOW write-out able for LED's by setting
			  // tristate bits GREEN and YELLOW to zero
	PORTB &= mask; // turn off yellow and green LED's   
      TRISD = 0; // port D as outputs for Servo cmds and digital output settings   
}