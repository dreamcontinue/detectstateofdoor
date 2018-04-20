package com.example.jerica.detcctstateofdoor;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.functions.Consumer;

public class RealTimeDetectActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = RealTimeDetectActivity.class.getSimpleName();

    private float scale = 2.5f;
    private long interval_time = 1500;
    private int algorithm_type = 1;


    public static final String INTENT_INTERVAL_TIME = "com.example.jerica.detectstateofdoor.111";
    public static final String INTENT_SCALE = "com.example.jerica.detectstateofdoor.222";
    public static final String INTENT_ALGORITHM = "com.example.jerica.detectstateofdoor.333";

    public static final String INTENT_URI_PATTERN = "com.example.jerica.detectstateofdoor.pattern";
    public static final String INTENT_URI_CLOSE = "com.example.jerica.detectstateofdoor.close";
    private Uri uri_pattern;
    private Uri uri_close_state;

    private CameraBridgeViewBase openCVCameraView;

    private TextView tv_area;
    private TextView tv_center_coord;
    private TextView tv_center_move_dis;
    private TextView tv_state;
    private TextView tv_detect_time;

    private Mat pattern;
    private Mat closeState;
    private double areaClosed;
    private Point centerClosed;//关闭下的中心点信息
    private LinkedList<Point> pointListCurrent;


    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    openCVCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
// 隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
// 定义全屏参数
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
// 获得窗口对象
        Window myWindow = this.getWindow();
// 设置 Flag 标识
        myWindow.setFlags(flag, flag);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_detect);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        Intent intent = getIntent();
        if (intent == null) ActivityCompat.finishAfterTransition(this);

        uri_pattern = intent.getParcelableExtra(INTENT_URI_PATTERN);
        uri_close_state = intent.getParcelableExtra(INTENT_URI_CLOSE);

        interval_time = intent.getLongExtra(INTENT_INTERVAL_TIME, 1500);
        scale = intent.getFloatExtra(INTENT_SCALE, 2.5f);
        algorithm_type = intent.getIntExtra(INTENT_ALGORITHM, 1);

        openCVCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        openCVCameraView.setCvCameraViewListener(this);
        openCVCameraView.setIntervalFlag(true);//设置间隔时间
        openCVCameraView.setIntervalTime(interval_time);

        tv_area = (TextView) findViewById(R.id.tv_area);
        tv_center_coord = (TextView) findViewById(R.id.tv_centre_coordinate);
        tv_center_move_dis = (TextView) findViewById(R.id.tv_centre_move_distance);
        tv_state = (TextView) findViewById(R.id.tv_state);
        tv_detect_time = (TextView) findViewById(R.id.tv_detect_time);

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        try {
            //读入模板
            Bitmap bmpPattern = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri_pattern));
            pattern = new Mat(bmpPattern.getHeight(), bmpPattern.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(bmpPattern, pattern);
            //:对模板进行滤波后进行下采样
            Imgproc.pyrDown(pattern, pattern);
            //读入关闭状态

            //////////////
            Bitmap bmpCloseState = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri_close_state));
            Mat closeState1 = new Mat(bmpCloseState.getHeight(), bmpCloseState.getWidth(), CvType.CV_8UC4);
            closeState = closeState1.clone();
