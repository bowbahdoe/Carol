from Carol.Sensors import Sensor
try:
    from kovan import digital
except ImportError:
    raise Exception("No robot connected")
class DigitalSensor(Sensor):
    """This is a class to encapsulate any digital sensors and get their input"""
    def __init__(self, port):
        super(DigitalSensor, self).__init__()
    def update(self):
        super(DigitalSensor, self).update()
