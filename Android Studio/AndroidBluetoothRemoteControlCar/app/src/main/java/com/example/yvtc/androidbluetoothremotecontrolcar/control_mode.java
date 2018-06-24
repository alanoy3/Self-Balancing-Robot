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
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class control_mode extends AppCompatActivity {

    private Context context;
    private Intent intent;
    private TextView textView_bluetooth_name;
    private Switch switch_bluetooth_link;
    private ImageView imageView_bluetooth_link;
    private String remote_Device_Info;
    private BluetoothAdapter btAdapter;
    private BTChatService mChatService;
    private String remoteMacAddress;
    private final String TAG = "App";
    private Spinner spinner;
    private Button play_song_button;
    private String songCMD;
    private Switch lamp1_Switch ,lamp2_Switch , fan1_Switch , fan2_Switch;
    private String lampCMD;

    private static final String Song_OFF = "0";
    private static final String Song_1 = "1";
    private static final String Song_2 = "2";
    private static final String Song_3 = "3";
    private static final String Song_4 = "4";
    private static final String Lamp1_ON = "x";
    private static final String Lamp1_OFF = "y";
    private static final String Lamp2_ON = "c";
    private static final String Lamp2_OFF = "d";
    private static final String Fan1_ON = "h";
    private static final String Fan1_OFF = "i";
    private static final String Fan2_ON = "j";
    private static final String Fan2_OFF = "k";
    private static boolean lamp1_Flage = false;
    private static boolean lamp2_Flage = false;
    private static boolean fan1_Flage = false;
    private static boolean fan2_Flage = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_mode);

        context = this;
        intent = getIntent();
        setTitle("Car In Button");
        textView_bluetooth_name = (TextView) findViewById(R.id.textView_bluetooth_name_control);
        switch_bluetooth_link = (Switch) findViewById(R.id.switch_bluetooth_link_control);
        imageView_bluetooth_link = (ImageView) findViewById(R.id.imageView_bluetooth_link_control);


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
                    if (mChatService != null) {
                        mChatService.stop();        //  停止藍芽傳輸
                        mChatService = null;       //  清除執行緒
                    }
                }
            }
        });
//        bluetooth link switch 開關  end-----------------------------------------------------


//----------------------------------歌曲列表------------------------------------------
        spinner = (Spinner) findViewById(R.id.spinner_song);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        songCMD = Song_OFF;
                        break;

                    case 1:
                        songCMD = Song_1;
                        break;

                    case 2:
                        songCMD = Song_2;
                        break;

                    case 3:
                        songCMD = Song_3;
                        break;

                    case 4:
                        songCMD = Song_4;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

//------------------撥放音樂-------------------------------------
        play_song_button = (Button) findViewById(R.id.button_song);
        play_song_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCMD(songCMD);
            }
        });


        lamp1_Switch = (Switch) findViewById(R.id.switch_control_lamp01);
        lamp1_Switch.setChecked(lamp1_Flage);
        lamp1_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                lamp1_Flage = isChecked;
                if(isChecked){
                    lampCMD = Lamp1_ON;
                }
                else {
                    lampCMD=Lamp1_OFF;
                }
                sendCMD(lampCMD);
            }
        });


        lamp2_Switch = (Switch) findViewById(R.id.switch_control_lamp02);
        lamp2_Switch.setChecked(lamp1_Flage);
        lamp2_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                lamp2_Flage = isChecked;
                if(isChecked){
                    lampCMD = Lamp2_ON;
                }
                else {
                    lampCMD=Lamp2_OFF;
                }
                sendCMD(lampCMD);
            }
        });

        fan1_Switch = (Switch) findViewById(R.id.switch_control_FAN01);
        fan1_Switch.setChecked(lamp1_Flage);
        fan1_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fan1_Flage = isChecked;
                if(isChecked){
                    lampCMD = Fan1_ON;
                }
                else {
                    lampCMD=Fan1_OFF;
                }
                sendCMD(lampCMD);
            }
        });

        fan2_Switch = (Switch) findViewById(R.id.switch_control_FAN02);
        fan2_Switch.setChecked(lamp1_Flage);
        fan2_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fan2_Flage = isChecked;
                if(isChecked){
                    lampCMD = Fan2_ON;
                }
                else {
                    lampCMD=Fan2_OFF;
                }
                sendCMD(lampCMD);
            }
        });


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
        if(mChatService!=null){
            mChatService.stop();
            mChatService=null;
        }
    }
}
