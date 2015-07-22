#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h> //for usleep

#include "serial.h"

int initCerebellum();

serialPort *cerebSerial = 0; //serial port for the cerebellum

int main(int argc, char **argv) {
  char responseBuf[50];

  if(argc != 2) {
    printf("usage: %s file.HEX", argv[0]);
    return -1;
  }

  struct stat hexStat;
  int rets = stat(argv[1], &hexStat);
  if(rets) { //failure
    printf("File %s does not exist!\n", argv[1]);
    return -1;
  }
  int size = (int)hexStat.st_size;

  int hexFd = open(argv[1], O_RDONLY);
  if(hexFd == -1){ //failure
    printf("Can't open hex file!\n");
    return -1;
  }
  char * hexData = new char[size];

  //now try to read in the whole hex file
  int readIn = 0, justRead;
  do {
    justRead = read(hexFd, hexData+readIn, size-readIn);
    readIn += justRead;
  }while(justRead > 0 && readIn < size);
  close(hexFd);
  if(readIn < size) { //failure
    printf("Error reading hex file!\n");
    delete hexData;
    return -1;
  }
  //check if it fits the format of a hex file - first char should be ':'
  if(*hexData != ':') {
    printf("The first character of the hex file should be ':'\n");
    delete hexData;
    return -1;
  }

  if(initCerebellum()) {
    delete hexData;
    return -1;
  }

  int mooTries = 1;
  while(mooTries <= 15) {
    cerebSerial->clearBuffer();
    cerebSerial->Write(" MOO\r\n", 6);
    readIn = 0;
    while(readIn < 32 && (justRead = cerebSerial->Read(responseBuf+readIn, 49-readIn, 25)))
      readIn += justRead;
    responseBuf[readIn] = 0;
    if(!strcmp(responseBuf, "Slave about to program main...\r\n")) {
      printf("MOO worked on try %d\n", mooTries);
      break; //success
    }
    usleep(25);
    mooTries++;
  }
  //printf("got %d characters from cereb: %s\n", readIn, responseBuf);
  //for(int i=0; i<readIn; i++)
  //printf("%d\t", (int)responseBuf[i]);
  printf(responseBuf);
  if(strcmp(responseBuf, "Slave about to program main...\r\n")) {
    printf("\nreceived improper response from cerebellum\n");
    printf("Can't get cerebellum into programming mode\n");
    delete hexData;
    return -1;
  } 

  readIn = 0;
  while(readIn < 19 && (justRead = cerebSerial->Read(responseBuf+readIn, 49-readIn, 2250)))
    readIn += justRead;
  responseBuf[readIn] = 0;
  printf(responseBuf); //"Now programming."
  /*printf("got %d characters from cereb: %s\n", readIn, responseBuf);
  for(int i=0; i<readIn; i++)
  printf("%d\t", (int)responseBuf[i]);*/
  if(strcmp(responseBuf, "Now programming.\r\nr")) {
    printf("\nreceived improper response from cerebellum\n");
    delete hexData;
    return -1;
  } 

  readIn = cerebSerial->Read(responseBuf, 49, 250);
  responseBuf[readIn] = 0;
  printf(responseBuf); //"\r\nr"

  //now, actually start programming the cerebellum
  int written=0, toWrite=1;
  for(int i=0; i<size; i++) {
    if(hexData[i] == '\n') {
      cerebSerial->Write(hexData+written, toWrite);
      //printf("written: %d toWrite: %d\n", written, toWrite);
      usleep(200000);
      readIn = cerebSerial->Read(responseBuf, 49, 2);
      if(readIn) {
	responseBuf[readIn] = 0;
	printf(responseBuf); fflush(stdout);
      }
      written += toWrite;
      toWrite = 1;
    }else
      toWrite++;
  }
  
  printf("\nAll done.\n");

  delete hexData;
  cerebSerial->Close();
  return 0;
}


int initCerebellum() {
  cerebSerial = new serialPort();

  init4(); //call 'init 4' so that I can use the serial port
  int tries;
  for(tries=0; cerebSerial->Open("/dev/ttyS0") < 0 && tries < 3; tries++)
    usleep(100000);
  if(tries >= 3) {
    printf("Couldn't open the serial port for the cerebellum!\n");
    FILE *error = fopen("/root/error.txt", "w");
    fprintf(error, "Couldn't open the serial port for the cerebellum!\n");
    fclose(error);
    return 1;
  }
  return 0;
}
