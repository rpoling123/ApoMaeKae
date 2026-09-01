package com.apomaekae.dragon;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import java.util.Locale;
import java.util.Set;

public class ThaiVoice {

    private static TextToSpeech tts;
    private static boolean ready = false;
    private static Context appContext;

    public static void init(Context context) {

        if (tts != null) return;

        appContext = context.getApplicationContext();

        tts = new TextToSpeech(appContext, status -> {

            if (status == TextToSpeech.SUCCESS) {

                int result = tts.setLanguage(new Locale("th", "TH"));

                ready =
                    result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED;

                // พยายามเลือกเสียงผู้หญิงภาษาไทย
                if (ready) {
                    try {
                        Set<Voice> voices = tts.getVoices();

                        if (voices != null) {
                            Voice femaleThai = null;
                            Voice thai = null;

                            for (Voice v : voices) {

                                if (v == null || v.getLocale() == null)
                                    continue;

                                Locale l = v.getLocale();

                                if (!"th".equalsIgnoreCase(l.getLanguage()))
                                    continue;

                                thai = v;

                                String name = v.getName().toLowerCase();

                                if (name.contains("female") ||
                                    name.contains("fem") ||
                                    name.contains("woman")) {
                                    femaleThai = v;
                                    break;
                                }
                            }

                            if (femaleThai != null) {
                                tts.setVoice(femaleThai);
                            } else if (thai != null) {
                                tts.setVoice(thai);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                // ปรับความเร็วและระดับเสียง
                tts.setSpeechRate(0.92f);
                tts.setPitch(1.05f);
            }
        });
    }

    public static void speak(Context context, String text) {

        if (text == null || text.trim().isEmpty())
            return;

        init(context);

        // รอให้ TTS พร้อมแล้วค่อยพูด
        new Thread(() -> {

            for (int i = 0; i < 30 && !ready; i++) {
                try {
                    Thread.sleep(100);
                } catch (Exception ignored) {
                }
            }

            if (!ready || tts == null)
                return;

            Bundle params = new Bundle();

            params.putString(
                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                "APO_" + System.currentTimeMillis()
            );

            tts.speak(
                text,
                TextToSpeech.QUEUE_ADD,
                params,
                "APO_" + System.currentTimeMillis()
            );

        }).start();
    }

    // ใช้ทดสอบเสียง
    public static void test(Context context) {
        speak(
            context,
            "สวัสดีครับ อาโปแมพแก ระบบเสียงภาษาไทยพร้อมใช้งานแล้ว"
        );
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
