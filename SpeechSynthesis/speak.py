#from ctypes import *
import os
#cdll.LoadLibrary(os.path.abspath(__file__)[0:-9]+"/libspeak.so")
#speak_library = CDLL(os.path.abspath(__file__)[0:-9]+"/libspeak.so")
#speak = speak_library.speak
def say(words):
    command = "padsp flite -voice slt -t '" + words + "'" #kal16
    os.system(command)
