import os
from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say
class ShutdownComputer(Command):
    def __init__(self):
        pass
    def checkIfMatches(self, input):
        pattern = re.compile(r'shut down computer')
        if(pattern.search(input.lower()) != None):
            return True
        else:
            return False
    def action(self):
        say("Shutting down computer")

        os.system("poweroff")
