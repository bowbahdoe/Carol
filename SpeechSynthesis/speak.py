
import os
import platform
#cdll.LoadLibrary(os.path.abspath(__file__)[0:-9]+"/libspeak.so")
#speak_library = CDLL(os.path.abspath(__file__)[0:-9]+"/libspeak.so")
#speak = speak_library.speak
def shutil_which(pgm):
    """
    python2 backport of python3's shutil.which()
    """
    path = os.getenv('PATH')
    for p in path.split(os.path.pathsep):
        p = os.path.join(p, pgm)
        if os.path.exists(p) and os.access(p, os.X_OK):
            return p

def say(words):
    if(platform.system() == 'Darwin'):
        command = "say " + words
    espeak = shutil_which("espeak")
    if espeak != None:
        command = "espeak  '" + words + "'" #-ven+f3 -p50 -s150
    else:
        command = "padsp flite -voice slt -t '" + words + "'" #kal16
    os.system(command)
