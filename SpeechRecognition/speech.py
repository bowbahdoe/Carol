from GoogleRecognition import Microphone, Recognizer
r = Recognizer()

with Microphone() as source:                # use the default microphone as the audio source
    audio = r.listen(source)                   # listen for the first phrase and extract it into audio data
    print "ge"
try:
    print("You said " + r.recognize(audio))    # recognize speech using Google Speech Recognition
except LookupError:                            # speech is unintelligible
    print("Could not understand audio")
