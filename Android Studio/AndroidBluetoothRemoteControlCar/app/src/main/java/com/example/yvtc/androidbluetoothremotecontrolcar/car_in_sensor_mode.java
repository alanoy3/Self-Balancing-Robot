package com.example.yvtc.androidbluetoothremotecontrolcar;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class car_in_sensor_mode extends AppCompatActivity {

    private Context context;
    private TextView textView_bluetooth_name;
    private Intent intent;
    private Switch switch_bluetooth_link;
    private ImageView imageView_bluetooth_link;
    private SurfaceView surfaceView;
    private SesorSurfaceView Sensor_Surface_View;
    private SensorManager sensor_manager;
    public static float X_value, Y_value, Z_value;
    private Fn_Accelerometer Listen_Accelerometer;
    private final float alpha = (float) 0.98;     //加速計濾波參數
    private Sensor sensor;
    private String remote_Device_Info;
    private BluetoothAdapter btAdapter;
    private BTChatService mChatService;
    private String remoteMacAddress;
    private final String TAG = "App";
    private static final String GO_FORWARD = "f";
    private static final String GO_BACKWARD = "b";
    private static final String TRUN_LEFT = "l";
    private static final String TRUN_RIGHT = "r";
    private static final String CAR_STOP = "s";
    private static final String Song_1 = "1";
    private static final String Song_2 = "2";
    private static final String Song_3 = "3";
    private static final String Song_4 = "4";
    private static final String Song_OFF = "0";
    private String directionCmd, songCmd;
    private final int Accelerometer_Car_Control_Value = 2;          // 加速計 控制 車子 的 最小值

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_in_sensor_mode);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   //禁止休眠
        context = this;
        intent = getIntent();
        setTitle("Car Sensor");
        textView_bluetooth_name = (TextView) findViewById(R.id.textView_bluetooth_name_sensor);
        switch_bluetooth_link = (Switch) findViewById(R.id.switch_bluetooth_link_sensor);
        imageView_bluetooth_link = (ImageView) findViewById(R.id.imageView_bluetooth_link_sensor);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);


//      畫面方向  設定為垂直-------------------------------------------------------------
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//      ---------------------------------------------------------------------------

//        取得藍芽資料 start--------------------------------------------------
        remote_Device_Info = intent.getStringExtra("btData");
//        取得藍芽資料 end--------------------------------------------------


//        顯示選擇 bluetooth 裝置名稱   start--------------------------------------------------
        textView_bluetooth_name.setText(remote_Device_Info.substring(10, remote_Device_Info.length() - 17));       //substring (起始索引（包括） , 结束索引（不包括）)
//        顯示選擇 bluetooth 裝置名稱   end----------------------------------------------------


        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.cancelDiscovery();

        mChatService = new BTChatService(context, mHandler);


//        bluetooth link switch 開關  start---------------------------------------------------
        switch_bluetooth_link.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)  //當 bluetooth link switch 打開時
                {
                    imageView_bluetooth_link.setImageResource(R.drawable.ic_bluetooth_searching_white_48dp);    //換成連線圖片

                    if (remote_Device_Info != null) {
                        remoteMacAddress = remote_Device_Info.substring(remote_Device_Info.length() - 17);
                        BluetoothDevice device = btAdapter.getRemoteDevice(remoteMacAddress);
                        mChatService.connect(device);       // 開啟藍芽傳輸 到  device
                    } else {
                        Toast.makeText(context, "No Paired BT Device", Toast.LENGTH_SHORT).show();
                    }

                } else if (!isChecked)    //當 bluetooth link switch 關閉時
                {
                    imageView_bluetooth_link.setImageResource(R.drawable.ic_bluetooth_disabled_white_48dp); //換成離線圖片
                    if (mChatService != null){
                        mChatService.stop();        //  停止藍芽傳輸
                        mChatService = null ;       //  清除執行緒
                    }
                }
            }
        });
//        bluetooth link switch 開關  end-----------------------------------------------------

//        SurfaceView   start---------------------------------------------------
        Sensor_Surface_View = new SesorSurfaceView(context, surfaceView);
//        SurfaceView   end---------------------------------------------------


//        啟用加速計 start---------------------------------------------------------
        sensor_manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensor_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Listen_Accelerometer = new Fn_Accelerometer();
        sensor_manager.registerListener(Listen_Accelerometer, sensor, SensorManager.SENSOR_DELAY_FASTEST);
//        啟用加速計 end-----------------------------------------------------------


    }
