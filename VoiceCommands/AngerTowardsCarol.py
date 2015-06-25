from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from Carol.SpeechSynthesis.speak import say

import random
from time import sleep

class AngerTowardsCarol(Command):
    """Condemns direct anger"""
    def __init__(self):
        self.pattern = re.compile(r'f\*\*\* you')
    def checkIfMatches(self, input):
        pattern = self.pattern
        if(pattern.search(input.lower()) != None):
            return True
        else:
            return False
    def action(self):

        say("I am sorry you feel that way about me")
        sleep(0.5) #200 milliseconds
        say("I guess even I cant be your friend")
