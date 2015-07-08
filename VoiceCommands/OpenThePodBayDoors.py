from Carol.VoiceCommands.Command import Command
from Carol.SpeechSynthesis import say
import re
class OpenThePodBayDoors(Command):
    """Responds in reference to 2001"""
    def __init__(self):
        self.regex = "pod bay doors"
        self.pattern = re.compile(self.regex)
    def checkIfMatches(self, input):
        if(self.pattern.search(input.lower()) != None):
            return True
        else:
            return False
    def action(self):
        say("I'm sorry. I'm afraid I cant do that Dave.")
