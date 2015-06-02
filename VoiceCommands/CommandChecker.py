from Carol.VoiceCommands.Fuck import Fuck
from Carol.VoiceCommands.SingMeASong import SingMeASong
from Carol.VoiceCommands.TellMeAJoke import TellMeAJoke
from Carol.VoiceCommands.WhatIsColderThanCold import WhatIsColderThanCold
from Carol.VoiceCommands.ILoveYou import ILoveYou
from Carol.VoiceCommands.AngerTowardsCarol import AngerTowardsCarol
from Carol.VoiceCommands.OpenWebsite import OpenYoutube,OpenGoogle,OpenFacebook
from Carol.VoiceCommands.ShutdownComputer import ShutdownComputer
class NoMatchError(Exception):
    pass

class CommandChecker:
    def __init__(self):
        self.commandsList = []
        self.commandsList.append(Fuck())
        self.commandsList.append(SingMeASong())
        self.commandsList.append(TellMeAJoke())
        self.commandsList.append(WhatIsColderThanCold())
        self.commandsList.append(ILoveYou())
        self.commandsList.append(AngerTowardsCarol())
        self.commandsList.append(OpenYoutube())
        self.commandsList.append(OpenGoogle())
        self.commandsList.append(OpenFacebook())
        self.commandsList.append(ShutdownComputer())

    def checkForMatches(self, text):
        matchFound = False
        for command in self.commandsList:
            if(command.checkIfMatches(text)):
                command.action()
                matchFound = True
            else:
                pass
        if(not(matchFound)):
            raise NoMatchError
