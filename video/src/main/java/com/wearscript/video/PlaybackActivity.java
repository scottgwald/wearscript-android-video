package com.wearscript.video;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import java.io.File;

public class PlaybackActivity extends Activity implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = "PlaybackActivity";
    private static final boolean DBG = true;
    private VideoView videoView;
    private Uri uri;
    private Intent myIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        this.videoView = (VideoView)findViewById(R.id.video);
        myIntent = getIntent();
        if (myIntent != null) {
            String path = getIntent().getStringExtra("path");
            if (path == null) {
                //TODO Handle
                finish();
            }
            this.uri = Uri.fromFile(new File(path));
            this.videoView.setVideoURI(this.uri);
            this.videoView.setOnPreparedListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DBG) Log.v(TAG, "Lifecycle: onResume");
    }

    @Override
    protected void onPause() {
        if (DBG) Log.v(TAG, "Lifecycle: onPause");
        this.videoView.stopPlayback();
        super.onPause();
        finish();

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (DBG) Log.v(TAG, "Player prepared!");
        this.videoView.setOnCompletionListener(this);
        this.videoView.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        this.videoView.seekTo(0);
        super.finish();
        finish();
    }
}
