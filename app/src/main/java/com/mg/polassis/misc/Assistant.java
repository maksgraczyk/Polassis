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

package com.mg.polassis.misc;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Address;
import android.location.Geocoder;
import android.provider.CallLog;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.speech.SpeechRecognizer;
import android.provider.ContactsContract;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.support.v7.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mg.polassis.gui.AboutActivity;
import com.mg.polassis.gui.AlarmActivity;
import com.mg.polassis.gui.LanguageSelectionActivity;
import com.mg.polassis.gui.MicrophoneButton;
import com.mg.polassis.gui.PossibilitiesActivity;
import com.mg.polassis.gui.SettingsActivity;
import com.mg.polassis.service.BackgroundListenerService;
import com.mg.polassis.service.BluetoothAndSMSListenerService;
import com.mg.polassis.R;
import com.mg.polassis.service.SpeechRecognitionService;
import com.mg.polassis.service.TextToSpeechService;
import com.mg.polassis.service.TimerService;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import static com.mg.polassis.misc.ContainFunctions.contains;
import static com.mg.polassis.misc.ContainFunctions.containsDigit;
import static com.mg.polassis.misc.ContainFunctions.containsDigitOnly;
import static com.mg.polassis.misc.ContainFunctions.containsMathematicalSymbol;
import static com.mg.polassis.misc.ContainFunctions.containsURL;
import static com.mg.polassis.misc.ContainFunctions.containsWord;

public class Assistant extends Activity implements PopupMenu.OnMenuItemClickListener {

    public static int layoutWidth;
    public static int layoutHeight;
    public static boolean conversationMode = false;
    public static boolean noNextRecognitionAttempt = false;
    public static boolean isGoingToBeRecording = false;
    public static Camera camera = null;
    public static boolean inForeground = false;
    public static boolean isCalling = false;
    public static HashMap<String, ArrayList<String>> syntaxMap;

    private TextView recognizedText;
    private TextView assistantAnswer;
    private MicrophoneButton microphoneButton;
    private boolean isObtainingApps = false;
    private AssistantCategory currentCategory = AssistantCategory.READY;
    private float textToSpeechSpeed;
    private final Handler handler = new Handler();
    private boolean messageProcessed = false;
    private boolean biggerButtons = false;
    private SharedPreferences savedValues;
    private PowerManager.WakeLock wakeLock;
    private ArrayList<ApplicationInfo> appList;
    private boolean isBackgroundTaskRunning = false;
    private int originalSettingsWidth = -1;
    private int originalSettingsHeight = -1;
    private int originalMicrophoneWidth = -1;
    private int originalMicrophoneHeight = -1;
    private boolean isSpeaking = false;
    private boolean isTextToSpeechEnabled = true;
    private EditText commandEditText;
    private boolean isHeadsetLoaded = false;
    private boolean isHeadSetLoading = false;
    private boolean exitMode = false;
    private PowerManager powerManager;
    private SharedPreferences customCommands;

    private SharedPreferences assistantSettings;

