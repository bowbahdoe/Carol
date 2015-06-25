import webbrowser
import re
from Carol.VoiceCommands.Command import Command
class SearchGoogle(Command):
    """Searches google for a phrase"""
    def __init__(self):
        self.site = "http://www.google.com#q="
        self.regex = r'search for'
        self.pattern = re.compile(self.regex)
    def checkIfMatches(self, input):
        if(self.pattern.search(input.lower()) != None):
            self.site+=input[input.find("search for")+11:]
            return True
            print self.site
        else:
            return False
    def action(self):
        print "Opening Website: " + self.site
        webbrowser.open(self.site)
        self.site="http://www.google.com#q="
