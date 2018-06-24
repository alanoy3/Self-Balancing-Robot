package com.example.yvtc.androidbluetoothremotecontrolcar;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private Switch switch_bluetooth;
    private ListView list_view_bluetooth;
    private int flage_mode_menu = 0;
    private TextView textView_show_now;
    private Intent intent;
    private BluetoothAdapter bluetooth_Adapter;         //藍芽管理者
    private static final int REQUEST_ENABLE_bluetooth = 2;
    private Set<BluetoothDevice> all_BT_Devices;
    private ArrayList<String> BT_Devices_List;
    private ArrayAdapter<String> list_view_bluetooth_adapter;       //藍芽資料
    private static final int Permission_REQUEST_CODE = 100;
    private boolean receiverFlag = false;
    private String itemData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        switch_bluetooth = (Switch) findViewById(R.id.switch_bluetooth_enable);
        list_view_bluetooth = (ListView) findViewById(R.id.list_view_bluetooth);
        textView_show_now = (TextView) findViewById(R.id.textView_show_now);


//      畫面方向  設定為垂直-------------------------------------------------------------
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//      ---------------------------------------------------------------------------

//      藍芽管理 start------------------------------------------------------
        bluetooth_Adapter = BluetoothAdapter.getDefaultAdapter();       //藍芽管理者
        if (bluetooth_Adapter == null)                              //沒有藍芽裝置
        {
            finish();
        } else if (!bluetooth_Adapter.isEnabled())               //檢查BT是否有打開
        {
            Intent bluetooth_Intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bluetooth_Intent, REQUEST_ENABLE_bluetooth);
        }
//      藍芽管理 end------------------------------------------------------


//      開關顯示藍芽列表 start------------------------------------------------
        switch_bluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    all_BT_Devices = bluetooth_Adapter.getBondedDevices();      //查詢所有配對過的BT設備
                    BT_Devices_List = new ArrayList<String>();                  //建一個String型態的ArrayList
                    if (all_BT_Devices.size() > 0) {
                        for (BluetoothDevice device : all_BT_Devices)            //foreach method
                        {
                            BT_Devices_List.add("Paired :  " + device.getName() + "\n" + device.getAddress());      //將取得的藍芽裝置放入btDeviceList
                        }


                        list_view_bluetooth_adapter = new ArrayAdapter<String>(context, android.R.layout.simple_expandable_list_item_1, BT_Devices_List);       //去這複製"simple_expandable_list_item_1"貼到layout下面即可自訂義_C:\Users\YVTC\AppData\Local\Android\Sdk\platforms\android-26\data\res\layout
                        list_view_bluetooth.setAdapter(list_view_bluetooth_adapter);

                        list_view_bluetooth.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                if (flage_mode_menu == 0) {
                                    Toast.makeText(context, "Please select the mode!!!", Toast.LENGTH_SHORT).show();  //沒選擇模式時跳出警告
                                } else {

                                    itemData = parent.getItemAtPosition(position).toString();
                                    intent.putExtra("btData", itemData);
                                    startActivity(intent);
                                }
                            }
                        });
                    }
                } else {
                    if (list_view_bluetooth_adapter != null) {
                        list_view_bluetooth_adapter.clear();
                        list_view_bluetooth_adapter.notifyDataSetChanged();
                    }
                }
            }
        });
//      開關顯示藍芽列表 end--------------------------------------------------


//      狀態顯示    start------------------------------------------------------
        textView_show_now.setText("NOW：Please select the mode!!!");
//      狀態顯示    end------------------------------------------------------


    }
//--------------------------副程式-------------------------------------------------------------


    //  建立選項選單  start--------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater ifflater = getMenuInflater();
        ifflater.inflate(R.menu.menu, menu);
        return true;
    }
//  建立選項選單  end----------------------------------------

    //  處理選單點擊事件    start---------------------------------
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                int permission = ActivityCompat.checkSelfPermission(context, "Manifest.permission.ACCESS_COARSE_LOCATION");

                if (permission != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                            Permission_REQUEST_CODE);
                }

                bluetooth_Adapter.startDiscovery();     //	發現周邊的 BT 裝置
                //***廣播接收器 Broadcast Receiver***//
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                receiverFlag = true;
                registerReceiver(mReceiver, filter);
                Toast.makeText(context, "Start to scan BT device", Toast.LENGTH_SHORT).show();

                break;
            case R.id.menu_discoverable:

                bluetooth_Adapter.cancelDiscovery();            // 關閉 BT 掃描
                Intent intent_discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                intent_discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 100);     // 100s(秒)
                startActivity(intent_discoverable);
                Toast.makeText(context, "Discoverable to others", Toast.LENGTH_SHORT).show();

                break;
            case R.id.menu_mode_car_button_mode:
                flage_mode_menu = 1;
                textView_show_now.setText("NOW：Car Button Mode");
                intent = new Intent(context, car_in_button_mode.class);
                break;
            case R.id.menu_mode_car_sensor_mode:
                flage_mode_menu = 2;
                textView_show_now.setText("NOW：Car Sensor Mode");
                intent = new Intent(context, car_in_sensor_mode.class);
                break;
            case R.id.menu_mode_control_mode:
                flage_mode_menu = 3;
                textView_show_now.setText("NOW：Control Mode");
                intent = new Intent(context, control_mode.class);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
//  處理選單點擊事件    end-----------------------------------


    //***廣播接受器 Broadcast Receiver***//
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String antion = intent.getAction();

            if (antion.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (list_view_bluetooth_adapter == null) {
                    Toast.makeText(context, "Please switch on", Toast.LENGTH_SHORT).show();
                } else {
                    list_view_bluetooth_adapter.add("Found :  " + device.getName() + "\n" + device.getAddress());
                }
            }
        }
    };


    // 向OS請求權限回傳的結果 start---------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case Permission_REQUEST_CODE:
                if (grantResults[0] == PERMISSION_GRANTED) {

                } else {

                }
                return;
        }
    }
// 向OS請求權限回傳的結果 end---------------------------------


    //  開啟藍芽    start-------------------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (resultCode) {
            case REQUEST_ENABLE_bluetooth:

                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(context, "Deny BT enable", Toast.LENGTH_SHORT).show();
                } else if (resultCode == RESULT_OK) {
                    Toast.makeText(context, "Turn on BT ", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    //    當Activity結束時關閉 start-----------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();

        bluetooth_Adapter.cancelDiscovery();        // 關閉 BT 掃描

        if (mReceiver != null) {
            if (receiverFlag) {
                unregisterReceiver(mReceiver);
                receiverFlag = false;
            }
        }
    }
//    當Activity結束時關閉 end-----------------------------------


}
