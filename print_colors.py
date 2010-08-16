#!/usr/bin/env python

import re,sys

text = open(sys.argv[1],"r").read()

print "<html><body bgcolor=\"#000000\">"


#pattern = "#FF([0-9A-F]+)"
pattern = "\\S+"

for color in re.findall(pattern,text):
	print "<font color=\"#%s\">%s</font><p/>" % (color,color)
print "</body></html>"
