class Sensor:
    """Abstract class for sensors"""
    def __init__(self,port):
        self.port = port
        self.reading = 0
    def update(self):
        pass
