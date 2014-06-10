# wearscript-android-video

Video recorder meant to be launched by BroadcastReceiver

Try it from the command line:

      adb shell am broadcast -a com.wearscript.video.RECORD --ei duration 10

Or specify path (or both)

      adb shell am broadcast -a com.wearscript.video.RECORD --es path /sdcard/wearscript/awesome_video.mp4
