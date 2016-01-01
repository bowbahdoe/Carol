import os
from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say
class ShutdownComputer(Command):
    """Shutsdown the computer. Needs to be ran in sudo"""
    def __init__(self):
        self.pattern = re.compile(r'shut down computer')
    def checkIfMatches(self, input):
        if(self.pattern.search(input.lower()) is not None):
            return True
        else:
            return False
    def action(self):
        say("Shutting down computer")
        os.system("poweroff")
