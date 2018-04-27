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

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.mg.polassis.R;
import com.mg.polassis.misc.Assistant;
import com.mg.polassis.service.TextToSpeechService;
import com.mg.polassis.misc.Translations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    public String screenKey;
    public TextToSpeechService textToSpeechService;

    public SettingsFragment settingsFragment;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setTitle(Translations.getStringResource(this, getSupportActionBar().getTitle().toString()));

        if (savedInstanceState == null) {
            if (getIntent().hasExtra("screen")) {
                screenKey = getIntent().getStringExtra("screen");
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                settingsFragment = new SettingsFragment();
                Bundle arguments = new Bundle();
                arguments.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, screenKey);
                settingsFragment.setArguments(arguments);
                transaction.replace(R.id.settingsFragment, settingsFragment, screenKey);
                transaction.commit();
            }
            else {
                settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(SettingsFragment.FRAGMENT_TAG);
                if (settingsFragment == null) settingsFragment = new SettingsFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.settingsFragment, settingsFragment, SettingsFragment.FRAGMENT_TAG);
                transaction.commit();
            }

            bindService(new Intent(SettingsActivity.this, TextToSpeechService.class), textToSpeechServiceConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int code, String permissions[], int[] results) {
        boolean notGranted = false;
        for (int i = 0; i < results.length; i++)
        {
            if (results[i] != PackageManager.PERMISSION_GRANTED) {
                notGranted = true;
                i = results.length+1;
            }
        }

        if (!notGranted) Toast.makeText(SettingsActivity.this, Translations.getStringResource(SettingsActivity.this, "settings_permission_granted"), Toast.LENGTH_LONG).show();
        else Toast.makeText(SettingsActivity.this, Translations.getStringResource(SettingsActivity.this, "settings_permission_not_granted"), Toast.LENGTH_LONG).show();
    }

    private Preference backgroundImagePreference;
    private Preference defaultBackgroundPreferenceButton;
    public void setBackgroundImage(Preference p1, Preference p2) {
        backgroundImagePreference = p1;
        defaultBackgroundPreferenceButton = p2;

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", Assistant.layoutWidth);
        intent.putExtra("aspectY", Assistant.layoutHeight);
        intent.putExtra("outputX", Assistant.layoutWidth);
        intent.putExtra("outputY", Assistant.layoutHeight);
        intent.putExtra("scale", false);
        if (PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).getBoolean("return-currentDate", false)) intent.putExtra("return-currentDate", true);
        startActivityForResult(intent, 4000);
    }

    private class ProcessImage extends AsyncTask<Object, Void, Exception>
    {
        private ProgressDialog progressDialog;
        private Preference backgroundImagePreference;
        private Preference defaultBackgroundPreferenceButton;

        public ProcessImage(Preference backgroundImagePreference, Preference defaultBackgroundPreferenceButton) {
            this.backgroundImagePreference = backgroundImagePreference;
            this.defaultBackgroundPreferenceButton = defaultBackgroundPreferenceButton;
        }

        @Override
        public void onPreExecute()
        {
            progressDialog = ProgressDialog.show(SettingsActivity.this, null, Translations.getStringResource(SettingsActivity.this, "wait"));
        }

        @Override
        public Exception doInBackground(Object... params)
        {
            try {
                Bitmap bitmap;
                Intent intent = (Intent)params[1];
                if (intent.getData() != null) {
                    InputStream inputStream = ((ContentResolver) params[0]).openInputStream(intent.getData());
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                }
                else {
                    if (intent.getExtras().get(Intent.EXTRA_STREAM) != null) {
                        Uri data = (Uri)intent.getExtras().get(Intent.EXTRA_STREAM);
                        InputStream inputStream = ((ContentResolver) params[0]).openInputStream(data);
                        bitmap = BitmapFactory.decodeStream(inputStream);
                        inputStream.close();
                    }
                    else bitmap = (Bitmap)intent.getExtras().get("currentDate");
                }
                File backgroundFile = new File(getFilesDir(), "background");
                if (backgroundFile.exists()) backgroundFile.delete();
                FileOutputStream backgroundFileOutputStream = openFileOutput("background", Context.MODE_PRIVATE);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, backgroundFileOutputStream);
                backgroundFileOutputStream.close();
                return null;
            }
            catch (Exception e) {
                return e;
            }
        }

        @Override
        public void onPostExecute(Exception result)
        {
            progressDialog.dismiss();
            if (result == null) {
                backgroundImagePreference.setSummary(Translations.getStringResource(SettingsActivity.this, "custom"));
                defaultBackgroundPreferenceButton.setEnabled(true);
                PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).edit().putBoolean("background_changed", true).apply();
            }
            else {
                if (PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).getBoolean("debug_mode", false))
                    Toast.makeText(SettingsActivity.this, result.toString(), Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(SettingsActivity.this, Translations.getStringResource(SettingsActivity.this, "change_background_error"), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 4000) {
            if (resultCode == RESULT_OK) {
                new ProcessImage(backgroundImagePreference, defaultBackgroundPreferenceButton).execute(getContentResolver(), data);
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        settingsFragment.registerSharedPreferencesListener();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        settingsFragment.unregisterSharedPreferencesListener();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unbindService(textToSpeechServiceConnection);
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat fragment, PreferenceScreen screen) {
        Intent intent = new Intent(SettingsActivity.this, SettingsActivity.class);
        intent.putExtra("screen", screen.getKey());
        startActivity(intent);
        return true;
    }
}
