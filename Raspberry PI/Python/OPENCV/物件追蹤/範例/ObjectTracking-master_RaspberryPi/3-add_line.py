import numpy as np
import cv2
import time
import os


# This system command loads the right drivers for the Raspberry Pi camera
os.system('sudo modprobe bcm2835-v4l2')

w=480
h=320

# This identifies the device and sets up the camera
my_camera = cv2.VideoCapture(0)
# The next two lines set the dimensions of the image to w*h pixels
my_camera.set(3,w)
my_camera.set(4,h)
# A short pause to allow the camera to warm up
time.sleep(2)

# A 'read' from the camera returns two things, a success indicator and the image
success, image = my_camera.read()
# Is the image the right way up? If not, flip it. Try changing the -1 to 0 or 1.
image = cv2.flip(image,-1)
cv2.imshow('View',image)
cv2.waitKey(0)
# We could apply a blur to remove noise
image = cv2.GaussianBlur(image,(5,5),0)

# change the colour space to HSV
image_HSV = cv2.cvtColor(image,cv2.COLOR_BGR2HSV)
# define the range of hues to detect
lower_pink = np.array([160,50,50])
upper_pink = np.array([175,255,255])
# create a mask that identifies the pixels in the range of hues
mask = cv2.inRange(image_HSV,lower_pink,upper_pink)
# Blur to remove noise
mask = cv2.GaussianBlur(mask,(5,5),0)
cv2.imshow('View',mask)
cv2.waitKey(0)        
# mask is a matrix of 0s and 255s, with 255 for pixels that were in the range 
# We can count the number of 255s in each column as follows
# LRarray is a list of numbers, one for each column, containing the sum of
# entries in that column divided by 255
# (change axis to 1 to sum over rows instead)
LRarray = np.sum(mask,axis=0)/255
max_x_intensity = 0
max_x_coordinate = 0
for i in range(w):
    if LRarray[i]>max_x_intensity:
        max_x_coordinate=i
        max_x_intensity=LRarray[i]
        
# Add a line to image, from point (max_x_coordinate,0) to point (max_x_coordinate,h-1) with colour [255,255,255]
cv2.line(image,(max_x_coordinate,0),(max_x_coordinate,h-1),[255,255,255])
cv2.imshow('View',image)
cv2.waitKey(0)
cv2.destroyAllWindows()
my_camera.release()
# due to a bug in openCV you need to call wantKey three times to get the
# window to dissappear properly. Each wait only last 10 milliseconds
cv2.waitKey(10)
time.sleep(0.1)
cv2.waitKey(10)
cv2.waitKey(10)
