try:
    from kovan import motor,off
except ImportError:
    raise Exception("No robot connected")
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
