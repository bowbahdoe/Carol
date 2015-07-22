#ifndef _SERIAL_H_
#define _SERIAL_H_

#include <string.h>
#include <sys/poll.h>
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h> //for fork
#include <sys/wait.h>
#include <unistd.h>

void init4();

class serialPort {
public:
  serialPort();
  ~serialPort();
  int Open(char *tty);
  int Close();

  int Read(char *buf, int count, int wait);
  int Write(char *buf, int count);
  void clearBuffer();
	
private:
  int fd;
};

#endif
