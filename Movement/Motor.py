import RPi.GPIO as GPIO
class Motor(Object):
    """Class to handle all tasks associated with motor control"""
    def __init__(self,port, maxPower=100):
        self.power = 0
        self.port = port
        self.maxPower = maxPower
    def setPower(power):
        assert(power<=100)
        self.power = (power/100) * self.maxPower
    def getPower():
        return self.power
    def stopMotor():
        self.power = 0
