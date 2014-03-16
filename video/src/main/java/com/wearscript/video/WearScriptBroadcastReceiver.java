package com.wearscript.video;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WearScriptBroadcastReceiver extends BroadcastReceiver {

    public static final String PLAYBACK_ACTION = "com.wearscript.video.PLAYBACK";
    public static final String RECORD_ACTION = "com.wearscript.video.RECORD";
    public static final String RECORD_RESULT_ACTION = "com.wearscript.video.RECORD_RESULT";
    private static final String TAG = "WearScriptBroadcastReceiver";
    private static final boolean DBG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (DBG) Log.v(TAG, "Actionnnn: " + action);
            if (action.equals(PLAYBACK_ACTION) || action.equals(RECORD_RESULT_ACTION)) {
                Intent playbackIntent = new Intent();
                playbackIntent.putExtras(intent.getExtras());
                playbackIntent.setClass(context, PlaybackActivity.class);
                playbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Log.v(TAG, "Attempting to start Playback Activity.");
                context.startActivity(playbackIntent);
            } else if (action.equals(RECORD_ACTION)) {
                Intent recordIntent = new Intent();
                recordIntent.putExtras(intent.getExtras());
                recordIntent.setClass(context, RecordActivity.class);
                recordIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Log.v(TAG, "Attempting to start RecordActivity");
                context.startActivity(recordIntent);
            } else {
                Log.w(TAG, "Why am I even here? Got unknown action: " + action);
            }
        }
        try {
            finalize();
        } catch (Throwable th) {
            Log.v(TAG, "Couldn't finalize", th);
        }
    }
}
