#! /usr/bin/python

import sys
import numpy as np
import matplotlib.pyplot as plt

# shapes = "os*xD^"
# colours = "kymcrgb"
# markers = [shapes[s] + colours[c] for s in range(0, len(shapes)) for c in range(0, len(colours)) ]

# sci.med rec.sport.baseball comp.sys.ibm.pc.hardware
markers = [ "or", "^g", "Dk" ]

pngFile = sys.argv[1]
fileNames = sys.argv[2:]

plt.figure(figsize=(15.0, 15.0), dpi=300) # inches

for i in range(0, len(fileNames)):
  data = np.genfromtxt(fileNames[i], delimiter=' ', names=['x', 'y'])
  plt.plot(data['x'], data['y'], markers[i], label=fileNames[i])

plt.legend()
# plt.show()
plt.savefig(pngFile)
