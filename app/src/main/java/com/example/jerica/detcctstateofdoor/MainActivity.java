package com.example.jerica.detcctstateofdoor;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int FILE_SELECT_CODE_PATTERN = 0;
    private static final int FILE_SELECT_CODE_CLOSE_STATE = 1;
    private static final int TAKE_PHOTO_CODE_PATTERN = 2;
    private static final int TAKE_PHOTO_CODE_CLOSE_STATE = 3;

    private Button btn_pattern;
    private Button btn_close_state;
    private Button btn_detect;
    private Button btn_pattern_tp;//tp:==:take photo
    private Button btn_close_state_tp;

    private EditText et_interval_time;
    private EditText et_scale;
    private EditText et_algorithm;

    private AppCompatImageView iv_pattern;
    private AppCompatImageView iv_close_state;

    private Uri uri_pattern;
    private Uri uri_close_state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new RxPermissions(MainActivity.this).request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (!aBoolean) {
                            ActivityCompat.finishAfterTransition(MainActivity.this);
                        }
                    }
                });

        btn_pattern = (Button) findViewById(R.id.btn_pattern);
        btn_close_state = (Button) findViewById(R.id.btn_close_state);
        btn_detect = (Button) findViewById(R.id.btn_realtime_detect);
        btn_pattern_tp = (Button) findViewById(R.id.btn_pattern_take_photo);
        btn_close_state_tp = (Button) findViewById(R.id.btn_close_state_take_photo);

        iv_pattern = (AppCompatImageView) findViewById(R.id.iv_pattern);
        iv_close_state = (AppCompatImageView) findViewById(R.id.iv_close_state);

        et_interval_time = (EditText) findViewById(R.id.et_interval_time);
        et_scale = (EditText) findViewById(R.id.et_scale);
        et_algorithm = (EditText) findViewById(R.id.et_algorithm);

        btn_pattern.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPhoto(FILE_SELECT_CODE_PATTERN);
            }
        });

        btn_close_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPhoto(FILE_SELECT_CODE_CLOSE_STATE);
            }
        });
        btn_pattern_tp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto(TAKE_PHOTO_CODE_PATTERN);
            }
        });
        btn_close_state_tp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto(TAKE_PHOTO_CODE_CLOSE_STATE);
            }
        });

        btn_detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uri_pattern == null) {
                    Toast.makeText(MainActivity.this, "pattern is empty", Toast.LENGTH_SHORT).show();
                } else if (uri_close_state == null) {
                    Toast.makeText(MainActivity.this, "close state is empty", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(MainActivity.this, RealTimeDetectActivity.class);
                    intent.putExtra(RealTimeDetectActivity.INTENT_URI_PATTERN, uri_pattern);
                    intent.putExtra(RealTimeDetectActivity.INTENT_URI_CLOSE, uri_close_state);
                    long interval_time = 1500;
                    float scale = 2.5f;
                    int algorithm_type = 1;
                    try {
                        interval_time = Long.parseLong(et_interval_time.getText().toString());
                        scale = Float.parseFloat(et_scale.getText().toString());
                        algorithm_type = Integer.parseInt(et_algorithm.getText().toString());
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, interval_time + "      " + scale + "       " + algorithm_type);
                    intent.putExtra(RealTimeDetectActivity.INTENT_INTERVAL_TIME, interval_time);
                    intent.putExtra(RealTimeDetectActivity.INTENT_SCALE, scale);
                    intent.putExtra(RealTimeDetectActivity.INTENT_ALGORITHM, algorithm_type);
                    ActivityCompat.startActivity(MainActivity.this, intent, null);
                }
            }
        });
    }

    private void selectPhoto(int file_select_code) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a image"), file_select_code);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private void takePhoto(int take_photo_code) {
//        //创建File对象，用于存储拍照后的图片
//        File outputImage = new File(getExternalCacheDir(), UUID.randomUUID() + "output_image.jpg");
//        try {
//            if (outputImage.exists()) {
//                outputImage.delete();
//            }
//            outputImage.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
//        if (Build.VERSION.SDK_INT >= 24) {
//        if (take_photo_code == TAKE_PHOTO_CODE_PATTERN) {
//            uri_pattern = FileProvider.getUriForFile(this, "com.example.jerica.detcctstateofdoor.MainActivity", outputImage);
//            //启动相机程序
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri_pattern);
//        } else if (take_photo_code == TAKE_PHOTO_CODE_CLOSE_STATE) {
//            uri_close_state = FileProvider.getUriForFile(this, "com.example.jerica.detcctstateofdoor.MainActivity", outputImage);
//            //启动相机程序
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri_close_state);
//        }
        Intent intent = new Intent(this, TakePhotoActivity.class);
        startActivityForResult(intent, take_photo_code);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

//        Log.e(TAG, data.getData().toString());
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case FILE_SELECT_CODE_PATTERN:
                    // Get the Uri of the selected file
                    uri_pattern = data.getData();
                    iv_pattern.setImageURI(uri_pattern);
                    break;
                case FILE_SELECT_CODE_CLOSE_STATE:
                    uri_close_state = data.getData();
                    iv_close_state.setImageURI(uri_close_state);
                    break;
                case TAKE_PHOTO_CODE_PATTERN:
                    uri_pattern = data.getData();
                    iv_pattern.setImageURI(uri_pattern);
                    break;
                case TAKE_PHOTO_CODE_CLOSE_STATE:
                    uri_close_state = data.getData();
                    iv_close_state.setImageURI(uri_close_state);
                    break;
            }
        }
        btn_detect.setEnabled(uri_pattern != null && uri_close_state != null);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
