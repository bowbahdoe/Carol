from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say
import webbrowser
class OpenWebsite(Command):
    def __init__(self):
        self.site = ""
        self.regex = r''
        self.pattern = ""
    def checkIfMatches(self, input):
        if(self.pattern.search(input.lower()) != None):
            return True
        else:
            return False
    def action(self):
        print "Opening Website: " + self.site
        webbrowser.open(self.site)
class OpenYoutube(OpenWebsite):
    def __init__(self):
        self.site = "http://www.youtube.com"
        self.regex = r'open youtube'
        self.pattern = re.compile(self.regex)
class OpenGoogle(OpenWebsite):
    def __init__(self):
        self.site = "http://www.google.com"
        self.regex = r'open google'
        self.pattern = re.compile(self.regex)
class OpenFacebook(OpenWebsite):
    def __init__(self):
        self.site = "http://www.facebook.com"
        self.regex = r'open facebook'
        self.pattern = re.compile(self.regex)
