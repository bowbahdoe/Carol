from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say
class WhatIsColderThanCold(Command):
    def __init__(self):
        pass
    def checkIfMatches(self, input):
        pattern = re.compile(r'what is colder than cold')
        if(pattern.search(input.lower()) != None):
            return True
        else:
            return False
    def action(self):
        say("Ice. Cold.")
