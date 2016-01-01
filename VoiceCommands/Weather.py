import re
from Carol.VoiceCommands.Command import Command
from Carol.SpeechSynthesis import say
forecast_api_exists = True
try:
    import forecastio
except ImportError:
    forecast_api_exists = False
class WeatherTomorrow(Command):
    """Tells You the weather for the next day"""
    def __init__(self):

        self.regex = r'weather.*tomorrow'
        self.pattern = re.compile(self.regex)
        self.api_key = "24390f355a2045622ae402fb1c8ebe8b"
        self.latitude = 51.5033630
        self.longitude = -0.1276250
    def checkIfMatches(self, input):
        if(not(forecast_api_exists)):
            return False
        if(self.pattern.search(input.lower()) is not None):
            return True
        else:
            return False

    def action(self):

        forecast = forecastio.load_forecast(self.api_key, self.latitude, self.longitude)
        x = 0
        for dat in forecast.daily().data:
            if(x==0):
                pass
                #say("Today it will be " + str(dat.summary))
            elif(x==1):
                say("To marrow will be" + str(dat.summary))
            else:
                pass
                #say("The day after that it will be "+str(dat.summary))
            x+=1
