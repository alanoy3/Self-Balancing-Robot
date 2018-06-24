package com.example.yvtc.androidbluetoothremotecontrolcar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Julia on 2017/10/27.
 */

public class BTChatService {

    private static final String TAG ="BT_Chat" ;
    private static final String Service_NAME="BT_Chat";

    private static final int STATE_NORMAL = 0;
    private static final int STATE_WaitingConnecting = 1;
    private static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    private static final int STATE_STOP = 4;


    // Unique UUID for this application
    private static final UUID UUID_String =
          //  UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
              UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");   //SPP UUID
          //   UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter btAdapter;
    private int btState;
    private final Handler btHandler;
    private AcceptThread btAcceptThread;
    private ConnectingThread  btConnectingThread;
    private ConnectedThread btConnectedThread;


    public BTChatService(Context context , Handler handler) {

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG,"new a BTChatService");
        btState = STATE_NORMAL;
        btHandler = handler;

    }

    //return current BT state to UI thread
    public int getState(){
        return btState;
    }

    // Start server mode AcceptThread
    public void serverStart(){

        if (btConnectingThread != null) {
            btConnectingThread.cancel();
            btConnectingThread = null;
        }

        if (btConnectedThread != null) {
            btConnectedThread.cancel();
            btConnectedThread = null;
        }

        if(btAcceptThread == null){
            Log.d(TAG,"new an AcceptThread");
            btAcceptThread = new AcceptThread();
            btAcceptThread.start();   //start executing thread run()
        }

    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device The BluetoothDevice to connect
     */
    public void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (btConnectingThread != null) {
              btConnectingThread.cancel();
              btConnectingThread = null;
        }

        if (btConnectedThread != null) {
            btConnectedThread.cancel();
            btConnectedThread = null;
        }

        if ( btAcceptThread != null) {
            btAcceptThread.cancel();
            btAcceptThread = null;
        }

        // Start the thread to connect with the given device
        Log.d(TAG, "New a connecting thread. " );
        btConnectingThread = new ConnectingThread(device);
        btConnectingThread.start();

    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write to btOutStream
     * @see ConnectedThread#write(byte[])
     */
    public void BTWrite(byte[] out) {

           if (btState != STATE_CONNECTED) return;    //if BT is not in connected state, do not send data to BT
           btConnectedThread.write(out);              // call ConnectedThread#write(byte[])

    }

    /**
     * Stop all threads when EditActivity destroy
     */
    public synchronized void stop() {
        Log.d(TAG, "All thread stop");

        if (btConnectingThread != null) {
            btConnectingThread.cancel();
            btConnectingThread = null;
        }

        if (btConnectedThread != null) {
            btConnectedThread.cancel();
            btConnectedThread = null;
        }

        if ( btAcceptThread != null) {
             btAcceptThread.cancel();
             btAcceptThread = null;
        }

        btState = STATE_STOP;

    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {

        private final BluetoothServerSocket BTServerSocket;
        BluetoothServerSocket tempSocket;
        BluetoothSocket btSocket;
        private BluetoothDevice device;

        public AcceptThread() {

            // get BT ServerSocket
            try {
                tempSocket = btAdapter.listenUsingInsecureRfcommWithServiceRecord(Service_NAME, UUID_String);
                Log.d(TAG,"Get BT ServerSocket OK");

            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG,"Get BT Server Socket fail : "+e);
            }

            BTServerSocket=tempSocket;
            btState = STATE_WaitingConnecting;
        }


        public void run(){

            while (btState != STATE_CONNECTED) {
                try {
               // This is a blocking call and will only return on a successful connection or an exception
                    btSocket = BTServerSocket.accept();             // BT server 等待連線

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG,"Get BT Socket fail"+e);

                    break;   //exit while loop , close accept thread
                }

                // If a connection was accepted
                if (btSocket != null) {

                     Log.d(TAG,"Accept a BTSocket and check current State : "+btState);

                        switch (btState) {
                            case STATE_WaitingConnecting:    // Start the connected thread.
                            case STATE_CONNECTING:

                                device= btSocket.getRemoteDevice();

                                // Cancel any thread attempting to make a connection
                                if (btConnectingThread != null) {
                                    btConnectingThread.cancel();
                                    btConnectingThread = null;
                                }

                                // Cancel any thread currently running a connection
                                if (btConnectedThread != null) {
                                    btConnectedThread.cancel();
                                    btConnectedThread = null;
                                }

                                // Cancel the accept thread because we only want to connect to one device
                                if (btAcceptThread != null) {
                                    btAcceptThread.cancel();
                                    btAcceptThread = null;
                                }

                                // Start the thread to manage the connection and perform transmissions
                                btConnectedThread = new ConnectedThread(btSocket);
                                btConnectedThread.start();

                                // Send the name of the connected device back to the UI Activity
                                Message msg = btHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
                                Bundle bundle = new Bundle();
                                bundle.putString(Constants.DEVICE_NAME, device.getName());
                                msg.setData(bundle);
                                btHandler.sendMessage(msg);

                                break;

                            case STATE_NORMAL:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                Log.d(TAG,"BTSocket close");
                                try {
                                    btSocket.close();
                                } catch (IOException e) {
                                    Log.d(TAG, "Could not close unwanted socket", e);
                                }
                                break;

                        }
                }
            }
        }


        public void cancel(){

              try {
                    Log.d(TAG, " close() of BTServerSocket. ");
                    BTServerSocket.close();
              } catch (IOException e) {
                    Log.d(TAG, "close() of BTServerSocket failed : "+ e);
              }

        }

    }


    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket btSocket;
        private final InputStream btInStream;
        private final OutputStream btOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread: " + socket);
            btSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "InputStream and OutputStream did not create", e);
            }

            btInStream = tmpIn;
            btOutStream = tmpOut;
            btState = STATE_CONNECTED;
        }

        public void run() {
            Log.d(TAG, "BEGIN get data in ConnectedThread");
            byte[] buffer = new byte[1024];
            int bytesReadLength;

            // Keep listening to the InputStream while connected
            while (btState == STATE_CONNECTED) {
                try {
                    // Read BT data from the InputStream and put in buffer
                    bytesReadLength = btInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    btHandler.obtainMessage(Constants.MESSAGE_READ, bytesReadLength, -1, buffer)
                            .sendToTarget();

                } catch (IOException e) {
                    Log.d(TAG, "disconnected", e);
                    // Send a failure message back to the Activity
                    Message msg = btHandler.obtainMessage(Constants.MESSAGE_TOAST);
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.TOAST, "Device connection was lost");
                    msg.setData(bundle);
                    btHandler.sendMessage(msg);

                    // Set BT state to server mode
                    if(btState != STATE_STOP) {
                        btState = STATE_NORMAL;
                        serverStart();
                    }
                    break;  //exit run() loop
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {

                btOutStream.write(buffer);            //send data to BT

            } catch (IOException e) {
                Log.d(TAG, "Exception during write data to BT outputStream", e);
            }
        }


        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "close() of connected BTsocket is failed", e);
            }
        }

    }



    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectingThread extends Thread {
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;

        public ConnectingThread(BluetoothDevice device) {
            btDevice = device;
            BluetoothSocket tmpSocket = null;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {

                    tmpSocket = device.createInsecureRfcommSocketToServiceRecord(UUID_String);
                    Log.d(TAG, "Get BTSocket in connecting mode");
            } catch (IOException e) {
                Log.d(TAG, "BTSocket failed in connecting mode"+ e);
            }
            btSocket = tmpSocket;
            btState = STATE_CONNECTING;
        }

        public void run() {
            Log.d(TAG, "ConnectingThread is running" );

            // Make a connection to the BluetoothSocket
            try {
            // This is a blocking call and will only return on a successful connection or an exception
                btSocket.connect();
                Log.d(TAG, "BT is connecting" );
            } catch (IOException e) {
                // Close the socket
                try {
                      btSocket.close();
                } catch (IOException e2) {
                    Log.d(TAG, "unable to close() BTSocket during connection failure", e2);
                }
                // Send a failure message back to the Activity
                Message msg = btHandler.obtainMessage(Constants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.TOAST, "Unable to connect device");
                msg.setData(bundle);
                btHandler.sendMessage(msg);

                // Connecting fail come back to Server mode
                if(btState != STATE_STOP) {
                    btState = STATE_NORMAL;
                    serverStart();
                }
                return;  //exit run()
            }

            // Cancel any thread currently running a connection
            if (btConnectedThread != null) {
                btConnectedThread.cancel();
                btConnectedThread = null;
            }

            // Cancel the accept thread because we only want to connect to one device
            if (btAcceptThread != null) {
                btAcceptThread.cancel();
                btAcceptThread = null;
            }

            // Start the connected thread to manage transmissions
            btConnectedThread = new ConnectedThread(btSocket);
            btConnectedThread.start();

            // Send the name of the connected device back to the UI Activity
            Message msg = btHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.DEVICE_NAME, btDevice.getName());
            msg.setData(bundle);
            btHandler.sendMessage(msg);

        }


        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "unable to close() BTSocket in connecting cancel state", e);
            }
        }
    }


}
