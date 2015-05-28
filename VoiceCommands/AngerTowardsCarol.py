from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from Carol.SpeechSynthesis.speak import say

import random
from time import sleep

class AngerTowardsCarol(Command):
    def __init__(self):
        pass
    def checkIfMatches(self, input):
        pattern = re.compile(r'f\*\*\* you')
        if(pattern.search(input.lower()) != None):
            return True
        else:
            return False
    def action(self):

        say("I am sorry")
        sleep(0.5) #200 milliseconds
        say("I guess even I cant be your friend")
