CHANNEL_RED = 0
CHANNEL_GREEN = 1
from kovan import get_object_count,camera_update
from kovan import get_object_center_x,get_object_center_y
from Carol.Math.Coordinate import Coordinate
from time import sleep
def getNumVisibleFoamBalls():
    """Returns the number of red balls in the camera's frame"""
    camera_update()
    count = get_object_count(CHANNEL_RED);
    return count

def getCoordinateOfFoamBall(pingPongBallNumber):
    """Returns the coordinate of the biggest red ball the camera sees"""
    camera_update()
    sleep(.1)
    x = get_object_center_x(CHANNEL_GREEN,pingPongBallNumber)
    y = get_object_center_y(CHANNEL_GREEN,pingPongBallNumber)
    cood = Coordinate(x,y)
    return cood
def getAllFoamBallCoordinates():
    """returns a list of all the coordinates of red balls the camera sees"""
    number = getNumVisibleFoamBalls()
    coods = []
    for i in range(number):
        coods.append(getCoordinateOfPingPongBall(i))
    return coods
