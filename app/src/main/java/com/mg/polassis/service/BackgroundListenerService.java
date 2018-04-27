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
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.mg.polassis.R;
import com.mg.polassis.misc.Assistant;
import com.mg.polassis.misc.Translations;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class BackgroundListenerService extends Service implements RecognitionListener{
    public static SpeechRecognizer speechRecognizer;
    private AsyncTask<Void, Void, Exception> task;
    private boolean specialCommandDetected = false;
    private Handler handler = new Handler();
    private SensorManager sensorManager;
    private Sensor sensor;

    private String activationType;

    private class BackgroundListenerServiceBinder extends Binder
    {
        public BackgroundListenerService getService()
        {
            return BackgroundListenerService.this;
        }
    }

    private final BackgroundListenerServiceBinder binder = new BackgroundListenerServiceBinder();

    private BroadcastReceiver screenOffOnBroadcastReceiver = new BroadcastReceiver() {

        private int howManyTimes = 0;
        private long currentTime = System.currentTimeMillis();

        @Override
        public void onReceive(Context arg0, Intent arg1)
        {
            if (!toBePaused) {
                if (arg1.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    if (System.currentTimeMillis() - currentTime <= 3000) {
                        howManyTimes += 1;
                        if (howManyTimes >= 3) {
                            howManyTimes = 0;
                            runActivity();
                        } else currentTime = System.currentTimeMillis();
                    } else {
                        howManyTimes = 1;
                        currentTime = System.currentTimeMillis();
                    }
                } else if (arg1.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    if (System.currentTimeMillis() - currentTime <= 3000) {
                        howManyTimes += 1;
                        if (howManyTimes >= 3) {
                            howManyTimes = 0;
                            runActivity();
                        } else currentTime = System.currentTimeMillis();
                    } else {
                        howManyTimes = 1;
                        currentTime = System.currentTimeMillis();
                    }
                }
            }
        }
    };

    private boolean toBePaused;

    private BroadcastReceiver backgroundListeningControlBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction())
            {
                case "com.mg.polassis.service.BackgroundListenerService.PAUSE_LISTENING":
                    toBePaused = true;
                    try {
                        if (speechRecognizer != null) speechRecognizer.stop();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    sendBroadcast(new Intent("com.mg.polassis.service.BackgroundListenerService.PAUSE_LISTENING_COMPLETED"));
                    break;

                case "com.mg.polassis.service.BackgroundListenerService.RESUME_LISTENING":
                    toBePaused = false;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!toBePaused) {
                                if (speechRecognizer != null)
                                    speechRecognizer.startListening("wakeup");
                            }
                            else toBePaused = false;
                            sendBroadcast(new Intent("com.mg.polassis.service.BackgroundListenerService.RESUME_LISTENING_COMPLETED"));
                        }
                    }, 250);
                    break;
            }
        }
    };

    private SensorEventListener proximitySensorListener = new SensorEventListener() {
        private int howManyTimes = 0;
        private long currentTime = 0;
        private boolean firstTime = true;
        private boolean near = false;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!toBePaused) {
                float currentDistance = event.values[0];

                if (firstTime) {
                    firstTime = false;
                    currentTime = System.currentTimeMillis();
                } else if (near && currentDistance >= 3 && System.currentTimeMillis() - currentTime <= 800) {
                    currentTime = System.currentTimeMillis();
                    near = false;
                    howManyTimes += 1;
                    if (howManyTimes == 2) {
                        howManyTimes = 0;
                        runActivity();
                    }
                } else if (!near && currentDistance < 3 && System.currentTimeMillis() - currentTime <= 800) {
                    currentTime = System.currentTimeMillis();
                    near = true;
                } else {
                    howManyTimes = 0;
                    near = currentDistance < 3;
                    currentTime = System.currentTimeMillis();
                }
            }
            else howManyTimes = 0;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public static boolean isWorking = false;

    @Override
    public void onCreate()
    {
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        toBePaused = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent != null && intent.hasExtra("cancel"))
        {
            PreferenceManager.getDefaultSharedPreferences(BackgroundListenerService.this).edit().putBoolean("activation", false).apply();
            stopSelf();
            return START_NOT_STICKY;
        }
        else {
            isWorking = true;
            specialCommandDetected = false;

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.mg.polassis.service.BackgroundListenerService.PAUSE_LISTENING");
            intentFilter.addAction("com.mg.polassis.service.BackgroundListenerService.RESUME_LISTENING");
            registerReceiver(backgroundListeningControlBroadcastReceiver, intentFilter);

            final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(BackgroundListenerService.this).setContentTitle(Translations.getStringResource(BackgroundListenerService.this, "activation_on_notify_title"));
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(Translations.getStringResource(BackgroundListenerService.this, "activation_on_notify_desc"))).setContentText(Translations.getStringResource(BackgroundListenerService.this, "activation_on_notify_desc"));
            notificationBuilder.addAction(new NotificationCompat.Action(R.drawable.close_black, Translations.getStringResource(BackgroundListenerService.this, "turn_off_activation"), PendingIntent.getService(BackgroundListenerService.this, 0, new Intent(BackgroundListenerService.this, BackgroundListenerService.class).putExtra("cancel", true), PendingIntent.FLAG_UPDATE_CURRENT)));

            notificationBuilder.setSmallIcon(R.drawable.mic_notification);

            activationType = PreferenceManager.getDefaultSharedPreferences(BackgroundListenerService.this).getString("activation_type", "voice");

            if (activationType.equals("press_power_button")) {
                IntentFilter intentFilterScreenOnOff = new IntentFilter();
                intentFilterScreenOnOff.addAction(Intent.ACTION_SCREEN_ON);
                intentFilterScreenOnOff.addAction(Intent.ACTION_SCREEN_OFF);
                registerReceiver(screenOffOnBroadcastReceiver, intentFilterScreenOnOff);
                Notification notification = notificationBuilder.build();
                notification.flags = Notification.FLAG_ONGOING_EVENT;
                startForeground(2, notification);
            }
            else if (activationType.equals("wave")) {
                sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                sensorManager.registerListener(proximitySensorListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
                Notification notification = notificationBuilder.build();
                notification.flags = Notification.FLAG_ONGOING_EVENT;
                startForeground(2, notification);
            }
            else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        task = new AsyncTask<Void, Void, Exception>() {
                            @Override
                            protected Exception doInBackground(Void... params) {
                                try {
                                    Assets assets = new Assets(BackgroundListenerService.this);
                                    File assetDir = assets.syncAssets();
                                    File modelsDir = new File(assetDir, "models");
                                    int threshold = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(BackgroundListenerService.this).getString("activation_threshold", "75"));
                                    SpeechRecognizerSetup pre_recognizer = defaultSetup()
                                            .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                                            .setDictionary(new File(modelsDir, "dict/cmu07a.dic"));

                                    if (threshold == 60)
                                        pre_recognizer.setKeywordThreshold(1e-40f);
                                    else if (threshold == 65)
                                        pre_recognizer.setKeywordThreshold(1e-35f);
                                    else if (threshold == 70)
                                        pre_recognizer.setKeywordThreshold(1e-30f);
                                    else if (threshold == 75)
                                        pre_recognizer.setKeywordThreshold(1e-25f);
                                    else if (threshold == 80)
                                        pre_recognizer.setKeywordThreshold(1e-20f);
                                    else if (threshold == 85)
                                        pre_recognizer.setKeywordThreshold(1e-15f);
                                    else if (threshold == 90)
                                        pre_recognizer.setKeywordThreshold(1e-10f);
                                    else if (threshold == 95)
                                        pre_recognizer.setKeywordThreshold(1e-5f);
                                    else pre_recognizer.setKeywordThreshold(1f);

                                    speechRecognizer = pre_recognizer.getRecognizer();
                                    speechRecognizer.addListener(BackgroundListenerService.this);
                                    speechRecognizer.addKeyphraseSearch("wakeup", PreferenceManager.getDefaultSharedPreferences(BackgroundListenerService.this).getString("activation_phrase", "hi polish app").toLowerCase());
                                } catch (Exception e) {
                                    return e;
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Exception result) {
                                if (result == null) {
                                    try {
                                        if (!toBePaused) speechRecognizer.startListening("wakeup");
                                        else toBePaused = false;
                                        Notification notification = notificationBuilder.build();
                                        notification.flags = Notification.FLAG_ONGOING_EVENT;
                                        startForeground(2, notification);
                                    } catch (RuntimeException e) {
                                        if (!PreferenceManager.getDefaultSharedPreferences(BackgroundListenerService.this).getBoolean("debug_mode", false))
                                            Toast.makeText(BackgroundListenerService.this, Translations.getStringResource(BackgroundListenerService.this, "activation_word_prohibited"), Toast.LENGTH_LONG).show();
                                        else
                                            Toast.makeText(BackgroundListenerService.this, e.toString(), Toast.LENGTH_LONG).show();
                                    } catch (Exception e) {
                                        if (!PreferenceManager.getDefaultSharedPreferences(BackgroundListenerService.this).getBoolean("debug_mode", false))
                                            Toast.makeText(BackgroundListenerService.this, Translations.getStringResource(BackgroundListenerService.this, "activation_error"), Toast.LENGTH_LONG).show();
                                        else
                                            Toast.makeText(BackgroundListenerService.this, e.toString(), Toast.LENGTH_LONG).show();
                                    }
                                } else if (result instanceof RuntimeException) {
                                    if (!PreferenceManager.getDefaultSharedPreferences(BackgroundListenerService.this).getBoolean("debug_mode", false))
                                        Toast.makeText(BackgroundListenerService.this, Translations.getStringResource(BackgroundListenerService.this, "activation_word_prohibited"), Toast.LENGTH_LONG).show();
                                    else
                                        Toast.makeText(BackgroundListenerService.this, result.toString(), Toast.LENGTH_LONG).show();
                                } else {
                                    if (!PreferenceManager.getDefaultSharedPreferences(BackgroundListenerService.this).getBoolean("debug_mode", false))
                                        Toast.makeText(BackgroundListenerService.this, Translations.getStringResource(BackgroundListenerService.this, "activation_error"), Toast.LENGTH_LONG).show();
                                    else
                                        Toast.makeText(BackgroundListenerService.this, result.toString(), Toast.LENGTH_LONG).show();
                                }
                            }
                        };

                        task.execute();
                    }
                }, 500);
            }

            return START_STICKY;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public void onDestroy()
    {
        isWorking = false;
        unregisterReceiver(backgroundListeningControlBroadcastReceiver);
        if (activationType.equals("voice") && !specialCommandDetected) {
            if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) task.cancel(true);
            if (speechRecognizer != null) {
                speechRecognizer.stop();
                speechRecognizer.removeListener(BackgroundListenerService.this);
                speechRecognizer.shutdown();
                speechRecognizer = null;
            }
        }
        else if (activationType.equals("press_power_button")) {
            try {
                unregisterReceiver(screenOffOnBroadcastReceiver);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (activationType.equals("wave")) {
            sensorManager.unregisterListener(proximitySensorListener);
            sensor = null;
        }
        stopForeground(true);
    }

    @Override
    public void onEndOfSpeech()
    {

    }

    @Override
    public void onResult(Hypothesis result)
    {
        phraseDetected(result);
    }

    @Override
    public void onTimeout()
    {

    }

    @Override
    public void onError(Exception e)
    {
        if (PreferenceManager.getDefaultSharedPreferences(BackgroundListenerService.this).getBoolean("debug_mode", false)) Toast.makeText(BackgroundListenerService.this, e.toString(), Toast.LENGTH_LONG).show();
        if (e.toString().toLowerCase().contains("microphone") || e.toString().toLowerCase().contains("mikrofon") || e.toString().toLowerCase().contains("in use")) {
            try {
                speechRecognizer.stop();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                speechRecognizer.shutdown();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            speechRecognizer = null;
            stopSelf();
        }
    }

    @Override
    public void onBeginningOfSpeech()
    {

    }

    public void runActivity()
    {
        if (!specialCommandDetected)
        {
            specialCommandDetected = true;
            PowerManager.WakeLock wakeLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Activation");
            wakeLock.acquire();
            wakeLock.release();
            if (speechRecognizer != null) speechRecognizer.stop();
            KeyguardManager keyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            if (!Assistant.inForeground && (android.os.Build.VERSION.SDK_INT < 23 || !keyguardManager.inKeyguardRestrictedInputMode())) startService(new Intent(BackgroundListenerService.this, BackgroundSpeechRecognitionService.class));
            else startActivity(new Intent(BackgroundListenerService.this, Assistant.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("activation", true).putExtra("nie_wylaczaj", true));
            specialCommandDetected = false;
        }
    }

    public void phraseDetected(Hypothesis result)
    {
            if (result != null) {
                runActivity();
            }
    }

    @Override
    public void onPartialResult(Hypothesis result)
    {
        phraseDetected(result);
    }
}
