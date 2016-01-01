import re
from Carol.VoiceCommands.Command import Command
from Carol.SpeechSynthesis import say

from os import path
import os
import threading
import platform
try:
    from kovan import motor
    robot = True
except:
    robot = False
def shutil_which(pgm):
    """
    python2 backport of python3's shutil.which()
    """
    path = os.getenv('PATH')
    for p in path.split(os.path.pathsep):
        p = os.path.join(p, pgm)
        if os.path.exists(p) and os.access(p, os.X_OK):
            return p
class RunWords(Command):
    """Runs Words, the latin english dictionary program in terminal"""
    def __init__(self):

        self.regex = r'(run words|run whitakers words|whitaker\'s words)'
        self.pattern = re.compile(self.regex)
    def checkIfMatches(self, input):
        if(platform.system()!="Linux"):
            return False
        if(robot):
            return False
        if(self.pattern.search(input.lower()) is not None):
            return True
        else:
            return False

    def action(self):
        words_thread = threading.Thread(target=runWords)
        words_thread.daemon = True
        words_thread.start()

def runWords():
    try:
        #os.system("cd "+os.path.dirname(os.path.realpath(__file__))+"; cd ..;cd Utilities/Words; gnome-terminal -e \"bin/words\"")
        os.system("cd "+os.path.dirname(os.path.realpath(__file__))+"; cd ..;cd Utilities/Words; python wordsGUI.py ")
    except Exception as e:
        print e
