package com.example.jerica.detcctstateofdoor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

public class TakePhotoActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = TakePhotoActivity.class.getName();
    private JavaCameraView openCVCameraView;
    private Button btn;
    private boolean takePhotoTag = false;
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
        setContentView(R.layout.activity_take_photo);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }


        openCVCameraView = (JavaCameraView) findViewById(R.id.camera_view);
        openCVCameraView.setCvCameraViewListener(this);
        btn = (Button) findViewById(R.id.take_photo);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhotoTag = true;
            }
        });
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
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

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (!takePhotoTag) return inputFrame.rgba();

        Mat inputMat = inputFrame.rgba();
        Bitmap bmp = Bitmap.createBitmap(inputMat.width(), inputMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputMat, bmp);


        if (bmp == null) {
            Toast.makeText(TakePhotoActivity.this, "bitmap null", Toast.LENGTH_SHORT).show();
            finish();
        }


        Uri bmpUri = Uri.fromFile(SaveImage.saveImage(bmp, getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath(), UUID.randomUUID() + ".jpg"));
//        bmpUri= Uri.fromFile(SaveImage.saveImage(bmp, getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath(), UUID.randomUUID() + ".jpg"))
        Log.e(TAG, bmpUri.toString());
        Intent intent = new Intent();
        intent.setData(bmpUri);
        setResult(RESULT_OK, intent);
        finish();

        return null;
    }
}
