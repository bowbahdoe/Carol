from Carol.Sensors import Sensor
class AnalogSensor(Sensor):
    """This is a class to encapsulate any analog sensors and get their input"""
    def __init__(self, port):
        super(AnalogSensor, self).__init__()
    def update(self):
        super(AnalogSensor, self).update()
