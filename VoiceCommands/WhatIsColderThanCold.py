from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say
class WhatIsColderThanCold(Command):
    """Responds to a stupid question with a stupid answer"""
    def __init__(self):
        self.pattern = re.compile(r'what is colder than cold')
    def checkIfMatches(self, input):
        if(self.pattern.search(input.lower()) != None):
            return True
        else:
            return False
    def action(self):
        say("Ice. Cold.")