//----------------------------------------- 副程式---------------------------------------------------

    //---------------------------------- 發送命令給藍芽設備--------------------------------------
    // Sends a Command to remote BT device.
    private void sendCMD(String message) {
        // Check that we're actually connected before trying anything
        int mState = mChatService.getState();
        Log.d(TAG, "btstate in sendMessage =" + mState);

        if (mState != BTChatService.STATE_CONNECTED) {
            Log.d(TAG, "btstate =" + mState);
            // Toast.makeText(context, "Bluetooth device is not connected. ", Toast.LENGTH_SHORT).show();
            return;

        } else {
            // Check that there's actually something to send
            if (message.length() > 0) {
                // Get the message bytes and tell the BluetoothChatService to write
                byte[] send = message.getBytes();
                mChatService.BTWrite(send);

            }
        }

    }

    //----------------------    處理 BluetoothChatService 的訊息-----------------------------------------
    // The Handler that gets information back from the BluetoothChatService
    //There is no message queue leak problem
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String mConnectedDevice = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(context, "Connected to " + mConnectedDevice, Toast.LENGTH_SHORT).show();
                    break;

                case Constants.MESSAGE_TOAST:
                    Toast.makeText(context, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    break;

                case Constants.MESSAGE_ServerMode:
                    // Toast.makeText(context,"Enter Server accept state.",Toast.LENGTH_SHORT).show();   //display on TextView
                    break;

                case Constants.MESSAGE_ClientMode:
                    //  Toast.makeText(context,"Enter Client connect state.",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    //  建立選項選單  start--------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Exit"); //在 menu 新增 Exit
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);    //設定menu Exit顯示在標題欄上，屬性設為 SHOW_AS_ACTION_ALWAYS=always
        return true;
    }
//  建立選項選單  end----------------------------------------

    //  處理選單點擊事件    start---------------------------------
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);     //建立警報對話框物件
        builder.setTitle("  Exit");                                         //設定標題
        builder.setIcon(android.R.drawable.ic_dialog_info);                 //設定縮圖

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {     //建立最右邊的按鈕，為OK
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mChatService != null){
                    mChatService.stop();        //  停止藍芽傳輸
                    mChatService = null ;       //  清除執行緒
                }
                finish();              //關閉這個Activity
                dialog.dismiss();       //關閉對話框
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {     //建立最左邊的按鈕，為OK
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();       //關閉對話框
            }
        });
        builder.create().show();        //建立對話框設定並顯示
        return super.onOptionsItemSelected(item);
    }
//  處理選單點擊事件    end-----------------------------------


    //    加速計實作 start----------------------------------------------------
    private class Fn_Accelerometer implements SensorEventListener {
        float X, Y, Z;

        @Override
        public void onSensorChanged(SensorEvent event) {

//  低通濾波 start-------------------------------------------------------------
//  分離出重力加速度
            X = alpha * X + (1 - alpha) * event.values[0];
            Y = alpha * Y + (1 - alpha) * event.values[1];
//            Z  = alpha * Z + (1 - alpha) * event.values[2];
//  低通濾波 end--------------------------------------------------------------

            X_value = X;
            Y_value = Y;
//            Z_value=Z;


//      遙控車子    start------------------------------------------------------------------------------------
            if (Y_value < -Accelerometer_Car_Control_Value) {
//      Toast.makeText(context, "car forward", Toast.LENGTH_SHORT).show();
                directionCmd = GO_FORWARD;
                sendCMD(directionCmd);
            }
            if (Y_value > Accelerometer_Car_Control_Value) {
//       Toast.makeText(context, "car back", Toast.LENGTH_SHORT).show();
                directionCmd = GO_BACKWARD;
                sendCMD(directionCmd);
            }
            if (X_value > Accelerometer_Car_Control_Value) {
//      Toast.makeText(context, "car left", Toast.LENGTH_SHORT).show();
                directionCmd = TRUN_LEFT;
                sendCMD(directionCmd);
            }

            if (X_value < -Accelerometer_Car_Control_Value) {
//      Toast.makeText(context, "car right", Toast.LENGTH_SHORT).show();
                directionCmd = TRUN_RIGHT;
                sendCMD(directionCmd);
            }

            if (
                    ( X_value < Accelerometer_Car_Control_Value &&
                      X_value > -Accelerometer_Car_Control_Value )
                            &&
                    ( Y_value < Accelerometer_Car_Control_Value &&
                      Y_value > -Accelerometer_Car_Control_Value )
                    ) {
//      Toast.makeText(context, "car stop", Toast.LENGTH_SHORT).show();
                directionCmd = CAR_STOP;
                sendCMD(directionCmd);
            }
//      遙控車子    end------------------------------------------------------------------------------------


        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
//     加速計實作 end------------------------------------------------------
    }


    //    當Activity結束時關閉感測器 start-----------------------------------
    @Override
    protected void onDestroy() {
        sensor_manager.unregisterListener(Listen_Accelerometer);
        super.onDestroy();
        if (mChatService != null){
            mChatService.stop();        //  停止藍芽傳輸
            mChatService = null ;       //  清除執行緒
        }
    }
//    當Activity結束時關閉感測器 end---------------------------------------


    //    當Activity重啟時關閉感測器 start-----------------------------------
    @Override
    protected void onStart() {
        sensor_manager.registerListener(Listen_Accelerometer, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        Sensor_Surface_View = new SesorSurfaceView(context, surfaceView);
        super.onStart();
    }
//    當Activity重啟時關閉感測器 end-----------------------------------

}
