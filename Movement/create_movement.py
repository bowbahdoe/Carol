try:
    from kovan import create_drive_direct,create_drive_straight,set_create_total_angle
    from kovan import get_create_distance,create_stop,set_create_normalized_angle
from time import sleep
def rotateLeftDegrees(degrees,speed):
    #Speed is given in mm/s
    set_create_total_angle(0)
    create_drive_direct(-speed,speed)
    while(get_create_total_angle()<degrees-1):
        pass
    create_stop();
	set_create_total_angle(0);

def rotateRightDegrees(degrees, speed):
    rotateLeftDegrees(degrees,-speed)
def create_drive_distance(speed, distance){

	if(speed<0 and distance>0):
        distance*=-1;
	if(speed>0 and distance<0):
        speed*=-1
	end=get_create_distance(0)+distance;
	create_drive_straight(speed);
	if(distance>0):
        while(get_create_distance(0)<end):
            msleep(10.0/1000)
	if(distance<0):
        while(get_create_distance(0)>end):
            sleep(10.0/1000)
	create_stop();
}