//            closeState = new Mat(bmpCloseState.getHeight(), bmpCloseState.getWidth(), CvType.CV_8UC4);
//            Utils.bitmapToMat(bmpCloseState, closeState);
            Utils.bitmapToMat(bmpCloseState, closeState1);
            Imgproc.resize(closeState1, closeState, new Size(closeState1.width() / scale, closeState1.height() / scale));//缩小scale倍处理
            ///////////////
            //关闭状态下的信息
            LinkedList<Point> pointListClosed = SIFT.getPointList(pattern, closeState, algorithm_type);

            //添加
            if (pointListClosed != null) {
                areaClosed = ComputeArea.getArea(pointListClosed);
                centerClosed = ComputeCenter.getCenterPoint(pointListClosed);

                Log.d(TAG, String.format("%.2f", areaClosed) + "\t");
                Log.d(TAG, String.format("%.2f", centerClosed.x) + "\t" + String.format("%.2f", centerClosed.y));
            }
            ((TextView) findViewById(R.id.tv_template_area)).setText(String.format("%.2f", areaClosed));

        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            ActivityCompat.finishAfterTransition(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (openCVCameraView != null)
            openCVCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (openCVCameraView != null)
            openCVCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    private long startTime = 0;
    private long endTime;
    private Mat scene;
    private Mat imgMatches;

    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//
//        endTime = System.currentTimeMillis();
//        if (endTime - startTime < 2000) {
//            return imgMatches == null || imgMatches.empty() ? scene : imgMatches;
//        } else {
//            startTime = endTime;
//        }


//        return inputFrame.rgba();
        //待检测图像
//        Mat original=inputFrame.rgba();Size dsize=new Size(original.width()*0.5,original.height()*0.5);Mat scene=new Mat(dsize,CvType.CV_8UC4);
        final long t1 = System.currentTimeMillis();
        Log.e(TAG, "start detect at " + t1);
        Mat scene1 = inputFrame.rgba();
        scene = scene1.clone();
        Log.e(TAG, "original" + scene.width() + "*" + scene.height());
        Imgproc.resize(scene1, scene, new Size(scene1.width() / scale, scene1.height() / scale));
        Log.e(TAG, "resize " + scene.width() + "*" + scene.height());
//        scene = inputFrame.rgba();
        imgMatches = scene.clone();

//        Imgproc.resize(original,scene,dsize);
        //待检测下的信息
//        fixedThreadPool.execute(new Runnable() {
//            @Override
//            public void run() {
        pointListCurrent = SIFT.getPointList(pattern, scene, algorithm_type);
//            }
//        });

        if (pointListCurrent == null || centerClosed == null) return scene1;

        final double areaCurrent = ComputeArea.getArea(pointListCurrent);
        Point centerCurrent = ComputeCenter.getCenterPoint(pointListCurrent);
        Log.d(TAG, String.format("%.2f", areaCurrent) + "\t");
        Log.d(TAG, String.format("%.2f", centerCurrent.x) + "\t" + String.format("%.2f", centerCurrent.y));

        //中心点偏移距离
        final double distance = ComputeCenterDistance.getCenterDistance(centerClosed, centerCurrent);

        //处理参数
        final StringBuffer buffer = new StringBuffer();
        buffer.append("(");
        buffer.append(String.format("%.2f", centerCurrent.x));
        buffer.append(",");
        buffer.append(String.format("%.2f", centerCurrent.x));
        buffer.append(")");
//        tv_center_coord.setText(buffer.toString());
//        tv_state.setText(flag ? "open" : "close");
//        tv_center_move_dis.setText(String.format("%.2f", distance));
//        tv_area.setText(String.format("%.2f", areaCurrent));

        final long t2 = System.currentTimeMillis();
        Log.e(TAG, "finish detect at " + t2 + "  use time " + (t2 - t1) + "ms");
        //检测结果
        final Judge.DOOR_STATE state = Judge.getFlag(areaClosed, centerClosed, areaCurrent, centerCurrent);
        if (state == Judge.DOOR_STATE.UNKNOWN) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_center_coord.setText("Unknown");
                    tv_state.setText("OPEN");
                    tv_center_move_dis.setText("Unknown");
                    tv_area.setText("Unknown");
                    tv_detect_time.setText((t2 - t1) + "ms");

                }
            });
//            openCVCameraView.disableView();
            return scene1;
        } else {
            //画出结果
            Point p1 = new Point(pointListCurrent.get(0).x, pointListCurrent.get(0).y);
            Point p2 = new Point(pointListCurrent.get(1).x, pointListCurrent.get(1).y);
            Point p3 = new Point(pointListCurrent.get(2).x, pointListCurrent.get(2).y);
            Point p4 = new Point(pointListCurrent.get(3).x, pointListCurrent.get(3).y);
            Imgproc.line(imgMatches, p1, p2, new Scalar(0, 255, 0), 3);
            Imgproc.line(imgMatches, p2, p3, new Scalar(0, 255, 0), 3);
            Imgproc.line(imgMatches, p3, p4, new Scalar(0, 255, 0), 3);
            Imgproc.line(imgMatches, p4, p1, new Scalar(0, 255, 0), 3);
            Imgproc.circle(imgMatches, centerCurrent, 3, new Scalar(255, 0, 0), 3);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_center_coord.setText(buffer.toString());
                    tv_state.setText(state.toString());
                    tv_center_move_dis.setText(String.format("%.2f", distance));
                    tv_area.setText(String.format("%.2f", areaCurrent));
                    tv_detect_time.setText((t2 - t1) + "ms");
                }
            });
//            openCVCameraView.disableView();
            Mat imgMatches1 = new Mat();
            imgMatches1 = imgMatches.clone();
            Imgproc.resize(imgMatches, imgMatches1, new Size(imgMatches.width() * scale, imgMatches.height() * scale));
            return imgMatches1;
        }
    }
}
