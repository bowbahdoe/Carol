

import re
from Carol.VoiceCommands.Command import Command
from Carol.SpeechSynthesis import say

from os import path
import os
import threading
pygame_installed = True
pygame_java = False
def shutil_which(pgm):
    """
    python2 backport of python3's shutil.which()
    """
    path = os.getenv('PATH')
    for p in path.split(os.path.pathsep):
        p = os.path.join(p, pgm)
        if os.path.exists(p) and os.access(p, os.X_OK):
            return p
try:
    from pygame.locals import *
except ImportError:
    pygame_installed = False
    if(shutil_which("jython")!=None):
        pygame_java = True
class RunKeys(Command):
    """Runs version 1.0 of keys"""
    def __init__(self):

        self.regex = r'(run kies|frankie\'s|yankees|remkees|open keys)'
        self.pattern = re.compile(self.regex)
    def checkIfMatches(self, input):
        if(not(pygame_installed) and not(pygame_java)):
            return False
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
        if(pygame_java):
            os.system("cd "+os.path.dirname(os.path.realpath(__file__))+"; cd ..;cd Games/Keys;jython main.py")
        os.system("cd "+os.path.dirname(os.path.realpath(__file__))+"; cd ..;cd Games/Keys;python main.py")
    except Exception as e:
        print e
