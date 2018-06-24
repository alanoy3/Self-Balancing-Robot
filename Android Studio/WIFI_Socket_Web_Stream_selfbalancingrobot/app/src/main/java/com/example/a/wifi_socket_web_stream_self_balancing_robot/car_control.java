package com.example.a.wifi_socket_web_stream_self_balancing_robot;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class car_control extends AppCompatActivity {

    private final String stream_path = ":8080/stream/video.mjpeg";
    private Context context;
    private Intent intent;
    private String ip_address = "";
    private WebView stream_Web_view;
    private int port;
    private Thread socket_thread;
    private Socket clientSocket;//客戶端的socket
    private BufferedWriter bw;  //取得網路輸出串流
    private BufferedReader br;  //取得網路輸入串流
    private String tmp;         //做為接收時的緩存
    private final String car_control_forward = "f";
    private final String car_control_back = "b";
    private final String car_control_left = "l";
    private final String car_control_right = "r";
    private final String car_control_stop = "s";
    private static String to_Car_Msg = "s";
    private Button button_forward;
    private Button button_back;
    private Button button_left;
    private Button button_right;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_control);

        context = this;

        //取得 主Activity 的 intent
        intent = getIntent();
        //從 intent 取得 ip address
        ip_address = intent.getStringExtra("ip_address").toString();
        //從 intent 取得 port ，當無資料時 使用預設值 9999
        port = intent.getIntExtra("port" , 9999);

        Stream_Web_View();

        Button_Car_Control();

        // 建立處理 socket 執行序
//        socket_thread=new Thread(socket_client);
//        socket_thread.start();
    }

    private void Button_Car_Control() {

        // 前進按鈕
        button_forward = (Button) findViewById(R.id.button_forward);
        button_forward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){

                    //按住事件
                    case MotionEvent.ACTION_DOWN:
                        to_Car_Msg = car_control_forward;
                        break;

                    //鬆開事件
                    case MotionEvent.ACTION_UP:
                        to_Car_Msg = car_control_stop;
                        break;

                    //移動事件
                    case MotionEvent.ACTION_MOVE:
                        break;
                }
                return false;
            }
        });

        // 後退按鈕
        button_left = (Button) findViewById(R.id.button_back);
        button_left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){

                    //按住事件
                    case MotionEvent.ACTION_DOWN:
                        to_Car_Msg = car_control_back;
                        break;

                    //鬆開事件
                    case MotionEvent.ACTION_UP:
                        to_Car_Msg = car_control_stop;
                        break;

                    //移動事件
                    case MotionEvent.ACTION_MOVE:
                        break;
                }
                return false;
            }
        });

        // 左轉按鈕
        button_back = (Button) findViewById(R.id.button_left);
        button_back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){

                    //按住事件
                    case MotionEvent.ACTION_DOWN:
                        to_Car_Msg = car_control_left;
                        break;

                    //鬆開事件
                    case MotionEvent.ACTION_UP:
                        to_Car_Msg = car_control_stop;
                        break;

                    //移動事件
                    case MotionEvent.ACTION_MOVE:
                        break;
                }
                return false;
            }
        });

        // 右轉按鈕
        button_right = (Button) findViewById(R.id.button_right);
        button_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){

                    //按住事件
                    case MotionEvent.ACTION_DOWN:
                        to_Car_Msg = car_control_right;
                        break;

                    //鬆開事件
                    case MotionEvent.ACTION_UP:
                        to_Car_Msg = car_control_stop;
                        break;

                    //移動事件
                    case MotionEvent.ACTION_MOVE:
                        break;
                }
                return false;
            }
        });

    }


    //連結socket伺服器做傳送與接收
    private Runnable socket_client = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            try{
                //輸入 Server 端的 IP
                InetAddress serverIp = InetAddress.getByName(ip_address);
                //自訂所使用的 Port
                int serverPort = port;
                //建立連線
                clientSocket = new Socket(serverIp, serverPort);
                //取得網路輸出串流
                bw = new BufferedWriter( new OutputStreamWriter(clientSocket.getOutputStream()));
                //取得網路輸入串流
                br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                //檢查是否已連線
                while (clientSocket.isConnected()) {

                    // 寫入訊息到串流
                    bw.write(to_Car_Msg);
                    // 立即發送
                    bw.flush();

                    //宣告一個緩衝,從br串流讀取 Server 端傳來的訊息
                    tmp = br.readLine();

                    if(tmp!=null){

                    }
                }
            }catch(Exception e){
                //當斷線時會跳到 catch,可以在這裡處理斷開連線後的邏輯
                e.printStackTrace();
                Log.e("text","Socket連線="+e.toString());
                //當斷線時自動關閉 Socket
                finish();
            }
        }
    };


    private void Stream_Web_View() {
        stream_Web_view = (WebView) findViewById(R.id.Web_View);

        // WebView的設定選項
        WebSettings webSettings = stream_Web_view.getSettings();
        // 不使用緩衝
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // Enable Javascript
        webSettings.setJavaScriptEnabled(true);
        // Enable LocalStorage
        webSettings.setDomStorageEnabled(true);

        // 將圖片調整到適合webview的大小
        webSettings.setUseWideViewPort(true);
        // 縮放至顯示大小
        webSettings.setLoadWithOverviewMode(true);

        // 要加setWebViewClient以避免點連結時跳出APP用瀏覽器開啟
        stream_Web_view.setWebViewClient(new WebViewClient());

        // 要設定 WebChromeClient 才能支援 JS 的 Alert, Confirm
        stream_Web_view.setWebChromeClient(new WebChromeClient());

        // API 19 以上可使用硬體加速
        if (Build.VERSION.SDK_INT >= 19) {
            stream_Web_view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            stream_Web_view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        // 載入網頁
        stream_Web_view.loadUrl("http://" + ip_address + stream_path);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*-----------------------------------------------
        // 在Activity 銷毀（ WebView ）的時候，
        // 先讓WebView 載入null內容，然後移除WebView，
        // 再銷毀WebView，最後清空。
        ----------------------------------------------*/
        if (stream_Web_view != null) {
            stream_Web_view.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            stream_Web_view.clearHistory();

            ((ViewGroup) stream_Web_view.getParent()).removeView(stream_Web_view);
            stream_Web_view.destroy();
            stream_Web_view = null;
        }

        try{
            //關閉輸出入串流後,關閉Socket
            bw.close();
            br.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



