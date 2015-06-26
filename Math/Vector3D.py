"""Class to contain 3D vectors"""
class Vector3D:
    def __init__(self,x=0,y=0,z=0):
        self.x = x
        self.y = y
        self.z = z
    def __add__(self,otherVector):
        """adds two vectors"""
        assert isinstance(otherVector,Vector3D)
        newX = self.x + otherVector.x
        newY = self.y + otherVector.y
        newZ = self.z + otherVector.z
        return Vector3D(x=newX,
                        y=newY,
                        z=newZ)
    def dot(self,otherVector):
        """finds dotProduct of two vectors"""
        assert isinstance(otherVector,Vector3D)
        pass
    def getXComponent(self):
        """returns the X component of the vector"""
        return self.x
    def getYComponent(self):
        """returns the Y component of the vector"""
        return self.y
    def getZComponent(self):
        """returns the Z component of the vector"""
        return self.z
