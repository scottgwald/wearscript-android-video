package com.wearscript.record;

import android.app.Activity;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RecordActivity extends Activity implements SurfaceHolder.Callback {
    private final static String TAG = "RecordActivity";
    private final static boolean DBG = true;

    private final static int DEFAULT_DURATION = 3;
    private final static String PATH_KEY = "path";
    private final static String DURATION_KEY = "duration";

    private int maximumWaitTimeForCamera = 5000;
    private FrameLayout frame;
    private Camera camera;
    private SurfaceView cameraPreview;
    private MediaRecorder mediaRecorder;

    private String path;
    private int duration;
    private Handler mHandler;
    private String outputPath;
    private PowerManager.WakeLock wl;
    private boolean wasAsleep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"RecordActivity created");
        super.onCreate(savedInstanceState);
        PowerManager pm = (PowerManager)getSystemService(
                POWER_SERVICE);
        wasAsleep = !pm.isScreenOn();
        if (DBG) Log.v(TAG, "Screen was " + (wasAsleep ? "" : "not ") + "sleeping");
            wl = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.ON_AFTER_RELEASE, TAG);
        wl.acquire();

        if (wasAsleep) {
            Intent i = getIntent();
            i.setClass(this, RecordActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } else {
            setContentView(R.layout.activity_record);
            mHandler = new Handler();
            Intent i = getIntent();
            path = i.getStringExtra(PATH_KEY);
            duration = i.getIntExtra(DURATION_KEY, DEFAULT_DURATION);

            if (path == null) {
                if (DBG) Log.d(TAG, "No path specified by intent");
            }
            Log.d(TAG, "Intent received, recording video of length " + duration);

            camera = getCameraInstanceRetry();
            Log.v(TAG, "Doing proceedWithMain");

            //cameraPreview = new CameraPreview(RecordActivity.this, this.camera);
            cameraPreview = new SurfaceView(this);
            cameraPreview.getHolder().addCallback(this);
            cameraPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            frame = (FrameLayout)super.findViewById(R.id.camera_preview);
            frame.addView(this.cameraPreview);
        }
    }

    @Override
    protected void onResume() {
        if (DBG) Log.v(TAG, "Lifecycle: onResume.");
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (DBG) Log.v(TAG, "Lifecycle: onPause");
        try {
            wl.release();
        } catch (Throwable th) {
            // ignore
        }
        releaseCamera();
        releaseMediaRecorder();
        finish();
        super.onPause();
    }

    void releaseCamera() {
        if (this.camera != null) {
            this.camera.stopPreview();
            this.camera.release();
            this.camera = null;
            this.frame.removeView(this.cameraPreview);
        }
    }

    void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private boolean prepareVideoRecorder(){
        try {
            camera.stopPreview();
            camera.setPreviewDisplay(null);
        } catch (java.io.IOException ioe) {
            Log.d(TAG, "IOException nullifying preview display: " + ioe.getMessage());
        }
        camera.unlock();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);


        //mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
        int profileInt = CamcorderProfile.QUALITY_720P;
        Log.v(TAG, "Checking for profile: " + CamcorderProfile.hasProfile(profileInt));
        CamcorderProfile profile = CamcorderProfile.get(profileInt);
        mediaRecorder.setOutputFormat(profile.fileFormat);
        mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mediaRecorder.setVideoEncoder(profile.videoCodec);
        mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mediaRecorder.setAudioChannels(profile.audioChannels);
        mediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        mediaRecorder.setAudioEncoder(profile.audioCodec);

        mediaRecorder.setOutputFile(getOutputMediaFile().toString());
        mediaRecorder.setPreviewDisplay(cameraPreview.getHolder().getSurface());

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void startRecording() {
        Log.d(TAG, "startRecording()");
        prepareVideoRecorder();
        mediaRecorder.start();
    }

    private void stopRecording() {
        Log.v(TAG, "Stopping recording.");
        mediaRecorder.stop();
        releaseMediaRecorder();
        releaseCamera();
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.wtf("Couldn't open camera", e);
        }
        return c; // returns null if camera is unavailable
    }

    private Camera getCameraInstanceRetry() {
        Camera c = null;
        Log.v(TAG,"getTheCamera");
        // keep trying to acquire the camera until "maximumWaitTimeForCamera" seconds have passed
        boolean acquiredCam = false;
        int timePassed = 0;
        while (!acquiredCam && timePassed < maximumWaitTimeForCamera) {
            try {
                c = Camera.open();
                Log.v(TAG,"acquired the camera");
                acquiredCam = true;
                return c;
            }
            catch (Exception e) {
                Log.e(TAG,"Exception encountered opening camera:" + e.getLocalizedMessage());
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ee) {
                Log.e(TAG,"Exception encountered sleeping:" + ee.getLocalizedMessage());
            }
            timePassed += 200;
        }
        return c;
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "WearscriptVideo");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        if (path != null) {
            outputPath = path;
        } else {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            outputPath = mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4";
        }
        File mediaFile = new File(outputPath);
        Log.v(TAG, "Output file: " + outputPath);
        return mediaFile;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (camera == null) {
            camera = getCameraInstanceRetry();
        }

        if (DBG) Log.v(TAG, "Is camera null? " + (camera == null));
        if (DBG) Log.v(TAG, "is holder null? " + (holder == null));

        try {
            camera.setPreviewDisplay(holder);

            //DEBUG
            Camera.Parameters params = camera.getParameters();
            params.setPreviewFormat(ImageFormat.NV21);
            params.setPreviewSize(640, 480);
            List<String> FocusModes = params.getSupportedFocusModes();
            if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            params.setPreviewFpsRange(30000, 30000);
            camera.setParameters(params);
            holder.setFixedSize(640, 360);
            camera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        } catch (Throwable tr) {
            Log.e(TAG, "OH. MY God. Throwable. ", tr);
        }

        startRecording();

        if (mHandler == null) {
            Log.e(TAG, "WTF, Handler is null!!!");
        } else {
            Log.v(TAG, "Got a handler!");
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopRecording();
                    Intent result = new Intent();
                    result.setAction(WearScriptBroadcastReceiver.RECORD_RESULT_ACTION);
                    result.putExtra("path", outputPath);
                    //sendBroadcast(result);
                    setResult(RESULT_OK, result);
                    Log.v(TAG, "Stopped recording, set result, finishing.");
                    wl.release();
                    finish();
                }
            }, duration * 1000);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (DBG) Log.v(TAG, "SurfaceChanged " + format + " " + width + " " + height);
        //Do nothing
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Do nothing
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroyyyyyy");
    }

}
