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
import threading
import uuid			# 藍芽 uuid
import bluetooth	# 藍芽
import curses		#鍵盤輸入

PI_camera_width = 640
PI_camera_high = 480
PI_camera_fps = 20

# 尋找藍芽 並獲取 MAC 位置#FFE500
bt_name = "cat pass"
bt_address = None
bt_port = 1

nearby_devices = bluetooth.discover_devices()

for bdaddr in nearby_devices:
    print (bluetooth.lookup_name( bdaddr )  )# for debug used
    if bt_name == bluetooth.lookup_name( bdaddr ):
        bt_address = bdaddr
        break

if bt_address is not None:
    print ("found target bluetooth device with address ", bt_address )
else:
    print ("could not find target bluetooth device nearby")

#  藍芽 server 的方法
'''
server_socket=bluetooth.BluetoothSocket(bluetooth.RFCOMM)
server_socket.bind(("", bluetooth.PORT_ANY))
server_socket.listen(1)
port = server_socket.getsockname()[1]
service_id = str(uuid.uuid4())


bluetooth.advertise_service(server_socket, "self-balancing-robot-Server",
                  service_id = service_id,
                  service_classes = [service_id, bluetooth.SERIAL_PORT_CLASS],
                  profiles = [SERIAL_PORT_PROFILE])


server_socket.connect( ( "00:21:13:02:C2:B2", 1 ) )		# 以 port = 1連線 藍芽

print('等待 RFCOMM 頻道 {} 的連線'.format(port))
client_socket, client_info = server_socket.accept()
print('接受來自 {} 的連線'.format(client_info))

#	read_data = client_socket.recv(1024).decode().lower()		# 接收數據 ， 一次最多接收1024個字符
'''
client_sock = bluetooth.BluetoothSocket( bluetooth.RFCOMM )
client_sock.connect((bt_address, bt_port))		# 藍雅連線


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
colorLower = (24, 100, 100)
colorUpper = (44, 255, 255)

class video():
	Video_show = vs.read()
	close_flag = 0
	x=0
	y=0
	radius=0



def my_main():
	# loop over the frames from the video stream
	while True:
		# grab the next frame from the video stream, Invert 180o, resize the
		# frame, and convert it to the HSV color space
		frame = vs.read()
		frame = cv2.flip(frame, 0) # flip video image vertically
	#	frame = imutils.resize(frame, width=500)
	#	frame = imutils.rotate(frame, angle=180)
		hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)

		# construct a mask for the object color, then perform
		# a series of dilations and erosions to remove any small
		# blobs left in the mask
		mask = cv2.inRange(hsv, colorLower, colorUpper)
		mask = cv2.erode(mask, None, iterations=2)
		mask = cv2.dilate(mask, None, iterations=2)

		# find contours in the mask and initialize the current
		# (x, y) center of the object
		cnts = cv2.findContours(mask.copy(), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
		cnts = cnts[0] if imutils.is_cv2() else cnts[1]
		center = None

		# only proceed if at least one contour was found
		if len(cnts) > 0:
			# find the largest contour in the mask, then use
			# it to compute the minimum enclosing circle and
			# centroid
			c = max(cnts, key=cv2.contourArea)
			((video.x, video.y), video.radius) = cv2.minEnclosingCircle(c)
			M = cv2.moments(c)
			center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))

			# only proceed if the radius meets a minimum size
			if video.radius > 5:
				# draw the circle and centroid on the frame,
				# then update the list of tracked points
				cv2.circle(frame, (int(video.x), int(video.y)), int(video.radius),
					(0, 255, 255), 2)
				cv2.circle(frame, center, 5, (0, 0, 255), -1)
				
				# position Servo at center of circle
				mapObjectPosition(int(video.x), int(video.y),int(video.radius) )


		video.Video_show = frame
		
		if video.close_flag == 1 :
			break
			
				


def main():
	t1 = threading.Thread(target=my_main)
	t2 = threading.Thread(target=my_main)
	t3 = threading.Thread(target=my_main)
	
	t1.start()
	t2.start()
#	t3.start()
	
	while True:

		if	int( video.radius ) < 5:
			client_sock.send("s")		# 藍芽輸出停止
		else :
			if 	( int( video.radius ) < ( 50+10 ) ) and	\
				( int( video.radius ) >(  50-10 ) ) and	\
				( int( video.x ) > ( (PI_camera_width/2)-100 ) ) and	\
				( int( video.x ) < ( (PI_camera_width/2)+100 ) ):
					client_sock.send("s")		# 藍芽輸出停止
			else:
				if int( video.radius ) > ( 50+10 ):
					client_sock.send("b")		# 藍芽輸出後退
				elif int( video.radius ) < ( 50-10 ):
					client_sock.send("f")		# 藍芽輸出前進
					
				if  int( video.x ) < ( (PI_camera_width/2)-100 ):
					client_sock.send("r")		# 藍芽輸出左轉
				elif  int( video.x ) > ( (PI_camera_width/2)+100 ):
					client_sock.send("l")		# 藍芽輸出右轉
#		client_sock.send("s")		# 藍芽輸出停止	

		video.x=0
		video.y=0
		video.radius=0


		# 	show the frame to our screen
#		cv2.imshow("Frame", video.Video_show)
		
		# if [ESC] key is pressed, stop the loop
#		key = cv2.waitKey(10) & 0xFF
		
		key = curses.self.screen.getch()	#當 鍵盤輸入 ESC 時

			
		if key == 27:
			video.close_flag = 1
			break
	# do a bit of cleanup
	print("\n [INFO] Exiting Program and cleanup stuff \n")
	client_sock.send("s")		# 藍芽輸出停止
	cv2.destroyAllWindows()

	client_sock.close()		# 藍芽客戶端關閉
#	server_socket.close()		# 藍芽伺服端關閉
	
	t1.join()
	t2.join()
#	t3.join()
	
	vs.stop()
			
		
	


if __name__ == "__main__":
    main()
