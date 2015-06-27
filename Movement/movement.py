try:
    from kovan import motor,off
except ImportError:
    raise Exception("No robot connected")
import threading
from time import sleep
leftMotorPort = 0
rightMotorPort = 0
SKEW_MOVEMENT_ADJUSTER = 1 #adjust to compensate for wheel difference
def motor_adjusted(port,power):
    """same as the motor function, except it adjusts for difference in
    how the weight is distributed to the wheels"""
    if(port==leftMotorPort):
        motor(port,power*SKEW_MOVEMENT_ADJUSTER)
    else:
        motor(port,power)
def driveForwards(power):
    motor_adjusted(leftMotorPort,power)
    motor_adjusted(rightMotorPort,power)
def driveBackwards(power):
    motor_adjusted(rightMotorPort,-power)
    motor_adjusted(leftMotorPort,-power)
def turnLeft(power):
    motor_adjusted(leftMotorPort,-power)
    motor_adjusted(rightMotorPort,power)
def turnRight(power):
    motor_adjusted(leftMotorPort,power)
    motor_adjusted(rightMotorPort,-power)
def stopMoving():
    off(leftMotorPort)
    off(rightMotorPort)
class motorController:
    def __init__(self):
        self.motorLocked = False
        self.motorThread = None
    def driveForwardsForTime(power,time):
        if(not(self.motorLocked)):
            driveForwards(power)
            self.timeForDrive = time
            motor_thread = threading.Thread(target=self.driveThread)
            motor_thread.daemon = True
            motor_thread.start()
            self.motor_thread = motor_thread
    def driveBackwardsForTime(power,time):
        if(not(self.motorLocked)):
            driveBackwards(power)
            self.timeForDrive = time
            motor_thread = threading.Thread(target=self.driveThread)
            motor_thread.daemon = True
            motor_thread.start()
            self.motor_thread = motor_thread
    def turnRightForTime(power,time):
        if(not(self.motorLocked)):
            turnRight(power)
            self.timeForDrive = time
            motor_thread = threading.Thread(target=self.driveThread)
            motor_thread.daemon = True
            motor_thread.start()
            self.motor_thread = motor_thread
    def turnLeftForTime(power,time):
        if(not(self.motorLocked)):
            turnLeft(power)
            self.timeForDrive = time
            motor_thread = threading.Thread(target=self.driveThread)
            motor_thread.daemon = True
            motor_thread.start()
            self.motor_thread = motor_thread
    def stopCurrentDrive():
        stopMoving()
    def driveThread(time):
        self.motorLocked = True
        sleep(self.timeForDrive)
        stopMoving()
        self.motorLocked = False
