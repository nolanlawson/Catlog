#!/usr/bin/env python

import re,sys

text = open(sys.argv[1],"r").read()
background = sys.argv[2]

print "<html><body bgcolor=\"#%s\">" % (background)


pattern = re.compile("#FF([0-9A-F]+)",re.IGNORECASE)
#pattern = "\\S+"

for color in pattern.findall(text):
	print "<font color=\"#%s\">%s</font><p/>" % (color,color)
print "</body></html>"
