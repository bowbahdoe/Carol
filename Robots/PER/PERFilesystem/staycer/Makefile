# set up the compiler

CC = /usr/local/arm-stargate/bin/arm-linux-g++
#CC = arm-linux-g++
#CFLAGS = -Wall -O2 -DVIDEO_DEV=\"/dev/video0\" -DHAVE_LIBJPEG=1
CFLAGS = -Wall -O2
LDFLAGS = -lpthread -ljpeg_p -L/usr/local/arm-stargate/lib
TARGET = exhibitNOSIFT
DLTARGET = cerebdl
DIR = exhibit2demo
ZIP = exhibitNOSIFT.tgz

#--------Suffix Rules------------------------

.SUFFIXES: .cpp

.cpp.o:
	$(CC) $(CFLAGS) -c $< -o $@

#--------File Dependencies-------------------

SRC = rover.cpp serial.cpp cereb.cpp constants.cpp highLevel.cpp \
	util/v4l.cpp webCam.cpp util/vector.cpp util/monitor.cpp \
        net/reliapack.cpp net/reliagram.cpp track.cpp

OBJ = $(addsuffix .o, $(basename $(SRC)))

#--------Rules-------------------------------

all: $(TARGET) $(DLTARGET)

$(TARGET): $(OBJ)
	$(CC) $(CFLAGS) $(OBJ) $(LDFLAGS) -o $@

$(DLTARGET): serial.o cerebdl.cpp
	$(CC) $(CFLAGS) cerebdl.cpp serial.o -o $@	

zip: #create a .tar.gz file
	make clean;cd ..;tar czf $(DIR)/$(ZIP) $(DIR) --exclude $(DIR)/$(ZIP)
clean:
	rm -f $(OBJ) *~ net/*~ util/*~ $(TARGET) $(ZIP) $(DLTARGET)
