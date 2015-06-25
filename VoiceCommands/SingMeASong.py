from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say

import random
from time import sleep

class SingMeASong(Command):
    """Sings Daisy Bell"""
    def __init__(self):
        self.pattern = re.compile(r'sing me a song')
    def checkIfMatches(self, input):
        if(self.pattern.search(input.lower()) != None):
            return True
        else:
            return False
    def action(self):
        say("daiii sey");sleep(.2)
        say("daiii sey");sleep(.2)
        say("give me your answer dooooo");
        sleep(.2)
        say(r'IM half crazy; all for the love of you');sleep(.2)
        say(r'It wont be a stylish marrige;');sleep(.2)
        say(r'I cant afford a carrige; But you ull look sweet; upon the seat; of a bicycle built for two')
if __name__ == "__main__":
    d = SingMeASong()
    d.action()
