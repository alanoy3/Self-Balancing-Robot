#!/usr/bin/python3
#coding=utf-8

'''
Object detection and tracking with OpenCV
    ==> Turning a LED on detection and
    ==> Printing object position Coordinates 

    Based on original tracking object code developed by Adrian Rosebrock
    Visit original post: https://www.pyimagesearch.com/2016/05/09/opencv-rpi-gpio-and-gpio-zero-on-the-raspberry-pi/

Developed by Marcelo Rovai - MJRoBot.org @ 9Feb2018 
'''

# import the necessary packages
from __future__ import print_function
from imutils.video import VideoStream
import imutils
import time
import cv2
import os
from multiprocessing import Process , Pool

PI_camera_width = 640
PI_camera_high = 480
PI_camera_fps = 30



# This system command loads the right drivers for the Raspberry Pi camera
os.system('sudo modprobe bcm2835-v4l2')

# print object coordinates
def mapObjectPosition (x, y,radius):
    print ("[INFO] Object Center coordenates at X0 = {0}	,	Y0 =  {1}	,	radius={2}".format(x, y,radius))

# initialize the video stream and allow the camera sensor to warmup
print("[INFO] waiting for camera to warmup...")
vs = VideoStream(src=0,usePiCamera=True,resolution=(PI_camera_width, PI_camera_high),framerate=PI_camera_fps).start()
time.sleep(2)

# define the lower and upper boundaries of the object
# to be tracked in the HSV color space
colorLower = (14, 100, 100)
colorUpper = (34, 255, 255)


def camera_Convert (hsv):
	# construct a mask for the object color, then perform
	# a series of dilations and erosions to remove any small
	# blobs left in the mask
	mask = cv2.inRange(hsv, colorLower, colorUpper)
	mask = cv2.erode(mask, None, iterations=2)
	mask = cv2.dilate(mask, None, iterations=2)
	return mask

def find_contours (mask):
	# find contours in the mask and initialize the current
	# (x, y) center of the object
	cnts = cv2.findContours(mask.copy(), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
	cnts = cnts[0] if imutils.is_cv2() else cnts[1]
	return cnts



def my_main():
	while True:
		# loop over the frames from the video stream
		frame = vs.read()
		
		# grab the next frame from the video stream, Invert 180o, resize the
		frame = cv2.flip(frame, 0) # flip video image vertically
		# frame, and convert it to the HSV color space
		hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
		
		pool= Pool()
		camera_Convert_reult = pool.map(camera_Convert, [hsv] )
		mask=camera_Convert_reult[0]
		
		find_contours_reult = pool.map(find_contours, [mask] )
		cnts = find_contours_reult[0]
		
		center = None

		# only proceed if at least one contour was found
		if len(cnts) > 0:
			# find the largest contour in the mask, then use
			# it to compute the minimum enclosing circle and
			# centroid
			c = max(cnts, key=cv2.contourArea)
			((x, y), radius) = cv2.minEnclosingCircle(c)
			M = cv2.moments(c)
			center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))

			# only proceed if the radius meets a minimum size
			if radius > 10:
				# draw the circle and centroid on the frame,
				# then update the list of tracked points
				cv2.circle(frame, (int(x), int(y)), int(radius),
					(0, 255, 255), 2)
				cv2.circle(frame, center, 5, (0, 0, 255), -1)
					
				# position Servo at center of circle
				mapObjectPosition(int(x), int(y),int(radius) )

		# 	show the frame to our screen
		cv2.imshow("Frame", frame[0] )
					
		# if [ESC] key is pressed, stop the loop
		key = cv2.waitKey(1) & 0xFF
		if key == 27:
			break

	
	
			
				


def main():

	
	my_main()
		
		
	# do a bit of cleanup
	print("\n [INFO] Exiting Program and cleanup stuff \n")
	cv2.destroyAllWindows()
	vs.stop()
	


if __name__ == "__main__":
    main()
