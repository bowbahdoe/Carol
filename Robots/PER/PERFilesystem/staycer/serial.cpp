#include "serial.h"

#define BAUDRATE B115200

//calls init 4 on the console.  If anything goes wrong, the program exits!
void init4() {
  pid_t pid;
  int status;FILE *error;

  switch(pid = fork()) {
  case -1: //failure
    printf("can't fork!, exiting. . .\n");
	error = fopen("/root/error.txt", "w");
	fprintf(error, "can't fork!, exiting. . .\n");
	fclose(error);
    exit(-1);
  case 0: //we're the child
    if(execl("/sbin/init", "init", "4", 0)) {
      printf("The exec failed!\n");
	error = fopen("/root/error.txt", "w");
	fprintf(error, "The exec failed!\n");
	fclose(error);
      exit(-1);
    }
  default: //we're the parent
    waitpid(pid, &status, 0); //wait for child to die
    if(status != 0) {
      printf("Calling init 4 failed!\n");
      error = fopen("/root/error.txt", "w");
      fprintf(error, "Calling init 4 failed!\n");
      fclose(error);
      exit(-1);
    }
  }
}

serialPort::serialPort() {
  fd = -1;
}

serialPort::~serialPort() {
  close(fd);
}

//tries to open the port and returns 0 upon success
int serialPort::Open(char *tty) {
  struct termios newtio;

  if(fd>=0) 
    close(fd);
  
  fd = open(tty, O_RDWR | O_NOCTTY | O_NONBLOCK);
 
  if(fd < 0)
    return fd;

  // set new port settings for canonical input processing 
  newtio.c_cflag = BAUDRATE | CS8 | CLOCAL | CREAD;
  newtio.c_iflag = IGNPAR ;
  newtio.c_oflag = 0;
  newtio.c_lflag = 0;
  newtio.c_cc[VMIN]=1;
  newtio.c_cc[VTIME]=0;

  if (tcsetattr(fd, TCSANOW, &newtio)) {
	close(fd);
	return -1;
  }
  return 0;
}

int serialPort::Close() {
  int rv = close(fd);
  fd = -1;
  return rv;
}

/* tries to read count bytes into the buffer buf and returns the number of 
   bytes read.  It will wait for up to wait ms before timing out and returning
   0.  It is not guaranteed to return count bytes. */
int serialPort::Read(char *buf, int count, int wait) {
  struct pollfd pfd;

  pfd.fd = fd;
  pfd.events = POLLIN;
  if(poll(&pfd, 1, wait) > 0) 
    return read(fd, buf, count);
  else 
    return 0;
  
}

int serialPort::Write(char *buf, int count) {
  return write(fd, buf, count);
}

void serialPort::clearBuffer() {
  char c;
  while(Read(&c, 1, 0));
}
