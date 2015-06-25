from math import sqrt
class Coordinate:
    """class to contain a Coordinate"""
    def __init__(self,x,y):
        assert(type(x)==int)
        assert(type(y)==int)
        self.x = x
        self.y = y
    def setX(self,x):
        """Sets the x value of the coordinate"""
        assert(type(x)==int)
        self.x = x
    def setY(self,y):
        """Sets the y value of the coordinate"""
        assert(type(y)==int)
        self.y = y
    def getX(self):
        """Gets the x value of the coordinate"""
        return self.x
    def getY(self):
        """Gets the Y value of the coordinate"""
        return self.y
    def getDistanceTo(self,otherCoordinate):
        """returns the absolute distance between two coordinates"""
        assert(type(otherCoordinate) == Coordinate)
        a = self.x-otherCoordinate.getX()
        b = self.y-otherCoordinate.getY()
        c_squared = a**2+b**2
        distance = sqrt(c_squared)
        return distance
