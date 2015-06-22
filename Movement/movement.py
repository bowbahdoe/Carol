try:
    from kovan import motor,off
except ImportError:
    raise Exception("No robot connected")
leftMotorPort = 0
rightMotorPort = 0
def driveForwards(power):
    motor(leftMotorPort,power)
    motor(rightMotorPort,power)
def driveBackwards(power):
    motor(rightMotorPort,-power)
    motor(leftMotorPort,-power)
def turnLeft(power):
    motor(leftMotorPort,-power)
    motor(rightMotorPort,power)
def turnRight(power):
    motor(leftMotorPort,power)
    motor(rightMotorPort,-power)
def stopMoving():
    off(leftMotorPort)
    off(rightMotorPort)
