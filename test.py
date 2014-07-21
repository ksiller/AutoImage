#Monica Li
#File: test.py
#Python program that takes json file as input and finds regions of interest

import json
import tifffile
from PIL import Image, ImageDraw
import os
import sys
import math
import numpy

def dist((x1, y1), (x2, y2)):
	return abs(math.sqrt(pow(x2 - x1, 2) + pow(y2 - y1, 2)))

def getROICoord(roiCoord, spotList, spotCount):
	spotPix = []
	if len(roiCoord) == 0:
                return spotList
        item1 = roiCoord[0]
        spotPix.append(item1)
        for k in range(0, len(roiCoord)):
                item2 = roiCoord[k]
		if dist(item1, item2) <= maxRes and dist(item1, item2) >= 0:
                        spotPix.append(item2)
	if len(spotPix) >= maxRes:
		spotCount += 1
                #spotList.append(item1)
		minX = min(spotPix, key=lambda x:x[0])
		minY = min(spotPix, key=lambda x:x[1])
		maxX = max(spotPix, key=lambda x:x[0])
		maxY = max(spotPix, key=lambda x:x[1])
		center = (numpy.mean([minX[0], maxX[0]]), numpy.mean([minY[1], maxY[1]]))
		draw.text(center, str(spotCount), fill=(0, 0, 0, 255))
		spotList.append(center)
	roiCoordSet = set(roiCoord)
	spotPixSet = set(spotPix)
	roiCoordSet -= spotPixSet
	roiCoord = list(roiCoordSet)
        getROICoord(roiCoord, spotList, spotCount)
	return spotList

maxRes = 100 #distance between pixels to be considered separate objects
threshold = 255

#Open file info as JSON and read file handle, open file with PIL
jsonFile = open("/usr/local/ImageJ/scripts/TestJSON.json").read()
jsonData = json.loads(jsonFile)
fileHandle = jsonData["workDir"] + "/" + jsonData["imageFiles"][1]
img = Image.open(fileHandle)

#Extract tiff info using tifffile.py, images are ImageJ but not micromanager
imgTiff = tifffile.TiffFile(fileHandle)

#get pixel size in um
metaFile = open(jsonData["workDir"] + "/metadata.txt")
data = json.load(metaFile)
subdata = data.get("Summary")
pixelSize =  subdata.get("PixelSize_um")

#process image with PIL
img.mode = 'I'
imgBox = img.getbbox()
width = imgBox[2]
height = imgBox[3]
imgCopy = img.convert("RGBA") #convert image to RGBA format
imgCopy.load()
draw = ImageDraw.Draw(imgCopy)
pixData = list(img.getdata()) #get pixel color data
roiCoord = []
spotList = []
spotCount = 0
for pixColor in range(0, len(pixData)):
	neighborCount = 0
	if pixData[pixColor] >= threshold:  #arbritarily set value to identify regions of interest based on experimentation
		y = pixColor / width
		x = pixColor % width
		pixelCoord = (x, y)
		imgCopy.putpixel(pixelCoord, (255, 255, 0, 255)) #color pixels in region of interest yellow
		roiCoord.append(pixelCoord)
spotList = getROICoord(roiCoord, spotList, 0)
outputfile = open(jsonData["workDir"] + "/" + "output.txt", "w")
for x in range(0, len(spotList)):
	microPos = (pixelSize * spotList[x][0], pixelSize * spotList[x][1])
	print "Spot " + str(x+1) + ":\t Pixels: " + str(spotList[x]) + "\t Microscope Position: " + str(microPos) + "\n"
outputfile.close() 
print "Num Spots: " + str(len(spotList))
saveStr = jsonData["workDir"] + "/testimage.png"
imgCopy.save(saveStr)
print "Done!"
