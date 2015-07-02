from Tkinter import *
#from Tkinter import ttk
import ttk
from commands import getoutput as check_output
import os
def runWords():
    def updateSpeechText(*args):
        try:
            latin = str(latinToTranslate.get())
            #print "bin/words \"" + latin + "\""
            translation.set(check_output("bin/words \"" + latin + "\"" ))
        except:
            pass
    root = Tk()
    root.title = "Wowdadwadadrds"
    mainframe = ttk.Frame(root, padding="3 3 12 12")
    mainframe.grid(column=0, row=0, sticky=(N, W, E, S))
    mainframe.columnconfigure(0, weight=1)
    mainframe.rowconfigure(0, weight=1)

    translation =  StringVar()
    latinToTranslate = StringVar()
    #commandBox =
    latin_entry = ttk.Entry(mainframe, width=30, textvariable=latinToTranslate)
    latin_entry.grid(column=1, row=1, sticky=(W, E))
    ttk.Label(mainframe, textvariable="Latin:").grid(column=0, row=2, sticky=(W, E))
    ttk.Label(mainframe, textvariable=translation).grid(column=1, row=2, sticky=(W, E))


    ttk.Button(mainframe, text="Translate", command=updateSpeechText).grid(column=3, row=3, sticky=(W,S))

    mainframe.title = "Words"
    for child in mainframe.winfo_children(): child.grid_configure(padx=5, pady=5)
    root.title = "Words"
    root.bind('<Return>', updateSpeechText)
    #latin_entry.focus()


    root.mainloop()
    mainloop()
try:
    runWords()
except:
    pass
