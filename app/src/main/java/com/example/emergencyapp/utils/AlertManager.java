package com.example.emergencyapp.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.example.emergencyapp.R;

import java.util.Locale;

public class AlertManager implements TextToSpeech.OnInitListener {
    private static final String TAG = "AlertManager";

    private Context context;
    private MediaPlayer mediaPlayer;
    private TextToSpeech tts;
    private Vibrator vibrator;
    private Handler handler;
    private boolean isPlaying = false;
    private boolean ttsReady = false;

    private String currentMessage;
    private int alarmSoundResId;
    private int currentCycle = 0;
    private static final int MAX_CYCLES = 3;

    public AlertManager(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.tts = new TextToSpeech(context, this);
        this.alarmSoundResId = R.raw.alarm_sound; // default alarm
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("tr", "TR"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault());
            }
            ttsReady = true;
            tts.setSpeechRate(1.0f);
            tts.setPitch(1.0f);
        }
    }

    public void startEmergencyAlert(String message, int emergencyType) {
        if (isPlaying) return;

        isPlaying = true;
        currentMessage = message;
        currentCycle = 0;

        // Set alarm sound based on emergency type
        setAlarmSoundForType(emergencyType);

        // Set max volume
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

        // Start alert sequence
        startAlertSequence();
    }

    private void setAlarmSoundForType(int emergencyType) {
        switch (emergencyType) {
            case Constants.EMERGENCY_KIDNAPPING:
                alarmSoundResId = R.raw.alarm_kidnapping;
                break;
            case Constants.EMERGENCY_BEING_FOLLOWED:
                alarmSoundResId = R.raw.alarm_followed;
                break;
            case Constants.EMERGENCY_AMBULANCE:
                alarmSoundResId = R.raw.alarm_ambulance;
                break;
            case Constants.EMERGENCY_COME_HERE:
                alarmSoundResId = R.raw.alarm_come_here;
                break;
            case Constants.EMERGENCY_FULL:
                alarmSoundResId = R.raw.alarm_full_emergency;
                break;
            default:
                alarmSoundResId = R.raw.alarm_sound;
        }
    }

    private void startAlertSequence() {
        // Sequence: 3s alarm -> speak -> 10s alarm -> speak -> 10s alarm
        if (!isPlaying) return;

        switch (currentCycle) {
            case 0:
                playAlarm(Constants.ALARM_SHORT_DURATION); // 3 seconds
                break;
            case 1:
            case 2:
                playAlarm(Constants.ALARM_LONG_DURATION); // 10 seconds
                break;
        }
    }

    private void playAlarm(long duration) {
        if (!isPlaying) return;

        startVibration();

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = MediaPlayer.create(context, alarmSoundResId);
            if (mediaPlayer == null) {
                // Fallback if custom sound not found
                mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound);
            }

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();

                handler.postDelayed(() -> {
                    stopAlarmSound();
                    speakMessage();
                }, duration);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm: " + e.getMessage());
            speakMessage();
        }
    }

    private void speakMessage() {
        if (!isPlaying) return;

        if (ttsReady && currentMessage != null) {
            tts.speak(currentMessage, TextToSpeech.QUEUE_FLUSH, null, "emergency_tts");

            // Wait for TTS to complete then continue
            handler.postDelayed(() -> {
                currentCycle++;
                if (currentCycle < MAX_CYCLES && isPlaying) {
                    startAlertSequence();
                } else if (isPlaying) {
                    // Restart from beginning
                    currentCycle = 0;
                    startAlertSequence();
                }
            }, 3000); // Approximate TTS duration
        } else {
            currentCycle++;
            if (currentCycle < MAX_CYCLES && isPlaying) {
                startAlertSequence();
            }
        }
    }

    private void startVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 200, 500, 200, 500};
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopAlarmSound() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping alarm: " + e.getMessage());
            }
        }
    }

    public void stopAlert() {
        isPlaying = false;
        handler.removeCallbacksAndMessages(null);
        stopAlarmSound();

        if (tts != null) {
            tts.stop();
        }

        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void release() {
        stopAlert();
        if (tts != null) {
            tts.shutdown();
        }
    }
}