import Carol.SpeechRecognition as sr
from Carol.VoiceCommands import CommandChecker
import time
import signal
import sys
#set up command checker
checker = CommandChecker.CommandChecker()

def onSpeech(recognizer, audio):
    print("something said")
    try:
        print("You said: " + recognizer.recognize(audio))
        speech = recognizer.recognize(audio)
        checker.checkForMatches(speech)
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
    
    while True:
        time.sleep(0.1)
        x = raw_input()
        checker.checkForMatches(x)
    pass

if __name__ == "__main__":
    run()
