#! /usr/bin/python

import sys
import numpy as np
import matplotlib.pyplot as plt

# shapes = "os*xD^"
# colours = "kymcrgb"
# markers = [shapes[s] + colours[c] for s in range(0, len(shapes)) for c in range(0, len(colours)) ]

# sci.med rec.sport.baseball comp.sys.ibm.pc.hardware
markers = [ "or", "^g", "Dk", "*m" ]

pngFile = sys.argv[1]
inFile = sys.argv[2]
names=['tfCalc', 'idfCalc', 'perplexity', 'distRatio']

data = np.genfromtxt(inFile, delimiter=' ', names=names)
# scale perplexity to match range 0 .. 3 of other dimensions
data['perplexity'] = (data['perplexity'] -1) / (29.0/3)

minVal = min(data['distRatio'])
minRow = data[data['distRatio'] == minVal]

plt.figure(figsize=(15.0, 15.0), dpi=300) # inches

# for each variable plot it with the other two fixed at the value in minRow
inNames = names[:-1]
for i in range(0, len(inNames)):
  name = inNames[i]
  others = set(inNames)
  others.remove(name)
  mask = data['idfCalc'] >= 0 # all True
  for o in others:
    mask = mask & (data[o] == minRow[o])
  d = np.sort(data[mask], order=[name]) # with lines, the data has to be ordered
  plt.plot(d[name], d['distRatio'], markers[i], linewidth=2.0, linestyle='-', label=name)

plt.legend()
# plt.show()
plt.savefig(pngFile)
