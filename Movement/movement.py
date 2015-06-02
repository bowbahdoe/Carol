from Carol.Movement.Motor import Motor
leftMotorPort = 0
rightMotorPort = 1

leftMotor = Motor(leftMotorPort)
rightMotor = Motor(rightMotorPort)
def driveForwards(power):
    leftMotor.setPower(power)
    rightMotor.setPower(power)
def driveBackwards(power):
    leftMotor.setPower(-power)
    rightMotor.setPower(-power)
def turnLeft(power):
    leftMotor.setPower(-power)
    rightMotor.setPower(power)
def turnRight(power):
    leftMotor.setPower(power)
    rightMotor.setPower(-power)
def stopMoving():
    leftMotor.stopMotor()
    rightMotor.stopMotor()
