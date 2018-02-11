package com.mg.polassis.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.mg.polassis.R;
import com.mg.polassis.misc.Translations;
import com.mg.polassis.gui.MicrophoneButton;

import java.util.ArrayList;

public class SpeechRecognitionService extends Service {

    private final SpeechRecognitionBinder binder = new SpeechRecognitionBinder();
    private final Handler handler = new Handler();
    private final Handler speechRecognitionRunningErrorHandler = new Handler();
    private final SharedPreferences.OnSharedPreferenceChangeListener onAssistantSettingsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (s.equals("force_google_speech_recognition") || s.equals("slow_speech_recognition_solution")) initialiseSpeechRecognition();
        }
    };

    private boolean isInitialised;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    private SharedPreferences assistantSettings;
    private MicrophoneButton microphoneButton;

    private TextToSpeechService textToSpeechService = null;
    private ServiceConnection textToSpeechServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TextToSpeechService.TextToSpeechBinder binder = (TextToSpeechService.TextToSpeechBinder)iBinder;
            textToSpeechService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            textToSpeechService = null;
        }
    };
    
    private Context context;

    private int howManyErrorsSoFar;

    private RecognitionStepsRunnable recognitionStepsRunnable;

    private boolean isSpeaking;

    public boolean isWorking;
    public boolean isRecording;
    public boolean isRecognizerWorking;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {

        }
    };

    private BroadcastReceiver backgroundListenerServiceControlCompletedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction())
            {
                case "com.mg.polassis.service.BackgroundListenerService.PAUSE_LISTENING_COMPLETED":
                    strictStartListening(recognitionStepsRunnable);
            }
        }
    };

    public class SpeechRecognitionBinder extends Binder
    {
        public SpeechRecognitionService getService() {
            return SpeechRecognitionService.this;
        }
    }

    public interface RecognitionStepsRunnable
    {
        void runAfterRecognition(ArrayList<String> results, float[] confidenceScores);
        void runIfHeardNothing();
        void runIfStartingError();
        void onBeginningOfSpeech();
        void onEndOfSpeech();
        void onReadyForSpeech();
        void beforeErrorProcessing(int error);
    }

    public class RecognizerListener implements RecognitionListener
    {
        private boolean areResultsAvailable;
        private boolean isBeingProcessed;
        private int howManySilentRecordingsSoFar;

        public RecognizerListener(RecognitionStepsRunnable recognitionStepsRunnable)
        {
            SpeechRecognitionService.this.recognitionStepsRunnable = recognitionStepsRunnable;
            this.areResultsAvailable = false;
            this.isBeingProcessed = false;
            this.howManySilentRecordingsSoFar = 0;
        }

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            isRecognizerWorking = true;
            isRecording = true;
            microphoneButton.changeState(MicrophoneButton.State.READY_TO_RECORD);
            
            recognitionStepsRunnable.onReadyForSpeech();
        }

        @Override
        public void onBeginningOfSpeech() {
            microphoneButton.changeState(MicrophoneButton.State.RECORDING);
            
            recognitionStepsRunnable.onBeginningOfSpeech();
        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {
            isRecording = false;
            microphoneButton.changeState(MicrophoneButton.State.INACTIVE_WAITING);
            recognitionStepsRunnable.onEndOfSpeech();
        }

        @Override
        public void onError(int error) {
            if (!isBeingProcessed) {
                isBeingProcessed = true;
                microphoneButton.changeState(MicrophoneButton.State.READY);
                recognitionStepsRunnable.beforeErrorProcessing(error);
                if (assistantSettings.getBoolean("debug_mode", false)) Toast.makeText(SpeechRecognitionService.this, "Kod " + error, Toast.LENGTH_SHORT).show();
                if (error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            if (howManySilentRecordingsSoFar <= 1) {
                                microphoneButton.changeState(MicrophoneButton.State.INACTIVE);
                                

                                howManySilentRecordingsSoFar += 1;
                                speak(Translations.getStringResource(SpeechRecognitionService.this, "nothing_heard"));
                                restartListening(recognitionStepsRunnable, false);
                            } else {
                                if (assistantSettings.getBoolean("activation", false)) {
                                    if (BackgroundListenerService.isWorking) sendBroadcast(new Intent("com.mg.polassis.service.BackgroundListenerService.RESUME_LISTENING"));
                                    else startService(new Intent(SpeechRecognitionService.this, BackgroundListenerService.class));
                                }
                                audioManager.abandonAudioFocus(onAudioFocusChangeListener);
                                speak(Translations.getStringResource(SpeechRecognitionService.this, "nothing_heard_again"));
                                microphoneButton.changeState(MicrophoneButton.State.READY);
                                

                                howManySilentRecordingsSoFar = 0;
                                speechRecognitionRunningErrorHandler.removeCallbacksAndMessages(null);
                                isRecognizerWorking = false;
                                isWorking = false;
                                recognitionStepsRunnable.runIfHeardNothing();
                            }
                    } else if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                        howManyErrorsSoFar = 0;
                        speak(Translations.getStringResource(SpeechRecognitionService.this, "repeat"));
                        restartListening(recognitionStepsRunnable, false);
                    } else {
                        try {
                            if (!BackgroundListenerService.isWorking && BackgroundListenerService.speechRecognizer != null) {
                                BackgroundListenerService.speechRecognizer.stop();
                                BackgroundListenerService.speechRecognizer.shutdown();
                                BackgroundListenerService.speechRecognizer = null;
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                        if (howManyErrorsSoFar <= 3) {
                            microphoneButton.changeState(MicrophoneButton.State.INACTIVE);
                            

                            howManyErrorsSoFar += 1;

                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    restartListening(recognitionStepsRunnable, false);
                                }
                            }, 500);
                        } else {
                            audioManager.abandonAudioFocus(onAudioFocusChangeListener);

                            if (assistantSettings.getBoolean("activation", false)) {
                                if (BackgroundListenerService.isWorking) sendBroadcast(new Intent("com.mg.polassis.service.BackgroundListenerService.RESUME_LISTENING"));
                                else startService(new Intent(SpeechRecognitionService.this, BackgroundListenerService.class));
                            }
                            microphoneButton.changeState(MicrophoneButton.State.READY);
                            

                            speak(Translations.getStringResource(SpeechRecognitionService.this, "speech_recognition_error").replace("%%CODE%%", Integer.toString(error)));
                            howManyErrorsSoFar = 0;

                            speechRecognitionRunningErrorHandler.removeCallbacksAndMessages(null);
                            isRecognizerWorking = false;
                            isWorking = false;
                        }
                    }
                }

                isBeingProcessed = false;
            }
        }

        @Override
        public void onResults(Bundle bundle) {
            if (!assistantSettings.getBoolean("slow_speech_recognition_solution", false) || !areResultsAvailable)
            {
                audioManager.abandonAudioFocus(onAudioFocusChangeListener);
                if (assistantSettings.getBoolean("activation", false)) {
                    if (BackgroundListenerService.isWorking) sendBroadcast(new Intent("com.mg.polassis.service.BackgroundListenerService.RESUME_LISTENING"));
                    else startService(new Intent(SpeechRecognitionService.this, BackgroundListenerService.class));
                }
                areResultsAvailable = true;
                microphoneButton.changeState(MicrophoneButton.State.READY);
                speechRecognitionRunningErrorHandler.removeCallbacksAndMessages(null);
                isRecognizerWorking = false;
                isWorking = false;
                recognitionStepsRunnable.runAfterRecognition(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION), bundle.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES));
            }
        }

        @Override
        public void onPartialResults(final Bundle bundle) {
            if (assistantSettings.getBoolean("slow_speech_recognition_solution", false)) {
                final ArrayList<String> resultsList = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (resultsList != null && resultsList.size() > 0 && resultsList.get(0) != null && !resultsList.get(0).trim().isEmpty()) {
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!areResultsAvailable) {
                                audioManager.abandonAudioFocus(onAudioFocusChangeListener);
                                if (assistantSettings.getBoolean("activation", false)) {
                                    if (BackgroundListenerService.isWorking) sendBroadcast(new Intent("com.mg.polassis.service.BackgroundListenerService.RESUME_LISTENING"));
                                    else startService(new Intent(SpeechRecognitionService.this, BackgroundListenerService.class));
                                }
                                areResultsAvailable = true;
                                microphoneButton.changeState(MicrophoneButton.State.READY);
                                speechRecognitionRunningErrorHandler.removeCallbacksAndMessages(null);
                                isRecognizerWorking = false;
                                isWorking = false;
                                recognitionStepsRunnable.runAfterRecognition(resultsList, bundle.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES));
                            }
                        }
                    }, (assistantSettings.getBoolean("longer_timeout", false) ? 1000 : 600));
                }
            }
        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }
    }

    @Override
    public void onCreate()
    {
        isInitialised = false;
        isWorking = false;
        isRecognizerWorking = false;
        isSpeaking = false;
        assistantSettings = PreferenceManager.getDefaultSharedPreferences(SpeechRecognitionService.this);
        howManyErrorsSoFar = 0;
        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (!isInitialised) initialiseSpeechRecognition();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        if (!isInitialised) initialiseSpeechRecognition();
        return binder;
    }

    @Override
    public void onDestroy()
    {
        isWorking = false;
        isInitialised = false;
        unregisterReceiver(backgroundListenerServiceControlCompletedBroadcastReceiver);
        handler.removeCallbacksAndMessages(null);
        speechRecognitionRunningErrorHandler.removeCallbacksAndMessages(null);
        isRecognizerWorking = false;
        try {
            speechRecognizer.cancel();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        try {
            speechRecognizer.destroy();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        speechRecognizer = null;
        speechRecognizerIntent = null;
        unbindService(textToSpeechServiceConnection);
        assistantSettings.unregisterOnSharedPreferenceChangeListener(onAssistantSettingsChangeListener);
    }

    private void initialiseSpeechRecognition()
    {
        if (assistantSettings.getBoolean("text_to_speech", true) && textToSpeechService == null) bindService(new Intent(SpeechRecognitionService.this, TextToSpeechService.class), textToSpeechServiceConnection, BIND_AUTO_CREATE);

        assistantSettings.registerOnSharedPreferenceChangeListener(onAssistantSettingsChangeListener);

        if (assistantSettings.getBoolean("force_google_speech_recognition", false))
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(SpeechRecognitionService.this, ComponentName.unflattenFromString("com.google.android.googlequicksearchbox/com.google.android.voicesearch.serviceapi.GoogleRecognitionService"));
        else speechRecognizer = SpeechRecognizer.createSpeechRecognizer(SpeechRecognitionService.this);

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        if (!speechRecognizerIntent.hasExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE)) speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Translations.getStringResource(SpeechRecognitionService.this, "language_code"));
        speechRecognizerIntent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{Translations.getStringResource(SpeechRecognitionService.this, "language_code"), "en-US", "en-GB"});
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Translations.getStringResource(SpeechRecognitionService.this, "language_code"));

        if (assistantSettings.getBoolean("slow_speech_recognition_solution", false)) speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mg.polassis.service.BackgroundListenerService.PAUSE_LISTENING_COMPLETED");
        intentFilter.addAction("com.mg.polassis.service.BackgroundListenerService.RESUME_LISTENING_COMPLETED");
        registerReceiver(backgroundListenerServiceControlCompletedBroadcastReceiver, new IntentFilter("com.mg.polassis.service.BackgroundListenerService.PAUSE_LISTENING_COMPLETED"));

        isInitialised = true;
    }

    private void speak(String text)
    {
        if (textToSpeechService != null) textToSpeechService.speak(text);
        else
        {
            isSpeaking = true;
            Toast.makeText(SpeechRecognitionService.this, text, Toast.LENGTH_LONG).show();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isSpeaking = false;
                }
            }, 1000);
        }
    }

    private boolean isSpeaking()
    {
        if (textToSpeechService != null) return textToSpeechService.isSpeaking;
        else return isSpeaking;
    }

    public void startListening(Context context, RecognitionStepsRunnable recognitionStepsRunnable, MicrophoneButton microphoneButton)
    {
        if (isInitialised && !isWorking) {
            howManyErrorsSoFar = 0;
            this.context = context;
            this.microphoneButton = microphoneButton;
            isWorking = true;
            if (BackgroundListenerService.isWorking)
            {
                this.recognitionStepsRunnable = recognitionStepsRunnable;
                sendBroadcast(new Intent("com.mg.polassis.service.BackgroundListenerService.PAUSE_LISTENING"));
            }
            else strictStartListening(recognitionStepsRunnable);
        }
    }

    public void stopListening()
    {
        if (isInitialised && isWorking)
        {
            speechRecognizer.stopListening();
        }
    }

    public void cancel()
    {
        if (isInitialised && isWorking)
        {
            try {
                speechRecognizer.cancel();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            audioManager.abandonAudioFocus(onAudioFocusChangeListener);
            isWorking = false;
            isRecording = false;
            speechRecognitionRunningErrorHandler.removeCallbacksAndMessages(null);
            isRecognizerWorking = false;
            if (assistantSettings.getBoolean("activation", false)) {
                if (BackgroundListenerService.isWorking) sendBroadcast(new Intent("com.mg.polassis.service.BackgroundListenerService.RESUME_LISTENING"));
                else startService(new Intent(SpeechRecognitionService.this, BackgroundListenerService.class));
            }
            microphoneButton.changeState(MicrophoneButton.State.READY);
            
        }
    }

    private void strictStartListening(final RecognitionStepsRunnable recognitionStepsRunnable)
    {
        microphoneButton.changeState(MicrophoneButton.State.INACTIVE_WAITING);
        audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isSpeaking()) {

                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.setRecognitionListener(new RecognizerListener(recognitionStepsRunnable));
                        speechRecognizer.startListening(speechRecognizerIntent);

                        speechRecognitionRunningErrorHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isRecognizerWorking)
                                {
                                    initialiseSpeechRecognition();
                                    restartListening(recognitionStepsRunnable, true);
                                    speechRecognitionRunningErrorHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                                    if (!isRecognizerWorking) {
                                                        try {
                                                            speechRecognizer.destroy();
                                                            speechRecognizer = null;
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                        initialiseSpeechRecognition();
                                                        microphoneButton.changeState(MicrophoneButton.State.READY);
                                                        
                                                        try {
                                                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                                            builder.setCancelable(false);
                                                            builder.setTitle(Translations.getStringResource(SpeechRecognitionService.this, "speech_recognition_not_starting_title"));
                                                            builder.setMessage(Translations.getStringResource(SpeechRecognitionService.this, "speech_recognition_not_starting_message"));
                                                            builder.setPositiveButton(Translations.getStringResource(SpeechRecognitionService.this, "ok"), new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    recognitionStepsRunnable.runIfStartingError();
                                                                }
                                                            });
                                                            builder.setNegativeButton(null, null);
                                                            builder.create().show();
                                                        }
                                                        catch (Exception e)
                                                        {
                                                            Toast.makeText(SpeechRecognitionService.this, "speech_recognition_running_error_toast", Toast.LENGTH_LONG).show();
                                                            recognitionStepsRunnable.runIfStartingError();
                                                        }
                                                    }
                                        }
                                    }, 2500);
                                }
                            }
                        }, 2500);
                    }
                });
            }
        }).start();
    }

    private void restartListening(final RecognitionStepsRunnable recognitionStepsRunnable, boolean noHandlerClearing)
    {
        if (isInitialised) {
            if (!noHandlerClearing) speechRecognitionRunningErrorHandler.removeCallbacksAndMessages(null);
            isRecognizerWorking = false;
            strictStartListening(recognitionStepsRunnable);
        }
    }
}