    private final SpeechRecognitionService.RecognitionStepsRunnable recognitionStepsRunnable = new SpeechRecognitionService.RecognitionStepsRunnable() {
        private Timer notSpeakingSuggestionTimer;

        @Override
        public void runAfterRecognition(ArrayList<String> results, float[] confidenceScores) {
            afterRecognition(results.get(0));
        }

        @Override
        public void runIfHeardNothing() {
            if (currentCategory == AssistantCategory.READ_SMS_NEW_NOTIFICATION)
                new TurnOffAfterSpeaking().execute();
        }

        @Override
        public void runIfStartingError() {

        }

        @Override
        public void onBeginningOfSpeech() {
            notSpeakingSuggestionTimer = new Timer();
            notSpeakingSuggestionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Assistant.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (speechRecognitionService.isRecording)
                                findViewById(R.id.notSpeakingAnymore).setVisibility(View.VISIBLE);
                            notSpeakingSuggestionTimer = null;
                        }
                    });
                }
            }, 1000);
        }

        @Override
        public void onEndOfSpeech() {
            try {
                if (notSpeakingSuggestionTimer != null) notSpeakingSuggestionTimer.cancel();
                findViewById(R.id.notSpeakingAnymore).setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onReadyForSpeech() {

        }

        @Override
        public void beforeErrorProcessing(int error) {
            if (error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
                findViewById(R.id.notSpeakingAnymore).setVisibility(View.INVISIBLE);
        }
    };

    private enum AssistantCategory {
        READY,
        SMS_RECIPIENT,
        SMS_CONTENT,
        SMS_CONFIRMATION,
        SMS_CORRECTION_QUESTION,
        SMS_CORRECTION_TYPE,
        SMS_CORRECTION_ADD_CONTENT,
        SMS_SIM_CHOICE,
        CALL_RECIPIENT,
        CALL_NUMBER_CHOICE,
        NAVIGATE_PLACE,
        POSSIBILITIES_QUESTION,
        SEARCH_QUERY,
        ALARM_TIME,
        WEATHER_PLACE,
        CREATE_NOTE_TITLE,
        CREATE_NOTE_CONTENT,
        CREATE_NOTE_CONFIRMATION,
        CREATE_NOTE_CORRECTION_QUESTION,
        CREATE_NOTE_CORRECTION_TYPE,
        EDIT_NOTE_TYPE,
        EDIT_NOTE_TITLE,
        EDIT_NOTE_CONTENT,
        EDIT_NOTE_CONFIRMATION,
        READ_SMS_NEW_NOTIFICATION,
        READ_SMS_ANSWER_QUESTION,
        ACTIVATED_COMMAND_CONFIRMATION,
        MAIL_RECIPIENT,
        MAIL_TOPIC,
        MAIL_CONTENT,
        MAIL_CONFIRMATION,
        MAIL_CORRECTION_QUESTION,
        MAIL_CORRECTION_TYPE,
        MAIL_CORRECTION_TOPIC,
        MAIL_CORRECTION_CONTENT_TYPE,
        MAIL_CORRECTION_ADD_CONTENT,
        MAIL_CORRECTION_RECIPIENT,
        REMINDER_TITLE,
        REMINDER_DATE,
        REMINDER_TIME,
        REMINDER_CONFIRMATION,
        REMINDER_CORRECTION_QUESTION,
        REMINDER_CORRECTION_TYPE,
        REMINDER_CORRECTION_TIME,
        REMINDER_CORRECTION_TITLE,
        REMINDER_CORRECTION_DATE,
        RESTART_CONFIRMATION,
        TURN_OFF_CONFIRMATION,
        CONTACT_ONE_SUGGESTION,
        CONTACT_SUGGESTION_CHOICE,
        CONTACT_NUMBER_CHOICE,
        LAST_OUTCOMING_CALL_AGAIN_QUESTION,
        LAST_INCOMING_CALL_BACK_QUESTION,
        LAST_CALL_CALL_QUESTION,
        DICTATE_TO_CLIPBOARD_TEXT,
    }

    private class GetApplications extends AsyncTask<Void, Void, ArrayList<ApplicationInfo>> {
        @Override
        protected void onPreExecute() {
            isObtainingApps = true;
        }

        @Override
        protected ArrayList<ApplicationInfo> doInBackground(Void... params) {
            if (/*isTextToSpeechLoaded && */!exitMode) {
                List<ApplicationInfo> allApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
                ArrayList<ApplicationInfo> runnableApps = new ArrayList<ApplicationInfo>();
                for (int i = 0; i < allApps.size(); i++) {
                    if (getPackageManager().getLaunchIntentForPackage(allApps.get(i).packageName) != null)
                        runnableApps.add(allApps.get(i));
                }
                Collections.sort(runnableApps, new Comparator<ApplicationInfo>() {
                    @Override
                    public int compare(ApplicationInfo applicationInfo, ApplicationInfo t1) {
                        return getPackageManager().getApplicationLabel(t1).length() - getPackageManager().getApplicationLabel(applicationInfo).length();
                    }
                });
                return runnableApps;
            } else return null;
        }

        @Override
        protected void onPostExecute(ArrayList<ApplicationInfo> result) {
            appList = result;
            isObtainingApps = false;
        }
    }

    private class WaitingForAnswerTask extends TimerTask {
        public WaitingForAnswerTask() {
            assistantAnswer.setText("");
        }

        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (assistantAnswer.getText().equals("...")) assistantAnswer.setText(".");
                    else assistantAnswer.setText(assistantAnswer.getText() + ".");
                }
            });
        }
    }

    private Timer waitingForAnswerTimer;

    private class DownloadData implements NetworkHandler.NetworkQueryListener {
        private String type;
        private String url;
        private ProgressDialog progressDialog;
        private boolean nextAttempt;

        public DownloadData(String type) {
            this.type = type;
            this.nextAttempt = false;
        }

        public DownloadData(String type, boolean nextAttempt) {
            this.type = type;
            this.nextAttempt = nextAttempt;
        }

        public DownloadData(String type, String url) {
            this.type = type;
            this.url = url;
            this.nextAttempt = false;
        }

        @Override
        public void onBeforeConnection() {
            isBackgroundTaskRunning = true;
            if (!type.equals("news") && !type.equals("version") && !type.equals("language")) {
                if (waitingForAnswerTimer == null) waitingForAnswerTimer = new Timer();
                try {
                    waitingForAnswerTimer.schedule(new WaitingForAnswerTask(), 1000, 1000);
                } catch (Exception e) {
                    waitingForAnswerTimer = new Timer();
                    waitingForAnswerTimer.schedule(new WaitingForAnswerTask(), 1000, 1000);
                }
            } else if (type.equals("news") || type.equals("version") || type.equals("language")) {
                if (progressDialog == null)
                    progressDialog = ProgressDialog.show(Assistant.this, getString(R.string.app_name), Translations.getStringResource(Assistant.this, "wait"));
            }
        }

        @Override
        public void onConnectionCompleted(int responseCode, Map<String, List<String>> header, byte[] data) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
            String result = "";
            String line;
            boolean error = false;

            try {
                while ((line = reader.readLine()) != null) {
                    result += line + "\n";
                }
            } catch (Exception e) {
                error = true;
                speak(Translations.getAnswer(Assistant.this, "connection_error"));
                e.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (error) return;

            if (progressDialog != null && !type.equals("news") && !type.equals("version")) {
                progressDialog.dismiss();
                progressDialog = null;
            }

            if (waitingForAnswerTimer != null) {
                waitingForAnswerTimer.cancel();
                waitingForAnswerTimer = null;
            }

            if (type.equals("pogoda")) {
                try {
                    String toBeSaid;

                    JSONObject mainObject = new JSONObject(result);

                    int howManyDaysAhead = Integer.parseInt(Assistant.this.howManyDaysAhead);

                    if (howManyDaysAhead == 0) {
                        JSONObject weatherObject = mainObject.getJSONObject("currently");
                        String summary = weatherObject.getString("summary");
                        String temperature = Long.toString(Math.round(weatherObject.getDouble("temperature"))).replace('.', ',');
                        toBeSaid = (summary.endsWith(".") ? summary : summary + ".") + " " + Translations.getAnswer(Assistant.this, "temperature") + " " + temperature + Translations.getStringResource(Assistant.this, "degrees_celsius") + ".";
                    } else {
                        JSONArray dailyArray = mainObject.getJSONObject("daily").getJSONArray("data");
                        JSONObject weatherObject = dailyArray.getJSONObject(howManyDaysAhead - 1);
                        String summary = weatherObject.getString("summary");
                        String temperature = Long.toString(Math.round(weatherObject.getDouble("temperatureMax"))).replace('.', ',');
                        toBeSaid = (summary.endsWith(".") ? summary : summary + ".") + " " + Translations.getAnswer(Assistant.this, "max_temperature") + " " + temperature + Translations.getStringResource(Assistant.this, "degrees_celsius") + ".";
                    }

                    if (!toBeSaid.equals("")) {
                        speak(toBeSaid, false);
                        assistantAnswer.setText(toBeSaid + " (Powered by Dark Sky)");
                    } else
                        speak(Translations.getAnswer(Assistant.this, "no_weather_forecast"));
                } catch (JSONException e) {
                    if (assistantSettings.getBoolean("debug_mode", false))
                        Toast.makeText(Assistant.this, (result.toLowerCase().contains("exception") || result.toLowerCase().contains("error") ? result : e.toString()), Toast.LENGTH_LONG).show();
                    speak(Translations.getAnswer(Assistant.this, "no_weather_forecast"));
                } catch (Exception e) {
                    if (assistantSettings.getBoolean("debug_mode", false))
                        Toast.makeText(Assistant.this, (result.toLowerCase().contains("exception") || result.toLowerCase().contains("error") ? result : e.toString()), Toast.LENGTH_LONG).show();
                    speak(Translations.getAnswer(Assistant.this, "weather_forecast_download_error2"));
                }

                if (!conversationMode) {
                    microphoneButton.changeState(MicrophoneButton.State.READY);
                }
            } else if (type.equals("wikipedia")) {
                try {
                    JSONArray mainArray = new JSONArray(result);
                    JSONArray rawTextsArray = mainArray.getJSONArray(2);
                    if (rawTextsArray.length() == 0)
                        speak(Translations.getAnswer(Assistant.this, "do_not_know"));
                    else {
                        showSource(R.layout.external_wikipedia, false, rawTextsArray.getString(0), mainArray.getJSONArray(3).getString(0));
                        assistantAnswer.setText(Translations.getStringResource(Assistant.this, "wikipedia"));
                        speak(rawTextsArray.getString(0), false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (nextAttempt)
                        speak(Translations.getAnswer(Assistant.this, "wikipedia_check_error"));
                    else
                        NetworkHandler.query(Assistant.this, url, "GET", new DownloadData(type, true), null, null, null);
                }
            } else if (type.equals("news")) {
                SharedPreferences read = getSharedPreferences("read_news", MODE_PRIVATE);
                read.edit().putLong("last_checked", System.currentTimeMillis()).apply();
                try
                {
                    String[] rawNews = result.split("\n");
                    ArrayList<News> newsList = new ArrayList<News>();
                    for (int i = 0; i < rawNews.length; i++) {
                        String[] rawParts = rawNews[i].split("\\|\\;\\|");
                        if (!read.getBoolean(rawParts[0], false)) {
                            News news = new News(Assistant.this, rawParts[0], Long.parseLong(rawParts[1]), rawParts[2], rawParts[3], rawParts[4], rawParts[5].equals("1"));
                            if (news.isValidForAndroidVersion(android.os.Build.VERSION.SDK_INT) && news.isValidForAssistantVersion(Assistant.this)) {
                                if (!news.isNewsImportant()) newsList.add(news);
                                else newsList.add(0, news);
                            }
                        }
                    }

                    if (newsList.size() > 0) displayMessage(newsList, 0);
                    else {
                        type = "version";
                        NetworkHandler.query(Assistant.this, getString(R.string.version_url), "GET", DownloadData.this, null, null, null);
                    }
                } catch (Exception e) {
                    if (assistantSettings.getBoolean("debug_mode", false))
                        Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(Assistant.this, "news_check_error", Toast.LENGTH_SHORT).show();
                }
            } else if (type.equals("version")) {
                try {
                    int newestVersionCode = Integer.parseInt(result.split("\\|\\;\\|")[0]);
                    int installedVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;

                    if (newestVersionCode > installedVersionCode) {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                            progressDialog = null;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(Assistant.this);
                        builder.setCancelable(false);
                        builder.setTitle(Translations.getStringResource(Assistant.this, "update"));
                        builder.setMessage(Translations.getStringResource(Assistant.this, "update_message").replace("%%VERSION%%", result.split("\\|\\;\\|")[1]));
                        builder.setPositiveButton(Translations.getStringResource(Assistant.this, "ok"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                type = "language";
                                NetworkHandler.query(Assistant.this, Translations.getStringResource(Assistant.this, "language_url") + "language.xml", "GET", DownloadData.this, null, null, null);
                            }
                        });
                        builder.setNegativeButton(null, null);
                        builder.create().show();
                    } else {
                        type = "language";
                        NetworkHandler.query(Assistant.this, Translations.getStringResource(Assistant.this, "language_url") + "language.xml", "GET", DownloadData.this, null, null, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (type.equals("language")) {
                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(new StringReader(result));

                    int eventType = parser.next();
                    int currentLanguageVersion = Integer.parseInt(Translations.getStringResource(Assistant.this, "language_version"));
                    int newestLanguageVersion = currentLanguageVersion;

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && parser.getName().equals("string") && parser.getAttributeValue(null, "name").equals("language_version")) {
                            parser.next();
                            newestLanguageVersion = Integer.parseInt(parser.getText());
                        }

                        eventType = parser.next();
                    }

                    if (newestLanguageVersion > currentLanguageVersion) {
                        String url = Translations.getStringResource(Assistant.this, "language_url");
                        if (url.equals("language_url")) url = null;
                        new AsyncTask<String, Void, Exception>() {
                            private ProgressDialog progressDialog;

                            @Override
                            public void onPreExecute() {
                                progressDialog = ProgressDialog.show(Assistant.this, getString(R.string.app_name), Translations.getStringResource(Assistant.this, "updating_language"));
                            }

                            @Override
                            public Exception doInBackground(String... params) {
                                try {
                                    Translations.setLanguage(Assistant.this, params[0], params[1], true);
                                    return null;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return e;
                                }
                            }

                            @Override
                            public void onPostExecute(Exception result) {
                                if (progressDialog != null) {
                                    progressDialog.dismiss();
                                    progressDialog = null;
                                }

                                if (result == null) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(Assistant.this);
                                    builder.setTitle(Translations.getStringResource(Assistant.this, "language_updated_title"));
                                    builder.setMessage(Translations.getStringResource(Assistant.this, "language_updated_message"));
                                    builder.setPositiveButton(Translations.getStringResource(Assistant.this, "ok"), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Assistant.this.finishAffinity();
                                        }
                                    });
                                    builder.setCancelable(false);
                                    builder.create().show();
                                } else
                                    Toast.makeText(Assistant.this, Translations.getStringResource(Assistant.this, "language_update_error"), Toast.LENGTH_LONG).show();
                            }
                        }.execute(Translations.getLanguageSet(Assistant.this), url);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isBackgroundTaskRunning = false;
                }
            }, 1000);
        }

        @Override
        public void onException(Exception exception, int responseCode) {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }

            Log.d(getString(R.string.app_name), Integer.toString(responseCode));
            exception.printStackTrace();
            if (!type.equals("news") && !type.equals("version") && !type.equals("language"))
                speak(Translations.getAnswer(Assistant.this, "connection_error"));
            else {
                SharedPreferences read = getSharedPreferences("read_news", MODE_PRIVATE);
                read.edit().putLong("last_checked", System.currentTimeMillis()).apply();
            }

            isBackgroundTaskRunning = false;
        }

        private void displayMessage(final ArrayList<News> list, final int index) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Assistant.this);
            builder.setCancelable(false);
            builder.setTitle(Translations.getStringResource(Assistant.this, "news_number").replace("%%CURRENT%%", Integer.toString(index + 1)).replace("%%TOTAL%%", Integer.toString(list.size())) + (list.get(index).isNewsImportant() ? " " + Translations.getStringResource(Assistant.this, "news_important_tag") : ""));
            builder.setMessage(Translations.getStringResource(Assistant.this, "publication_date") + " " + list.get(index).getNewsDate() + "\n\n" + list.get(index).getNewsContent().replace("((NL))", "\n"));
            if (index < list.size() - 1)
                builder.setPositiveButton(Translations.getStringResource(Assistant.this, "ok"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSharedPreferences("read_news", MODE_PRIVATE).edit().putBoolean(list.get(index).getId(), true).apply();
                        displayMessage(list, index + 1);
                    }
                });
            else
                builder.setPositiveButton(Translations.getStringResource(Assistant.this, "ok"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSharedPreferences("read_news", MODE_PRIVATE).edit().putBoolean(list.get(index).getId(), true).apply();
                        type = "version";
                        NetworkHandler.query(Assistant.this, "http://polassis.pl/news_s/version.dat", "GET", DownloadData.this, null, null, null);
                    }
                });
            builder.setNegativeButton(null, null);
            builder.create().show();
        }
    }

    public int longestCommonSubsequenceLength(String a, String b) {
        int lengthA = a.length();
        int lengthB = b.length();

        int[][] matrix = new int[lengthA + 1][lengthB + 1];

        for (int i = 0; i <= lengthA; i++) {
            matrix[i][0] = 0;
        }

        for (int i = 0; i <= lengthB; i++) {
            matrix[0][i] = 0;
        }

        for (int i = 1; i <= lengthA; i++) {
            for (int j = 1; j <= lengthB; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) matrix[i][j] = matrix[i - 1][j - 1] + 1;
                else matrix[i][j] = Math.max(matrix[i - 1][j], matrix[i][j - 1]);
            }
        }

        int maximum = 0;

        for (int i = 0; i <= lengthA; i++) {
            for (int j = 0; j <= lengthB; j++) {
                if (matrix[i][j] > maximum) maximum = matrix[i][j];
            }
        }

        return maximum;
    }

    private class TurnOffAfterSpeaking extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... params) {
            while (isSpeaking()) {

            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    exitMode = true;
                    finish();
                }
            });
            return null;
        }
    }

    private Runnable startRecordingTask = new Runnable() {
        @Override
        public void run() {
            while ((isSpeaking() && !exitMode) || speechRecognitionService == null) {

            }
            if (!exitMode) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizedText.setText("");
                        speechRecognitionService.startListening(Assistant.this, recognitionStepsRunnable, microphoneButton);
                    }
                });
            }
            isGoingToBeRecording = false;
        }
    };

    private class Call extends AsyncTask<Void, Void, Void> {
        private String phoneNumber;

        public Call(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (isSpeaking()) {

            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent i = new Intent(Intent.ACTION_CALL);
                        i.setData(Uri.parse("tel:" + phoneNumber));
                        savedValues.edit().putBoolean("call_in_progress", true).commit();
                        isCalling = true;
                        startActivity(i);
                        if (assistantSettings.getBoolean("assistant_auto_turn_off", false))
                            finish();
                    } catch (SecurityException e) {
                        speak(Translations.getAnswer(Assistant.this, "permission_error"));
                    }
                }
            });
            return null;
        }
    }

    private class Navigate extends AsyncTask<Void, Void, Void> {
        private String placeToNavigateTo;

        public Navigate(String placeToNavigateTo) {
            this.placeToNavigateTo = placeToNavigateTo;
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (isSpeaking()) {

            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + URLEncoder.encode(placeToNavigateTo, "UTF-8")));
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            return null;
        }
    }

    private class FindApplication extends AsyncTask<String, Void, ArrayList<String>> {
        private Timer waitingForAnswerTimer;

        @Override
        protected void onPreExecute() {
            isBackgroundTaskRunning = true;
            waitingForAnswerTimer = new Timer();
            waitingForAnswerTimer.schedule(new WaitingForAnswerTask(), 1000, 1000);
        }

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            while (appList == null) {

            }

            ArrayList<String> foundAppInfoList = new ArrayList<String>();

            String searchQuery = params[0];
            String alias = findAlias(searchQuery, true);

            if (alias != null) searchQuery = alias;

            int syntaxSimilarAppIndex = -1;

            for (int i = 0; i < appList.size(); i++) {
                if (searchQuery.toLowerCase().contains(getPackageManager().getApplicationLabel(appList.get(i)).toString().toLowerCase())) {
                    if (syntaxSimilarAppIndex != -1 || !contains(syntaxMap.get("runApplication"), getPackageManager().getApplicationLabel(appList.get(i)).toString())) {
                        foundAppInfoList.add((String) getPackageManager().getApplicationLabel(appList.get(i)));
                        foundAppInfoList.add(appList.get(i).packageName);
                        i = appList.size();
                    } else syntaxSimilarAppIndex = i;
                }
            }

            if (syntaxSimilarAppIndex != -1 && foundAppInfoList.size() == 0) {
                foundAppInfoList.add((String) getPackageManager().getApplicationLabel(appList.get(syntaxSimilarAppIndex)));
                foundAppInfoList.add(appList.get(syntaxSimilarAppIndex).packageName);
            }

            return foundAppInfoList;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            waitingForAnswerTimer.cancel();
            if (result.size() == 0) {
                speak(Translations.getStringResource(Assistant.this, "no_app_found"));
                if (conversationMode) {
                    noNextRecognitionAttempt = false;
                } else {
                    microphoneButton.changeState(MicrophoneButton.State.READY);
                }
            } else {
                if (getPackageManager().getLaunchIntentForPackage(result.get(1)) != null) {
                    speak(Translations.getAnswer(Assistant.this, "running_app").replace("%%APP%%", result.get(0)));
                    new ExecuteIntent(getPackageManager().getLaunchIntentForPackage(result.get(1))).execute();
                } else {
                    speak(Translations.getAnswer(Assistant.this, "running_app_error").replace("%%APP%%", result.get(0)));
                    if (conversationMode) {
                        noNextRecognitionAttempt = false;
                    } else {
                        microphoneButton.changeState(MicrophoneButton.State.READY);
                    }
                }
            }

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isBackgroundTaskRunning = false;
                }
            }, 1000);
        }
    }

    private class ExecuteIntent extends AsyncTask<Void, Void, Void> {
        private Intent intentToBeExecuted;

        public ExecuteIntent(Intent intentToBeExecuted) {
            this.intentToBeExecuted = intentToBeExecuted;
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (isSpeaking()) {

            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    microphoneButton.changeState(MicrophoneButton.State.READY);

                    exitMode = true;
                    startActivity(intentToBeExecuted);
                    if (assistantSettings.getBoolean("assistant_auto_turn_off", false))
                        finish();
                }
            });
            return null;
        }
    }

    private TextToSpeechService textToSpeechService;

    private ServiceConnection textToSpeechServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TextToSpeechService.TextToSpeechBinder textToSpeechBinder = (TextToSpeechService.TextToSpeechBinder) iBinder;
            textToSpeechService = textToSpeechBinder.getService();
            textToSpeechService.setSpeechRate(textToSpeechSpeed);

            if (textToBeSaid != null)
            {
                textToSpeechService.speak(textToBeSaid);
                textToBeSaid = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            textToSpeechService = null;
        }
    };

    private SpeechRecognitionService speechRecognitionService;

    private ServiceConnection speechRecognitionServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SpeechRecognitionService.SpeechRecognitionBinder binder = (SpeechRecognitionService.SpeechRecognitionBinder) iBinder;
            speechRecognitionService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            speechRecognitionService = null;
        }
    };

    public void prepareLanguage() {
        Intent intent = new Intent(this, LanguageSelectionActivity.class);
        startActivity(intent);
        finish();
    }

    public void prepareGUI() {
        findViewById(R.id.microphoneButton).setContentDescription(Translations.getStringResource(Assistant.this, "start_recognition"));
        findViewById(R.id.settingsButton).setContentDescription(Translations.getStringResource(Assistant.this, "settings"));
        ((TextView) findViewById(R.id.notSpeakingAnymore)).setText(Translations.getStringResource(Assistant.this, "not_speaking_anymore"));
        ((TextView) findViewById(R.id.problem)).setText(Translations.getStringResource(Assistant.this, "problem"));
        findViewById(R.id.problem).setContentDescription(Translations.getStringResource(Assistant.this, "problem_button"));
        ((EditText) findViewById(R.id.commandEditText)).setHint(Translations.getStringResource(Assistant.this, "write_command"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inForeground = true;
        if (!Translations.isLanguageSet(this)) prepareLanguage();
        else {
            setContentView(R.layout.activity_main);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

            View mainLayout = findViewById(R.id.mainLayout);
            layoutWidth = mainLayout.getWidth();
            layoutHeight = mainLayout.getHeight();

            recognizedText = (TextView) findViewById(R.id.recognisedText);
            assistantAnswer = (TextView) findViewById(R.id.assistantAnswer);
            microphoneButton = new MicrophoneButton((ImageButton) findViewById(R.id.microphoneButton));

            powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "WakeUp");

            assistantSettings = PreferenceManager.getDefaultSharedPreferences(Assistant.this);
            customCommands = getSharedPreferences("commands", MODE_PRIVATE);

            commandEditText = (EditText) findViewById(R.id.commandEditText);
            prepareGUI();

            if (assistantSettings.getBoolean("text_to_speech", true)) {
                isTextToSpeechEnabled = true;
                startService(new Intent(Assistant.this, TextToSpeechService.class));
                bindService(new Intent(Assistant.this, TextToSpeechService.class), textToSpeechServiceConnection, BIND_AUTO_CREATE);
            }

            startService(new Intent(Assistant.this, SpeechRecognitionService.class));
            bindService(new Intent(Assistant.this, SpeechRecognitionService.class), speechRecognitionServiceConnection, BIND_AUTO_CREATE);

            savedValues = getPreferences(Context.MODE_PRIVATE);

            if (savedValues.getBoolean("first_run", true)) {
                //TODO: Tutorial when Polassis is run for the first time
                savedValues.edit().putBoolean("first_run", false).apply();
            }

            conversationMode = assistantSettings.getBoolean("conversation_mode", false);

            prepareSyntaxMap();
        }
    }

    public void prepareSyntaxMap() {
        try {
            syntaxMap = new HashMap<>();

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser syntaxParser = factory.newPullParser();

            syntaxParser.setInput(new FileReader(new File(getFilesDir(), "lang/" + Translations.getLanguageSet(this) + "/syntax.xml")));

            int eventType = syntaxParser.next();

            String identifier = "";
            HashSet<String> words = new HashSet<>();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    switch (syntaxParser.getName()) {
                        case "Identifier":
                            if (!identifier.equals("")) {
                                syntaxMap.put(identifier, new ArrayList<>(words));
                                words = new HashSet<>();
                            }
                            syntaxParser.next();
                            identifier = syntaxParser.getText();
                            break;

                        case "Part":
                            syntaxParser.next();
                            words.add("__p__" + syntaxParser.getText());
                            break;

                        case "Word":
                            syntaxParser.next();
                            words.add("__w__" + syntaxParser.getText());
                            break;

                        case "Example":
                            syntaxParser.next();
                            String[] bagOfWords = syntaxParser.getText().split(" ");
                            for (int i = 0; i < bagOfWords.length; i++) {
                                bagOfWords[i] = "__w__" + bagOfWords[i];
                            }
                            Collections.addAll(words, bagOfWords);
                    }
                }

                eventType = syntaxParser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String originalRecognisedText;
    private String processedRecognisedText;

    public void afterRecognition(String result) {
        final String recognitionResult = Numbers.processText(Assistant.this, result);

        originalRecognisedText = recognitionResult;
        processedRecognisedText = processText(recognitionResult);

        recognizedText.setText(processedRecognisedText);

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                processCommand(processedRecognisedText);
            }
        };

        if (isTextToSpeechEnabled && assistantSettings.getBoolean("read_recognised_phrase", false)) {
            float speechRate = Float.parseFloat(assistantSettings.getString("reading_recognised_phrase_speed", "1.0"));
            textToSpeechService.setSpeechRate(speechRate);

            speak(recognitionResult, false);

            Runnable afterReadingRunnable = new Runnable() {
                @Override
                public void run() {
                    while (isSpeaking()) {

                    }
                    textToSpeechService.setSpeechRate(textToSpeechSpeed);
                    handler.post(runnable);
                }
            };
            new Thread(afterReadingRunnable).start();
        } else runnable.run();

        microphoneButton.changeState(MicrophoneButton.State.READY);
    }

    public String getStringFromBundle(Bundle bundle, String key, String defaultValue) {
        String value = bundle.getString(key);
        if (value == null) return defaultValue;
        else return value;
    }

    public String extractName(String functionCalling, String command, boolean startFromBeginning) {
        ArrayList<String> syntaxWords = syntaxMap.get(functionCalling);
        String[] commandWords = command.split(" ");
        ArrayList<String> extractedWords = new ArrayList<>();

        if (startFromBeginning) {
            for (int i = 0; i < commandWords.length; i++) {
                if (!contains(syntaxWords, commandWords[i])) extractedWords.add(commandWords[i]);
                else if (!functionCalling.equals("playMusic") || extractedWords.size() > 0)
                    i = commandWords.length;
            }

            String extractedName = "";
            for (int i = 0; i < extractedWords.size(); i++) {
                extractedName += extractedWords.get(i);
                if (i < extractedWords.size() - 1) extractedName += " ";
            }

            return extractedName;
        } else {
            for (int i = commandWords.length - 1; i >= 0; i--) {
                if (!contains(syntaxWords, commandWords[i])) extractedWords.add(commandWords[i]);
                else if (!functionCalling.equals("playMusic") || extractedWords.size() > 0) i = 0;
            }

            String extractedName = "";
            for (int i = extractedWords.size() - 1; i >= 0; i--) {
                extractedName += extractedWords.get(i);
                if (i > 0) extractedName += " ";
            }

            return extractedName;
        }
    }

    private void turnScreenOn() {
        if ((android.os.Build.VERSION.SDK_INT < 20 && !powerManager.isScreenOn()) || (android.os.Build.VERSION.SDK_INT >= 20 && !powerManager.isInteractive())) {
            wakeLock.acquire();
            wakeLock.release();
        }
    }

    private final Runnable taskToDo = new Runnable() {
        @Override
        public void run() {
            Bundle bundle = (getIntent() != null ? getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle() : new Bundle());

            if (bundle != null) {
                if (bundle.containsKey("message") && !messageProcessed) {
                    turnScreenOn();

                    messageProcessed = true;
                    messageSender = bundle.getStringArray("message")[0];
                    messageContent = bundle.getStringArray("message")[1];
                    messageSenderNumber = bundle.getStringArray("message")[2];

                    speak(Translations.getAnswer(Assistant.this, "new_message_received").replace("%%SENDER%%", messageSender));

                    currentCategory = AssistantCategory.READ_SMS_NEW_NOTIFICATION;
                    startRecording();
                } else if (!bundle.containsKey("message") && !messageProcessed && (bundle.getBoolean("activation", false) || bundle.getBoolean("activation_via_bluetooth", false))) {
                    if (bundle.getBoolean("activation", false)) {
                        turnScreenOn();
                    }

                    startRecording();

                } else if (!bundle.containsKey("message") && !messageProcessed && !getStringFromBundle(bundle, "command", "").equals("")) {
                    if (bundle.getBoolean("show_command", false))
                        recognizedText.setText(getStringFromBundle(bundle, "command", ""));
                    if (!bundle.getBoolean("ask_for_confirmation", false)) {
                        processCommand(getStringFromBundle(bundle, "command", ""));

                    } else {
                        currentCategory = AssistantCategory.ACTIVATED_COMMAND_CONFIRMATION;
                        intendedCommand = getStringFromBundle(bundle, "command", "");

                        speak(Translations.getAnswer(Assistant.this, "command_confirmation").replace("%%COMMAND%%", intendedCommand));
                        startRecording();
                    }
                } else {
                    microphoneButton.changeState(MicrophoneButton.State.READY);
                }
            } else {
                microphoneButton.changeState(MicrophoneButton.State.READY);
            }
        }
    };

    public void turnOffAssistant(String text, int category) {
        exitMode = true;
        finish();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case R.id.assistantSettings:
                try {
                    exitMode = true;
                    startActivity(new Intent(Assistant.this, SettingsActivity.class));
                } catch (Exception e) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(Assistant.this);
                    builder.setTitle(Translations.getStringResource(Assistant.this, "settings_screen_error_title"));
                    builder.setMessage(Translations.getStringResource(Assistant.this, "settings_screen_error_message").replace("%%ERROR%%", e.toString()));
                    builder.setPositiveButton(Translations.getStringResource(Assistant.this, "ok"), null);
                    builder.setNegativeButton(null, null);
                    builder.create().show();
                }
                break;

            case R.id.possibilities:
                intent = new Intent(Assistant.this, PossibilitiesActivity.class);
                exitMode = true;
                startActivity(intent);
                break;

            case R.id.about:
                intent = new Intent(Assistant.this, AboutActivity.class);
                exitMode = true;
                startActivity(intent);
                break;
        }
        return true;
    }

    private boolean backgroundSetUp = false;

    @Override
    protected void onResume() {
        super.onResume();

        if (!exitMode && !isSpeaking()) {
            inForeground = true;

            if (!BluetoothAndSMSListenerService.isWorking)
                startService(new Intent(Assistant.this, BluetoothAndSMSListenerService.class));

            if (!assistantSettings.getBoolean("text_to_speech", true) && isTextToSpeechEnabled) {
                isTextToSpeechEnabled = false;
                unbindService(textToSpeechServiceConnection);
            } else if (!isTextToSpeechEnabled) {
                isTextToSpeechEnabled = true;
                bindService(new Intent(Assistant.this, TextToSpeechService.class), textToSpeechServiceConnection, BIND_AUTO_CREATE);
            }

            if (!BackgroundListenerService.isWorking && !isGoingToBeRecording && (speechRecognitionService == null || !speechRecognitionService.isWorking) && assistantSettings.getBoolean("activation", false)) {
                Intent intent = new Intent(Assistant.this, BackgroundListenerService.class);
                startService(intent);
            }

            if (assistantSettings.getBoolean("button_text_at_the_beginning", false))
                findViewById(R.id.problem).setContentDescription(Translations.getStringResource(Assistant.this, "problem_button_content_description_beginning"));
            else
                findViewById(R.id.problem).setContentDescription(Translations.getStringResource(Assistant.this, "problem_button_content_description_end"));

            if (assistantSettings.getBoolean("display_always_on", true))
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            textToSpeechSpeed = Float.parseFloat(assistantSettings.getString("text_to_speech_speed", "1.0"));
            if (isTextToSpeechEnabled && textToSpeechService != null)
                textToSpeechService.setSpeechRate(textToSpeechSpeed);

            Intent intent = getIntent();
            final Bundle bundle;

            if (intent != null)
                bundle = (intent.getExtras() == null ? new Bundle() : intent.getExtras());
            else bundle = new Bundle();

            if (intent != null) {
                if (Intent.ACTION_VOICE_COMMAND.equals(intent.getAction()) || bundle.getBoolean("activation", false) || bundle.containsKey("message") || !getStringFromBundle(bundle, "command", "").equals("")) {
                    turnScreenOn();
                    if (Intent.ACTION_VOICE_COMMAND.equals(intent.getAction()))
                        bundle.putBoolean("activation_via_bluetooth", true);
                } else {
                    //long updatesLastCheckedDate = getSharedPreferences("read_news", MODE_PRIVATE).getLong("last_checked", 0);
                    //if (System.currentTimeMillis() - updatesLastCheckedDate >= 10800000)
                    //    NetworkHandler.query(Assistant.this, getString(R.string.news_url), "GET", new DownloadData("news"), null, null, null);
                }
            } else {
                //long updatesLastCheckedDate = getSharedPreferences("read_news", MODE_PRIVATE).getLong("last_checked", 0);
                //if (System.currentTimeMillis() - updatesLastCheckedDate >= 10800000)
                //    NetworkHandler.query(Assistant.this, getString(R.string.news_url), "GET", new DownloadData("news"), null, null, null);
            }

            conversationMode = assistantSettings.getBoolean("conversation_mode", false);

            if (!assistantSettings.getBoolean("dark_colours", false)) {
                recognizedText.setShadowLayer(15, 4, 4, Color.BLACK);
                recognizedText.setTextColor(Color.WHITE);
                assistantAnswer.setShadowLayer(15, 4, 4, Color.BLACK);
                assistantAnswer.setTextColor(Color.WHITE);
                try {
                    Field field1 = View.class.getDeclaredField("mScrollCache");
                    field1.setAccessible(true);
                    Object cache = field1.get(findViewById(R.id.scrollView));

                    Field field2 = cache.getClass().getDeclaredField("scrollBar");
                    field2.setAccessible(true);
                    Object bar = field2.get(cache);

                    Method m = bar.getClass().getDeclaredMethod("setVerticalThumbDrawable", Drawable.class);
                    m.setAccessible(true);

                    m.invoke(bar, getResources().getDrawable(R.drawable.scrollbar_light));
                } catch (Exception e) {
                    if (assistantSettings.getBoolean("debug_mode", false))
                        Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                }
                ((TextView) findViewById(R.id.problem)).setShadowLayer(15, 4, 4, Color.BLACK);
                ((TextView) findViewById(R.id.problem)).setTextColor(Color.WHITE);
                ((TextView) findViewById(R.id.notSpeakingAnymore)).setShadowLayer(15, 4, 4, Color.BLACK);
                ((TextView) findViewById(R.id.notSpeakingAnymore)).setTextColor(Color.YELLOW);
            } else {
                recognizedText.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                recognizedText.setTextColor(Color.BLACK);
                assistantAnswer.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                assistantAnswer.setTextColor(Color.BLACK);
                try {
                    Field field1 = View.class.getDeclaredField("mScrollCache");
                    field1.setAccessible(true);
                    Object cache = field1.get(findViewById(R.id.scrollView));

                    Field field2 = cache.getClass().getDeclaredField("scrollBar");
                    field2.setAccessible(true);
                    Object bar = field2.get(cache);

                    Method m = bar.getClass().getDeclaredMethod("setVerticalThumbDrawable", Drawable.class);
                    m.setAccessible(true);

                    m.invoke(bar, getResources().getDrawable(R.drawable.scrollbar_dark));
                } catch (Exception e) {
                    if (assistantSettings.getBoolean("debug_mode", false))
                        Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                }
                ((TextView) findViewById(R.id.problem)).setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                ((TextView) findViewById(R.id.problem)).setTextColor(Color.BLACK);
                ((TextView) findViewById(R.id.notSpeakingAnymore)).setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                ((TextView) findViewById(R.id.notSpeakingAnymore)).setTextColor(Color.DKGRAY);
            }

            ((TextView) findViewById(R.id.problem)).setPaintFlags(((TextView) findViewById(R.id.problem)).getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

                File background = new File(getFilesDir(), "background");
                if (background.exists() && (!backgroundSetUp || assistantSettings.getBoolean("background_changed", false))) {
                    Bitmap bitmap = BitmapFactory.decodeFile(background.getAbsolutePath());
                    Drawable converted = new BitmapDrawable(getResources(), bitmap);
                    findViewById(R.id.mainLayout).setBackground(converted);
                    backgroundSetUp = true;
                    assistantSettings.edit().remove("background_changed").apply();
                }
                else if (!background.exists()) findViewById(R.id.mainLayout).setBackgroundResource(R.drawable.background);

            if (assistantSettings.getBoolean("larger_buttons", false) && !biggerButtons) {
                biggerButtons = true;

                ImageButton settingsButton = ((ImageButton) findViewById(R.id.settingsButton));
                ImageButton microphoneButton = this.microphoneButton.getButton();

                settingsButton.setScaleType(ImageView.ScaleType.FIT_XY);
                microphoneButton.setScaleType(ImageView.ScaleType.FIT_XY);

                originalSettingsWidth = settingsButton.getWidth();
                originalSettingsHeight = settingsButton.getHeight();
                originalMicrophoneWidth = microphoneButton.getWidth();
                originalMicrophoneHeight = microphoneButton.getHeight();

                settingsButton.setMinimumWidth(originalSettingsWidth + originalSettingsWidth / 2);
                settingsButton.setMinimumHeight(originalSettingsHeight + originalSettingsHeight / 2);
                microphoneButton.setMinimumWidth(originalMicrophoneWidth + originalMicrophoneWidth / 2);
                microphoneButton.setMinimumHeight(originalMicrophoneHeight + originalMicrophoneHeight / 2);
            } else if (!assistantSettings.getBoolean("larger_buttons", false) && originalSettingsWidth != -1) {
                biggerButtons = false;

                ImageButton settingsButton = ((ImageButton) findViewById(R.id.settingsButton));
                ImageButton microphoneButton = this.microphoneButton.getButton();

                settingsButton.setScaleType(ImageView.ScaleType.FIT_XY);
                microphoneButton.setScaleType(ImageView.ScaleType.FIT_XY);

                settingsButton.setMinimumWidth(originalSettingsWidth);
                settingsButton.setMinimumHeight(originalSettingsHeight);
                microphoneButton.setMinimumWidth(originalMicrophoneWidth);
                microphoneButton.setMinimumHeight(originalMicrophoneHeight);

                settingsButton.setMaxWidth(originalSettingsWidth);
                settingsButton.setMaxHeight(originalSettingsHeight);
                microphoneButton.setMaxWidth(originalMicrophoneWidth);
                microphoneButton.setMaxHeight(originalMicrophoneHeight);
            }

            try {
                if (savedValues.getBoolean("note", false))
                    savedValues.edit().remove("note").apply();
            } catch (Exception e) {
                savedValues.edit().remove("note").apply();
            }

            try {
                if (savedValues.getBoolean("call_in_progress", false))
                    savedValues.edit().remove("call_in_progress").apply();
            } catch (Exception e) {
                savedValues.edit().remove("call_in_progress").apply();
            }

            if (isHeadsetConnected() && !isHeadSetLoading && !isHeadsetLoaded) {
                microphoneButton.changeState(MicrophoneButton.State.INACTIVE);
                turnOnHeadset(true);
            } else if (!isHeadSetLoading && !isHeadsetLoaded) {
                taskToDo.run();
            } else {
                microphoneButton.changeState(MicrophoneButton.State.READY);
            }
        }

        setIntent(new Intent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (inForeground) {
            final Bundle bundle = (intent.getExtras() != null ? intent.getExtras() : new Bundle());
            if (bundle.getBoolean("bluetooth_connected", false)) {
                microphoneButton.changeState(MicrophoneButton.State.INACTIVE);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        turnOnHeadset(false);
                    }
                }, 1000);
            } else if (bundle.getBoolean("bluetooth_disconnected", false)) turnOffHeadset();
            else {
                if (Intent.ACTION_VOICE_COMMAND.equals(getIntent().getAction()))
                    bundle.putBoolean("activation_via_bluetooth", true);

                if (isHeadsetConnected() && !isHeadsetLoaded) turnOnHeadset(true);
                else taskToDo.run();
            }
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        exitMode = true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exitMode = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exitMode && (speechRecognitionService == null || !speechRecognitionService.isWorking)) {
            setIsSpeaking(false);
            if (speechRecognitionService != null) speechRecognitionService.cancel();
            if (assistantSettings.getBoolean("activation", false) && !BackgroundListenerService.isWorking) {
                Intent intent = new Intent(Assistant.this, BackgroundListenerService.class);
                startService(intent);
            }
            exitMode = false;
            turnOffHeadset();
            handler.removeCallbacksAndMessages(null);
            findViewById(R.id.frameLayout).setVisibility(View.GONE);
            inForeground = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (exitMode && (speechRecognitionService == null || !speechRecognitionService.isWorking)) {
            /*
            if (speechRecognizer != null) {
                try {
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                }
                catch (Exception e) {
                    speechRecognizer = null;
                }
            }
            */
            isBackgroundTaskRunning = false;
            inForeground = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        turnOffHeadset();
        inForeground = false;
        if (textToSpeechService != null) unbindService(textToSpeechServiceConnection);
        if (speechRecognitionService != null) unbindService(speechRecognitionServiceConnection);
    }

    public void onCancelClick(View v) {
        findViewById(R.id.notSpeakingAnymore).setVisibility(View.INVISIBLE);
        if (speechRecognitionService != null) speechRecognitionService.stopListening();
    }

    public void onMicClick(View v) {
        textToSpeechService.cancelSpeaking();
        setIsSpeaking(false);

        currentCategory = AssistantCategory.READY;

        commandEditText.clearFocus();

        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(commandEditText.getWindowToken(), 0);

        if (commandEditText.getText().toString().equals("")) {
            if (speechRecognitionService == null || !speechRecognitionService.isWorking) {
                startRecording();
            } else {
                findViewById(R.id.notSpeakingAnymore).setVisibility(View.INVISIBLE);
                if (speechRecognitionService != null) speechRecognitionService.cancel();

                currentCategory = AssistantCategory.READY;
            }
        } else {
            afterRecognition(commandEditText.getText().toString());
            commandEditText.getText().clear();
        }
    }

    public void requestPermissions(String... permissions) {
        speak(Translations.getAnswer(Assistant.this, "permissions_required_message"));

        noNextRecognitionAttempt = true;
        ActivityCompat.requestPermissions(Assistant.this, permissions, 0);
    }

    public boolean checkPermissions(String functionIdentifier) {
        switch (functionIdentifier) {
            case "pauseResumeTimer":
                return true;

            case "removeAlarm":
                return true;

            case "timerRemaining":
                return true;

            case "playMusic":
                return true;

            case "speakPhrase":
                return true;

            case "readMessage":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS);
                    return false;
                }

            case "lastIncomingCall":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.READ_CALL_LOG);
                    return false;
                }

            case "lastOutcomingCall":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.READ_CALL_LOG);
                    return false;
                }

            case "lastCall":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.READ_CALL_LOG);
                    return false;
                }

            case "mail":
                return true;

            case "showAllNotes":
                return true;

            case "editNote":
                return true;

            case "deleteNote":
                return true;

            case "showNote":
                return true;

            case "createNote":
                return true;

            case "messageReadMode":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS);
                    return false;
                }

            case "sendMessage":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE);
                    return false;
                }

            case "takePhoto":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Assistant.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    return false;
                }

            case "weatherForecast":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
                    return false;
                }

            case "setTimer":
                return true;

            case "makeCall":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE);
                    return false;
                }

            case "doCalculations":
                return true;

            case "whichDayOfWeekItWas":
                return true;

            case "playerControl":
                return true;

            case "torch":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.CAMERA);
                    return false;
                }

            case "changeWifiBluetoothState":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN);
                    return false;
                }

            case "turnOffSilentMode":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.MODIFY_AUDIO_SETTINGS);
                    return false;
                }

            case "turnOnSilentMode":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.MODIFY_AUDIO_SETTINGS);
                    return false;
                }

            case "openWebpage":
                return true;

            case "runApplication":
                return true;

            case "navigate":
                return true;

            case "getLocation":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION);
                    return false;
                }

            case "showMap":
                return true;

            case "wikipedia":
                return true;

            case "searchOnInternet":
                return true;

            case "batteryLevel":
                return true;

            case "setAlarm":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.SET_ALARM) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.SET_ALARM);
                    return false;
                }

            case "defaultNumber":
                if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
                    return true;
                else {
                    requestPermissions(Manifest.permission.READ_CONTACTS);
                    return false;
                }

            case "currentHour":
                return true;

            case "currentDayOfWeek":
                return true;

            case "currentDate":
                return true;

            case "setReminder":
                return true;

            case "restart":
                return true;

            case "turnOff":
                return true;

            case "turnOffAssistant":
                return true;

            case "dictateToClipboard":
                return true;

            case "assistantPossibilities":
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == 10) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File file = new File(imagePath);
                Uri uri = Uri.fromFile(file);
                intent.setData(uri);
                this.sendBroadcast(intent);

                speak(Translations.getStringResource(Assistant.this, "picture_saved"));
            }
        }
    }

    public void onMenuClick(View v) {

        PopupMenu popup = new PopupMenu(Assistant.this, v);
        popup.setOnMenuItemClickListener(Assistant.this);
        popup.inflate(R.menu.menu_main);
        Menu menu = popup.getMenu();

        menu.findItem(R.id.assistantSettings).setTitle(Translations.getStringResource(Assistant.this, "settings"));
        menu.findItem(R.id.possibilities).setTitle(Translations.getStringResource(Assistant.this, "possibilities"));
        menu.findItem(R.id.about).setTitle(Translations.getStringResource(Assistant.this, "about"));
        popup.show();
    }

    public boolean isHeadsetConnected() {
        SharedPreferences shared = assistantSettings;
        return shared.getBoolean("bluetooth_is_connected", false) && shared.getBoolean("bluetooth_headset_support", false);
    }

    public void turnOnHeadset(final boolean doTheTask) {
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent inten) {
                int state = inten.getExtras().getInt(AudioManager.EXTRA_SCO_AUDIO_STATE);
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    isHeadsetLoaded = true;
                    isHeadSetLoading = false;

                    if (doTheTask) taskToDo.run();
                    else {
                        microphoneButton.changeState(MicrophoneButton.State.READY);

                    }

                    unregisterReceiver(this);
                }
            }
        }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (isHeadsetConnected() && audioManager.getMode() != AudioManager.MODE_IN_COMMUNICATION) {
            savedValues.edit().putInt("old_audio_mode", audioManager.getMode()).apply();
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            isHeadSetLoading = true;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isHeadSetLoading && !isHeadsetLoaded) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(Assistant.this);
                    builder.setCancelable(false);
                    builder.setTitle(Translations.getStringResource(Assistant.this, "bluetooth_headset_connection_error_title"));
                    builder.setMessage(Translations.getStringResource(Assistant.this, "bluetooth_headset_connection_error_message"));
                    builder.setPositiveButton(Translations.getStringResource(Assistant.this, "close"), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isHeadSetLoading = false;
                            exitMode = true;
                            finish();
                        }
                    });
                    builder.setNegativeButton(null, null);
                    builder.show();
                }
            }
        }, 10000);
    }

    public void turnOffHeadset() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
            if (savedValues.getInt("old_audio_mode", AudioManager.MODE_NORMAL) == AudioManager.MODE_IN_COMMUNICATION)
                audioManager.setMode(AudioManager.MODE_NORMAL);
            else
                audioManager.setMode(savedValues.getInt("old_audio_mode", AudioManager.MODE_NORMAL));
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
            isHeadsetLoaded = false;
        }
    }

    private int getIsSpeakingDelay(int howManyWords)
    {
        return (howManyWords == 1 ? 1000 : (howManyWords >= 2 && howManyWords <= 3 ? 1500 : 1500 + (howManyWords - 3) * 320));
    }

    private String textToBeSaid;

    public void speak(String text) {
        setIsSpeaking(true);
            assistantAnswer.setText(text);
            if (isTextToSpeechEnabled) {
                if (textToSpeechService != null) textToSpeechService.speak(text);
                else textToBeSaid = text;
            }
            else {
                int howManyWords = assistantAnswer.getText().toString().split(" ").length;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setIsSpeaking(false);
                    }
                }, getIsSpeakingDelay(howManyWords));
            }
    }

    public void speak(String text, boolean indicateInAnswer) {
        setIsSpeaking(true);
            if (indicateInAnswer) assistantAnswer.setText(text);
            if (isTextToSpeechEnabled) {
                if (textToSpeechService != null) textToSpeechService.speak(text);
                else textToBeSaid = text;
            }
            else {
                if (!indicateInAnswer) {
                    Toast.makeText(Assistant.this, text, Toast.LENGTH_LONG).show();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setIsSpeaking(false);
                        }
                    }, 3250);
                } else {
                    int howManyWords = assistantAnswer.getText().toString().split(" ").length;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setIsSpeaking(false);
                        }
                    }, getIsSpeakingDelay(howManyWords));
                }
            }
    }

    public void speak(String text, String toBeSpoken, String toBeWritten) {
        setIsSpeaking(true);
        assistantAnswer.setText(text.replace("%%CONTENT%%", toBeWritten));
        String txt = text.replace("%%CONTENT%%", toBeSpoken);
        if (isTextToSpeechEnabled) {
            if (textToSpeechService != null) textToSpeechService.speak(txt);
            else textToBeSaid = txt;
        }
        else {
            int howManyWords = assistantAnswer.getText().toString().split(" ").length;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setIsSpeaking(false);
                }
            }, getIsSpeakingDelay(howManyWords));
        }
    }

    public void showSource(int layoutId, boolean matchScreenHeight, final Object... data) {
        RelativeLayout sourceLayout = (RelativeLayout) findViewById(R.id.sourceLayout);
        sourceLayout.removeAllViews();

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) sourceLayout.getLayoutParams();
        if (matchScreenHeight) layoutParams.addRule(RelativeLayout.ABOVE, R.id.notSpeakingAnymore);
        else layoutParams.addRule(RelativeLayout.ABOVE, 0);
        sourceLayout.setLayoutParams(layoutParams);

        RelativeLayout addedLayout = (RelativeLayout) View.inflate(Assistant.this, layoutId, null);

        if (layoutId == R.layout.external_wikipedia) {
            TextView wikipediaText = (TextView) addedLayout.findViewById(R.id.wikipediaText);
            TextView readMoreLink = (TextView) addedLayout.findViewById(R.id.readMoreLink);

            wikipediaText.setText((String) data[0]);
            readMoreLink.setText(Translations.getStringResource(Assistant.this, "read_more"));
            readMoreLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) data[1]));
                    exitMode = true;
                    startActivity(intent);
                }
            });
        } else if (layoutId == R.layout.external_webpage) {
            WebView webView = (WebView) addedLayout.findViewById(R.id.webpageView);
            TextView openInBrowserLink = (TextView) addedLayout.findViewById(R.id.openInBrowserLink);

            webView.setWebViewClient(new WebViewClient());
            webView.getSettings().setJavaScriptEnabled(false);
            webView.loadUrl((String) data[0]);

            openInBrowserLink.setText(Translations.getStringResource(Assistant.this, "open_in_browser"));
            openInBrowserLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) data[0]));
                    exitMode = true;
                    startActivity(intent);
                }
            });
        } else if (layoutId == R.layout.internal_read_message) {
            TextView messageSender = (TextView) addedLayout.findViewById(R.id.messageSender);
            TextView messageContent = (TextView) addedLayout.findViewById(R.id.messageContent);

            ((TextView) addedLayout.findViewById(R.id.messageSenderTitle)).setText(Translations.getStringResource(Assistant.this, "message_sender"));
            ((TextView) addedLayout.findViewById(R.id.messageContentTitle)).setText(Translations.getStringResource(Assistant.this, "message_content"));

            messageSender.setText((String) data[0]);
            messageContent.setText((String) data[1]);
        } else if (layoutId == R.layout.internal_send_message) {
            TextView messageRecipient = (TextView) addedLayout.findViewById(R.id.messageRecipient);
            TextView messageContent = (TextView) addedLayout.findViewById(R.id.messageContent);

            ((TextView) addedLayout.findViewById(R.id.messageRecipientTitle)).setText(Translations.getStringResource(Assistant.this, "message_recipient"));
            ((TextView) addedLayout.findViewById(R.id.messageContentTitle)).setText(Translations.getStringResource(Assistant.this, "message_content"));

            messageRecipient.setText(data[0] + " (" + data[1] + ")");
            messageContent.setText((String) data[2]);
        } else if (layoutId == R.layout.internal_note) {
            TextView noteTitle = (TextView) addedLayout.findViewById(R.id.noteTitle);
            TextView noteContent = (TextView) addedLayout.findViewById(R.id.noteContent);

            ((TextView) addedLayout.findViewById(R.id.noteTitleLabel)).setText(Translations.getStringResource(Assistant.this, "note_title"));
            ((TextView) addedLayout.findViewById(R.id.noteContentLabel)).setText(Translations.getStringResource(Assistant.this, "message_content"));

            noteTitle.setText((String) data[0]);
            noteContent.setText((String) data[1]);
        } else if (layoutId == R.layout.internal_all_notes) {
            TextView noteList = (TextView) addedLayout.findViewById(R.id.noteList);
            String[] notes = (String[]) data[0];
            String text = "";
            for (int i = 0; i < notes.length; i++) {
                text += (i + 1) + ": " + notes[i];
                if (i < notes.length - 1) text += "\n";
            }

            noteList.setText(text);
        } else if (layoutId == R.layout.internal_reminder) {
            TextView reminderDate = (TextView) addedLayout.findViewById(R.id.reminderDate);
            TextView reminderTime = (TextView) addedLayout.findViewById(R.id.reminderTime);
            TextView reminderTitle = (TextView) addedLayout.findViewById(R.id.reminderTitle);

            ((TextView) addedLayout.findViewById(R.id.reminderDateTitle)).setText(Translations.getStringResource(Assistant.this, "reminder_date"));
            ((TextView) addedLayout.findViewById(R.id.reminderTimeTitle)).setText(Translations.getStringResource(Assistant.this, "reminder_time"));
            ((TextView) addedLayout.findViewById(R.id.reminderTitleLabel)).setText(Translations.getStringResource(Assistant.this, "note_title"));

            reminderDate.setText((String) data[1]);
            reminderTime.setText((String) data[2]);
            reminderTitle.setText((String) data[0]);
        }

        sourceLayout.addView(addedLayout);
        sourceLayout.setVisibility(View.VISIBLE);
    }

    public void setIsSpeaking(boolean newValue) {
        if (textToSpeechService == null) isSpeaking = newValue;
    }

    public boolean isSpeaking() {
        if (textToSpeechService != null)
            return textToSpeechService.isSpeaking || isBackgroundTaskRunning;
        else return isSpeaking || isBackgroundTaskRunning;
    }

    public void startRecording() {
        if (ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(Assistant.this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (!isGoingToBeRecording) {
                isGoingToBeRecording = true;
                microphoneButton.changeState(MicrophoneButton.State.INACTIVE);
                
                new Thread(startRecordingTask).start();
            }
        } else
            ActivityCompat.requestPermissions(Assistant.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE}, 1);
    }

    private String findAlias(String text, boolean isItApplication) {
        String prefix;
        if (isItApplication) prefix = "__*#ALIAS_A#*__ ";
        else prefix = "__*#ALIAS#*__ ";
        String found = null;
        text = text.toLowerCase();
        int length = 0;
        for (Map.Entry<String, ?> entry : assistantSettings.getAll().entrySet()) {
            if (entry.getKey().startsWith(prefix))
            {
                String toBeChecked = assistantSettings.getString(entry.getKey(), "").toLowerCase();
                if ((toBeChecked.split(" ").length == 1 && containsWord(text, toBeChecked)) || (toBeChecked.split(" ").length > 1 && text.contains(toBeChecked))) {
                    if (toBeChecked.length() > length) {
                        found = entry.getKey().replace(prefix, "");
                        length = toBeChecked.length();
                    }
                }
            } else if (entry.getKey().startsWith("__*#H_ALIAS#*__"))
            {
                Set<String> stringSet = assistantSettings.getStringSet(entry.getKey(), null);
                if (stringSet != null) {
                    for (String toBeChecked : stringSet) {
                        toBeChecked = toBeChecked.toLowerCase();
                        if ((toBeChecked.split(" ").length == 1 && containsWord(text, toBeChecked)) || (toBeChecked.split(" ").length > 1 && text.contains(toBeChecked))) {
                            if (toBeChecked.length() > length) {
                                found = entry.getKey().replace("__*#H_ALIAS#*__", "");
                                length = toBeChecked.length();
                            }
                        }
                    }
                }
            }
        }

        return found;
    }

    public double determineSimilarity(String functionCalling, String one, String two, boolean noExtracting) {
        if (!noExtracting) one = extractName(functionCalling, one, false);

        one = one.toLowerCase();
        two = two.toLowerCase();

        char[] array1 = one.toCharArray();
        char[] array2 = two.toCharArray();
        Arrays.sort(array1);
        Arrays.sort(array2);

        int j = 0;
        int letters = 0;

        for (int i = 0; i < array1.length; i++) {
            if (j >= array2.length) i = array1.length;
            else {
                if (array1[i] == array2[j]) {
                    letters += 1;
                    j++;
                } else if (array1[i] > array2[j]) {
                    i--;
                    j++;
                }
            }
        }

        return ((double) letters) / ((double) (one.length() > two.length() ? one.length() : two.length())) * 100.0;
    }

    private class ContactComparator implements Comparator<Pair<String, Double>> {
        @Override
        public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
            return -1 * o1.second.compareTo(o2.second);
        }
    }

    public ArrayList<String> returnPhoneNumbers(String name) {
        ArrayList<String> possibleNumbers = new ArrayList<>();
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, "LOWER(" + ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + ")" + " = LOWER('" + name + "')", null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            do {
                possibleNumbers.add(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
            }
            while (cursor.moveToNext());
            cursor.close();
        }

        return possibleNumbers;
    }

    private ArrayList<String> toBeReturnedByFindContact;
    private String searchedName;
    private ArrayList<String> possibleContacts;
    private ArrayList<String> possibleNumbers;

    public ArrayList<String> findContact(String functionCalling, String name, int category, boolean noExtracting) {
        if (category == 0) {
            toBeReturnedByFindContact = new ArrayList<>();
            toBeReturnedByFindContact.add(functionCalling);

            name = extractName(functionCalling, name, false);
            searchedName = name;

            boolean nameMustBeExact = false;
            String alias = findAlias(name, false);

            if (alias != null) {
                name = alias;
                nameMustBeExact = true;
            }

            if (containsDigitOnly(name.replace(Translations.getStringResource(Assistant.this, "number"), ""))) {
                String temporary_number = returnNumber(name);
                String number = "";

                for (int i = 0; i < temporary_number.length(); i++) {
                    if (i < temporary_number.length() - 1)
                        number += temporary_number.charAt(i) + "-";
                    else number += temporary_number.charAt(i);
                }

                toBeReturnedByFindContact.add(number);
                toBeReturnedByFindContact.add(temporary_number);
                return toBeReturnedByFindContact;
            } else {
                possibleContacts = new ArrayList<>();

                ArrayList<Pair<String, Double>> similarContacts = new ArrayList<>();

                Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, "length(" + ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + ") DESC, " + ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC");
                if (cursor != null) {
                    cursor.moveToFirst();
                    do {
                        String contactDisplayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
                        if (name.toLowerCase().equals(contactDisplayName.toLowerCase()))
                            possibleContacts.add(0, contactDisplayName);
                        else if (!nameMustBeExact) {
                            if (name.toLowerCase().contains(contactDisplayName.toLowerCase()))
                                possibleContacts.add(contactDisplayName);
                            else if (assistantSettings.getBoolean("similarity", true)) {
                                double similarity = determineSimilarity(functionCalling, name, contactDisplayName, noExtracting);
                                if (similarity > 0) {
                                    Pair<String, Double> pairToAdd = new Pair<>(contactDisplayName, similarity);
                                    similarContacts.add(pairToAdd);
                                }
                            }
                        }
                    }
                    while (cursor.moveToNext());
                    cursor.close();

                    Collections.sort(similarContacts, new ContactComparator());

                    int index = 0;

                    while (possibleContacts.size() < Integer.parseInt(assistantSettings.getString("number_of_similar_contacts_proposed", "3")) && index < similarContacts.size()) {
                        possibleContacts.add(similarContacts.get(index).first);
                        index += 1;
                    }

                    if (possibleContacts.size() == 0) return toBeReturnedByFindContact;
                    else if (possibleContacts.size() == 1) {
                        if (nameMustBeExact || possibleContacts.get(0).toLowerCase().equals(name)) {
                            if (savedValues.contains(possibleContacts.get(0)) && !functionCalling.equals("defaultNumber"))
                            {
                                toBeReturnedByFindContact.add(possibleContacts.get(0));
                                toBeReturnedByFindContact.add(savedValues.getString(possibleContacts.get(0), ""));
                                return toBeReturnedByFindContact;
                            } else if (savedValues.contains(possibleContacts.get(0)) && functionCalling.equals("defaultNumber"))
                            {
                                toBeReturnedByFindContact.add(possibleContacts.get(0));
                                return toBeReturnedByFindContact;
                            } else {
                                possibleNumbers = returnPhoneNumbers(name);

                                if (possibleNumbers.size() == 0) {
                                    toBeReturnedByFindContact.add(possibleContacts.get(0));
                                    return toBeReturnedByFindContact;
                                } else if (possibleNumbers.size() == 1) {
                                    toBeReturnedByFindContact.add(possibleContacts.get(0));
                                    toBeReturnedByFindContact.add(possibleNumbers.get(0));
                                    if (functionCalling.equals("defaultNumber"))
                                    {
                                        speak(Translations.getAnswer(Assistant.this, "contact_one_number_only"));
                                        return null;
                                    } else return toBeReturnedByFindContact;
                                } else {
                                    toBeReturnedByFindContact.add(possibleContacts.get(0));
                                    String toBeSaid = Translations.getAnswer(Assistant.this, "number_choice_question") + "\n";
                                    for (int i = 0; i < possibleNumbers.size(); i++) {
                                        toBeSaid += "(" + (i + 1) + ") " + processNumber(possibleNumbers.get(i));
                                        if (i < possibleNumbers.size() - 1) toBeSaid += ", ";
                                        else toBeSaid += ".";
                                    }
                                    speak(toBeSaid);
                                    currentCategory = AssistantCategory.CONTACT_NUMBER_CHOICE;
                                    startRecording();
                                    return null;
                                }
                            }
                        } else {
                            speak(Translations.getAnswer(Assistant.this, "contact_suggestion_question").replace("%%NAME%%", possibleContacts.get(0)));
                            currentCategory = AssistantCategory.CONTACT_ONE_SUGGESTION;
                            startRecording();
                            return null;
                        }
                    } else {
                        String toBeSaid = Translations.getAnswer(Assistant.this, "contact_suggestion_choice_question") + "\n";
                        int i;
                        for (i = 0; i < possibleContacts.size(); i++) {
                            toBeSaid += "(" + (i + 1) + ") " + possibleContacts.get(i) + ", ";
                        }

                        toBeSaid += "(" + (i + 1) + ") " + Translations.getAnswer(Assistant.this, "no_contact_at_all");

                        speak(toBeSaid);
                        currentCategory = AssistantCategory.CONTACT_SUGGESTION_CHOICE;
                        startRecording();
                        return null;
                    }
                } else return toBeReturnedByFindContact;
            }
        } else if (category == 1) {
            if (isAnswerYes(name)) {
                Set<String> stringSet = assistantSettings.getStringSet("__*#H_ALIAS#*__" + possibleContacts.get(0).toLowerCase(), new HashSet<String>());
                stringSet.add(searchedName);
                assistantSettings.edit().putStringSet("__*#H_ALIAS#*__" + possibleContacts.get(0).toLowerCase(), stringSet).apply();
                if (savedValues.contains(possibleContacts.get(0)) && !functionCalling.equals("defaultNumber"))
                    {
                        toBeReturnedByFindContact.add(possibleContacts.get(0));
                        toBeReturnedByFindContact.add(savedValues.getString(possibleContacts.get(0), ""));
                        return toBeReturnedByFindContact;
                    } else if (savedValues.contains(possibleContacts.get(0)) && functionCalling.equals("defaultNumber"))
                    {
                        toBeReturnedByFindContact.add(possibleContacts.get(0));
                        return toBeReturnedByFindContact;
                    } else {
                        possibleNumbers = returnPhoneNumbers(possibleContacts.get(0));
                        if (possibleNumbers.size() == 0) {
                            toBeReturnedByFindContact.add(possibleContacts.get(0));
                            return toBeReturnedByFindContact;
                        } else if (possibleNumbers.size() == 1) {
                            toBeReturnedByFindContact.add(possibleContacts.get(0));
                            toBeReturnedByFindContact.add(possibleNumbers.get(0));
                            if (functionCalling.equals("defaultNumber")) {
                                speak(Translations.getAnswer(Assistant.this, "contact_one_number_only"));
                                return null;
                            } else return toBeReturnedByFindContact;
                        } else {
                            toBeReturnedByFindContact.add(possibleContacts.get(0));
                            String toBeSaid = Translations.getAnswer(Assistant.this, "number_choice_question") + "\n";
                            int i;
                            for (i = 0; i < possibleNumbers.size(); i++) {
                                toBeSaid += "(" + (i + 1) + ") " + processNumber(possibleNumbers.get(i));
                                if (i < possibleNumbers.size() - 1) toBeSaid += ", ";
                                else toBeSaid += ".";
                            }

                            speak(toBeSaid);
                            currentCategory = AssistantCategory.CONTACT_NUMBER_CHOICE;
                            startRecording();
                            return null;
                        }
                    }
            } else if (isAnswerNo(name)) {
                speak(Translations.getAnswer(Assistant.this, "no_contact_found"));
                currentCategory = AssistantCategory.READY;
                return null;
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
                return null;
            }
        } else if (category == 2) {
            try {
                int index = Integer.parseInt(name) - 1;
                if (index == possibleContacts.size()) {
                    speak(Translations.getAnswer(Assistant.this, "no_contact_found"));
                    currentCategory = AssistantCategory.READY;
                    return null;
                } else {
                    Set<String> stringSet = assistantSettings.getStringSet("__*#H_ALIAS#*__" + possibleContacts.get(index).toLowerCase(), new HashSet<String>());
                    stringSet.add(searchedName);
                    assistantSettings.edit().putStringSet("__*#H_ALIAS#*__" + possibleContacts.get(index).toLowerCase(), stringSet).apply();
                    if (savedValues.contains(possibleContacts.get(index)) && !functionCalling.equals("defaultNumber"))
                    {
                        toBeReturnedByFindContact.add(possibleContacts.get(index));
                        toBeReturnedByFindContact.add(savedValues.getString(possibleContacts.get(index), ""));
                        return toBeReturnedByFindContact;
                    } else if (savedValues.contains(possibleContacts.get(index)) && functionCalling.equals("defaultNumber"))
                    {
                        toBeReturnedByFindContact.add(possibleContacts.get(index));
                        return toBeReturnedByFindContact;
                    } else {
                        possibleNumbers = returnPhoneNumbers(possibleContacts.get(index));
                        if (possibleNumbers.size() == 0) {
                            toBeReturnedByFindContact.add(possibleContacts.get(index));
                            return toBeReturnedByFindContact;
                        } else if (possibleNumbers.size() == 1) {
                            toBeReturnedByFindContact.add(possibleContacts.get(index));
                            toBeReturnedByFindContact.add(possibleNumbers.get(0));
                            if (functionCalling.equals("defaultNumber")) {
                                speak(Translations.getAnswer(Assistant.this, "contact_one_number_only"));
                                return null;
                            } else return toBeReturnedByFindContact;
                        } else {
                            toBeReturnedByFindContact.add(possibleContacts.get(index));
                            String toBeSaid = Translations.getAnswer(Assistant.this, "number_choice_question") + "\n";
                            for (int i = 0; i < possibleNumbers.size(); i++) {
                                toBeSaid += "(" + (i + 1) + ") " + processNumber(possibleNumbers.get(i));
                                if (i < possibleNumbers.size() - 1) toBeSaid += ", ";
                                else toBeSaid += ".";
                            }
                            speak(toBeSaid);
                            currentCategory = AssistantCategory.CONTACT_NUMBER_CHOICE;
                            startRecording();
                            return null;
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                speak(Translations.getAnswer(Assistant.this, "wrong_position"));
                startRecording();
                return null;
            } catch (NumberFormatException e) {
                speak(Translations.getAnswer(Assistant.this, "wrong_position_parse_error"));
                startRecording();
                return null;
            }
        } else if (category == 3) {
            try {
                int index = Integer.parseInt(name) - 1;
                toBeReturnedByFindContact.add(possibleNumbers.get(index));
                return toBeReturnedByFindContact;
            } catch (IndexOutOfBoundsException e) {
                speak(Translations.getAnswer(Assistant.this, "wrong_position"));
                startRecording();
                return null;
            } catch (NumberFormatException e) {
                speak(Translations.getAnswer(Assistant.this, "wrong_position_parse_error"));
                startRecording();
                return null;
            }
        } else return null;
    }

    public ArrayList<String> selectContact(ArrayList<String> contactsList, String contactName) {
        ArrayList<String> contact = new ArrayList<>();
        Set<String> numbers = new HashSet<>();
        boolean found = false;
        for (int i = 0; i < contactsList.size(); i++) {
            if (contactsList.get(i).toLowerCase().equals(contactName.toLowerCase())) {
                found = true;
                contact.add(contactsList.get(i));
            } else {
                boolean newNameFound = contactsList.get(i).matches("^.*\\p{L}+.*$");

                if (found) {
                    if (newNameFound) break;
                    else numbers.add(contactsList.get(i));
                }
            }
        }

        Iterator iterator = numbers.iterator();
        while (iterator.hasNext()) {
            contact.add((String) iterator.next());
        }

        return contact;
    }

    public String processText(String text) {
        String result = text;
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("dot", Translations.getStringResource(Assistant.this, "dot"))), ".");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("comma", Translations.getStringResource(Assistant.this, "comma"))), ",");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("question_mark", Translations.getStringResource(Assistant.this, "question_mark"))), "?");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("semicolon", Translations.getStringResource(Assistant.this, "semicolon"))), ";");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("exclamation_mark", Translations.getStringResource(Assistant.this, "exclamation_mark"))), "!");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("percent", Translations.getStringResource(Assistant.this, "percent"))), "%");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("bracket_beginning", Translations.getStringResource(Assistant.this, "bracket_start"))), "(");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("bracket_end", Translations.getStringResource(Assistant.this, "bracket_end"))), ")");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("space", Translations.getStringResource(Assistant.this, "space"))), " ");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("dash", Translations.getStringResource(Assistant.this, "dash"))), "-");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("colon", Translations.getStringResource(Assistant.this, "colon"))), ":");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("slash", Translations.getStringResource(Assistant.this, "slash"))), "/");
        result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("quotation_mark", Translations.getStringResource(Assistant.this, "quotation_mark"))), "\"");
        if (currentCategory == AssistantCategory.MAIL_RECIPIENT || currentCategory == AssistantCategory.MAIL_CORRECTION_RECIPIENT)
            result = result.replaceAll(Pattern.quote(" ") + "(?i)" + Pattern.quote(assistantSettings.getString("at", Translations.getStringResource(Assistant.this, "at_value"))) + " ", "@");
        else if (!this.assistantSettings.getBoolean("no_capitalization", false)) {
            result = result.substring(0, 1).toUpperCase() + result.substring(1);
            for (int i = 0; i < result.length(); i++) {
                if ((result.charAt(i) == '.' || result.charAt(i) == '!' || result.charAt(i) == '?') && i + 2 <= result.length() - 1) {
                    StringBuilder stringBuilder = new StringBuilder(result);
                    stringBuilder.setCharAt(i + 2, Character.toString(result.charAt(i + 2)).toUpperCase().charAt(0));
                    result = stringBuilder.toString();
                }
            }
        }
        return result;
    }



    public String returnNumber(String text) {
        String result = "";

        for (int i = 0; i < text.length(); i++) {
            if (Character.toString(text.charAt(i)).matches("\\d"))
                result += text.charAt(i);
        }

        return result;
    }

    public ArrayList<Integer> returnNumbers(String text) {
        ArrayList<Integer> result = new ArrayList<>();

        String number = "";

        for (int i = 0; i < text.length(); i++) {
            if (Character.toString(text.charAt(i)).matches("\\d"))
            {
                number += text.charAt(i);
            } else if (!number.equals("")) {
                result.add(Integer.parseInt(number));
                number = "";
            }

            if (i == text.length() - 1 && !number.equals("")) {
                result.add(Integer.parseInt(number));
                number = "";
            }
        }

        return result;
    }

    public ArrayList<Pair<Integer, Integer>> returnNumbers(String text, int start, int end) {
        ArrayList<Pair<Integer, Integer>> result = new ArrayList<>();

        String number = "";
        int index = 0;

        for (int i = start; i <= end; i++) {
            if (Character.toString(text.charAt(i)).matches("\\d"))
            {
                if (number.equals("")) index = i;
                number += text.charAt(i);
            } else if (!number.equals("")) {
                result.add(new Pair<>(Integer.parseInt(number), index));
                number = "";
            }

            if (i == text.length() - 1 && !number.equals("")) {
                result.add(new Pair<>(Integer.parseInt(number), index));
                number = "";
            }
        }

        return result;
    }

    public int convertToCalendarField(int type) {
        switch (type) {
            case 0:
                return Calendar.SECOND;

            case 1:
                return Calendar.MINUTE;

            case 2:
                return Calendar.HOUR_OF_DAY;

            case 3:
                return Calendar.DAY_OF_YEAR;

            case 4:
                return Calendar.WEEK_OF_YEAR;

            case 5:
                return Calendar.MONTH;

            default:
                return Calendar.YEAR;
        }
    }

    public int AMorPM(String text) {
        String[] am = Translations.getStringResource(Assistant.this, "am").split("\\|");
        String[] pm = Translations.getStringResource(Assistant.this, "pm").split("\\|");

        for (int i = 0; i < am.length; i++) {
            if (!am[i].equals("")) {
                String[] words = am[i].split("\\|");
                for (int j = 0; j < words.length; j++) {
                    if ((words[j].contains(" ") && text.contains(words[j])) || containsWord(text, words[j]))
                        return 0;
                }
            }
        }

        for (int i = 0; i < pm.length; i++) {
            if (!pm[i].equals("")) {
                String[] words = pm[i].split("\\|");
                for (int j = 0; j < words.length; j++) {
                    if ((words[j].contains(" ") && text.contains(words[j])) || containsWord(text, words[j]))
                        return 1;
                }
            }
        }

        if ((am.length == 0 && pm.length == 0) || (am.length == 1 && am[0].equals("") && pm.length == 1 && pm[0].equals("")))
            return -1;
        else if (am.length == 0 || (am.length == 1 && am[0].equals(""))) return 0;
        else return 1;
    }

    public int indexOfWord(String text, String word) {
        String[] words = text.split(" ");
        int index = 0;
        for (int i = 0; i < words.length; i++) {
            if (words[i].equalsIgnoreCase(word)) return index;
            index += words[i].length() + 1;
        }
        return -1;
    }

    public Calendar processTime(String text, boolean timeAddedAllowedOnly) {
        try {
            text = text.toLowerCase();

            boolean timeAdded = false;

            String[] units = Translations.getStringArrayResource(Assistant.this, "time_units");
            for (int i = 0; i < units.length && !timeAdded; i++) {
                String[] words = units[i].split("\\|");
                for (int j = 0; j < words.length && !timeAdded; j++) {
                    if (text.contains(words[j].toLowerCase())) timeAdded = true;
                }
            }

            Calendar calendar = new GregorianCalendar();

            if (timeAdded) {
                ArrayList<Integer> occupiedIndices = new ArrayList<>();
                for (int i = 0; i < units.length; i++) {
                    String[] words = units[i].split("\\|");
                    for (int j = 0; j < words.length; j++) {
                        int index;
                        if (words[j].contains(" ")) index = text.indexOf(words[j]);
                        else index = indexOfWord(text, words[j]);
                        if (index != -1) {
                            ArrayList<Pair<Integer, Integer>> numbersFromBeginning = returnNumbers(text, 0, index);
                            ArrayList<Pair<Integer, Integer>> numbersToEnd = returnNumbers(text, index, text.length() - 1);

                            int beginning = -1;
                            int beginningIndex = -1;
                            if (numbersFromBeginning.size() > 0) {
                                beginning = numbersFromBeginning.get(numbersFromBeginning.size() - 1).first;
                                beginningIndex = numbersFromBeginning.get(numbersFromBeginning.size() - 1).second;
                            }
                            int end = -1;
                            int endIndex = -1;
                            if (numbersToEnd.size() > 0) {
                                end = numbersToEnd.get(0).first;
                                endIndex = numbersToEnd.get(0).second;
                            }

                            if (beginning != -1 && !occupiedIndices.contains(beginningIndex)) {
                                calendar.add(convertToCalendarField(i), beginning);
                                occupiedIndices.add(beginningIndex);
                            } else if (end != -1 && !occupiedIndices.contains(endIndex)) {
                                calendar.add(convertToCalendarField(i), end);
                                occupiedIndices.add(endIndex);
                            } else if (beginning == -1 && end == -1)
                                calendar.add(convertToCalendarField(i), 1);
                        }
                    }
                }
                return calendar;
            } else if (!timeAddedAllowedOnly) {
                ArrayList<Integer> numbers = returnNumbers(text);

                if (numbers.size() == 0) return null;
                else if (numbers.size() == 1) {
                    String numberString = Integer.toString(numbers.get(0));
                    if (numberString.length() == 1 || numberString.length() == 2) {
                        int number = numbers.get(0);
                        int hours = number;
                        int minutes = 0;

                        if (number > 24) {
                            hours = number / 10;
                            minutes = number % 10;
                        }

                        calendar.set(Calendar.MINUTE, minutes);
                        calendar.set(Calendar.SECOND, 0);

                        int part = AMorPM(text);
                        if (part == -1) calendar.set(Calendar.HOUR_OF_DAY, hours);
                        else if (part == 0) {
                            calendar.set(Calendar.HOUR, hours);
                            calendar.set(Calendar.AM_PM, Calendar.AM);
                        } else {
                            calendar.set(Calendar.HOUR, hours);
                            calendar.set(Calendar.AM_PM, Calendar.PM);
                        }
                    } else if (numberString.length() == 3) {
                        int number = Integer.parseInt(Character.toString(numberString.charAt(0)) + numberString.charAt(1));
                        int hours = number;
                        int minutes = Integer.parseInt(Character.toString(numberString.charAt(2)));

                        if (number > 24) {
                            hours = number / 10;
                            minutes = Integer.parseInt(Character.toString(numberString.charAt(1)) + numberString.charAt(2));
                        }

                        calendar.set(Calendar.MINUTE, minutes);
                        calendar.set(Calendar.SECOND, 0);

                        int part = AMorPM(text);
                        if (part == -1) calendar.set(Calendar.HOUR_OF_DAY, hours);
                        else if (part == 0) {
                            calendar.set(Calendar.HOUR, hours);
                            calendar.set(Calendar.AM_PM, Calendar.AM);
                        } else {
                            calendar.set(Calendar.HOUR, hours);
                            calendar.set(Calendar.AM_PM, Calendar.PM);
                        }
                    } else if (numberString.length() == 4) {
                        int hours = Integer.parseInt(Character.toString(numberString.charAt(0)) + numberString.charAt(1));
                        int minutes = Integer.parseInt(Character.toString(numberString.charAt(2)) + numberString.charAt(3));

                        calendar.set(Calendar.MINUTE, minutes);
                        calendar.set(Calendar.SECOND, 0);

                        int part = AMorPM(text);
                        if (part == -1) calendar.set(Calendar.HOUR_OF_DAY, hours);
                        else if (part == 0) {
                            calendar.set(Calendar.HOUR, hours);
                            calendar.set(Calendar.AM_PM, Calendar.AM);
                        } else {
                            calendar.set(Calendar.HOUR, hours);
                            calendar.set(Calendar.AM_PM, Calendar.PM);
                        }
                    }
                } else if (numbers.size() == 2) {
                    int hours = numbers.get(0);
                    int minutes = numbers.get(1);

                    calendar.set(Calendar.MINUTE, minutes);
                    calendar.set(Calendar.SECOND, 0);

                    int part = AMorPM(text);
                    if (part == -1) calendar.set(Calendar.HOUR_OF_DAY, hours);
                    else if (part == 0) {
                        calendar.set(Calendar.HOUR, hours);
                        calendar.set(Calendar.AM_PM, Calendar.AM);
                    } else {
                        calendar.set(Calendar.HOUR, hours);
                        calendar.set(Calendar.AM_PM, Calendar.PM);
                    }
                } else {
                    int hours = numbers.get(0);
                    int minutes = 0;

                    for (int i = 1; i < numbers.size(); i++) {
                        minutes += numbers.get(i);
                    }

                    if (minutes >= 60) {
                        hours = numbers.get(0) + numbers.get(1) / 10;
                        minutes = 0;
                        for (int i = 2; i < numbers.size(); i++) {
                            minutes += numbers.get(i);
                        }
                    }

                    calendar.set(Calendar.MINUTE, minutes);
                    calendar.set(Calendar.SECOND, 0);

                    int part = AMorPM(text);
                    if (part == -1) calendar.set(Calendar.HOUR_OF_DAY, hours);
                    else if (part == 0) {
                        calendar.set(Calendar.HOUR, hours);
                        calendar.set(Calendar.AM_PM, Calendar.AM);
                    } else {
                        calendar.set(Calendar.HOUR, hours);
                        calendar.set(Calendar.AM_PM, Calendar.PM);
                    }
                }

                return calendar;
            } else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private class Function {
        private String identifier;
        private int category;

        public Function(String identifier, int category) {
            this.identifier = identifier;
            this.category = category;
        }

        public String getIdentifier() {
            return this.identifier;
        }

        public int getCategory() {
            return this.category;
        }
    }

    public Function findFunction(String command) throws Exception {
        String commandLowerCase = command.toLowerCase();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xmlParser = factory.newPullParser();

        xmlParser.setInput(new FileReader(new File(getFilesDir(), "lang/" + Translations.getLanguageSet(this) + "/syntax.xml")));

        String identifier = null;
        int category = -1;

        int eventType = xmlParser.next();
        boolean success = false;
        boolean beginning = false;

        ArrayList<Boolean> modes = new ArrayList<>();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && xmlParser.getName().equals("Function")) {
                if (!success) {
                    modes.clear();
                    beginning = true;
                } else eventType = XmlPullParser.END_DOCUMENT;
            } else if (eventType == XmlPullParser.START_TAG) {
                if (!beginning && modes.size() > 0 && !modes.get(modes.size() - 1) && success) {
                    int newEitherBlocks = 1;
                    do {
                        eventType = xmlParser.next();
                        if (eventType == XmlPullParser.START_TAG && xmlParser.getName().equals("Either"))
                            newEitherBlocks += 1;
                        else if (eventType == XmlPullParser.END_TAG && xmlParser.getName().equals("Either"))
                            newEitherBlocks -= 1;
                    }
                    while (newEitherBlocks > 0);

                    modes.remove(modes.size() - 1);
                } else if (!beginning && modes.size() > 0 && modes.get(modes.size() - 1) && !success) {
                    int newAllCompulsoryBlocks = 1;
                    do {
                        eventType = xmlParser.next();
                        if (eventType == XmlPullParser.START_TAG && xmlParser.getName().equals("AllCompulsory"))
                            newAllCompulsoryBlocks += 1;
                        else if (eventType == XmlPullParser.END_TAG && xmlParser.getName().equals("AllCompulsory"))
                            newAllCompulsoryBlocks -= 1;
                    }
                    while (newAllCompulsoryBlocks > 0);

                    modes.remove(modes.size() - 1);
                } else {
                    switch (xmlParser.getName()) {
                        case "Identifier":
                            xmlParser.next();
                            identifier = xmlParser.getText();
                            break;

                        case "Category":
                            xmlParser.next();
                            category = Integer.parseInt(xmlParser.getText());
                            break;

                        case "AllCompulsory":
                            modes.add(true);
                            beginning = true;
                            break;

                        case "Either":
                            modes.add(false);
                            beginning = true;
                            break;

                        case "Part":
                            xmlParser.next();
                            success = commandLowerCase.contains(xmlParser.getText());
                            beginning = false;
                            break;

                        case "Word":
                            xmlParser.next();
                            success = containsWord(commandLowerCase, xmlParser.getText());
                            beginning = false;
                            break;

                        case "MathematicalSymbol":
                            success = containsMathematicalSymbol(commandLowerCase);
                            beginning = false;
                            break;

                        case "Digit":
                            success = containsDigit(commandLowerCase);
                            beginning = false;
                            break;

                        case "URL":
                            success = containsURL(commandLowerCase);
                            beginning = false;
                            break;

                        case "Prohibited":
                            xmlParser.next();
                            success = !commandLowerCase.contains(xmlParser.getText());
                            beginning = false;
                            break;
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (modes.size() > 0 && (xmlParser.getName().equals("AllCompulsory") || xmlParser.getName().equals("Either")))
                    modes.remove(modes.size() - 1);
            }

            if (eventType != XmlPullParser.END_DOCUMENT)
                eventType = xmlParser.next();
        }

        if (success) return new Function(identifier, category);
        else return null;
    }

    private String intendedCommand;

    public void processCommand(String command) {
        if (!isSpeaking()) {
            findViewById(R.id.sourceLayout).setVisibility(View.GONE);
            String commandLowerCase = command.toLowerCase();

            if (currentCategory == AssistantCategory.READY) {
                try {
                    if (customCommands.contains(commandLowerCase)) {
                        processCommand(customCommands.getString(commandLowerCase, "").split("\\|\\|")[1]);
                    } else {
                        Function function = findFunction(command);

                        if (function != null) {
                            String identifier = function.getIdentifier();
                            int category = function.getCategory();
                            if (checkPermissions(identifier)) {
                                try {
                                    getClass().getMethod(identifier, String.class, int.class).invoke(Assistant.this, commandLowerCase, category);
                                } catch (Exception e) {
                                    try {
                                        getClass().getMethod(identifier, String.class, int.class, ArrayList.class).invoke(Assistant.this, commandLowerCase, category, findContact(identifier, commandLowerCase, 0, false));
                                    } catch (Exception ex) {
                                        e.printStackTrace();
                                        ex.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            speak(answerNoUnderstanding(false));
                        }
                    }
                } catch (Exception e) {
                    speak(Translations.getStringResource(Assistant.this, "error"));
                    e.printStackTrace();
                }
            } else if (currentCategory == AssistantCategory.SMS_RECIPIENT)
                sendMessage(command, 1, findContact("sendMessage", command, 0, true));
            else if (currentCategory == AssistantCategory.SMS_CONTENT)
                sendMessage(command, 2, null);
            else if (currentCategory == AssistantCategory.SMS_CONFIRMATION)
                sendMessage(command, 3, null);
            else if (currentCategory == AssistantCategory.SMS_CORRECTION_QUESTION)
                sendMessage(command, 4, null);
            else if (currentCategory == AssistantCategory.CALL_RECIPIENT)
                makeCall(command, 1, findContact("makeCall", command, 0, true));
            else if (currentCategory == AssistantCategory.CALL_NUMBER_CHOICE)
                makeCall(command, 2, null);
            else if (currentCategory == AssistantCategory.NAVIGATE_PLACE) navigate(command, 1);
            else if (currentCategory == AssistantCategory.POSSIBILITIES_QUESTION) {
                currentCategory = AssistantCategory.READY;
                if (isAnswerYes(command)) {
                    noNextRecognitionAttempt = true;
                    speak(Translations.getAnswer(Assistant.this, "fine"));
                    Intent i = new Intent(Assistant.this, PossibilitiesActivity.class).putExtra("possibilities", true);
                    new ExecuteIntent(i).execute();
                } else speak(Translations.getAnswer(Assistant.this, "fine"));
            } else if (currentCategory == AssistantCategory.SEARCH_QUERY)
                searchOnInternet(command, 1);
            else if (currentCategory == AssistantCategory.ALARM_TIME) setAlarm(command, 1);
            else if (currentCategory == AssistantCategory.WEATHER_PLACE)
                weatherForecast(command, 1);
            else if (currentCategory == AssistantCategory.CREATE_NOTE_TITLE)
                createNote(command, 1);
            else if (currentCategory == AssistantCategory.CREATE_NOTE_CONTENT)
                createNote(command, 2);
            else if (currentCategory == AssistantCategory.CREATE_NOTE_CONFIRMATION)
                createNote(command, 3);
            else if (currentCategory == AssistantCategory.CREATE_NOTE_CORRECTION_QUESTION)
                createNote(command, 4);
            else if (currentCategory == AssistantCategory.CREATE_NOTE_CORRECTION_TYPE)
                createNote(command, 5);
            else if (currentCategory == AssistantCategory.EDIT_NOTE_TYPE) editNote(command, 1);
            else if (currentCategory == AssistantCategory.EDIT_NOTE_TITLE) editNote(command, 2);
            else if (currentCategory == AssistantCategory.EDIT_NOTE_CONTENT)
                editNote(command, 3);
            else if (currentCategory == AssistantCategory.EDIT_NOTE_CONFIRMATION)
                editNote(command, 4);
            else if (currentCategory == AssistantCategory.READ_SMS_NEW_NOTIFICATION)
                readMessage(command, 0);
            else if (currentCategory == AssistantCategory.READ_SMS_ANSWER_QUESTION)
                readMessage(command, 1);
            else if (currentCategory == AssistantCategory.ACTIVATED_COMMAND_CONFIRMATION) {
                if (isAnswerYes(command)) {
                    currentCategory = AssistantCategory.READY;
                    processCommand(intendedCommand);
                } else if (containsWord(command, Translations.getStringResource(Assistant.this, "no_lowercase"))) {
                    speak(Translations.getAnswer(Assistant.this, "fine"));
                    currentCategory = AssistantCategory.READY;
                } else {
                    speak(answerNoUnderstanding(true));
                    startRecording();
                }
            } else if (currentCategory == AssistantCategory.SMS_CORRECTION_TYPE)
                sendMessage(command, 6, null);
            else if (currentCategory == AssistantCategory.SMS_CORRECTION_ADD_CONTENT)
                sendMessage(command, 7, null);
            else if (currentCategory == AssistantCategory.MAIL_RECIPIENT) mail(command, 1);
            else if (currentCategory == AssistantCategory.MAIL_TOPIC) mail(command, 2);
            else if (currentCategory == AssistantCategory.MAIL_CONTENT) mail(command, 3);
            else if (currentCategory == AssistantCategory.MAIL_CONFIRMATION) mail(command, 4);
            else if (currentCategory == AssistantCategory.MAIL_CORRECTION_QUESTION)
                mail(command, 5);
            else if (currentCategory == AssistantCategory.MAIL_CORRECTION_TYPE)
                mail(command, 6);
            else if (currentCategory == AssistantCategory.MAIL_CORRECTION_TOPIC)
                mail(command, 7);
            else if (currentCategory == AssistantCategory.MAIL_CORRECTION_CONTENT_TYPE)
                mail(command, 8);
            else if (currentCategory == AssistantCategory.MAIL_CORRECTION_ADD_CONTENT)
                mail(command, 9);
            else if (currentCategory == AssistantCategory.MAIL_CORRECTION_RECIPIENT)
                mail(command, 10);
            else if (currentCategory == AssistantCategory.REMINDER_TITLE)
                setReminder(command, 1);
            else if (currentCategory == AssistantCategory.REMINDER_DATE)
                setReminder(command, 2);
            else if (currentCategory == AssistantCategory.REMINDER_TIME)
                setReminder(command, 3);
            else if (currentCategory == AssistantCategory.REMINDER_CONFIRMATION)
                setReminder(command, 4);
            else if (currentCategory == AssistantCategory.REMINDER_CORRECTION_QUESTION)
                setReminder(command, 5);
            else if (currentCategory == AssistantCategory.REMINDER_CORRECTION_TYPE)
                setReminder(command, 6);
            else if (currentCategory == AssistantCategory.REMINDER_CORRECTION_TIME)
                setReminder(command, 7);
            else if (currentCategory == AssistantCategory.REMINDER_CORRECTION_TITLE)
                setReminder(command, 8);
            else if (currentCategory == AssistantCategory.REMINDER_CORRECTION_DATE)
                setReminder(command, 9);
            else if (currentCategory == AssistantCategory.RESTART_CONFIRMATION)
                restart(command, 1);
            else if (currentCategory == AssistantCategory.TURN_OFF_CONFIRMATION)
                turnOff(command, 1);
            else if (currentCategory == AssistantCategory.CONTACT_ONE_SUGGESTION) {
                if (toBeReturnedByFindContact.get(0).equals("makeCall"))
                    makeCall(command, 0, findContact("makeCall", command, 1, true));
                else if (toBeReturnedByFindContact.get(0).equals("sendMessage"))
                    sendMessage(command, 0, findContact("sendMessage", command, 1, true));
                else if (toBeReturnedByFindContact.get(0).equals("defaultNumber"))
                    defaultNumber(command, 0, findContact("defaultNumber", command, 1, true));
            } else if (currentCategory == AssistantCategory.CONTACT_SUGGESTION_CHOICE) {
                if (toBeReturnedByFindContact.get(0).equals("makeCall"))
                    makeCall(command, 0, findContact("makeCall", command, 2, true));
                else if (toBeReturnedByFindContact.get(0).equals("sendMessage"))
                    sendMessage(command, 0, findContact("sendMessage", command, 2, true));
                else if (toBeReturnedByFindContact.get(0).equals("defaultNumber"))
                    defaultNumber(command, 0, findContact("defaultNumber", command, 2, true));
            } else if (currentCategory == AssistantCategory.CONTACT_NUMBER_CHOICE) {
                if (toBeReturnedByFindContact.get(0).equals("makeCall"))
                    makeCall(command, 0, findContact("makeCall", command, 3, true));
                else if (toBeReturnedByFindContact.get(0).equals("sendMessage"))
                    sendMessage(command, 0, findContact("sendMessage", command, 3, true));
                else if (toBeReturnedByFindContact.get(0).equals("defaultNumber"))
                    defaultNumber(command, 0, findContact("defaultNumber", command, 3, true));
            } else if (currentCategory == AssistantCategory.SMS_SIM_CHOICE)
                sendMessage(command, 8, null);
            else if (currentCategory == AssistantCategory.LAST_OUTCOMING_CALL_AGAIN_QUESTION)
                lastOutcomingCall(command, 1);
            else if (currentCategory == AssistantCategory.LAST_INCOMING_CALL_BACK_QUESTION)
                lastIncomingCall(command, 1);
            else if (currentCategory == AssistantCategory.LAST_CALL_CALL_QUESTION)
                lastCall(command, 1);
            else if (currentCategory == AssistantCategory.DICTATE_TO_CLIPBOARD_TEXT)
                dictateToClipboard(command, 1);

            if (currentCategory != AssistantCategory.ACTIVATED_COMMAND_CONFIRMATION) {
                Runnable toBeRun = new Runnable() {
                    @Override
                    public void run() {
                        while (isSpeaking()) {

                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if ((speechRecognitionService == null || !speechRecognitionService.isWorking) && !isGoingToBeRecording) {
                                    if (conversationMode && !noNextRecognitionAttempt)
                                        startRecording();
                                }
                            }
                        });
                    }
                };

                new Thread(toBeRun).start();

                if (!isGoingToBeRecording && !isTextToSpeechEnabled && noNextRecognitionAttempt)
                    noNextRecognitionAttempt = false;
            }
        }
    }

    public void speakPhrase(String text, int category) {
        String toBeSaid = extractName("speakPhrase", text, false);
        speak(processText(toBeSaid));
    }

    private String torchOnCameraId = null;

    public void torch(String text, int category) {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

                    if (torchOnCameraId == null) {
                        String[] ids = cameraManager.getCameraIdList();
                        String cameraToControlId = null;
                        for (int i = 0; i < ids.length; i++) {
                            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(ids[i]);
                            if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK && characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                                cameraToControlId = ids[i];
                                i = ids.length;
                            } else if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))
                                cameraToControlId = ids[i];
                        }

                        if (cameraToControlId != null) {
                            cameraManager.setTorchMode(cameraToControlId, true);
                            torchOnCameraId = cameraToControlId;
                            speak(Translations.getAnswer(Assistant.this, "torch_on"));
                        } else
                            speak(Translations.getAnswer(Assistant.this, "no_torch_detected"));
                    } else {
                        cameraManager.setTorchMode(torchOnCameraId, false);
                        torchOnCameraId = null;
                        speak(Translations.getAnswer(Assistant.this, "torch_off"));
                    }
                } else {
                    if (camera == null) {
                        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                            Camera.CameraInfo info = new Camera.CameraInfo();
                            Camera.getCameraInfo(i, info);
                            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                if (camera != null) camera.release();
                                camera = Camera.open(i);
                                break;
                            } else {
                                if (camera != null) camera.release();
                                camera = Camera.open(i);
                            }
                        }
                    }
                    if (camera != null) {
                        Camera.Parameters parameters = camera.getParameters();
                        if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                            camera.stopPreview();
                            camera.release();
                            camera = null;
                            speak(Translations.getAnswer(Assistant.this, "torch_off"));
                        } else {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            camera.setParameters(parameters);
                            camera.startPreview();
                            speak(Translations.getAnswer(Assistant.this, "torch_on"));
                        }
                    } else speak(Translations.getAnswer(Assistant.this, "no_cameras_detected"));
                }
            } catch (Exception e) {
                if (assistantSettings.getBoolean("debug_mode", false))
                    Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                speak(Translations.getAnswer(Assistant.this, "torch_error"));
            }
        } else speak(Translations.getAnswer(Assistant.this, "no_torch_detected"));
    }

    public void pauseResumeTimer(String text, int category) {
        if (TimerService.isWorking) {
            bindService(new Intent(Assistant.this, TimerService.class), new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    TimerService timerService = ((TimerService.TimerServiceBinder) service).getService();
                    if (!timerService.isPaused()) {
                        timerService.pauseTimer();
                        speak(Translations.getAnswer(Assistant.this, "timer_paused"));
                    } else {
                        timerService.resumeTimer();
                        speak(Translations.getAnswer(Assistant.this, "timer_resumed"));
                    }

                    unbindService(this);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            }, BIND_AUTO_CREATE);
        } else speak(Translations.getAnswer(Assistant.this, "no_timer_set"));
    }

    public void openWebpage(String text, int category) {
        String address = extractName("openWebpage", text, false);
        speak(Translations.getAnswer(Assistant.this, "opening_webpage"));
        new ExecuteIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(address.startsWith("http://") || address.startsWith("https://") ? address : "http://" + address))).execute();
    }

    public void dictateToClipboard(String text, int category) {
        if (category == 0) {
            speak(Translations.getAnswer(Assistant.this, "clipboard_text_request"));
            currentCategory = AssistantCategory.DICTATE_TO_CLIPBOARD_TEXT;
            startRecording();
        } else if (category == 1) {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.app_name), processText(text));
            clipboardManager.setPrimaryClip(clip);
            speak(Translations.getAnswer(Assistant.this, "text_copied_to_clipboard"));
            currentCategory = AssistantCategory.READY;
        }
    }

    public String processNumber(@Nullable String number) {
        if (number == null)
            return Translations.getStringResource(Assistant.this, "private_number");
        else {
            String newNumber = "";
            number = number.replace(" ", "");
            for (int i = 0; i < number.length(); i++) {
                newNumber += number.charAt(i);
                if (i < number.length() - 1) newNumber += "-";
            }
            return newNumber;
        }
    }

    public String lastNumber;

    public void lastOutcomingCall(String text, int category) {
        if (category == 0) {
            lastCallContactName = null;
            try {
                Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, CallLog.Calls.TYPE + " = " + CallLog.Calls.OUTGOING_TYPE, null, "datetime(" + CallLog.Calls.DATE + ")" + " DESC");
                if (cursor != null) {
                    if (cursor.moveToLast()) {
                        lastNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                        lastCallContactName = getContactName(lastNumber);
                        if (lastCallContactName != null)
                            speak(Translations.getAnswer(Assistant.this, "last_outcoming_call").replace("%%NUMBER%%", lastCallContactName) + (lastNumber != null ? " " + Translations.getAnswer(Assistant.this, "call_willingness_question") : ""));
                        else {
                            lastCallContactName = processNumber(lastNumber);
                            speak(Translations.getAnswer(Assistant.this, "last_outcoming_call").replace("%%NUMBER%%", processNumber(lastNumber)) + (lastNumber != null ? " " + Translations.getAnswer(Assistant.this, "call_willingness_question") : ""));
                        }
                        if (lastNumber != null) {
                            currentCategory = AssistantCategory.LAST_OUTCOMING_CALL_AGAIN_QUESTION;
                            startRecording();
                        }
                    } else speak(Translations.getAnswer(Assistant.this, "could_not_determine"));

                    cursor.close();
                } else speak(Translations.getAnswer(Assistant.this, "could_not_determine"));
            }
            catch (SecurityException e) {
                e.printStackTrace();
                speak(Translations.getAnswer(Assistant.this, "could_not_determine"));
            }
            catch (Exception e) {
                e.printStackTrace();
                speak(Translations.getAnswer(Assistant.this, "could_not_determine"));
            }
        } else if (category == 1) {
            if (isAnswerYes(text)) {
                noNextRecognitionAttempt = true;
                speak(Translations.getAnswer(Assistant.this, "calling").replace("%%CONTACT%%", lastCallContactName));
                currentCategory = AssistantCategory.READY;
                new Call(lastNumber).execute();
            } else if (containsWord(text, Translations.getStringResource(Assistant.this, "no_lowercase"))) {
                speak(Translations.getAnswer(Assistant.this, "fine"));
                currentCategory = AssistantCategory.READY;
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        }
    }

    public String getContactName(String number) {
        number = number.replace("-", "");

        String name = null;

        Uri filterUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Cursor cursor = getContentResolver().query(filterUri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if (cursor != null) cursor.close();

        return name;
    }

    public void lastIncomingCall(String text, int category) {
        if (category == 0) {
            lastCallContactName = null;
            try {
                Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, CallLog.Calls.TYPE + " = " + CallLog.Calls.INCOMING_TYPE, null, "datetime(" + CallLog.Calls.DATE + ")" + " DESC");
                if (cursor != null) {
                    if (cursor.moveToLast()) {
                        lastNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                        lastCallContactName = getContactName(lastNumber);
                        if (lastCallContactName != null)
                            speak(Translations.getAnswer(Assistant.this, "last_incoming_call").replace("%%NUMBER%%", lastCallContactName) + (lastNumber != null ? " " + Translations.getAnswer(Assistant.this, "call_willingness_question") : ""));
                        else {
                            lastCallContactName = processNumber(lastNumber);
                            speak(Translations.getAnswer(Assistant.this, "last_incoming_call").replace("%%NUMBER%%", processNumber(lastNumber)) + (lastNumber != null ? " " + Translations.getAnswer(Assistant.this, "call_willingness_question") : ""));
                        }
                        if (lastNumber != null) {
                            currentCategory = AssistantCategory.LAST_INCOMING_CALL_BACK_QUESTION;
                            startRecording();
                        }
                    } else speak(Translations.getAnswer(Assistant.this, "could_not_determine"));

                    cursor.close();
                } else speak(Translations.getAnswer(Assistant.this, "could_not_determine"));
            }
            catch (SecurityException e) {
                speak(Translations.getAnswer(Assistant.this, "could_not_determine"));
            }
            catch (Exception e) {
                speak(Translations.getAnswer(Assistant.this, "could_not_determine"));
            }
        } else if (category == 1) {
            if (isAnswerYes(text)) {
                noNextRecognitionAttempt = true;
                speak(Translations.getAnswer(Assistant.this, "calling").replace("%%CONTACT%%", lastCallContactName));
                currentCategory = AssistantCategory.READY;
                new Call(lastNumber).execute();
            } else if (containsWord(text, Translations.getStringResource(Assistant.this, "no_lowercase"))) {
                speak(Translations.getAnswer(Assistant.this, "fine"));
                currentCategory = AssistantCategory.READY;
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        }
    }

    private String lastCallContactName;

    public void lastCall(String text, int category) {
        if (category == 0) {
            lastCallContactName = null;
            try {
                Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, "datetime(" + CallLog.Calls.DATE + ")" + " DESC");
                if (cursor != null) {
                    if (cursor.moveToLast()) {
                        boolean outgoing = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE)) == CallLog.Calls.OUTGOING_TYPE;
                        lastNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                        lastCallContactName = getContactName(lastNumber);
                        if (lastCallContactName != null) {
                            if (outgoing)
                                speak(Translations.getAnswer(Assistant.this, "last_outcoming_call").replace("%%NUMBER%%", lastCallContactName) + (lastNumber != null ? " " + Translations.getAnswer(Assistant.this, "call_willingness_question") : ""));
                            else
                                speak(Translations.getAnswer(Assistant.this, "last_incoming_call").replace("%%NUMBER%%", lastCallContactName) + (lastNumber != null ? " " + Translations.getAnswer(Assistant.this, "call_willingness_question") : ""));
                        } else {
                            lastCallContactName = processNumber(lastNumber);
                            if (outgoing)
                                speak(Translations.getAnswer(Assistant.this, "last_outcoming_call").replace("%%NUMBER%%", processNumber(lastNumber)) + (lastNumber != null ? " " + Translations.getAnswer(Assistant.this, "call_willingness_question") : ""));
                            else
                                speak(Translations.getAnswer(Assistant.this, "last_incoming_call").replace("%%NUMBER%%", processNumber(lastNumber)) + (lastNumber != null ? " " + Translations.getAnswer(Assistant.this, "call_willingness_question") : ""));
                        }
                        if (lastNumber != null) {
                            currentCategory = AssistantCategory.LAST_CALL_CALL_QUESTION;
                            startRecording();
                        }
                    } else speak(Translations.getAnswer(Assistant.this, "could_not_determine"));

                    cursor.close();
                } else speak(Translations.getAnswer(Assistant.this, "could_not_determine"));
            }
            catch (SecurityException e) {
                speak(Translations.getAnswer(Assistant.this, "could_not_determine"));
            }
            catch (Exception e) {
                speak(Translations.getAnswer(Assistant.this, "could_not_determine"));
            }
        } else if (category == 1) {
            if (isAnswerYes(text)) {
                noNextRecognitionAttempt = true;
                speak(Translations.getAnswer(Assistant.this, "calling").replace("%%CONTACT%%", lastCallContactName));
                currentCategory = AssistantCategory.READY;
                new Call(lastNumber).execute();
            } else if (containsWord(text, Translations.getStringResource(Assistant.this, "no_lowercase"))) {
                speak(Translations.getAnswer(Assistant.this, "fine"));
                currentCategory = AssistantCategory.READY;
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        }
    }

    public void restart(String text, int category) {
        if (category == 0) {
            speak(Translations.getAnswer(Assistant.this, "restart_confirmation"));
            currentCategory = AssistantCategory.RESTART_CONFIRMATION;
            startRecording();
        } else if (category == 1) {
            if (isAnswerYes(text)) {
                try {
                    java.lang.Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
                    proc.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                speak(Translations.getAnswer(Assistant.this, "restart_error"));
            } else if (isAnswerNo(text)) {
                speak(Translations.getAnswer(Assistant.this, "fine"));
                currentCategory = AssistantCategory.READY;
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        }
    }

    public boolean isAnswerNo(String text) {
        text = text.toLowerCase();
        String[] no = Translations.getStringResource(Assistant.this, "no_words").split("\\|");

        for (int i = 0; i < no.length; i++) {
            if (text.contains(no[i].toLowerCase())) return true;
        }

        return false;
    }

    public void turnOff(String text, int category) {
        if (category == 0) {
            speak(Translations.getAnswer(Assistant.this, "turn_off_confirmation"));
            currentCategory = AssistantCategory.TURN_OFF_CONFIRMATION;
            startRecording();
        } else if (category == 1) {
            if (isAnswerYes(text)) {
                try {
                    java.lang.Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot -p"});
                    proc.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                speak(Translations.getAnswer(Assistant.this, "turn_off_error"));
            } else if (isAnswerNo(text)) {
                speak(Translations.getAnswer(Assistant.this, "fine"));
                currentCategory = AssistantCategory.READY;
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        }
    }

    public boolean isAnswerYes(String text) {
        text = text.toLowerCase();
        String[] yes = Translations.getStringResource(Assistant.this, "yes_words").split("\\|");

        for (int i = 0; i < yes.length; i++) {
            if (text.contains(yes[i].toLowerCase())) return true;
        }

        return false;
    }

    private String reminderTitle;
    private GregorianCalendar reminderDate;

    public void setReminder(String text, int category) {
        if (category == 0) {
            reminderTitle = extractName("setReminder", text, false);

            if (reminderTitle != null && !reminderTitle.equals("")) {
                reminderTitle = processText(reminderTitle);
                speak(Translations.getAnswer(Assistant.this, "reminder_date_question"));
                currentCategory = AssistantCategory.REMINDER_DATE;
                startRecording();
            } else {
                speak(Translations.getAnswer(Assistant.this, "reminder_title_question"));
                currentCategory = AssistantCategory.REMINDER_TITLE;
                startRecording();
            }
        } else if (category == 1) {
            reminderTitle = text;
            speak(Translations.getAnswer(Assistant.this, "reminder_date_question"));
            currentCategory = AssistantCategory.REMINDER_DATE;
            startRecording();
        } else if (category == 2) {
            int daysForward = returnDaysForward(text);
            if (daysForward == -1) {
                try {
                    ArrayList<Integer> numbers = returnNumbers(text);
                    int day = numbers.get(0);
                    int month;
                    int year = (numbers.size() == 2 ? numbers.get(1) : Calendar.getInstance().get(Calendar.YEAR));

                    month = returnMonth(text);

                    reminderDate = new GregorianCalendar(year, month, day);
                } catch (Exception e) {
                    speak(answerNoUnderstanding(true));
                    startRecording();
                }
            } else {
                reminderDate = new GregorianCalendar();
                reminderDate.add(Calendar.DAY_OF_YEAR, daysForward);
            }
            speak(Translations.getAnswer(Assistant.this, "reminder_time_question"));
            currentCategory = AssistantCategory.REMINDER_TIME;
            startRecording();
        } else if (category == 3) {
            Calendar calendar = processTime(text, false);
            if (calendar != null) {
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                reminderDate.set(Calendar.HOUR_OF_DAY, hour);
                reminderDate.set(Calendar.MINUTE, minute);

                showSource(R.layout.internal_reminder, false, reminderTitle, DateFormat.format("dd.MM.yyyy", reminderDate), DateFormat.format("HH:mm", reminderDate));
                assistantAnswer.setText(Translations.getAnswer(Assistant.this, "reminder_confirmation_answer"));
                speak(Translations.getAnswer(Assistant.this, "reminder_confirmation").replace("%%TITLE%%", reminderTitle).replace("%%DATE%%", DateFormat.format("dd.MM.yyyy", reminderDate)).replace("%%TIME%%", DateFormat.format("HH:mm", reminderDate)), false);
                currentCategory = AssistantCategory.REMINDER_CONFIRMATION;
                startRecording();
            } else {
                speak(Translations.getAnswer(Assistant.this, "time_processing_error"));
                startRecording();
            }
        } else if (category == 4) {
            if (isAnswerYes(text)) {
                try {
                    reminderDate.set(GregorianCalendar.SECOND, 0);
                    reminderDate.set(GregorianCalendar.MILLISECOND, 0);

                    long timeInMillis = reminderDate.getTimeInMillis();
                    int time = (int) (timeInMillis / 1000);

                    SharedPreferences remindersPreferences = getSharedPreferences("reminders", MODE_PRIVATE);
                    remindersPreferences.edit().putString(Long.toString(timeInMillis), reminderTitle).apply();

                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), time, new Intent(getApplicationContext(), AlarmActivity.class).putExtra("reminder", true).putExtra("title", reminderTitle), PendingIntent.FLAG_UPDATE_CURRENT);

                    if (android.os.Build.VERSION.SDK_INT >= 19)
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                    else alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);

                    speak(Translations.getAnswer(Assistant.this, "reminder_saved"));
                } catch (Exception e) {
                    if (assistantSettings.getBoolean("debug_mode", false))
                        Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                    speak(Translations.getAnswer(Assistant.this, "reminder_save_error"));
                }

                currentCategory = AssistantCategory.READY;
            } else if (isAnswerNo(text)) {
                speak(Translations.getAnswer(Assistant.this, "reminder_correction_type_question"));
                currentCategory = AssistantCategory.REMINDER_CORRECTION_TYPE;
                startRecording();
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        } else if (category == 5) {
            if (isAnswerYes(text)) {
                speak(Translations.getAnswer(Assistant.this, "reminder_correction_type_question"));
                currentCategory = AssistantCategory.REMINDER_CORRECTION_TYPE;
                startRecording();
            } else if (isAnswerNo(text)) {
                speak(Translations.getAnswer(Assistant.this, "reminder_not_saved"));
                currentCategory = AssistantCategory.READY;
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        } else if (category == 6) {
            if (text.contains("1")) {
                speak(Translations.getAnswer(Assistant.this, "reminder_not_saved"));
                currentCategory = AssistantCategory.READY;
            } else if (text.contains("2")) {
                speak(Translations.getAnswer(Assistant.this, "reminder_new_title_request"));
                currentCategory = AssistantCategory.REMINDER_CORRECTION_TITLE;
                startRecording();
            } else if (text.contains("3")) {
                speak(Translations.getAnswer(Assistant.this, "reminder_new_date_request"));
                currentCategory = AssistantCategory.REMINDER_CORRECTION_DATE;
                startRecording();
            } else if (text.contains("4")) {
                speak(Translations.getAnswer(Assistant.this, "reminder_new_time_request"));
                currentCategory = AssistantCategory.REMINDER_CORRECTION_TIME;
                startRecording();
            } else if (text.contains("5")) {
                setReminder(text, 0);
            } else {
                speak(Translations.getAnswer(Assistant.this, "position_number_required") + "\n" + Translations.getAnswer(Assistant.this, "reminder_correction_type_question"));
                startRecording();
            }
        } else if (category == 7) {
            Calendar calendar = processTime(text, false);
            if (calendar != null) {
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                reminderDate.set(Calendar.HOUR_OF_DAY, hour);
                reminderDate.set(Calendar.MINUTE, minute);
                showSource(R.layout.internal_reminder, false, reminderTitle, DateFormat.format("dd.MM.yyyy", reminderDate), DateFormat.format("HH:mm", reminderDate));
                assistantAnswer.setText(Translations.getAnswer(Assistant.this, "reminder_confirmation_answer"));
                speak(Translations.getAnswer(Assistant.this, "reminder_confirmation").replace("%%TITLE%%", reminderTitle).replace("%%DATE%%", DateFormat.format("dd.MM.yyyy", reminderDate)).replace("%%TIME%%", DateFormat.format("HH:mm", reminderDate)), false);
                currentCategory = AssistantCategory.REMINDER_CONFIRMATION;
                startRecording();
            } else {
                speak(Translations.getAnswer(Assistant.this, "time_processing_error"));
                startRecording();
            }
        } else if (category == 8) {
            reminderTitle = text;
            showSource(R.layout.internal_reminder, false, reminderTitle, DateFormat.format("dd.MM.yyyy", reminderDate), DateFormat.format("HH:mm", reminderDate));
            assistantAnswer.setText(Translations.getAnswer(Assistant.this, "reminder_confirmation_answer"));
            speak(Translations.getAnswer(Assistant.this, "reminder_confirmation").replace("%%TITLE%%", reminderTitle).replace("%%DATE%%", DateFormat.format("dd.MM.yyyy", reminderDate)).replace("%%TIME%%", DateFormat.format("HH:mm", reminderDate)), false);
            currentCategory = AssistantCategory.REMINDER_CONFIRMATION;
            startRecording();
        } else if (category == 9) {
            int daysForward = returnDaysForward(text);
            if (daysForward == -1) {
                try {
                    ArrayList<Integer> numbers = returnNumbers(text);
                    int day = numbers.get(0);
                    int month;
                    int year = (numbers.size() == 2 ? numbers.get(1) : Calendar.getInstance().get(Calendar.YEAR));

                    month = returnMonth(text);

                    reminderDate.set(Calendar.DAY_OF_MONTH, day);
                    reminderDate.set(Calendar.MONTH, month);
                    reminderDate.set(Calendar.YEAR, year);
                } catch (Exception e) {
                    speak(answerNoUnderstanding(true));
                    startRecording();
                    return;
                }
            } else {
                GregorianCalendar calendar = new GregorianCalendar();
                reminderDate.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR));
                reminderDate.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
                reminderDate.add(Calendar.DAY_OF_YEAR, daysForward);
            }

            showSource(R.layout.internal_reminder, false, reminderTitle, DateFormat.format("dd.MM.yyyy", reminderDate), DateFormat.format("HH:mm", reminderDate));
            assistantAnswer.setText(Translations.getAnswer(Assistant.this, "reminder_confirmation_answer"));
            speak(Translations.getAnswer(Assistant.this, "reminder_confirmation").replace("%%TITLE%%", reminderTitle).replace("%%DATE%%", DateFormat.format("dd.MM.yyyy", reminderDate)).replace("%%TIME%%", DateFormat.format("HH:mm", reminderDate)), false);
            currentCategory = AssistantCategory.REMINDER_CONFIRMATION;
            startRecording();
        }
    }

    public void playerControl(String text, int category) {
        assistantAnswer.setText(Translations.getAnswer(Assistant.this, "fine"));
        if (!assistantSettings.getBoolean("conversation_mode_when_controlling_music", false))
            noNextRecognitionAttempt = true;
        Intent intent;
        KeyEvent event;

        String[] controlledPlayerValues = assistantSettings.getString("controlled_media_player", "").split("\\|");
        String playerPackageName = null;
        String playerReceiverName = null;
        if (controlledPlayerValues.length == 2) {
            playerPackageName = controlledPlayerValues[0];
            playerReceiverName = controlledPlayerValues[1];
        }

        if (category == 0) {
            intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (playerPackageName != null)
                intent.setComponent(new ComponentName(playerPackageName, playerReceiverName));
            sendBroadcast(intent);

            intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (playerPackageName != null)
                intent.setComponent(new ComponentName(playerPackageName, playerReceiverName));
            sendBroadcast(intent);
        } else if (category == 1) {
            intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (playerPackageName != null)
                intent.setComponent(new ComponentName(playerPackageName, playerReceiverName));
            sendBroadcast(intent);

            intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (playerPackageName != null)
                intent.setComponent(new ComponentName(playerPackageName, playerReceiverName));
            sendBroadcast(intent);
        } else if (category == 2) {
            intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (playerPackageName != null)
                intent.setComponent(new ComponentName(playerPackageName, playerReceiverName));
            sendBroadcast(intent);

            intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (playerPackageName != null)
                intent.setComponent(new ComponentName(playerPackageName, playerReceiverName));
            sendBroadcast(intent);
        } else if (category == 3) {
            intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (playerPackageName != null)
                intent.setComponent(new ComponentName(playerPackageName, playerReceiverName));
            sendBroadcast(intent);

            intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (playerPackageName != null)
                intent.setComponent(new ComponentName(playerPackageName, playerReceiverName));
            sendBroadcast(intent);

            intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (playerPackageName != null)
                intent.setComponent(new ComponentName(playerPackageName, playerReceiverName));
            sendBroadcast(intent);

            intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (playerPackageName != null)
                intent.setComponent(new ComponentName(playerPackageName, playerReceiverName));
            sendBroadcast(intent);
        }
    }

    private String mailRecipient;
    private String mailSubject;
    private String mailContent;
    private String mailOriginalContent;

    public void mail(String text, int category) {
        if (category == 0) {
            speak(Translations.getAnswer(Assistant.this, "mail_recipient_question"));
            currentCategory = AssistantCategory.MAIL_RECIPIENT;
            startRecording();
        } else if (category == 1) {
            mailRecipient = processText(text);
            speak(Translations.getAnswer(Assistant.this, "mail_topic_question"));
            currentCategory = AssistantCategory.MAIL_TOPIC;
            startRecording();
        } else if (category == 2) {
            mailSubject = processText(text);
            speak(Translations.getAnswer(Assistant.this, "mail_content_question"));
            currentCategory = AssistantCategory.MAIL_CONTENT;
            startRecording();
        } else if (category == 3) {
            mailContent = text;
            mailOriginalContent = originalRecognisedText;
            speak(Translations.getAnswer(Assistant.this, "mail_confirmation_question").replace("%%RECIPIENT%%", mailRecipient).replace("%%SUBJECT%%", mailSubject), mailOriginalContent, mailContent);
            currentCategory = AssistantCategory.MAIL_CONFIRMATION;
            startRecording();
        } else if (category == 4) {
            if (isAnswerYes(text)) {
                speak(Translations.getAnswer(Assistant.this, "mail_sent"));
                currentCategory = AssistantCategory.READY;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("mailto:" + mailRecipient + "?subject=" + mailSubject + "&body=" + mailContent));
                new ExecuteIntent(i).execute();
            } else if (isAnswerNo(text)) {
                speak(Translations.getAnswer(Assistant.this, "mail_correction_type_question"));
                currentCategory = AssistantCategory.MAIL_CORRECTION_TYPE;
                startRecording();
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        } else if (category == 5) {
            if (isAnswerYes(text)) {
                speak(Translations.getAnswer(Assistant.this, "mail_correction_type_question"));
                currentCategory = AssistantCategory.MAIL_CORRECTION_TYPE;
                startRecording();
            } else if (isAnswerNo(text)) {
                speak(Translations.getAnswer(Assistant.this, "mail_not_sent"));
                currentCategory = AssistantCategory.READY;
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        } else if (category == 6) {
            if (text.contains("1")) {
                speak(Translations.getAnswer(Assistant.this, "mail_not_sent"));
                currentCategory = AssistantCategory.READY;
            } else if (text.contains("3")) {
                speak(Translations.getAnswer(Assistant.this, "mail_new_subject_request"));
                currentCategory = AssistantCategory.MAIL_CORRECTION_TOPIC;
                startRecording();
            } else if (text.contains("4")) {
                speak(Translations.getAnswer(Assistant.this, "mail_content_correction_type_question"));
                currentCategory = AssistantCategory.MAIL_CORRECTION_CONTENT_TYPE;
                startRecording();
            } else if (text.contains("2")) {
                speak(Translations.getAnswer(Assistant.this, "mail_new_address_request"));
                currentCategory = AssistantCategory.MAIL_CORRECTION_RECIPIENT;
                startRecording();
            } else {
                speak(Translations.getAnswer(Assistant.this, "position_number_required") + "\n" + Translations.getAnswer(Assistant.this, "mail_correction_type_question"));
                startRecording();
            }
        } else if (category == 7) {
            mailSubject = text;
            speak(Translations.getAnswer(Assistant.this, "mail_confirmation_question").replace("%%RECIPIENT%%", mailRecipient).replace("%%SUBJECT%%", mailSubject), mailOriginalContent, mailContent);
            currentCategory = AssistantCategory.MAIL_CONFIRMATION;
            startRecording();
        } else if (category == 8) {
            if (isAnswerYes(text)) {
                speak(Translations.getAnswer(Assistant.this, "mail_content_to_add_request"));
                currentCategory = AssistantCategory.MAIL_CORRECTION_ADD_CONTENT;
                startRecording();
            } else if (isAnswerNo(text)) {
                speak(Translations.getAnswer(Assistant.this, "mail_new_content_request"));
                currentCategory = AssistantCategory.MAIL_CONTENT;
                startRecording();
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        } else if (category == 9) {
            mailContent += " " + text;
            mailOriginalContent += " " + originalRecognisedText;
            speak(Translations.getAnswer(Assistant.this, "mail_confirmation_question").replace("%%RECIPIENT%%", mailRecipient).replace("%%SUBJECT%%", mailSubject), mailOriginalContent, mailContent);
            currentCategory = AssistantCategory.MAIL_CONFIRMATION;
            startRecording();
        } else if (category == 10) {
            mailRecipient = text;
            speak(Translations.getAnswer(Assistant.this, "mail_confirmation_question").replace("%%RECIPIENT%%", mailRecipient).replace("%%SUBJECT%%", mailSubject), mailOriginalContent, mailContent);
            currentCategory = AssistantCategory.MAIL_CONFIRMATION;
            startRecording();
        }
    }

    private String imagePath;

    public void takePhoto(String text, int category) {
        assistantAnswer.setText("...");
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                try {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String imageFileName = "PolassisPhoto_" + timeStamp;
                    File storageDir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM);
                    File image = File.createTempFile(imageFileName, ".jpg", storageDir);
                    imagePath = image.getAbsolutePath();
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
                    startActivityForResult(intent, 10);
                } catch (IOException e) {
                    speak(Translations.getAnswer(Assistant.this, "data_save_error"));
                }
            } else speak(Translations.getAnswer(Assistant.this, "no_photo_app_error"));
        } else speak(Translations.getAnswer(Assistant.this, "no_camera_error"));
    }

    public void defaultNumber(String text, int category, ArrayList<String> data) {
        if (data != null) {
            if (data.size() == 3) {
                savedValues.edit().putString(data.get(1), data.get(2)).apply();
                speak(Translations.getAnswer(Assistant.this, "default_number_set"));
                currentCategory = AssistantCategory.READY;
            } else if (data.size() == 1) {
                speak(answerNoContact());
                currentCategory = AssistantCategory.READY;
            } else {
                savedValues.edit().remove(data.get(1)).apply();
                speak(Translations.getAnswer(Assistant.this, "default_number_removed"));
                currentCategory = AssistantCategory.READY;
            }
        }
    }

    public void whichDayOfWeekItWas(String text, int category) {
        ArrayList<Integer> numbers = returnNumbers(text);
        int day = numbers.get(0);
        int month;
        int year = (numbers.size() == 2 ? numbers.get(1) : Calendar.getInstance().get(Calendar.YEAR));

        month = returnMonth(text);

        GregorianCalendar calendar = new GregorianCalendar(year, month, day);

        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY:
                speak(Translations.getStringResource(Assistant.this, "sunday") + ".");
                break;

            case Calendar.MONDAY:
                speak(Translations.getStringResource(Assistant.this, "monday") + ".");
                break;

            case Calendar.TUESDAY:
                speak(Translations.getStringResource(Assistant.this, "tuesday") + ".");
                break;

            case Calendar.WEDNESDAY:
                speak(Translations.getStringResource(Assistant.this, "wednesday") + ".");
                break;

            case Calendar.THURSDAY:
                speak(Translations.getStringResource(Assistant.this, "thursday") + ".");
                break;

            case Calendar.FRIDAY:
                speak(Translations.getStringResource(Assistant.this, "friday") + ".");
                break;

            case Calendar.SATURDAY:
                speak(Translations.getStringResource(Assistant.this, "saturday") + ".");
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String permissions[], int[] results) {
        boolean notGranted = false;
        for (int i = 0; i < results.length; i++) {
            if (results[i] != PackageManager.PERMISSION_GRANTED) {
                notGranted = true;
                i = results.length + 1;
            }
        }

        currentCategory = AssistantCategory.READY;

        if (code == 0) {
            if (!notGranted)
                speak(Translations.getAnswer(Assistant.this, "permissions_granted"));
            else
                speak(Translations.getAnswer(Assistant.this, "not_all_permissions_granted"));
        } else if (code == 1) {
            if (!notGranted) startRecording();
            else
                speak(Translations.getStringResource(Assistant.this, "speech_recognition_permission_required"));
        }

        
    }

    public void getLocation(String text, int category) {
        final Timer timer = new Timer();
        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                timer.cancel();
                
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + location.getLatitude() + "," + location.getLongitude()));
                if (location.getProvider().equals(LocationManager.GPS_PROVIDER))
                    speak(Translations.getAnswer(Assistant.this, "starting_map"));
                else
                    speak(Translations.getAnswer(Assistant.this, "starting_map_not_accurate_location"));
                isBackgroundTaskRunning = false;
                new ExecuteIntent(intent).execute();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        try {
            if (gpsEnabled) locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null);
            else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null);
            else {
                speak(Translations.getAnswer(Assistant.this, "location_off"));
                return;
            }
        } catch (SecurityException e) {
            speak(Translations.getAnswer(Assistant.this, "location_off"));
            return;
        }

        
        microphoneButton.changeState(MicrophoneButton.State.INACTIVE);

        

        noNextRecognitionAttempt = true;
        if (gpsEnabled) speak(Translations.getStringResource(Assistant.this, "wait"));
        else assistantAnswer.setText("...");

        isBackgroundTaskRunning = true;

        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (assistantAnswer.getText().toString().endsWith("..."))
                                assistantAnswer.setText(assistantAnswer.getText().subSequence(0, assistantAnswer.length() - 2));
                            else assistantAnswer.setText(assistantAnswer.getText() + ".");
                        }
                    });
                }
            }, 1000, 1000);

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                locationManager.removeUpdates(listener);
                                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null);
                                else {
                                    timer.cancel();
                                    speak(Translations.getAnswer(Assistant.this, "location_failed_timeout"));
                                    isBackgroundTaskRunning = false;
                                }
                            } catch (SecurityException e) {
                                speak(Translations.getAnswer(Assistant.this, "location_off"));
                                isBackgroundTaskRunning = false;
                            }
                        }
                    });
                }
            }, 180000);
        } catch (Exception e) {
            if (assistantSettings.getBoolean("debug_mode", false))
                Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
            isBackgroundTaskRunning = false;
        }
    }

    public void turnOffSilentMode(String text, int category) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager.getRingerMode() != savedValues.getInt("old_ringer_mode", AudioManager.RINGER_MODE_NORMAL)) {
            audioManager.setRingerMode(savedValues.getInt("old_ringer_mode", AudioManager.RINGER_MODE_NORMAL));
            speak(Translations.getAnswer(Assistant.this, "silent_mode_off"));
        } else speak(Translations.getAnswer(Assistant.this, "silent_mode_already_off"));
    }

    public void turnOnSilentMode(String text, int category) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int newRingerMode = (assistantSettings.getBoolean("vibration_mode", true) ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_SILENT);
        if (audioManager.getRingerMode() != newRingerMode) {
            savedValues.edit().putInt("old_ringer_mode", audioManager.getRingerMode()).apply();
            audioManager.setRingerMode(newRingerMode);
            speak(Translations.getAnswer(Assistant.this, "silent_mode_on"));
        } else speak(Translations.getAnswer(Assistant.this, "silent_mode_already_on"));
    }

    public void setTimer(String text, int category) {
        Intent intent = new Intent(Assistant.this, TimerService.class);
        if (!TimerService.isWorking) {
            int seconds = (int) Math.ceil((processTime(text, false).getTimeInMillis() - System.currentTimeMillis()) / 1000.0);
            if (seconds > 0) {
                intent.putExtra("s", seconds);
                startService(intent);

                int hoursLeft = seconds / 3600;
                int minutesLeft = (seconds / 60) % 60;
                int secondsLeft = seconds % 60;

                String toBeSaid = "";

                if (hoursLeft > 0)
                    toBeSaid += Translations.getAnswer(Assistant.this, "hours_remaining").replace("%%HOURS%%", Integer.toString(hoursLeft)) + ", ";
                if (minutesLeft > 0)
                    toBeSaid += Translations.getAnswer(Assistant.this, "minutes_remaining").replace("%%MINUTES%%", Integer.toString(minutesLeft)) + ", ";
                if (secondsLeft > 0)
                    toBeSaid += Translations.getAnswer(Assistant.this, "seconds_remaining").replace("%%SECONDS%%", Integer.toString(secondsLeft)) + ", ";

                toBeSaid = toBeSaid.substring(0, toBeSaid.length() - 2);
                if (!toBeSaid.endsWith(".")) toBeSaid += ".";

                speak(Translations.getAnswer(Assistant.this, "timer_set") + " " + toBeSaid);
            } else
                speak(Translations.getAnswer(Assistant.this, "no_timer_set_or_no_time_provided"));
        } else {
            stopService(intent);
            speak(Translations.getAnswer(Assistant.this, "timer_off"));
        }
    }

    public void currentDate(String text, int category) {
        speak(DateFormat.format("dd.MM.yyyy", new Date()) + ".");
    }

    public void currentDayOfWeek(String text, int category) {
        Calendar calendar = Calendar.getInstance();
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case 1:
                speak(Translations.getStringResource(Assistant.this, "sunday") + ".");
                break;

            case 2:
                speak(Translations.getStringResource(Assistant.this, "monday") + ".");
                break;

            case 3:
                speak(Translations.getStringResource(Assistant.this, "tuesday") + ".");
                break;

            case 4:
                speak(Translations.getStringResource(Assistant.this, "wednesday") + ".");
                break;

            case 5:
                speak(Translations.getStringResource(Assistant.this, "thursday") + ".");
                break;

            case 6:
                speak(Translations.getStringResource(Assistant.this, "friday") + ".");
                break;

            case 7:
                speak(Translations.getStringResource(Assistant.this, "saturday") + ".");
                break;
        }
    }

    public void currentHour(String text, int category) {
        Date date = new Date();
        String hour = (String) DateFormat.format("hh", date);
        String minute = (String) DateFormat.format("mm", date);
        String AMorPM = (String) DateFormat.format("a", date);
        if (AMorPM.equalsIgnoreCase("pm"))
        {
            if (!hour.equals("12"))
                speak(Integer.toString(Integer.parseInt(hour) + 12) + ":" + minute + ".");
            else speak(hour + ":" + minute + ".");
        } else if (hour.equals("12")) speak("00:" + minute + ".");
        else speak(hour + ":" + minute + ".");
    }

    private String messageSender;
    private String messageContent;
    private String messageSenderNumber;
    private boolean atRequest = false;

    public void readMessage(String text, int category) {
        if (category == 0) {
            atRequest = false;
            if (isAnswerYes(text)) {
                assistantAnswer.setText(Translations.getAnswer(Assistant.this, "received_message_content_answer"));
                showSource(R.layout.internal_read_message, false, messageSender, messageContent);
                speak(Translations.getAnswer(Assistant.this, "received_sms_content").replace("%%MESSAGE%%", messageContent), false);
                currentCategory = AssistantCategory.READ_SMS_ANSWER_QUESTION;
                startRecording();
            } else if (isAnswerNo(text)) {
                currentCategory = AssistantCategory.READY;
                noNextRecognitionAttempt = true;
                speak(Translations.getAnswer(Assistant.this, "fine"));
                new TurnOffAfterSpeaking().execute();
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        } else if (category == 1) {
            if (isAnswerYes(text)) {
                ArrayList<String> data = new ArrayList<>();
                data.add("sendMessage");
                data.add(messageSender);
                data.add(messageSenderNumber);
                sendMessage(messageSender, 0, data);
            } else if (isAnswerNo(text)) {
                currentCategory = AssistantCategory.READY;
                speak(Translations.getAnswer(Assistant.this, "fine"));
                if (!atRequest) {
                    noNextRecognitionAttempt = true;
                    new TurnOffAfterSpeaking().execute();
                } else atRequest = false;
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        } else if (category == 2) {
            try {
                Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        messageSenderNumber = cursor.getString(cursor.getColumnIndex("address"));
                        messageSender = processNumber(messageSenderNumber);
                        messageContent = cursor.getString(cursor.getColumnIndex("body"));
                    } else {
                        speak(Translations.getAnswer(Assistant.this, "read_last_sms_error"));
                        return;
                    }
                    cursor.close();
                } else {
                    speak(Translations.getAnswer(Assistant.this, "read_last_sms_error"));
                    return;
                }
            } catch (Exception e) {
                speak(Translations.getAnswer(Assistant.this, "read_last_sms_error"));
                return;
            }

            String name = getContactName(messageSenderNumber);
            if (name != null) messageSender = name;

            atRequest = true;
            assistantAnswer.setText(Translations.getAnswer(Assistant.this, "last_sms_content_answer"));
            showSource(R.layout.internal_read_message, false, messageSender, messageContent);
            speak(Translations.getAnswer(Assistant.this, "last_sms_content").replace("%%SENDER%%", messageSender).replace("%%MESSAGE%%", messageContent), false);
            currentCategory = AssistantCategory.READ_SMS_ANSWER_QUESTION;
            startRecording();
        }
    }

    private int noteId = -1;
    private boolean titleToBeChanged;
    private String newNoteTitle;
    private String newNoteContent;

    public void editNote(String text, int category) {
        if (category == 0) {
            String title = extractName("editNote", text, false);

            if (!title.equals("")) {
                int howManyNotes = savedValues.getInt("notes_count", 0);
                title = processText(title).toLowerCase().replace(".", "").replace(",", "").replace("!", "").replace("?", "").replace("(", "").replace(")", "").replace(":", "").replace(";", "").replace("%", "");

                for (int i = 1; i <= howManyNotes; i++) {
                    String toBeChecked = savedValues.getString("note" + Integer.toString(i), "").toLowerCase().replace(".", "").replace(",", "").replace("!", "").replace("?", "").replace("(", "").replace(")", "").replace(":", "").replace(";", "").replace("%", "");
                    if (toBeChecked.equals(title)) {
                        noteId = i;
                        break;
                    }
                }

                if (noteId != -1) {
                    speak(Translations.getAnswer(Assistant.this, "note_correction_type_question"));
                    currentCategory = AssistantCategory.EDIT_NOTE_TYPE;
                    startRecording();
                }
            } else speak(Translations.getAnswer(Assistant.this, "no_note_title_error"));
        } else if (category == 1) {
            if (text.contains("1"))
            {
                speak(Translations.getAnswer(Assistant.this, "new_note_title_request").replace("%%TITLE%%", savedValues.getString("note" + Integer.toString(noteId), "")));
                currentCategory = AssistantCategory.EDIT_NOTE_TITLE;
                startRecording();
            } else if (text.contains("2"))
            {
                speak(Translations.getAnswer(Assistant.this, "new_note_content_request").replace("%%TITLE%%", savedValues.getString("note" + Integer.toString(noteId), "")));
                currentCategory = AssistantCategory.EDIT_NOTE_CONTENT;
                startRecording();
            } else {
                speak(Translations.getAnswer(Assistant.this, "position_number_required") + "\n" + Translations.getAnswer(Assistant.this, "note_correction_type_question"));
                startRecording();
            }
        } else if (category == 2) {
            showSource(R.layout.internal_note, false, text, Translations.getStringResource(Assistant.this, "no_changes"));
            assistantAnswer.setText(Translations.getAnswer(Assistant.this, "note_title_change_confirmation_answer"));
            speak(Translations.getAnswer(Assistant.this, "note_title_change_confirmation_question").replace("%%TITLE%%", savedValues.getString("note" + Integer.toString(noteId), "")).replace("%%NEW_TITLE%%", text), false);
            currentCategory = AssistantCategory.EDIT_NOTE_CONFIRMATION;
            titleToBeChanged = true;
            newNoteTitle = text;
            startRecording();
        } else if (category == 3) {
            newNoteContent = text;
            showSource(R.layout.internal_note, false, savedValues.getString("note" + Integer.toString(noteId), ""), newNoteContent);
            assistantAnswer.setText(Translations.getAnswer(Assistant.this, "note_content_change_confirmation_answer"));
            speak(Translations.getAnswer(Assistant.this, "note_content_change_confirmation_question").replace("%%TITLE%%", savedValues.getString("note" + Integer.toString(noteId), "")).replace("%%NEW_CONTENT%%", originalRecognisedText), false);
            currentCategory = AssistantCategory.EDIT_NOTE_CONFIRMATION;
            titleToBeChanged = false;
            startRecording();
        } else if (category == 4) {
            if (isAnswerYes(text)) {
                SharedPreferences.Editor edit = savedValues.edit();
                if (titleToBeChanged)
                    edit.putString("note" + Integer.toString(noteId), newNoteTitle);
                else edit.putString("note_content" + Integer.toString(noteId), newNoteContent);
                edit.apply();
                speak(Translations.getAnswer(Assistant.this, "note_changed"));
                currentCategory = AssistantCategory.READY;
            } else if (isAnswerNo(text)) {
                speak(Translations.getAnswer(Assistant.this, "note_not_changed"));
                currentCategory = AssistantCategory.READY;
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        }
    }

    public void deleteNote(String text, int category) {
        String title = extractName("deleteNote", text, false);

        if (!title.equals("")) {
            int howManyNotes = savedValues.getInt("notes_count", 0);
            title = processText(title).toLowerCase().replace(".", "").replace(",", "").replace("!", "").replace("?", "").replace("(", "").replace(")", "").replace(":", "").replace(";", "").replace("%", "");

            boolean deleted = false;
            for (int i = 1; i <= howManyNotes; i++) {
                String toBeChecked = savedValues.getString("note" + Integer.toString(i), "").toLowerCase().replace(".", "").replace(",", "").replace("!", "").replace("?", "").replace("(", "").replace(")", "").replace(":", "").replace(";", "").replace("%", "");
                if (toBeChecked.equals(title)) {
                    SharedPreferences.Editor editor = savedValues.edit();
                    editor.remove("note" + Integer.toString(i));
                    editor.remove("note_content" + Integer.toString(i));
                    editor.putInt("notes_count", savedValues.getInt("notes_count", 0) - 1);
                    for (int j = i + 1; j <= howManyNotes; j++) {
                        editor.putString("note" + Integer.toString(j - 1), savedValues.getString("note" + Integer.toString(j), ""));
                        editor.putString("note_content" + Integer.toString(j - 1), savedValues.getString("note_content" + Integer.toString(j), ""));
                        editor.remove("note" + Integer.toString(j));
                        editor.remove("note_content" + Integer.toString(j));
                    }

                    editor.apply();
                    deleted = true;
                    break;
                }
            }

            if (deleted) speak(Translations.getAnswer(Assistant.this, "note_deleted"));
            else speak(Translations.getAnswer(Assistant.this, "note_not_found"));
        } else speak(Translations.getAnswer(Assistant.this, "no_note_title_error"));
    }

    public void showNote(String text, int category) {
        String title = extractName("showNote", text, false);

        if (!title.equals("")) {
            int howManyNotes = savedValues.getInt("notes_count", 0);
            title = processText(title).toLowerCase().replace(".", "").replace(",", "").replace("!", "").replace("?", "").replace("(", "").replace(")", "").replace(":", "").replace(";", "").replace("%", "");

            String titleToBeRead = null;
            String contentToBeRead = null;

            for (int i = 1; i <= howManyNotes; i++) {
                String toBeChecked = savedValues.getString("note" + Integer.toString(i), "");
                if (!toBeChecked.equals("")) {
                    toBeChecked = toBeChecked.toLowerCase().replace(".", "").replace(",", "").replace("!", "").replace("?", "").replace("(", "").replace(")", "").replace(":", "").replace(";", "").replace("%", "");
                    if (toBeChecked.equals(title)) {
                        titleToBeRead = toBeChecked;
                        contentToBeRead = savedValues.getString("note_content" + Integer.toString(i), "");
                        break;
                    }
                }
            }

            if (titleToBeRead != null) {
                showSource(R.layout.internal_note, false, titleToBeRead, contentToBeRead);
                assistantAnswer.setText(Translations.getAnswer(Assistant.this, "note"));
                speak(Translations.getAnswer(Assistant.this, "note_tts").replace("%%TITLE%%", titleToBeRead).replace("%%CONTENT%%", contentToBeRead), false);
            } else speak(Translations.getAnswer(Assistant.this, "note_not_found"));
        } else speak(Translations.getAnswer(Assistant.this, "no_note_title_error"));
    }

    public void showAllNotes(String text, int category) {
        int howManyNotes = savedValues.getInt("notes_count", 0);
        if (howManyNotes > 0) {
            noNextRecognitionAttempt = true;
            String[] titles = new String[howManyNotes];
            for (int i = 1; i <= howManyNotes; i++) {
                titles[i - 1] = savedValues.getString("note" + Integer.toString(i), "");
            }

            showSource(R.layout.internal_all_notes, false, (Object) titles);
            assistantAnswer.setText(Translations.getAnswer(Assistant.this, "all_notes"));
            String textToBeRead = Translations.getAnswer(Assistant.this, "all_notes_number_tts").replace("%%NUMBER%%", Integer.toString(titles.length)) + " ";
            for (int i = 0; i < titles.length; i++) {
                if (i != titles.length - 1) textToBeRead += titles[i] + ", ";
                else textToBeRead += titles[i] + ". ";
            }
            speak(textToBeRead, false);
        } else speak(Translations.getAnswer(Assistant.this, "no_notes_saved"));
    }

    private String noteTitle;
    private String noteContent;
    private String noteOriginalContent;

    public void createNote(String text, int category) {
        if (category == 0) {
            String title = extractName("createNote", text, false);

            if (!title.equals("")) {
                speak(Translations.getAnswer(Assistant.this, "note_content_request"));
                noteTitle = processText(title);
                currentCategory = AssistantCategory.CREATE_NOTE_CONTENT;
                startRecording();
            } else {
                speak(Translations.getAnswer(Assistant.this, "note_title_request"));
                currentCategory = AssistantCategory.CREATE_NOTE_TITLE;
                startRecording();
            }
        } else if (category == 1) {
            noteTitle = processText(text);
            if (noteContent == null) {
                speak(Translations.getAnswer(Assistant.this, "note_content_request"));
                currentCategory = AssistantCategory.CREATE_NOTE_CONTENT;
                startRecording();
            } else {
                assistantAnswer.setText(Translations.getAnswer(Assistant.this, "note_save_confirmation_answer"));
                showSource(R.layout.internal_note, false, noteTitle, noteContent);
                speak(Translations.getAnswer(Assistant.this, "note_save_confirmation_question").replace("%%TITLE%%", noteTitle).replace("%%CONTENT%%", noteOriginalContent), false);
                currentCategory = AssistantCategory.CREATE_NOTE_CONFIRMATION;
                startRecording();
            }
        } else if (category == 2) {
            noteContent = text;
            noteOriginalContent = originalRecognisedText;
            assistantAnswer.setText(Translations.getAnswer(Assistant.this, "note_save_confirmation_answer"));
            showSource(R.layout.internal_note, false, noteTitle, noteContent);
            speak(Translations.getAnswer(Assistant.this, "note_save_confirmation_question").replace("%%TITLE%%", noteTitle).replace("%%CONTENT%%", noteOriginalContent), false);
            currentCategory = AssistantCategory.CREATE_NOTE_CONFIRMATION;
            startRecording();
        } else if (category == 3) {
            if (isAnswerYes(text)) {
                SharedPreferences.Editor editor = savedValues.edit();
                int howManyNotes = savedValues.getInt("notes_count", 0);
                editor.putString("note" + Integer.toString(howManyNotes + 1), noteTitle);
                editor.putString("note_content" + Integer.toString(howManyNotes + 1), noteContent);
                editor.putInt("notes_count", howManyNotes + 1);
                editor.apply();
                speak(Translations.getAnswer(Assistant.this, "note_saved"));
                currentCategory = AssistantCategory.READY;
            } else if (isAnswerNo(text)) {
                speak(Translations.getAnswer(Assistant.this, "note_correction_type_question_when_creating"));
                currentCategory = AssistantCategory.CREATE_NOTE_CORRECTION_TYPE;
                startRecording();
            } else {
                speak(answerNoUnderstanding(true));
                startRecording();
            }
        } else if (category == 5) {
            if (text.contains("1")) {
                speak(Translations.getAnswer(Assistant.this, "fine"));
                currentCategory = AssistantCategory.READY;
            } else if (text.contains("2"))
            {
                speak(Translations.getAnswer(Assistant.this, "new_note_title_request"));
                currentCategory = AssistantCategory.CREATE_NOTE_TITLE;
                startRecording();
            } else if (text.contains("3"))
            {
                speak(Translations.getAnswer(Assistant.this, "new_note_content_request"));
                currentCategory = AssistantCategory.CREATE_NOTE_CONTENT;
                startRecording();
            } else {
                speak(Translations.getAnswer(Assistant.this, "position_number_required") + "\n" + Translations.getAnswer(Assistant.this, "note_correction_type_question_when_creating"));
                startRecording();
            }
        }
    }

    public void showMap(String text, int category) {
        String place = extractName("showMap", text, false);

        if (!place.equals("")) {
            speak(Translations.getAnswer(Assistant.this, "starting_map"));
            try {
                new ExecuteIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + URLEncoder.encode(place, "UTF-8")))).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else speak(Translations.getAnswer(Assistant.this, "no_place_provided"));
    }

    public boolean isAnswerTurnOn(String text) {
        String[] turnOnWords = Translations.getStringResource(Assistant.this, "turn_on_words").split("\\|");

        for (int i = 0; i < turnOnWords.length; i++) {
            if (text.contains(turnOnWords[i])) return true;
        }

        return false;
    }

    public boolean isAnswerTurnOff(String text) {
        String[] turnOffWords = Translations.getStringResource(Assistant.this, "turn_off_words").split("\\|");

        for (int i = 0; i < turnOffWords.length; i++) {
            if (text.contains(turnOffWords[i])) return true;
        }

        return false;
    }

    public void changeWifiBluetoothState(String text, int category) {
        if (text.contains("wifi")) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (isAnswerTurnOn(text) && wifiManager.isWifiEnabled())
                speak(Translations.getAnswer(Assistant.this, "wifi_already_on"));
            else if (isAnswerTurnOn(text) && !wifiManager.isWifiEnabled()) {
                speak(Translations.getAnswer(Assistant.this, "wifi_on"));
                wifiManager.setWifiEnabled(true);
            } else if (isAnswerTurnOff(text) && !wifiManager.isWifiEnabled())
                speak(Translations.getAnswer(Assistant.this, "wifi_already_off"));
            else {
                speak(Translations.getAnswer(Assistant.this, "wifi_off"));
                wifiManager.setWifiEnabled(false);
            }
        } else if (text.contains("bluetooth")) {
            BluetoothAdapter bluetoothManager = BluetoothAdapter.getDefaultAdapter();
            if (isAnswerTurnOn(text) && bluetoothManager.isEnabled())
                speak(Translations.getAnswer(Assistant.this, "bluetooth_already_on"));
            else if (isAnswerTurnOn(text) && !bluetoothManager.isEnabled()) {
                speak(Translations.getAnswer(Assistant.this, "bluetooth_on"));
                bluetoothManager.enable();
            } else if (isAnswerTurnOff(text) && !bluetoothManager.isEnabled())
                speak(Translations.getAnswer(Assistant.this, "bluetooth_already_off"));
            else {
                speak(Translations.getAnswer(Assistant.this, "bluetooth_off"));
                bluetoothManager.disable();
            }
        } else
            speak(Translations.getAnswer(Assistant.this, "no_mode_provided"));
    }

    public void timerRemaining(String text, int category) {
        if (TimerService.isWorking) {
            bindService(new Intent(Assistant.this, TimerService.class), new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    TimerService timerService = ((TimerService.TimerServiceBinder) service).getService();
                    int hoursLeft = timerService.getHoursLeft();
                    int minutesLeft = timerService.getMinutesLeft();
                    int secondsLeft = timerService.getSecondsLeft();

                    String toBeSaid = "";

                    if (hoursLeft > 0)
                        toBeSaid += Translations.getAnswer(Assistant.this, "hours_remaining").replace("%%HOURS%%", Integer.toString(hoursLeft)) + ", ";
                    if (minutesLeft > 0)
                        toBeSaid += Translations.getAnswer(Assistant.this, "minutes_remaining").replace("%%MINUTES%%", Integer.toString(minutesLeft)) + ", ";
                    if (secondsLeft > 0)
                        toBeSaid += Translations.getAnswer(Assistant.this, "seconds_remaining").replace("%%SECONDS%%", Integer.toString(secondsLeft)) + ", ";

                    toBeSaid = toBeSaid.substring(0, toBeSaid.length() - 2);
                    if (!toBeSaid.endsWith(".")) toBeSaid += ".";

                    speak(processText(toBeSaid));

                    unbindService(this);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            }, BIND_AUTO_CREATE);
        } else speak(Translations.getAnswer(Assistant.this, "no_timer_set"));
    }

    private String howManyDaysAhead;

    public void weatherForecast(String text, int category) {
        String place;
        double latitude, longitude;
        if (category == 0) {
            place = extractName("weatherForecast", text, false);

            howManyDaysAhead = Integer.toString(returnDaysForward(text));

            if (howManyDaysAhead.equals("-1")) howManyDaysAhead = "0";

            if (Integer.parseInt(howManyDaysAhead) > 8)
                speak(Translations.getAnswer(Assistant.this, "weather_forecast_7_days_max"));
            else {
                if (!place.equals("")) {
                    assistantAnswer.setText("...");
                    microphoneButton.changeState(MicrophoneButton.State.INACTIVE);

                    

                    try {
                        Geocoder geo = new Geocoder(Assistant.this);
                        Address address;
                        try {
                            address = geo.getFromLocationName(place, 1).get(0);
                        } catch (IndexOutOfBoundsException e) {
                            speak(Translations.getAnswer(Assistant.this, "place_not_found"));
                            return;
                        }
                        latitude = address.getLatitude();
                        longitude = address.getLongitude();
                        NetworkHandler.query(Assistant.this, "https://api.darksky.net/forecast/" + getString(R.string.darksky_api_key) + "/" + latitude + "," + longitude + "?lang=" + Translations.getStringResource(Assistant.this, "short_language_code") + "&units=si&exclude=minutely,hourly,alerts,flags", "GET", new DownloadData("pogoda"), null, null, null);
                    } catch (UnsupportedEncodingException e) {
                        if (assistantSettings.getBoolean("debug_mode", false))
                            Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        if (assistantSettings.getBoolean("debug_mode", false))
                            Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                        speak(Translations.getAnswer(Assistant.this, "could_not_determine_place_coordinates"));
                    }
                } else {
                    LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                    if (!locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                        speak(Translations.getAnswer(Assistant.this, "weather_forecast_place_question"));
                        currentCategory = AssistantCategory.WEATHER_PLACE;
                        startRecording();
                    } else {
                        try {
                            Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                NetworkHandler.query(Assistant.this, "https://api.darksky.net/forecast/3be1d1d88c71d5ecf44cb176bcf167d4/" + latitude + "," + longitude + "?lang=" + Translations.getStringResource(Assistant.this, "short_language_code") + "&units=si&exclude=minutely,hourly,alerts,flags", "GET", new DownloadData("pogoda"), null, null, null);
                            } else {
                                speak(Translations.getAnswer(Assistant.this, "weather_forecast_place_question"));
                                currentCategory = AssistantCategory.WEATHER_PLACE;
                                startRecording();
                            }
                        } catch (SecurityException e) {
                            speak(Translations.getAnswer(Assistant.this, "weather_forecast_place_question"));
                            currentCategory = AssistantCategory.WEATHER_PLACE;
                            startRecording();
                        }
                    }
                }
            }
        } else if (category == 1) {
            place = text;
            assistantAnswer.setText("...");
            microphoneButton.changeState(MicrophoneButton.State.INACTIVE);
            
            try {
                Geocoder geo = new Geocoder(Assistant.this);
                Address address;
                try {
                    address = geo.getFromLocationName(place, 1).get(0);
                } catch (IndexOutOfBoundsException e) {
                    speak(Translations.getAnswer(Assistant.this, "place_not_found"));
                    currentCategory = AssistantCategory.READY;
                    return;
                }
                latitude = address.getLatitude();
                longitude = address.getLongitude();
                NetworkHandler.query(Assistant.this, "https://api.darksky.net/forecast/3be1d1d88c71d5ecf44cb176bcf167d4/" + latitude + "," + longitude + "?lang=" + Translations.getStringResource(Assistant.this, "short_language_code") + "&units=si&exclude=minutely,hourly,alerts,flags", "GET", new DownloadData("pogoda"), null, null, null);
            } catch (UnsupportedEncodingException e) {
                if (assistantSettings.getBoolean("debug_mode", false))
                    Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                if (assistantSettings.getBoolean("debug_mode", false))
                    Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                speak(Translations.getAnswer(Assistant.this, "could_not_determine_place_coordinates"));
            }
            currentCategory = AssistantCategory.READY;
        }
    }

    public int returnMonth(String command) {
        command = command.toLowerCase();
        String[] months = Translations.getStringArrayResource(Assistant.this, "months");
        for (int i = 0; i < months.length; i++) {
            String[] words = months[i].split("\\|");
            for (String word : words) {
                if (command.contains(word.toLowerCase())) return i;
            }
        }

        return -1;
    }

    public int returnDaysForward(String command) {
        command = command.toLowerCase();
        String[] items = Translations.getStringArrayResource(Assistant.this, "days_forward");
        int index = -1;
        for (int i = 0; i < items.length; i++) {
            String[] words = items[i].split("\\|");
            for (String word : words) {
                if (command.contains(word.toLowerCase())) index = i;
            }
        }

        if (index != -1) return index;

        Calendar calendar = processTime(command, true);
        if (calendar != null)
            return Math.round((float) (calendar.getTimeInMillis() - System.currentTimeMillis()) / (float) (24 * 60 * 60 * 1000));
        else return -1;
    }

    public int returnDaysBackward(String command) {
        command = command.toLowerCase();
        String[] items = Translations.getStringArrayResource(Assistant.this, "days_backward");
        int index = -1;
        for (int i = 0; i < items.length; i++) {
            String[] words = items[i].split("\\|");
            for (String word : words) {
                if (command.contains(word.toLowerCase())) index = i + 1;
            }
        }

        if (index != -1) return index;

        ArrayList<Integer> numbers = returnNumbers(command);

        return (numbers == null || numbers.size() == 0 ? 1 : numbers.get(0));
    }

    public void wikipedia(String text, int category) {
        String toBeChecked = extractName("wikipedia", text, false);
        try {
            String url = "https://" + Translations.getStringResource(Assistant.this, "short_language_code") + ".wikipedia.org/w/api.php?format=json&action=opensearch&search=" + URLEncoder.encode(toBeChecked, "UTF-8") + "&limit=1&namespace=0&profile=fuzzy&redirects=resolve";
            NetworkHandler.query(Assistant.this, url, "GET", new DownloadData("wikipedia", url), null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeAlarm(String text, int category) {
        Calendar calendar = processTime(text, false);
        SharedPreferences alarmsPreferences = getSharedPreferences("alarms", MODE_PRIVATE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (calendar != null) {
            if (calendar.getTimeInMillis() <= System.currentTimeMillis())
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (alarmsPreferences.contains(Long.toString(calendar.getTimeInMillis()))) {
                try {
                    int alarmIdentifier = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), alarmIdentifier, new Intent(getApplicationContext(), AlarmActivity.class).putExtra("alarm", true), PendingIntent.FLAG_UPDATE_CURRENT);
                    alarmManager.cancel(pendingIntent);
                    alarmsPreferences.edit().remove(Long.toString(calendar.getTimeInMillis())).apply();
                    if (android.os.Build.VERSION.SDK_INT < 21 && alarmsPreferences.getAll().keySet().size() == 0)
                        sendBroadcast(new Intent("android.intent.action.ALARM_CHANGED").putExtra("alarmSet", false));
                    speak(Translations.getAnswer(Assistant.this, "alarm_turned_off").replace("%%TIME%%", DateFormat.format("HH:mm", calendar.getTime())));
                } catch (Exception e) {
                    e.printStackTrace();
                    speak(Translations.getAnswer(Assistant.this, "alarm_turning_off_error"));
                }
            } else speak(Translations.getAnswer(Assistant.this, "alarm_provided_not_set"));
        } else {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
                    if (alarmClockInfo != null) {
                        GregorianCalendar alarmCalendar = new GregorianCalendar();
                        alarmCalendar.setTimeInMillis(alarmClockInfo.getTriggerTime());
                        int alarmIdentifier = alarmCalendar.get(Calendar.HOUR_OF_DAY) * 100 + alarmCalendar.get(Calendar.MINUTE);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), alarmIdentifier, new Intent(getApplicationContext(), AlarmActivity.class).putExtra("alarm", true), PendingIntent.FLAG_UPDATE_CURRENT);
                        alarmManager.cancel(pendingIntent);
                        alarmsPreferences.edit().remove(Long.toString(alarmCalendar.getTimeInMillis())).apply();
                        speak(Translations.getAnswer(Assistant.this, "alarm_turned_off").replace("%%TIME%%", DateFormat.format("HH:mm", alarmCalendar.getTime())));
                    } else speak(Translations.getAnswer(Assistant.this, "no_alarms_set"));
                } else {
                    long closestAlarmTime = Long.MAX_VALUE;
                    Set<String> keySet = alarmsPreferences.getAll().keySet();

                    if (keySet.size() > 0) {
                        for (String alarm : keySet) {
                            long alarmTime = Long.parseLong(alarm);
                            if (alarmTime < closestAlarmTime) closestAlarmTime = alarmTime;
                        }

                        GregorianCalendar alarmCalendar = new GregorianCalendar();
                        alarmCalendar.setTimeInMillis(closestAlarmTime);
                        int alarmIdentifier = alarmCalendar.get(Calendar.HOUR_OF_DAY) * 100 + alarmCalendar.get(Calendar.MINUTE);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), alarmIdentifier, new Intent(getApplicationContext(), AlarmActivity.class).putExtra("alarm", true), PendingIntent.FLAG_UPDATE_CURRENT);
                        alarmManager.cancel(pendingIntent);
                        alarmsPreferences.edit().remove(Long.toString(alarmCalendar.getTimeInMillis())).apply();
                        if (alarmsPreferences.getAll().keySet().size() == 0)
                            sendBroadcast(new Intent("android.intent.action.ALARM_CHANGED").putExtra("alarmSet", false));
                        speak(Translations.getAnswer(Assistant.this, "alarm_turned_off").replace("%%TIME%%", DateFormat.format("HH:mm", alarmCalendar.getTime())));
                    } else
                        speak(Translations.getAnswer(Assistant.this, "no_alarms_set_in_assistant"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                speak(Translations.getAnswer(Assistant.this, "alarm_turning_off_error"));
            }
        }
    }

    public void setAlarm(String text, int category) {
        if (category == 0) {
            Calendar calendar = processTime(text, false);

            if (calendar == null) {
                speak(Translations.getAnswer(Assistant.this, "alarm_time_question"));
                currentCategory = AssistantCategory.ALARM_TIME;
                startRecording();
            } else {
                if (calendar.getTimeInMillis() <= System.currentTimeMillis())
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                GregorianCalendar currentDate = new GregorianCalendar();
                if (calendar.get(Calendar.DAY_OF_YEAR) != currentDate.get(Calendar.DAY_OF_YEAR) && calendar.get(Calendar.HOUR_OF_DAY) >= currentDate.get(Calendar.HOUR_OF_DAY) && calendar.get(Calendar.MINUTE) >= currentDate.get(Calendar.MINUTE))
                    speak(Translations.getAnswer(Assistant.this, "alarm_24_h_error"));
                else {

                    try {
                        int alarmIdentifier = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), alarmIdentifier, new Intent(getApplicationContext(), AlarmActivity.class).putExtra("alarm", true), PendingIntent.FLAG_UPDATE_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        if (android.os.Build.VERSION.SDK_INT >= 21)
                            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), null), pendingIntent);
                        else {
                            if (android.os.Build.VERSION.SDK_INT >= 19)
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                            else
                                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

                            sendBroadcast(new Intent("android.intent.action.ALARM_CHANGED").putExtra("alarmSet", true));
                        }
                        getSharedPreferences("alarms", MODE_PRIVATE).edit().putBoolean(Long.toString(calendar.getTimeInMillis()), true).apply();
                        speak(Translations.getAnswer(Assistant.this, "alarm_set").replace("%%TIME%%", DateFormat.format("HH:mm", calendar.getTime())));
                    } catch (Exception e) {
                        if (assistantSettings.getBoolean("debug_mode", false))
                            Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                        speak(Translations.getAnswer(Assistant.this, "setting_alarm_error"));
                        currentCategory = AssistantCategory.READY;
                    }
                }
            }
        } else if (category == 1) {
            Calendar calendar = processTime(text, false);

            if (calendar == null) {
                speak(Translations.getAnswer(Assistant.this, "time_processing_error"));
                startRecording();
            } else {
                if (calendar.getTimeInMillis() <= System.currentTimeMillis())
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                GregorianCalendar currentDate = new GregorianCalendar();
                if (calendar.get(Calendar.DAY_OF_YEAR) != currentDate.get(Calendar.DAY_OF_YEAR) && calendar.get(Calendar.HOUR_OF_DAY) >= currentDate.get(Calendar.HOUR_OF_DAY) && calendar.get(Calendar.MINUTE) >= currentDate.get(Calendar.MINUTE))
                    speak(Translations.getAnswer(Assistant.this, "alarm_24_h_error"));
                else {
                    try {
                        int alarmIdentifier = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), alarmIdentifier, new Intent(getApplicationContext(), AlarmActivity.class).putExtra("alarm", true), PendingIntent.FLAG_UPDATE_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        if (android.os.Build.VERSION.SDK_INT >= 21)
                            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), null), pendingIntent);
                        else {
                            if (android.os.Build.VERSION.SDK_INT >= 19)
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                            else
                                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

                            sendBroadcast(new Intent("android.intent.action.ALARM_CHANGED").putExtra("alarmSet", true));
                        }
                        getSharedPreferences("alarms", MODE_PRIVATE).edit().putBoolean(Long.toString(calendar.getTimeInMillis()), true).apply();
                        speak(Translations.getAnswer(Assistant.this, "alarm_set").replace("%%TIME%%", DateFormat.format("HH:mm", calendar.getTime())));
                    } catch (Exception e) {
                        if (assistantSettings.getBoolean("debug_mode", false))
                            Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                        speak(Translations.getAnswer(Assistant.this, "setting_alarm_error"));
                        currentCategory = AssistantCategory.READY;
                    }
                }
            }
        }
    }

    public void openBrowser(final String url) {
        if (assistantSettings.getBoolean("default_browser", false))
        {
            new ExecuteIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url))).execute();
        } else {
            showSource(R.layout.external_webpage, true, url);
        }
    }

    public void searchOnInternet(String text, int category) {
        if (category == 0) {
            String query = extractName("searchOnInternet", text, false);

            if (!query.equals("")) {
                noNextRecognitionAttempt = true;
                speak(Translations.getAnswer(Assistant.this, "searching"));
                String url;
                String searchEngine = assistantSettings.getString("search_engine", "Google");
                if (searchEngine.equals("Google"))
                    url = "https://www.google.com/search?q=";
                else if (searchEngine.equals("Bing"))
                    url = "https://www.bing.com/search?q=";
                else if (searchEngine.equals("DuckDuckGo"))
                    url = "https://duckduckgo.com/?q=";
                else url = "https://search.yahoo.com/search?p=";
                try {
                    openBrowser(url + URLEncoder.encode(query, "UTF-8"));
                } catch (Exception e) {
                    openBrowser(url + query);
                }
            } else {
                speak(Translations.getAnswer(Assistant.this, "search_query_question"));
                currentCategory = AssistantCategory.SEARCH_QUERY;
                startRecording();
            }
        } else if (category == 1) {
            currentCategory = AssistantCategory.READY;
            noNextRecognitionAttempt = true;
            speak(Translations.getAnswer(Assistant.this, "searching"));
            String url;
            String searchEngine = assistantSettings.getString("search_engine", "Google");
            if (searchEngine.equals("Google"))
                url = "https://www.google.com/search?q=";
            else if (searchEngine.equals("Bing"))
                url = "https://www.bing.com/search?q=";
            else if (searchEngine.equals("DuckDuckGo"))
                url = "https://duckduckgo.com/?q=";
            else url = "https://search.yahoo.com/search?p=";
            try {
                openBrowser(url + URLEncoder.encode(text, "UTF-8"));
            } catch (Exception e) {
                openBrowser(url + text);
            }
        }
    }

    public void batteryLevel(String text, int category) {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntent = this.registerReceiver(null, intentFilter);
        int level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryLevel = (float) level / (float) scale;
        if (batteryLevel < 1)
            speak(Translations.getAnswer(Assistant.this, "battery_level").replace("%%BATTERY_LEVEL%%", Integer.toString((int) (batteryLevel * 100)) + "%"));
        else speak(Translations.getAnswer(Assistant.this, "battery_fully_charged"));
    }

    public void runApplication(String text, int category) {
        noNextRecognitionAttempt = true;
        assistantAnswer.setText("...");
        microphoneButton.changeState(MicrophoneButton.State.INACTIVE);
        
        if (appList == null && !isObtainingApps) new GetApplications().execute();
        new FindApplication().execute(text);
    }

    public void navigate(String text, int category) {
        if (category == 0) {
            String placeToNavigateTo = extractName("navigate", text, false);
            if (!placeToNavigateTo.equals("")) {
                noNextRecognitionAttempt = true;
                speak(answerNavigate(placeToNavigateTo));
                new Navigate(placeToNavigateTo).execute();
            } else {
                speak(Translations.getAnswer(Assistant.this, "navigation_destiny_question"));
                currentCategory = AssistantCategory.NAVIGATE_PLACE;
                startRecording();
            }
        } else if (category == 1) {
            noNextRecognitionAttempt = true;
            speak(answerNavigate(text));
            currentCategory = AssistantCategory.READY;
            new Navigate(text).execute();
        }
    }

    private String smsRecipient;
    private String smsRecipientNumber;
    private String smsContent;
    private String smsOriginalContent;
    private ArrayList<Integer> simCardsIds;
    private boolean answerToMessage = false;

    public void sendMessage(String text, int category, ArrayList<String> data) {
        if (category == 0) {
            if (data != null) {
                if (data.size() == 3) {
                    smsRecipient = data.get(1);
                    smsRecipientNumber = data.get(2);
                } else if (data.size() == 2) {
                    speak(Translations.getAnswer(Assistant.this, "contact_no_phone_number"));
                    currentCategory = AssistantCategory.READY;
                    return;
                } else smsRecipient = "";

                if (smsRecipient.equals("")) {
                    speak(Translations.getAnswer(Assistant.this, "message_recipient_question"));
                    currentCategory = AssistantCategory.SMS_RECIPIENT;
                    startRecording();
                } else {
                    speak(Translations.getAnswer(Assistant.this, "message_content_question"));
                    currentCategory = AssistantCategory.SMS_CONTENT;
                    startRecording();
                }
            }
        } else if (category == 1) {
            if (data != null) {
                if (currentCategory == AssistantCategory.READ_SMS_ANSWER_QUESTION)
                    answerToMessage = true;

                if (data.size() == 3) {
                    smsRecipient = data.get(1);
                    smsRecipientNumber = data.get(2);
                } else if (data.size() == 2) {
                    speak(Translations.getAnswer(Assistant.this, "contact_no_phone_number"));
                    currentCategory = AssistantCategory.READY;
                    return;
                } else smsRecipient = "";

                if (smsRecipient.equals("")) {
                    speak(answerNoContact());
                    currentCategory = AssistantCategory.READY;
                } else {
                    speak(Translations.getAnswer(Assistant.this, "message_content_question"));
                    currentCategory = AssistantCategory.SMS_CONTENT;
                    startRecording();
                }
            }
        } else if (category == 2) {
            smsContent = text;
            smsOriginalContent = originalRecognisedText;
            assistantAnswer.setText(Translations.getAnswer(Assistant.this, "message_confirmation_answer"));
            showSource(R.layout.internal_send_message, false, smsRecipient, smsRecipientNumber, smsContent);
            speak(Translations.getAnswer(Assistant.this, "message_confirmation").replace("%%RECIPIENT%%", smsRecipient).replace("%%CONTENT%%", smsOriginalContent), false);
            currentCategory = AssistantCategory.SMS_CONFIRMATION;
            startRecording();
        } else if (category == 3) {
            if (!text.equals("")) {
                if (isAnswerYes(text)) {
                    boolean sendDirectlyToExternalApp = assistantSettings.getBoolean("run_message_app", false);
                    if (!sendDirectlyToExternalApp) {
                        if (android.os.Build.VERSION.SDK_INT < 22) {
                            SmsManager smsManager = SmsManager.getDefault();
                            ArrayList<String> toBeSent = smsManager.divideMessage(processText(smsContent));
                            smsManager.sendMultipartTextMessage(smsRecipientNumber, null, toBeSent, null, null);
                            speak(Translations.getAnswer(Assistant.this, "message_sent"));
                        } else {
                            SubscriptionManager subscriptionManager = SubscriptionManager.from(getApplicationContext());
                            List<SubscriptionInfo> subscriptionList = subscriptionManager.getActiveSubscriptionInfoList();
                            simCardsIds = new ArrayList<>();
                            ArrayList<Integer> slots = new ArrayList<>();
                            for (SubscriptionInfo info : subscriptionList) {
                                simCardsIds.add(info.getSubscriptionId());
                                slots.add(info.getSimSlotIndex());
                            }

                            if (simCardsIds.size() <= 1) {
                                SmsManager smsManager = SmsManager.getDefault();
                                ArrayList<String> toBeSent = smsManager.divideMessage(processText(smsContent));
                                smsManager.sendMultipartTextMessage(smsRecipientNumber, null, toBeSent, null, null);
                                speak(Translations.getAnswer(Assistant.this, "message_sent"));
                            } else {
                                String toBeSaid = Translations.getAnswer(Assistant.this, "sim_card_message_question") + "\n";
                                for (int i = 0; i < slots.size(); i++) {
                                    toBeSaid += "(" + (i + 1) + ") " + Translations.getAnswer(Assistant.this, "sim_card_description").replace("%%SLOT_NUMBER%%", Integer.toString(slots.get(i)));
                                    if (i < slots.size() - 1) toBeSaid += ", ";
                                    else toBeSaid += ".\n";
                                }
                                toBeSaid += Translations.getAnswer(Assistant.this, "sim_position_question");
                                speak(toBeSaid);
                                currentCategory = AssistantCategory.SMS_SIM_CHOICE;
                                startRecording();
                            }
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("sms:" + smsRecipientNumber));
                        intent.putExtra("sms_body", processText(smsContent));
                        speak(Translations.getAnswer(Assistant.this, "opening_message_app"));
                        new ExecuteIntent(intent).execute();
                    }

                    if (currentCategory != AssistantCategory.SMS_SIM_CHOICE) {
                        smsRecipient = "";
                        smsContent = "";
                        smsRecipientNumber = "";
                        currentCategory = AssistantCategory.READY;

                        if (answerToMessage) {
                            answerToMessage = false;
                            noNextRecognitionAttempt = true;
                            if (!sendDirectlyToExternalApp) new TurnOffAfterSpeaking().execute();
                        }
                    }
                } else if (isAnswerNo(text)) {
                    speak(Translations.getAnswer(Assistant.this, "message_correction_type_question"));
                    currentCategory = AssistantCategory.SMS_CORRECTION_TYPE;
                    startRecording();
                } else {
                    speak(answerNoUnderstanding(true));
                    startRecording();
                }
            }
        } else if (category == 6) {
            if (text.contains("1")) {
                speak(Translations.getAnswer(Assistant.this, "fine"));
                currentCategory = AssistantCategory.READY;
            } else if (text.contains("2")) {
                speak(Translations.getAnswer(Assistant.this, "message_add_content_request"));
                currentCategory = AssistantCategory.SMS_CORRECTION_ADD_CONTENT;
                startRecording();
            } else if (text.contains("3")) {
                speak(Translations.getAnswer(Assistant.this, "message_new_content_request"));
                currentCategory = AssistantCategory.SMS_CONTENT;
                startRecording();
            } else {
                speak(Translations.getAnswer(Assistant.this, "position_number_required") + "\n" + Translations.getAnswer(Assistant.this, "message_correction_type_question"));
                startRecording();
            }
        } else if (category == 7) {
            smsContent += " " + text;
            smsOriginalContent += " " + originalRecognisedText;
            assistantAnswer.setText(Translations.getAnswer(Assistant.this, "message_confirmation_answer"));
            showSource(R.layout.internal_send_message, false, smsRecipient, smsRecipientNumber, smsContent);
            speak(Translations.getAnswer(Assistant.this, "message_confirmation").replace("%%RECIPIENT%%", smsRecipient).replace("%%CONTENT%%", smsOriginalContent), false);
            currentCategory = AssistantCategory.SMS_CONFIRMATION;
            startRecording();
        } else if (category == 8) {
            int number;
            try {
                number = Integer.parseInt(text.replace("o2", "2").replace("O2", "2"));
            } catch (Exception e) {
                if (assistantSettings.getBoolean("debug_mode", false))
                    Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
                speak(Translations.getAnswer(Assistant.this, "wrong_position_parse_error"));
                startRecording();
                number = -1;
            }

            if (number != -1 && android.os.Build.VERSION.SDK_INT >= 22) {
                int id = simCardsIds.get(number - 1);
                SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(id);
                ArrayList<String> toBeSent = smsManager.divideMessage(processText(smsContent));
                smsManager.sendMultipartTextMessage(smsRecipientNumber, null, toBeSent, null, null);
                speak(Translations.getAnswer(Assistant.this, "message_sent"));

                smsRecipient = "";
                smsContent = "";
                smsRecipientNumber = "";
                currentCategory = AssistantCategory.READY;

                if (answerToMessage) {
                    answerToMessage = false;
                    noNextRecognitionAttempt = true;
                    new TurnOffAfterSpeaking().execute();
                }
            }
        }
    }

    public void makeCall(String text, int category, ArrayList<String> data) {
        if (category == 0) {
            if (data != null) {
                if (data.size() == 3) {
                    noNextRecognitionAttempt = true;
                    speak(answerCall(data.get(1)));
                    currentCategory = AssistantCategory.READY;
                    new Call(data.get(2)).execute();
                } else if (data.size() == 2) {
                    speak(Translations.getAnswer(Assistant.this, "contact_no_phone_number"));
                    currentCategory = AssistantCategory.READY;
                } else {
                    speak(Translations.getAnswer(Assistant.this, "call_recipient_question"));
                    currentCategory = AssistantCategory.CALL_RECIPIENT;
                    startRecording();
                }
            }
        } else if (category == 1) {
            if (data != null) {
                if (data.size() == 3) {
                    noNextRecognitionAttempt = true;
                    speak(answerCall(data.get(1)));
                    currentCategory = AssistantCategory.READY;
                    new Call(data.get(2)).execute();
                } else if (data.size() == 2) {
                    speak(Translations.getAnswer(Assistant.this, "contact_no_phone_number"));
                    currentCategory = AssistantCategory.READY;
                } else {
                    speak(answerNoContact());
                    currentCategory = AssistantCategory.READY;
                }
            }
        }
    }

    public void doCalculations(String text, int category) {
        try {
            String[] mathsWords = Translations.getStringArrayResource(Assistant.this, "math_operations");

            for (int i = 0; i < mathsWords.length; i++) {
                String[] words = mathsWords[i].split("\\|");
                for (int j = 0; j < words.length; j++) {
                    switch (i) {
                        case 0:
                            text = text.replace(words[j], "+");
                            break;

                        case 1:
                            text = text.replace(words[j], "-");
                            break;

                        case 2:
                            text = text.replace(words[j], "*");
                            break;

                        case 3:
                            text = text.replace(words[j], "/");
                            break;

                        case 4:
                            text = text.replace(words[j], "^2");
                            break;

                        case 5:
                            text = text.replace(words[j], "^3");
                            break;

                        case 6:
                            text = text.replace(words[j], "^");
                            break;
                    }
                }
            }

            String newText = "";

            for (int i = 0; i < text.length(); i++) {
                String character = Character.toString(text.charAt(i));
                if (character.matches("\\d") || character.equals("+") || character.equals("-") || character.equals("/") || character.equals("*") || character.equals("^") || character.equals(".") || character.equals(","))
                    newText += character;
            }

            newText = newText.replace(",", ".");

            Expression expression = new ExpressionBuilder(newText).build();
            double result = expression.evaluate();
            speak(processResult(result));
        } catch (IllegalArgumentException e) {
            if (assistantSettings.getBoolean("debug_mode", false))
                Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
            speak(Translations.getAnswer(Assistant.this, "math_expression_processing_error"));
        } catch (ArithmeticException e) {
            if (assistantSettings.getBoolean("debug_mode", false))
                Toast.makeText(Assistant.this, e.toString(), Toast.LENGTH_LONG).show();
            if (e.getMessage().toLowerCase().contains("division by zero") || e.getMessage().toLowerCase().contains("division by 0"))
                speak(Translations.getAnswer(Assistant.this, "division_by_zero_error"));
            else speak(Translations.getAnswer(Assistant.this, "calculation_error"));
        }
    }

    public String processResult(double result) {
        String resultString = String.format(Locale.getDefault(), "%.4f", result);
        boolean behindDecimalPoint = false;
        boolean zeroEncountered = false;
        ArrayList<Integer> indicesToBeCleared = new ArrayList<>();
        for (int i = 0; i < resultString.length(); i++) {
            if (resultString.charAt(i) == ',' || resultString.charAt(i) == '.') behindDecimalPoint = true;
            else if (resultString.charAt(i) == '0' && behindDecimalPoint && !zeroEncountered) {
                indicesToBeCleared.add(i);
                zeroEncountered = true;
            } else if (resultString.charAt(i) == '0' && zeroEncountered) {
                indicesToBeCleared.add(i);
            } else if (resultString.charAt(i) != '0' && zeroEncountered) {
                indicesToBeCleared.clear();
                zeroEncountered = false;
            }
        }

        for (int i = 0; i < indicesToBeCleared.size(); i++) {
            StringBuilder stringBuilder = new StringBuilder(resultString);
            stringBuilder.setCharAt(indicesToBeCleared.get(i), ' ');
            resultString = stringBuilder.toString();
        }

        resultString = resultString.replace(" ", "");
        if (resultString.endsWith(",")) resultString = resultString.replace(",", "");
        if (resultString.endsWith(".")) resultString = resultString.replace(".", "");

        return resultString;
    }

    public String answerNavigate(String place) {
        return Translations.getAnswer(Assistant.this, "starting_navigation").replace("%%PLACE%%", place);
    }

    public String answerPossibilities() {
        return Translations.getAnswer(Assistant.this, "possibilities_question");
    }

    public String cancel() {
        return Translations.getAnswer(Assistant.this, "cancelled");
    }

    public String answerCall(String contact) {
        return Translations.getAnswer(Assistant.this, "calling").replace("%%CONTACT%%", contact);
    }

    public String answerNoContact() {
        return Translations.getAnswer(Assistant.this, "no_contact_found");
    }

    public String answerNoUnderstanding(boolean askToRepeat) {
        if (!askToRepeat) {
            return Translations.getAnswer(Assistant.this, "not_understood");
        } else {
            return Translations.getAnswer(Assistant.this, "not_understood") + " " + Translations.getAnswer(Assistant.this, "repeat");
        }
    }

    public void onNotWorkingClick(View v) {

    }
}
