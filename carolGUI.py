from Tkinter import *
#from Tkinter import ttk
import ttk
from commands import getoutput as check_output
import os
from __main__ import speechText as speech
def runCarolGUI():
    def updateSpeechText(*args):
        try:
            pass
        except:
            pass
    root = Tk()

    mainframe = ttk.Frame(root, padding="3 3 12 12")
    mainframe.grid(column=0, row=0, sticky=(N, W, E, S))
    mainframe.columnconfigure(0, weight=1)
    mainframe.rowconfigure(0, weight=1)
    global speech

    command = StringVar()
    #commandBox =
    command_entry = ttk.Entry(mainframe, width=20, textvariable=command)
    command_entry.grid(column=1, row=1, sticky=(W, E))
    ttk.Label(mainframe, text="You Said:").grid(column=0, row=2, sticky=(W, E))
    ttk.Label(mainframe, textvariable=speech).grid(column=1, row=2, sticky=(W, E))


    ttk.Button(mainframe, text="Translate", command=updateSpeechText).grid(column=3, row=3, sticky=(W,S))

    mainframe.title = "Carol"
    for child in mainframe.winfo_children(): child.grid_configure(padx=5, pady=5)
    root.title = "Carol"
    root.bind('<Return>', updateSpeechText)
    #latin_entry.focus()


    root.mainloop()
    mainloop()
try:
    runCarolGUI()
except:
    pass
