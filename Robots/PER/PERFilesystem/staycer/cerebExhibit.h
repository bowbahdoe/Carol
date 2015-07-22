// Declarations file for Cerebellum Programming Software 
/* Version Information:
	This is version 2 for the second version of Cerebellum.
	the location of Button 1 changed, as did the motor direction bit.
*/

#define SER_9600 129
#define SER_19200 64
#define SER_38400 31
#define SER_57600 21
#define SER_115200 10
#define CREN 4
#define GIE 7
#define TMR2ON 2
/// #define T0IE 5
//  #define T0IF 2


#pragma CLOCK_FREQ 20000000 // correct clock for timing on cerebellum

// Cerebellum LEDs and buttons //
//#define GREEN 4  // this is on port b, pin 4
//#define YELLOW 5 // this is on port b, pin 5
//old buttons were 1 and 4?

// for new cerebellum
#define GREEN 0  // this is on port b, pin 0
#define YELLOW 2 // this is on port b, pin 2
#define BTN1 4   // on port b-first pin (B4);  button further from LEDs
#define BTN2 5   // on port b-third pin (B5);  button closer to LEDs
