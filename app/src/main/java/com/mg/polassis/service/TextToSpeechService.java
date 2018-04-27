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

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;


import com.mg.polassis.misc.Translations;

import java.util.HashMap;

public class TextToSpeechService extends Service {
    private TextToSpeech textToSpeech = null;
    private final TextToSpeechBinder binder = new TextToSpeechBinder();
    private SharedPreferences assistantSettings;
    public boolean isSpeaking;
    private boolean isInitialised;
    private String toBeSaid = null;
    private float rateToBeSet = -1;
    private HashMap<String, String> textToSpeechSettings;
    private HashMap<String, String> textToSpeechSettingsForBluetooth;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {

        }
    };

    public class TextToSpeechBinder extends Binder
    {
        public TextToSpeechService getService()
        {
            return TextToSpeechService.this;
        }
    }

    @Override
    public void onCreate()
    {
        assistantSettings = PreferenceManager.getDefaultSharedPreferences(TextToSpeechService.this);
        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
    }

    private void initialiseSystemTextToSpeech()
    {
        textToSpeech = new TextToSpeech(TextToSpeechService.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int result) {
                if (result == TextToSpeech.SUCCESS)
                {
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String s) {

                        }

                        @Override
                        public void onDone(String s) {
                            audioManager.abandonAudioFocus(onAudioFocusChangeListener);
                            isSpeaking = false;
                            textToSpeechSettings.remove(TextToSpeech.Engine.KEY_PARAM_STREAM);
                        }

                        @Override
                        public void onError(String s) {
                            audioManager.abandonAudioFocus(onAudioFocusChangeListener);
                            isSpeaking = false;
                            textToSpeechSettings.remove(TextToSpeech.Engine.KEY_PARAM_STREAM);
                            Toast.makeText(TextToSpeechService.this, Translations.getStringResource(TextToSpeechService.this, "tts_error"), Toast.LENGTH_LONG).show();
                        }
                    });


                    if (rateToBeSet != -1)
                    {
                        textToSpeech.setSpeechRate(rateToBeSet);
                        rateToBeSet = -1;
                    }

                    isInitialised = true;

                    if (toBeSaid != null)
                    {
                        textToSpeech.speak(toBeSaid, TextToSpeech.QUEUE_FLUSH, (forceStream ? textToSpeechSettings : (isHeadsetConnected() ? textToSpeechSettingsForBluetooth : textToSpeechSettings)));
                        toBeSaid = null;
                        forceStream = false;
                    }
                }
                else
                {
                    Toast.makeText(TextToSpeechService.this, Translations.getStringResource(TextToSpeechService.this, "tts_load_error").replace("%CODE%", Integer.toString(result)), Toast.LENGTH_LONG).show();
                }
            }
        });

        textToSpeechSettings = new HashMap<>();
        textToSpeechSettingsForBluetooth = new HashMap<>();

        textToSpeechSettings.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Polassis");
        textToSpeechSettingsForBluetooth.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Polassis");
        textToSpeechSettingsForBluetooth.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        start();
        return START_STICKY;
    }

    public void start()
    {
        isSpeaking = false;
        isInitialised = false;

        initialiseSystemTextToSpeech();
    }

    @Override
    public void onDestroy()
    {
        cancelSpeaking();

        try
        {
            textToSpeech.shutdown();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        textToSpeech = null;
        isInitialised = false;
        isSpeaking = false;
        textToSpeechSettings = null;

    }

    @Override
    public IBinder onBind(Intent intent) {
        start();
        return binder;
    }

    public void speak(String text)
    {
        audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

            isSpeaking = true;
            if (isInitialised) textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, (isHeadsetConnected() ? textToSpeechSettingsForBluetooth : textToSpeechSettings));
            else toBeSaid = text;
    }

    private boolean forceStream = false;
    public void speak(String text, int stream)
    {
        audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

            isSpeaking = true;
            textToSpeechSettings.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(stream));
            if (isInitialised) textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, textToSpeechSettings);
            else
            {
                toBeSaid = text;
                forceStream = true;
            }
    }

    public void restart()
    {
        onDestroy();
        start();
    }

    public void cancelSpeaking()
    {
        if (isSpeaking)
        {
            audioManager.abandonAudioFocus(onAudioFocusChangeListener);
            textToSpeech.stop();
            isSpeaking = false;
        }
    }

    public void setSpeechRate(float rate)
    {
        if (isInitialised) textToSpeech.setSpeechRate(rate);
        else rateToBeSet = rate;
    }

    private boolean isHeadsetConnected()
    {
        SharedPreferences shared = assistantSettings;
        return shared.getBoolean("bluetooth_is_connected", false) && shared.getBoolean("bluetooth_headset_support", false);
    }
}
