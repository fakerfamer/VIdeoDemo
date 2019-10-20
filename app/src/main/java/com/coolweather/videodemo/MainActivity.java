package com.coolweather.videodemo;

import androidx.annotation.MainThread;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.coolweather.videodemo.filter.ColorFilter;
import com.coolweather.videodemo.render.CameraPreviewRender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private SurfaceView mSurfaceView;
    private Button btn;
    private SurfaceHolder mSurfaceHolder;
    private CameraManager mCameraManager;
    private int cameraId;
    private int BACK_CAM = 0;
    private int FRONT_CAM = 1;
    private CameraDevice mCameraDevice;
    private Handler mMainHandler;
    private Handler mChildHander;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    List<Surface> mSurfaceList;
    private CameraCaptureSession mCameraCaptureSession;
    private MediaRecorder mMediaRecorder;
    private String mMediaPath;
    GLSurfaceView glSurfaceView;
    SurfaceTexture surfaceTexture;
    private boolean FLAG_isRecording = false;
    private CameraPreviewRender cameraPreviewRender;
    private Button btnColorFilter;
    private Surface surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        btn = findViewById(R.id.button_start);
        btn.setOnClickListener(this);
    }

    private void initView() {
        /*mSurfaceView = findViewById(R.id.preview_surface);
        btn = findViewById(R.id.button_start);
        mSurfaceHolder = mSurfaceView.getHolder();*/

        glSurfaceView = findViewById(R.id.preview_surface);
        glSurfaceView.setEGLContextClientVersion(3);
        cameraPreviewRender = new CameraPreviewRender();
        glSurfaceView.setRenderer(cameraPreviewRender);
        btnColorFilter = findViewById(R.id.button_fill);
        btnColorFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ColorFilter.COLOR_FLAG < 7){
                    ColorFilter.COLOR_FLAG++;
                }else {
                    ColorFilter.COLOR_FLAG = 0;
                }
            }
        });
        surfaceTexture = cameraPreviewRender.getSurfaceTexture();
        Log.d("startTime", "" +System.currentTimeMillis());
        Log.d("startTime2", "" +System.currentTimeMillis());
      /* while(surfaceTexture == null){
           Log.d("asdf", "asdf");
       }*/
        if(surfaceTexture == null){
            return;
        }
        surfaceTexture.setDefaultBufferSize(1440, 1920);
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                glSurfaceView.requestRender();
            }
        });
        surface = new Surface(surfaceTexture);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);
            } else {
                initCamera();
            }
        } else {
            initCamera();
        }


        
        mSurfaceHolder.setFixedSize(1440, 1920);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);
                    } else {
                        initCamera();
                    }
                } else {
                    initCamera();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });
        initCamera();
    }

    private void initCamera() {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraId = BACK_CAM;
        mMainHandler = new Handler(getMainLooper());
        HandlerThread handlerThread = new HandlerThread("childThread");
        handlerThread.start();
        mChildHander = new Handler(handlerThread.getLooper());
        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
            mCameraManager.openCamera(String.valueOf(cameraId), new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    takePreview();
                }

                @Override
                public void onDisconnected(CameraDevice cameraDevice) {

                }

                @Override
                public void onError(CameraDevice cameraDevice, int i) {

                }
            }, mMainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePreview() {
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            Log.d(TAG, "mSurfaceHolder.getSurface==null:" + (mSurfaceHolder.getSurface() == null));
            if (surface == null) {
                Toast.makeText(MainActivity.this, "surface==null", Toast.LENGTH_SHORT).show();
                return;
            }
            mSurfaceList = new ArrayList<>();
            mSurfaceList.add(surface);
            mCameraDevice.createCaptureSession(mSurfaceList, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {

                    if (mCameraDevice == null)
                        return;
                        mCameraCaptureSession = cameraCaptureSession;
                       updatePreviewSession();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                }
            }, mChildHander);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreviewSession(){
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        CaptureRequest previewRequest = mPreviewRequestBuilder.build();
        try {
            mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mMainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(getOutputMediaFile());
        mMediaRecorder.setVideoEncodingBitRate(100000000);
        mMediaRecorder.setCaptureRate(60);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mSurfaceView.getWidth(), mSurfaceView.getHeight());
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setOrientationHint(90);
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
        }
    }


    /*
     * 获取手机外部存储路径
     * */
    private String getOutputFile() {
        File mediaFile = null;
        boolean OutputExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if (OutputExist) {
            mediaFile = Environment.getExternalStorageDirectory();
            Log.d(TAG, "mediaFileToString" +mediaFile.toString());
            return mediaFile.toString();
        }
        return null;
    }

    /*
     * 获取录制视频的日期 作为存储文件路径一部分
     * */
    private String getDate() {
        Log.d(TAG, "获取录制视频的日期 ");
        Calendar ca = Calendar.getInstance();
        int year = ca.get(Calendar.YEAR);           // 获取年份
        int month = ca.get(Calendar.MONTH);         // 获取月份
        int day = ca.get(Calendar.DATE);            // 获取日
        String date = "" + year + "_" + (month + 1) + "_" + day;
        return date;
    }

    /*
     *创建视频存储文件夹 录制好的视频存储在手机外部存储中 以录像时间+mp4格式命名
     * */
    private String getOutputMediaFile() {
        Log.d(TAG, "获取视频存储的位置 ");
        String mediaPath = getOutputFile();
        if (mediaPath != null) {
            File mediaFile = new File(mediaPath + "/recordVideo");
            if (!mediaFile.exists()) {
                mediaFile.mkdir();
            }
            return mMediaPath = mediaFile.getAbsolutePath() + File.separator + getDate() + ".mp4";
        }
        return null;
    }

    /*
     *
     * 开始录制
     */
    private void startRecord() {
        setupMediaRecorder();
        try {
            mPreviewRequestBuilder = null;
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mSurfaceHolder.setFixedSize(1440, 1920);
            final Surface recorderSurface = mMediaRecorder.getSurface();
            mSurfaceList.clear();
            mSurfaceList.add(mSurfaceHolder.getSurface());
            mSurfaceList.add(recorderSurface);
            mCameraDevice.createCaptureSession(mSurfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    stopPreviewSession();
                    mCameraCaptureSession = cameraCaptureSession;
                    mMediaRecorder.start();
                    btn.setText("stop");
                    mCameraCaptureSession = cameraCaptureSession;
                    mPreviewRequestBuilder.addTarget(recorderSurface);
                    mPreviewRequestBuilder.addTarget(mSurfaceHolder.getSurface());
                    updatePreviewSession();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                }
            }, mMainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopPreviewSession() {
        if(mCameraCaptureSession != null){
            mCameraCaptureSession.close();
        }
    }

    @Override
    public void onClick(View view) {
        if (!FLAG_isRecording) {
            Log.d(TAG, "开始录像");
            FLAG_isRecording = true;
            setupMediaRecorder();
            startRecord();
        }else{
            Log.d(TAG, "结束录像");
            btn.setText("start");
            FLAG_isRecording = false;
            stopRecord();
            if(mCameraCaptureSession != null){
                mCameraCaptureSession.close();
            }
        }
    }
}
