import re
from Carol.VoiceCommands.Command import Command
from Carol.SpeechSynthesis import say

from os import path
import os
import threading
import platform
def shutil_which(pgm):
    """
    python2 backport of python3's shutil.which()
    """
    path = os.getenv('PATH')
    for p in path.split(os.path.pathsep):
        p = os.path.join(p, pgm)
        if os.path.exists(p) and os.access(p, os.X_OK):
            return p
class Run2048(Command):
    """Runs 2048 in terminal"""
    def __init__(self):

        self.regex = r'run 2048'
        self.pattern = re.compile(self.regex)
    def checkIfMatches(self, input):
        if(platform.system()!="Linux"):
            return False
        if(self.pattern.search(input.lower()) != None):
            return True
        else:
            return False

    def action(self):
        keys_thread = threading.Thread(target=run2048)
        keys_thread.daemon = True
        keys_thread.start()

def run2048():
    try:
        os.system("cd "+os.path.dirname(os.path.realpath(__file__))+"; cd ..;cd Games/2048; gnome-terminal -e \"./2048\"")
    except Exception as e:
        print e
