package com.wearscript.video;

import android.app.Activity;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

    private final static int DEFAULT_DURATION = 10;
    private final static String PATH_KEY = "path";
    private final static String DURATION_KEY = "duration";

    private FrameLayout frame;
    private Camera camera;
//    private CameraPreview cameraPreview;
    private SurfaceView cameraPreview;
    private MediaRecorder mediaRecorder;

    private String path;
    private int duration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"RecordActivity created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        Intent i = getIntent();
        path = i.getStringExtra(PATH_KEY);
        duration = i.getIntExtra(DURATION_KEY, DEFAULT_DURATION);
        if (path == null) {
            Log.d(TAG, "No path specified by intent");
            //TODO: handle
            finish();
        }
        Log.d(TAG,"Intent received, recording video of length " +
                duration + " and saving it to " + path);
        camera = getCameraInstance();
        //cameraPreview = new CameraPreview(RecordActivity.this, this.camera);
        cameraPreview = new SurfaceView(this);
        cameraPreview.getHolder().addCallback(this);
        cameraPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        frame = (FrameLayout)super.findViewById(R.id.camera_preview);
        frame.addView(this.cameraPreview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera == null) {
            camera = getCameraInstance();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        releaseMediaRecorder();
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
            camera.setPreviewDisplay(null);
        } catch (java.io.IOException ioe) {
            Log.d(TAG, "IOException nullifying preview display: " + ioe.getMessage());
        }
        camera.stopPreview();
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

        //TODO: in duration time, run stop Recording
    }

    private void stopRecording() {
        mediaRecorder.stop();
        releaseMediaRecorder();
        camera.lock();
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

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "WearscriptVideo");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        return mediaFile;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
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
        }

        startRecording();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //Do nothing
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Do nothing
    }
}
