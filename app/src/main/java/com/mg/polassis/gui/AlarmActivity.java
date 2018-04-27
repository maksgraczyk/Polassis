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

package com.mg.polassis.gui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.mg.polassis.R;
import com.mg.polassis.misc.Assistant;
import com.mg.polassis.service.TextToSpeechService;
import com.mg.polassis.misc.Translations;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmActivity extends Activity {

    private SoundPool soundPool;
    private int clockStreamID;
    private int clockSoundID;
    private Timer blinkingTextTimer;
    private TextView timeText;
    private TextView titleText;
    private final Handler handler = new Handler();
    private TextToSpeechService textToSpeechService;
    private String toBeSaid;

    private final ServiceConnection textToSpeechServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            textToSpeechService = ((TextToSpeechService.TextToSpeechBinder)service).getService();
            if (toBeSaid != null)
            {
                textToSpeechService.speak(toBeSaid, AudioManager.STREAM_ALARM);
                toBeSaid = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            textToSpeechService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        bindService(new Intent(this, TextToSpeechService.class), textToSpeechServiceConnection, BIND_AUTO_CREATE);

        timeText = (TextView) findViewById(R.id.noTimeRemaining);
        timeText.setShadowLayer(15, 4, 4, Color.BLACK);

        titleText = (TextView) findViewById(R.id.timeIsUp);
        titleText.setShadowLayer(15, 4, 4, Color.BLACK);

        PowerManager.WakeLock wakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "WakeUp");

        if (!((PowerManager) getSystemService(POWER_SERVICE)).isScreenOn()) {
            wakeLock.acquire();
            wakeLock.release();
        }

        if (!Assistant.inForeground)
            sendBroadcast(new Intent().setAction("com.mg.polassis.ACTION_FINISH"));

        if (getIntent() == null) prepareForTimer();
        else if (getIntent().getBooleanExtra("alarm", false)) prepareForAlarmClock();
        else if (getIntent().getBooleanExtra("reminder", false)) prepareForReminder();
        else prepareForTimer();
    }

    private void startAlarmSound(final boolean continuous)
    {
        if (android.os.Build.VERSION.SDK_INT >= 21)
        {
            SoundPool.Builder builder = new SoundPool.Builder();
            AudioAttributes.Builder attributesBuilder = new AudioAttributes.Builder();
            attributesBuilder.setUsage(AudioAttributes.USAGE_ALARM);
            builder.setAudioAttributes(attributesBuilder.build());
            builder.setMaxStreams(1);
            soundPool = builder.build();
        }
        else soundPool = new SoundPool(1, AudioManager.STREAM_ALARM, 0);

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                clockStreamID = soundPool.play(clockSoundID, 1.0f, 1.0f, Integer.MAX_VALUE, (continuous ? -1 : 0), 1.0f);
            }
        });

        clockSoundID = soundPool.load(this, R.raw.timer, 1);
    }

    private void prepareForAlarmClock()
    {
        titleText.setText(Translations.getStringResource(this, "alarm_message"));
        timeText.setVisibility(View.INVISIBLE);

        SharedPreferences alarmsPreferences = getSharedPreferences("alarms", MODE_PRIVATE);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        alarmsPreferences.edit().remove(Long.toString(calendar.getTimeInMillis())).apply();
        if (android.os.Build.VERSION.SDK_INT < 21 && alarmsPreferences.getAll().keySet().size() == 0)
            sendBroadcast(new Intent("android.intent.action.ALARM_CHANGED").putExtra("alarmSet", false));

        startAlarmSound(true);
    }

    private void prepareForTimer()
    {
        titleText.setText(Translations.getStringResource(this, "time_is_up"));
        timeText.setVisibility(View.VISIBLE);

        blinkingTextTimer = new Timer();
        blinkingTextTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (timeText.getVisibility() == View.INVISIBLE)
                            timeText.setVisibility(View.VISIBLE);
                        else timeText.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }, 400, 400);

        startAlarmSound(true);
    }

    private void prepareForReminder()
    {
        String reminderTitle = getIntent().getStringExtra("title");
        titleText.setText(reminderTitle);
        timeText.setVisibility(View.INVISIBLE);

        SharedPreferences remindersPreferences = getSharedPreferences("reminders", MODE_PRIVATE);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        remindersPreferences.edit().remove(Long.toString(calendar.getTimeInMillis())).apply();

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("text_to_speech", true))
        {
            toBeSaid = Translations.getStringResource(this, "reminder") + reminderTitle;
            if (textToSpeechService != null)
            {
                textToSpeechService.speak(toBeSaid, AudioManager.STREAM_ALARM);
                toBeSaid = null;
            }
        }
        else startAlarmSound(false);
    }

    public void onStopClick(View v)
    {
        finish();
    }

    public void onStop()
    {
        super.onStop();
        if (isFinishing()) {
            unbindService(textToSpeechServiceConnection);

            if (blinkingTextTimer != null) {
                blinkingTextTimer.cancel();
                blinkingTextTimer = null;
            }

            if (soundPool != null)
            {
                soundPool.stop(clockStreamID);
                soundPool.unload(clockSoundID);
                soundPool.release();
                soundPool = null;
            }
        }
    }
}
