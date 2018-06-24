package com.example.yvtc.androidbluetoothremotecontrolcar;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class car_in_button_mode extends AppCompatActivity {

    private Context context;
    private TextView textView_bluetooth_name;
    private Intent intent;
    private Switch switch_bluetooth_link;
    private ImageView imageView_bluetooth_link;
    private String remote_Device_Info;
    private BluetoothAdapter btAdapter;
    private BTChatService mChatService;
    private String remoteMacAddress;
    private ImageButton button_Top, button_Down, button_Left, button_Right;
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
    private final String TAG = "App";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_in_button_mode);

        context = this;
        intent = getIntent();
        setTitle("Car Button");
        textView_bluetooth_name = (TextView) findViewById(R.id.textView_bluetooth_name_button);
        switch_bluetooth_link = (Switch) findViewById(R.id.switch_bluetooth_link_button);
        imageView_bluetooth_link = (ImageView) findViewById(R.id.imageView_bluetooth_link_button);

        button_Top = (ImageButton) findViewById(R.id.imageButton_up);
        button_Down = (ImageButton) findViewById(R.id.imageButton_down);
        button_Left = (ImageButton) findViewById(R.id.imageButton_left);
        button_Right = (ImageButton) findViewById(R.id.imageButton_right);


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
                        mChatService.connect(device);
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


        button_Top.setOnTouchListener(new Button_OnTouch());
        button_Down.setOnTouchListener(new Button_OnTouch());
        button_Left.setOnTouchListener(new Button_OnTouch());
        button_Right.setOnTouchListener(new Button_OnTouch());

    }

//----------------------------------------- 副程式---------------------------------------------------

    private class Button_OnTouch implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:              //按住事件
                    switch (v.getId()) {
                        case R.id.imageButton_up:
//                            Toast.makeText(context, "car forward", Toast.LENGTH_SHORT).show();
                            directionCmd = GO_FORWARD;
                            sendCMD(directionCmd);
                            break;
                        case R.id.imageButton_down:
//                            Toast.makeText(context, "car back", Toast.LENGTH_SHORT).show();
                            directionCmd = GO_BACKWARD;
                            sendCMD(directionCmd);
                            break;
                        case R.id.imageButton_left:
//                            Toast.makeText(context, "car left", Toast.LENGTH_SHORT).show();
                            directionCmd = TRUN_LEFT;
                            sendCMD(directionCmd);
                            break;
                        case R.id.imageButton_right:
//                            Toast.makeText(context, "car right", Toast.LENGTH_SHORT).show();
                            directionCmd = TRUN_RIGHT;
                            sendCMD(directionCmd);
                            break;
                    }
                    break;

                case MotionEvent.ACTION_UP:                 //鬆開事件
//                    Toast.makeText(context, "car stop", Toast.LENGTH_SHORT).show();
                    directionCmd = CAR_STOP;
                    sendCMD(directionCmd);
                    break;

                case MotionEvent.ACTION_MOVE:            //移動事件
                    break;

            }
            return false;
        }

    }

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
                mChatService.stop();        //  停止藍芽傳輸
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChatService != null){
            mChatService.stop();        //  停止藍芽傳輸
            mChatService = null ;       //  清除執行緒
        }
    }
}
