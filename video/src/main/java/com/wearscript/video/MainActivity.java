package com.wearscript.video;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import java.io.File;

public class MainActivity extends Activity implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = "PlaybackActivity";
    private VideoView videoView;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        this.videoView = (VideoView)findViewById(R.id.video);
        this.uri = getIntent().getData();
        if (this.uri != null) {
            Log.d(TAG, "this.uri: " + this.uri.toString());
        } else {
            String path = getIntent().getStringExtra("path");
            if (path == null)
                path = "/sdcard/wearscript/my_video.mp4";
            //String path = "/sdcard/wearscript/my_video.mp4";
            this.uri = Uri.fromFile(new File(path));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.videoView.setVideoURI(this.uri);
        this.videoView.setOnPreparedListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.videoView.stopPlayback();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        this.videoView.setOnCompletionListener(this);
        this.videoView.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        this.videoView.seekTo(0);
        super.finish();
    }
}
