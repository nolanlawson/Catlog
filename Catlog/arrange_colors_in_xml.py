#!/usr/bin/env python

import sys,re

#!/usr/bin/env python

import re,sys

text = open(sys.argv[1],"r").read()

counter = 1

for color in re.findall("\\S+",text):
	
	print "<color name=\"tag_color_%s\">#FF%s</color>" % (str(counter).zfill(2),color)
	counter += 1

