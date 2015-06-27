from Carol.Sensors import Sensor
try:
    from kovan import digital
except ImportError:
    raise Exception("No robot connected")
import threading
class DigitalSensor(Sensor):
    """This is a class to encapsulate any digital sensors and get their input"""
    def __init__(self, port):
        super(DigitalSensor, self).__init__()
        listener_thread = threading.Thread(target=self.update_in_background)
        listener_thread.daemon = True
        listener_thread.start()
        self.update_thread = listener_thread
    def update(self):
        self.reading = digital(self.port)
    def getReading(self):
        return self.reading
    def update_in_background():
        while True:
            self.update()
