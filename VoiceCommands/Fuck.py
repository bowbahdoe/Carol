from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say
class Fuck(Command):
    """Condemns swearing"""
    def __init__(self):
        self.pattern = re.compile(r'f\*\*\*')
    def checkIfMatches(self, input):
        pattern = self.pattern
        if(pattern.search(input.lower()) is not None):
            return True
        else:
            return False
    def action(self):
        say("Watch your language.")
