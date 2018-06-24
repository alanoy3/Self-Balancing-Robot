package com.example.a.wifi_socket_web_stream_self_balancing_robot;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLite_DataBase extends SQLiteOpenHelper {

    public static final String DatabaseName = "socket_client";
    public static final int DatabaseVersion = 1;

    public SQLite_DataBase(Context context) {
        super(context, DatabaseName+".db", null, DatabaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DatabaseName +      // 建立 資料表 Socket_Client
                "(_id INTEGER PRIMARY KEY," +                // 自動產生主鍵 ID
                "ip_address TEXT," +                        // IP 位置
                "port INTEGER," +                           // 連線 Port
                "created_time TIMESTAMP default CURRENT_TIMESTAMP)");       // 自動建立時間
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {      // 更新資料庫
        db.execSQL("DROP TABLE IF EXISTS "+DatabaseName);
        onCreate(db);
    }
}
