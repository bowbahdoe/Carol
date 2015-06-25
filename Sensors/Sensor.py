class Sensor:
    """Abstract class for sensors"""
    def __init__(self,port):
        self.port = port
    def update(self):
        pass
