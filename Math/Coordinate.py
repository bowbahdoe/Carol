from math import sqrt
class Coordinate:
    def __init__(self,x,y):
        assert(type(x)==int)
        assert(type(y)==int)
        self.x = x
        self.y = y
    def setX(self,x):
        assert(type(x)==int)
        self.x = x
    def setY(self,y):
        assert(type(y)==int)
        self.y = y
    def getX(self):
        return self.x
    def getY(self):
        return self.y
    def getDistanceTo(self,otherCoordinate):
        assert(type(otherCoordinate) == Coordinate)
        a = self.x-otherCoordinate.getX()
        b = self.y-otherCoordinate.getY()
        c_squared = a**2+b**2
        distance = sqrt(c_squared)
        return distance
