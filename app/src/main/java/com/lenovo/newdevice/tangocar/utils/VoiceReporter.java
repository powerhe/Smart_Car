package com.lenovo.newdevice.tangocar.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;

public class VoiceReporter {

    private static VoiceReporter sReporter;

    private TextToSpeech mProgressTts;

    private VoiceReporter(Context context) {
        mProgressTts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.d("VoiceReporter", "TTS init status:" + status);
            }
        });
    }

    public synchronized static VoiceReporter from(Context context) {
        if (sReporter == null) sReporter = new VoiceReporter(context);
        return sReporter;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void report(String content) {
        if (TextUtils.isEmpty(content)) return;
        mProgressTts.speak(content, TextToSpeech.QUEUE_FLUSH, null, "UUID:" + content);
    }
}
