package com.example.a.wifi_socket_web_stream_self_balancing_robot;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import static com.example.a.wifi_socket_web_stream_self_balancing_robot.SQLite_DataBase.DatabaseName;

public class MainActivity extends AppCompatActivity {

    private SQLite_DataBase sqlite_database_object;
    private SQLiteDatabase sqlite_database;
    private Cursor SQLite_Cursor;
    private SimpleCursorAdapter adapter;
    private ListView IP_list_ListView;
    private FloatingActionButton Floating_Action_Button;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        // 建立(打開)資料庫
        sqlite_database_object = new SQLite_DataBase(context);
        sqlite_database = sqlite_database_object.getWritableDatabase();

        ListView_Socket();

        Add_FloatingActionButton();

    }

    private void Add_FloatingActionButton() {
        Floating_Action_Button = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        Floating_Action_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //建立氣球 Layout
                LayoutInflater inflater = getLayoutInflater();
                // 建立顯示物件
                View dialog_layout = inflater.inflate(R.layout.dialog_layout, (ViewGroup) findViewById(R.id.dialog_ID));

                final EditText editText_IP_Address = (EditText) dialog_layout.findViewById(R.id.editText_IP_Address);
                final EditText editText_Port = (EditText) dialog_layout.findViewById(R.id.editText_Port);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Add new item");
                builder.setIcon(android.R.drawable.ic_input_add);
                builder.setView(dialog_layout);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // android 提供放資料的類別
                        ContentValues Content_Values = new ContentValues();
                        Content_Values.put("ip_address", editText_IP_Address.getText().toString());
                        Content_Values.put("port", Integer.parseInt(editText_Port.getText().toString()));

                        //將資料放入資料庫，並回傳值
                        long id = sqlite_database.insert(DatabaseName, null, Content_Values);

                        // 更新ListView畫面
                        Refresh_ListView_Socket();

                        // 關閉視窗
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 關閉視窗
                        dialog.dismiss();
                    }
                });

                // 將 builder 建立並顯示
                builder.create().show();
            }
        });


    }

    private void ListView_Socket() {
        IP_list_ListView = (ListView) findViewById(R.id.socket_list);

        /*----------------------------
        // 當列表項目被 長按 時，進到修改選項畫面
        ----------------------------*/
        IP_list_ListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int col_Index;

                //建立氣球 Layout
                LayoutInflater inflater = getLayoutInflater();
                // 建立顯示物件
                View dialog_layout = inflater.inflate(R.layout.dialog_layout, (ViewGroup) findViewById(R.id.dialog_ID));

                final EditText editText_IP_Address = (EditText) dialog_layout.findViewById(R.id.editText_IP_Address);
                final EditText editText_Port = (EditText) dialog_layout.findViewById(R.id.editText_Port);

                //建立游標
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);

                // 取得選項的IP_Address的索引
                col_Index = cursor.getColumnIndex("ip_address");
                // 從游標取得資料，並設定 editText_IP_Address 文字內容
                editText_IP_Address.setText(cursor.getString(col_Index).toString());
                // 取得選項的Port的索引
                col_Index = cursor.getColumnIndex("port");
                // 從游標取得資料，並設定 editText_Port 文字內容
                editText_Port.setText(cursor.getString(col_Index).toString());
                // 取得選項的ID的索引
                col_Index = cursor.getColumnIndex("_id");
                //從游標取得資料，建立item_ID變數
                final String item_ID = cursor.getString(col_Index);


                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Modify item");
                builder.setIcon(android.R.drawable.ic_input_add);
                builder.setView(dialog_layout);

                builder.setPositiveButton("Modify", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // android 提供放資料的類別
                        ContentValues Content_Values = new ContentValues();
                        Content_Values.put("ip_address", editText_IP_Address.getText().toString());
                        Content_Values.put("port", Integer.parseInt(editText_Port.getText().toString()));

                        //  修改選擇的資料，_id=? 為語法格式
                        int rowCount = sqlite_database.update(DatabaseName, Content_Values, "_id=?", new String[]{item_ID});

                        // 更新ListView畫面
                        Refresh_ListView_Socket();

                        // 關閉視窗
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // 關閉視窗
                        dialog.dismiss();
                    }
                });

                builder.setNeutralButton("Del", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //  刪除選擇的資料，_id=? 為語法格式
                        int rowCount = sqlite_database.delete(DatabaseName, "_id=?", new String[]{item_ID});

                        // 更新ListView畫面
                        Refresh_ListView_Socket();

                        // 關閉視窗
                        dialog.dismiss();
                    }
                });

                // 將 builder 建立並顯示
                builder.create().show();

                // return 要為 true，為false會執行onItemClick
                return true;
            }
        });

        /*----------------------------
        // 當列表項目被 按 時，移動到遙控畫面
        ----------------------------*/
        IP_list_ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int col_Index;

                // 建立 一個 Intent到 car_control Activity
                Intent intent = new Intent(context, car_control.class);

                //建立游標
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);

                // 取得選項的IP_Address的索引
                col_Index = cursor.getColumnIndex("ip_address");
                // 從游標取得資料，並放入intent
                intent.putExtra("ip_address", cursor.getString(col_Index).toString());
                // 取得選項的Port的索引
                col_Index = cursor.getColumnIndex("port");
                // 從游標取得資料，並設定 editText_Port 文字內容
                intent.putExtra("port", cursor.getString(col_Index).toString());


                // 啟動 Activity
                startActivity(intent);
            }
        });

        // 更新ListView畫面
        Refresh_ListView_Socket();
    }

    /*----------------------
    // 更新 RecyclerView 內容
    -----------------------*/
    private void Refresh_ListView_Socket() {
        if (SQLite_Cursor == null) {         // 當 SQLite_Cursor 無查詢結果

            //資料庫查詢
            SQLite_Cursor = sqlite_database.rawQuery("SELECT _id, ip_address, port FROM " + DatabaseName, null);

            adapter = new SimpleCursorAdapter(context, R.layout.refresh_item_layout, SQLite_Cursor,
                    new String[]{"_id", "ip_address", "port"},
                    new int[]{R.id.textView_ID, R.id.textView_IP_Address, R.id.textView_Port},
                    CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            IP_list_ListView.setAdapter(adapter);
        } else {
            if (SQLite_Cursor.isClosed()) {      // 判斷 SQLite_Cursor 是否被關閉
                SQLite_Cursor = null;
                Refresh_ListView_Socket();  // 更新ListView畫面
            } else {
                // 重新取SQLite_Cursor資料，官方不建議使用 requery()，當資料很多時，會超時當掉
                SQLite_Cursor.requery();
                adapter.changeCursor(SQLite_Cursor);    // 重新加入 Cursor
                adapter.notifyDataSetChanged(); // 告知更新資料
            }
        }
    }


}
