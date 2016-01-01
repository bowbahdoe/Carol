from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say

import random
from time import sleep

class TellMeAJoke(Command):
    """Tells a random joke"""
    def __init__(self):
        self.pattern = re.compile(r'tell me a joke')
    def checkIfMatches(self, input):
        if(self.pattern.search(input.lower()) is not None):
            return True
        else:
            return False
    def action(self):
        self.puns =[
        ["Why did the chicken cross the road?", "To get to the other side"],
        ["A man walked into a bar", "it hurt"],
        ["What is the name of a baby whose parents are a vampire and a snowman", "Frostbite"],
        ["Did you hear about the circus fire", "It was in tents"],
        ["Which vedge tables do golfers like best", "Greens!"],
        ["Why is a book like a tree", "Because it is full of leaves"],
        ["What do you get when you cross the sea with a burgular", "A crime wave"],
        ["What is a vampires least favorite puzzle", "a cross word"],
        ["What do you get when you cross an elephant and a rhino", "Hell if I know"]
        ]
        joke = random.choice(self.puns)
        print joke[0]
        say(joke[0])
        print joke[1]
        sleep(0.5) #200 milliseconds
        say(joke[1])
if __name__ == "__main__":
    d = TellMeAJoke()
    d.action()
