/*
Polassis: personal voice assistant for Android devices
Copyright (C) 2018 Maksymilian Graczyk

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.mg.polassis.service;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.mg.polassis.misc.Numbers;
import com.mg.polassis.R;
import com.mg.polassis.misc.Translations;
import com.mg.polassis.misc.Assistant;
import com.mg.polassis.gui.MicrophoneButton;
import com.mg.polassis.gui.SettingsActivity;

import java.util.ArrayList;

public class BackgroundSpeechRecognitionService extends Service {
    private WindowManager windowManager;
    private ImageButton microphoneButton;
    private RelativeLayout layout;
    private boolean wasHeadVisible = false;
    private KeyguardManager.KeyguardLock keyguardLock;
    private Handler handler = new Handler();
    private boolean cancelled = false;
    private boolean areResultsAvailable = false;
    private final SpeechRecognitionService.RecognitionStepsRunnable recognitionStepsRunnable = new SpeechRecognitionService.RecognitionStepsRunnable() {
        @Override
        public void runAfterRecognition(ArrayList<String> results, float[] confidenceScores) {
            String result = Numbers.processText(BackgroundSpeechRecognitionService.this, (String) results.get(0));
            Intent in = new Intent(BackgroundSpeechRecognitionService.this, Assistant.class);
            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            in.putExtra("command", result);
            in.putExtra("show_command", true);
            startActivity(in);

            stopSelf();
        }

        @Override
        public void runIfHeardNothing() {
            stopSelf();
        }

        @Override
        public void runIfStartingError() {
            stopSelf();
        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onReadyForSpeech() {

        }

        @Override
        public void beforeErrorProcessing(int error) {

        }
    };

    private SpeechRecognitionService speechRecognitionService;
    private ServiceConnection speechRecognitionServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SpeechRecognitionService.SpeechRecognitionBinder binder = (SpeechRecognitionService.SpeechRecognitionBinder)iBinder;
            speechRecognitionService = binder.getService();

            final Runnable toBeRun = new Runnable() {
                @Override
                public void run() {
                    if (((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode()) {
                        keyguardLock = ((KeyguardManager) getSystemService(KEYGUARD_SERVICE)).newKeyguardLock(getClass().getName());
                        keyguardLock.disableKeyguard();
                    }

                    final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(BackgroundSpeechRecognitionService.this).setContentTitle("Polassis");
                    notificationBuilder.setContentText(Translations.getStringResource(BackgroundSpeechRecognitionService.this, "speech_recognition_working"));
                    notificationBuilder.setSmallIcon(R.drawable.in_progress);
                    notificationBuilder.setContentIntent(PendingIntent.getActivity(BackgroundSpeechRecognitionService.this, 9999, new Intent(BackgroundSpeechRecognitionService.this, SettingsActivity.class), PendingIntent.FLAG_CANCEL_CURRENT));

                    layout.setBackgroundColor(Color.argb(128, 0, 0, 0));
                    layout.addView(microphoneButton);

                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) microphoneButton.getLayoutParams();
                    params.addRule(RelativeLayout.CENTER_IN_PARENT, 1);

                    microphoneButton.setContentDescription(Translations.getStringResource(BackgroundSpeechRecognitionService.this, "microphone"));
                    microphoneButton.setLayoutParams(params);
                    microphoneButton.setImageResource(R.drawable.mic_inactive);
                    microphoneButton.setBackgroundColor(Color.TRANSPARENT);

                    microphoneButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            cancelled = true;
                            speechRecognitionService.cancel();
                            stopSelf();
                        }
                    });
                    microphoneButton.setClickable(false);

                    final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.FILL_PARENT,
                            WindowManager.LayoutParams.FILL_PARENT,
                            WindowManager.LayoutParams.TYPE_PHONE,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            PixelFormat.TRANSLUCENT);

                    if (AssistantHeadService.isWorking) {
                        wasHeadVisible = true;
                        AssistantHeadService.toBeShutDown = true;
                        stopService(new Intent(BackgroundSpeechRecognitionService.this, AssistantHeadService.class));
                    }

                    startForeground(10, notificationBuilder.build());

                    windowManager.addView(layout, layoutParams);

                    speechRecognitionService.startListening(BackgroundSpeechRecognitionService.this, recognitionStepsRunnable, new MicrophoneButton(microphoneButton));

                    ImageButton button = new ImageButton(BackgroundSpeechRecognitionService.this);
                    button.setContentDescription(Translations.getStringResource(BackgroundSpeechRecognitionService.this, "turn_off"));
                    button.setImageDrawable(getResources().getDrawable(R.drawable.close));
                    button.setBackgroundColor(Color.TRANSPARENT);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                speechRecognitionService.cancel();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            stopSelf();
                        }
                    });
                    RelativeLayout.LayoutParams buttonLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    buttonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
                    buttonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
                    button.setLayoutParams(buttonLayoutParams);
                    layout.addView(button);
                    windowManager.updateViewLayout(layout, layoutParams);
                }
            };

            if (isHeadsetConnected()) {
                registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int extra = intent.getExtras().getInt(AudioManager.EXTRA_SCO_AUDIO_STATE);
                        if (extra == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                            toBeRun.run();
                            unregisterReceiver(this);
                        }
                    }
                }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
                turnOnHeadset();
            }
            else toBeRun.run();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            speechRecognitionService = null;
        }
    };

    public boolean isHeadsetConnected()
    {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(BackgroundSpeechRecognitionService.this);
        if (shared.getBoolean("bluetooth_is_connected", false) && shared.getBoolean("bluetooth_headset_support", false)) return true;
        else return false;
    }

    public void turnOffHeadset()
    {
        AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        if (audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
        }
    }

    public void turnOnHeadset() {
        AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        if (isHeadsetConnected() && audioManager.getMode() != AudioManager.MODE_IN_COMMUNICATION) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        PowerManager.WakeLock wakeLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "WakeUpWywolanie");

        wakeLock.acquire();
        wakeLock.release();

        layout = new RelativeLayout(BackgroundSpeechRecognitionService.this);
        microphoneButton = new ImageButton(BackgroundSpeechRecognitionService.this);

        bindService(new Intent(BackgroundSpeechRecognitionService.this, SpeechRecognitionService.class), speechRecognitionServiceConnection, BIND_AUTO_CREATE);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        if (layout != null) {
            try {
                windowManager.removeView(layout);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (cancelled || !areResultsAvailable) turnOffHeadset();

        handler.removeCallbacksAndMessages(null);
        stopForeground(true);

        if (keyguardLock != null) keyguardLock.reenableKeyguard();
        if (wasHeadVisible && !AssistantHeadService.isWorking) startService(new Intent(BackgroundSpeechRecognitionService.this, AssistantHeadService.class));

        unbindService(speechRecognitionServiceConnection);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
