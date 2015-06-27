import re
from Carol.VoiceCommands.Command import Command
from Carol.SpeechSynthesis import say
from Carol.Games.Keys.main import main
from os import path
import os
import threading
class RunKeys(Command):
    """Runs version 1.0 of keys"""
    def __init__(self):

        self.regex = r'(run kies|frankie\'s|yankees|remkees)'
        self.pattern = re.compile(self.regex)
    def checkIfMatches(self, input):
        if(self.pattern.search(input.lower()) != None):
            return True
        else:
            return False

    def action(self):
        keys_thread = threading.Thread(target=runKeys)
        keys_thread.daemon = True
        keys_thread.start()

def runKeys():
    try:
        os.system("cd "+os.path.dirname(os.path.realpath(__file__))+"; cd ..;cd Games/Keys;python main.py")
    except Exception as e:
        print e
