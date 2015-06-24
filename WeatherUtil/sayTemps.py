import forecastio

import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say

def forecast():
    api_key = "24390f355a2045622ae402fb1c8ebe8b"
    latitude = 51.5033630
    longitude = -0.1276250
    forecast = forecastio.load_forecast(api_key, latitude, longitude)
    x = 0
    for dat in forecast.daily().data:
        print(dat.icon)
        if(x==0):
            say("Today it will be " + str(dat.summary))
        elif(x==1):
            say("To marrow will be" + str(dat.summary))
        else:
            say("The day after that it will be "+str(dat.summary))
        x+=1
    #speak.say(str(dat.temperature) + " degrees Celsius")
