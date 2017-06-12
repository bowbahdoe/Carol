from Command import Command
import re
import sys
from os import path
sys.path.append( path.dirname( path.dirname( path.abspath(__file__) ) ) )

from SpeechSynthesis.speak import say
import webbrowser
class OpenWebsite(Command):
    """Abstract class for opening a website"""
    def __init__(self):
        self.site = ""
        self.regex = r''
        self.pattern = ""
    def checkIfMatches(self, input):
        if(self.pattern.search(input.lower()) is not None):
            return True
        else:
            return False
    def action(self):
        print "Opening Website: " + self.site
        webbrowser.open(self.site)
        say("Opening" + self.site[self.site.index("www.")+4:])
class OpenYoutube(OpenWebsite):
    """Opens Youtube"""
    def __init__(self):
        self.site = "http://www.youtube.com"
        self.regex = r'open youtube'
        self.pattern = re.compile(self.regex)
class OpenGoogle(OpenWebsite):
    """Opens Google"""
    def __init__(self):
        self.site = "http://www.google.com"
        self.regex = r'open google'
        self.pattern = re.compile(self.regex)
class OpenFacebook(OpenWebsite):
    """Opens Facebook"""
    def __init__(self):
        self.site = "http://www.facebook.com"
        self.regex = r'open facebook'
        self.pattern = re.compile(self.regex)
