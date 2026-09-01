package com.apomaekae.dragon;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class ThaiVoice {

    private static TextToSpeech tts;
    private static boolean ready = false;

    public static void init(Context context) {
        if (tts != null) return;

        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("th", "TH"));
                ready = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
    }

    public static void speak(Context context, String text) {
        init(context);

        if (ready && tts != null) {
            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "APOMAEKAE_" + System.currentTimeMillis()
            );
        }
    }

    public static void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public static void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            ready = false;
        }
    }
}
