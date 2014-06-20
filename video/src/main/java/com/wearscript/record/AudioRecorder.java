package com.wearscript.record;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.File;

public class AudioRecorder extends Service {
	
	private final String LOG_TAG = "AudioRecorder";

    private AudioRecordThread recorder;
    public static String MILLIS_EXTRA_KEY = "millis";

	@Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "Service Started");
        createDirectory();
    }

    @Override
    public void onDestroy() {
    	Log.d(LOG_TAG, "Service Destroy");
    	recorder.interrupt();
        super.onDestroy();
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	private void createDirectory() {
        File directory = new File(AudioRecordThread.directoryAudio);
        if (!directory.isDirectory()){
        	directory.mkdirs();
        }
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals("com.wearscript.record.RECORD_AUDIO")) {
                recorder = new AudioRecordThread(this, intent.getStringExtra("filePath"));
                recorder.start();
            } else if (intent.getAction().equals("com.wearscript.record.SAVE_AUDIO")) {
                recorder.writeAudioDataToFile();
            } else if (intent.getAction().equals("com.wearscript.record.STOP_AUDIO")) {
                recorder.stopRecording();
                stopSelf();
            }
        }
        return 0;
    }
	
}
