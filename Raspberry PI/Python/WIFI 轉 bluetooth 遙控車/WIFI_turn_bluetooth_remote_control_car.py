#!/usr/bin/python3
#coding=utf-8

from __future__ import print_function
import socket
import threading
import uuid			# 藍芽 uuid
import bluetooth	# 藍芽
import os
import sys
import time


#------------------------
# 設定 SERVER IP 位置
bind_ip = "0.0.0.0"

# 設定連線的port
bind_port = 9999
#-------------------------

#+++++++++++++++++++++++++
# 設定要連線的藍芽名稱
class bluetooth_value():
    bt_name = "cat pass"
    # bt_name = "htc 628"
    bt_address = None
    bt_port = 1
#+++++++++++++++++++++++++


#-------------------------
# 尋找藍芽 並獲取 MAC 位置
#-------------------------
def bluetooth_MAC_address_search():

    print ("尋找藍芽=%s 並獲取 MAC 位置" %( bluetooth_value.bt_name ) )

    nearby_devices = bluetooth.discover_devices()
    for bdaddr in nearby_devices:
        print (bluetooth.lookup_name( bdaddr )  )# for debug used
        if bluetooth_value.bt_name == bluetooth.lookup_name( bdaddr ):
            bluetooth_value.bt_address = bdaddr
            break
    if bluetooth_value.bt_address is not None:
        print ("found target bluetooth device with address ", bluetooth_value.bt_address )
    else:
        print ("could not find target bluetooth device nearby")




#--------------------------------------------------------
# WIFI socket 初始化
#--------------------------------------------------------
class WIFI_socket_init():
    # create socket
    # AF_INET 代表使用標準 IPv4 位址或主機名稱
    # SOCK_STREAM 代表這會是一個 TCP
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # 設定 server 端的 IP 與 port
    server.bind((bind_ip, bind_port))

    # 監聽是否有連線進入，設定最大連線數 5
    server.listen(5)
    
    print ( "[*] Listening on %s:%d" % (bind_ip, bind_port) )



if __name__ == "__main__":

    os.system('sudo service bluetooth restart')

    time.sleep(1)

    try:
        # WIFI socket 初始化
        WIFI_socket_init()

        # 尋找藍芽 並獲取 MAC 位置
        bluetooth_MAC_address_search()

        # 建立藍芽連線
        client_sock = bluetooth.BluetoothSocket( bluetooth.RFCOMM )
        client_sock.connect((bluetooth_value.bt_address, bluetooth_value.bt_port))		# 藍芽連線

        # client 為 WIFI socket Server 與客戶端溝通物件
        # addr 為 WIFI socket 客戶端位置
        client, addr = WIFI_socket_init.server.accept()
        print ( "[*] Acepted connection from: %s:%d" % (addr[0],addr[1]) )

        temp = ""

        while True:

            # WIFI socket 接收資料，設定一筆資料最大為 1 byte
            request = client.recv(1)
            request=request.decode()    # 二進制編碼轉換成一般字碼
            

            # WIFI socket 回傳資料給客戶端
            msg ='ACK!' + "\r\n" 
            client.send( msg.encode('utf-8') )  # 回傳字碼轉換，並輸出

            if str(request) != str(temp) :

                temp = request

                print ("request=" , request)
                
                client_sock.send( str(request) )	   # WIFI 轉 藍芽輸出


    except KeyboardInterrupt:
        # 停止
        #client.close()    # WIFI socket 停止接收
        #time.sleep(1)
        client.shutdown()   # WIFI socket 關閉
        client_sock.close()		# 藍芽客戶端關閉
        time.sleep(1)
        sys.exit()  # 停止程式

