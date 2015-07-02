import Carol.SpeechRecognition as sr
from Carol.VoiceCommands import CommandChecker
import time
import signal
import sys
#set up command checker
checker = CommandChecker.CommandChecker()
try:
    from Tkinter import *
    speechText = StringVar()
    gui = True
except:
    gui = False
def onSpeech(recognizer, audio):
    print("something said")
    try:
        print("You said: " + recognizer.recognize(audio))
        speech = recognizer.recognize(audio)
        checker.checkForMatches(speech)
        if gui:
            speechText.set(speech)
    except LookupError as err:
        print("Oops! Audio not recognized")
        print err
    except Exception as e:
        print("No matching command")
        print(recognizer.recognize(audio))
def run():
    #set up speech recogntion
    audioSource = sr.Microphone()
    recognizer = sr.Recognizer()

    recognizerThread = recognizer.listen_in_background(audioSource,onSpeech)
    try:
        global gui
        gui = True
        from carolGUI import runCarolGUI
        runCarolGUI()
    except ImportError:
        global gui
        gui = False
        while True:
            pass
    except:
        pass
def runRobot():
    import time
    from Carol.Movement.movement import driveForwards, stopMoving
    driveForwards()
    time.sleep(3)
    stopMoving()

if __name__ == "__main__":
    try:
        import kovan
        runRobot()
    except:
        run()
