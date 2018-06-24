package com.example.yvtc.androidbluetoothremotecontrolcar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import static com.example.yvtc.androidbluetoothremotecontrolcar.car_in_sensor_mode.X_value;
import static com.example.yvtc.androidbluetoothremotecontrolcar.car_in_sensor_mode.Y_value;


public class SesorSurfaceView  implements SurfaceHolder.Callback, Runnable {
    private Thread thread;


    public static   SurfaceHolder holder;
    private final String TAG="app";
    private int SesorSurfaceViewsize_width,SesorSurfaceViewsize_height;

//    public SesorSurfaceView(Context context) {
//        super(context);
//        getHolder().addCallback(this);
//    }

    public SesorSurfaceView(Context context, SurfaceView surfaceview) {
        surfaceview.setZOrderOnTop( true ); //使surfaceview放到最頂層
        holder = surfaceview.getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT); //使視窗啟用透明度
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
//        Canvas canvas = holder.lockCanvas();
//        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        SesorSurfaceViewsize_width=width;
        SesorSurfaceViewsize_height=height;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        thread=null;
        holder.removeCallback(this);
    }

    @Override
    public void run() {
        while (thread != null) {
            doDraw(holder);
        }
    }
    private void doDraw(SurfaceHolder holder) {

        Canvas canvas = holder.lockCanvas();

        if (canvas != null){
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);     //設底色為透明
            /*繪製底圖*/
            Paint paint_Circle = new Paint();      //底圖畫筆物件
            paint_Circle.setColor(0xff5a5a5a);
            paint_Circle.setAntiAlias(true);   //設定反鋸齒
            paint_Circle.setStrokeWidth(20);       //線寬
//            paint_Circle.setAlpha(80);      //設定透明度
            paint_Circle.setStyle(Paint.Style.STROKE);      //無填滿
//drawCircle (圓心的x坐標 , 圓心的y坐標 , 圓的半徑 , 繪製時所使用的畫筆)
            canvas.drawCircle(SesorSurfaceViewsize_width / 2f, SesorSurfaceViewsize_height / 2f, (SesorSurfaceViewsize_width / 2f)-(SesorSurfaceViewsize_width / 2f/12.5f*2), paint_Circle);           //繪製外圓
            canvas.drawCircle(SesorSurfaceViewsize_width / 2f, SesorSurfaceViewsize_height / 2f, (SesorSurfaceViewsize_width / 2f/12.5f*2), paint_Circle);           //繪製內圓
//drawLine(X坐標的起點 , Y坐標的起點 , X坐標的終點 , Y坐標的終點 , 繪製時所使用的畫筆)
            canvas.drawLine(SesorSurfaceViewsize_width / 2f,SesorSurfaceViewsize_height / 2f/12.5f*2,SesorSurfaceViewsize_width / 2f,SesorSurfaceViewsize_height-(SesorSurfaceViewsize_height / 2f/12.5f*2),paint_Circle);  //垂直
            canvas.drawLine(SesorSurfaceViewsize_width / 2f/12.5f*2,SesorSurfaceViewsize_height / 2f,SesorSurfaceViewsize_width-(SesorSurfaceViewsize_width / 2f/12.5f*2),SesorSurfaceViewsize_height / 2f,paint_Circle);   //水平


            Paint paint = new Paint();
            paint.setColor(0xffffcc00);
            paint.setAntiAlias(true);   //設定反鋸齒
//            paint.setAlpha(100);      //設定透明度

//          計算繪圖中心座標    start----------------------------------------------------------------------------
            float X = (SesorSurfaceViewsize_width / 2f) - (SesorSurfaceViewsize_width / 2f /12.5f * X_value);   //左右，先算出SurfaceView的中間位置，再加入偏移量
            float Y = (SesorSurfaceViewsize_height / 2f) +(SesorSurfaceViewsize_height / 2f /12.5f*Y_value);    //上下，先算出SurfaceView的中間位置，再加入偏移量
//          計算繪圖中心座標    end------------------------------------------------------------------------------

            /*繪製星星*/
            Path path = new Path();
            float theta = (float)(Math.PI * 72 / 180);
            float r = 50f;
            PointF center = new PointF(X,Y);
            float dx1 = (float)(r*Math.sin(theta));
            float dx2 = (float)(r*Math.sin(2*theta));
            float dy1 = (float)(r*Math.cos(theta));
            float dy2 = (float)(r*Math.cos(2*theta));
            path.moveTo(center.x, center.y-r);
            path.lineTo(center.x-dx2, center.y-dy2);
            path.lineTo(center.x+dx1, center.y-dy1);
            path.lineTo(center.x-dx1, center.y-dy1);
            path.lineTo(center.x+dx2, center.y-dy2);
            path.lineTo(center.x, center.y-r);
            canvas.drawPath(path, paint);



            holder.unlockCanvasAndPost(canvas);
        }
    }
}
