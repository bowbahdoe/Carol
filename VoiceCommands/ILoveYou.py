from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say
class ILoveYou(Command):
    """responds to claims of affection accordingly"""
    def __init__(self):
        self.pattern =re.compile(r'i love you')
    def checkIfMatches(self, input):
        pattern = self.pattern
        if(pattern.search(input.lower()) is not None):
            return True
        else:
            return False
    def action(self):
        say("I love you too")
