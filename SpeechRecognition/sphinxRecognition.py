from subprocess import Popen, PIPE

import re
print "STAIFIOAEKFIEJAFPOEAKFPOJEFPOEJSAFPE"
import os
DEVNULL = open(os.devnull, 'wb')
asd = open("da.txt", "w+")
speech = Popen("pocketsphinx_continuous",stdout=asd,stderr=DEVNULL)
from time import sleep
listening = re.compile(r"listening")
result = re.compile (r'00000001: *')
x=0
print "STAIFIOAEKFIEJAFPOEAKFPOJEFPOEJSAFPEJ"
while(True):

    a = open("da.txt","r")
    b = open("c.txt","w")
    for line in a:
        print line
        if(result.search(line)!=None):
            b.write(str(result.search(line).group()))

    x+=1
print "IFEJFOIESJFOSJEFELJFO"
