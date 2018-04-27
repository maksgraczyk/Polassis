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

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.AppCompatEditText;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.mg.polassis.misc.Assistant;
import com.mg.polassis.misc.ContainFunctions;
import com.mg.polassis.service.AssistantHeadService;
import com.mg.polassis.service.BackgroundListenerService;
import com.mg.polassis.R;
import com.mg.polassis.misc.Translations;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private ListPreference thresholdPreference;
    private EditTextPreference specialCommandPreference;
    private Handler handler = new Handler();

    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            Preference preference = findPreference(s);
            if (preference instanceof ListPreference) preference.setSummary(((ListPreference) preference).getEntry());
        }
    };

    public static final String FRAGMENT_TAG = "settings_fragment";

    private class LoadApplications extends AsyncTask<Void, Void, ArrayList<Preference>> {
        private PreferenceScreen preferenceScreen;
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            try {
                preferenceScreen = (PreferenceScreen) findPreference("apps_aliases");
                preferenceScreen.removeAll();
                progressDialog = ProgressDialog.show(getActivity(), null, Translations.getStringResource(getContext(), "app_list_loading"));
            } catch (final Exception e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("debug_mode", false))
                            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getActivity(), Translations.getStringResource(getContext(), "app_list_load_error"), Toast.LENGTH_LONG).show();
                    }
                });
                this.cancel(true);
            }
        }

        @Override
        protected ArrayList<Preference> doInBackground(Void... params) {
            try {
                ArrayList<ApplicationInfo> processedApplicationsList = new ArrayList<ApplicationInfo>();

                final PackageManager packageManager = getActivity().getPackageManager();
                List<ApplicationInfo> applicationsList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

                for (int i = 0; i < applicationsList.size(); i++) {
                    String applicationName = (String) packageManager.getApplicationLabel(applicationsList.get(i));
                    if (packageManager.getLaunchIntentForPackage(applicationsList.get(i).packageName) != null) {
                        if (!applicationName.startsWith("com.") && !applicationName.startsWith("org."))
                            processedApplicationsList.add(applicationsList.get(i));
                    }
                }

                Collections.sort(processedApplicationsList, new Comparator<ApplicationInfo>() {
                    @Override
                    public int compare(ApplicationInfo one, ApplicationInfo two) {
                        Collator collator = Collator.getInstance(Locale.getDefault());
                        return collator.compare(packageManager.getApplicationLabel(one), packageManager.getApplicationLabel(two));
                    }
                });

                for (int i = 0; i < processedApplicationsList.size(); i++) {
                    for (int j = 0; j < processedApplicationsList.size(); j++) {
                        if (i != j) {
                            if (processedApplicationsList.get(i).equals(processedApplicationsList.get(j))) {
                                processedApplicationsList.remove(j);
                                j -= 1;
                            }
                        }
                    }
                }

                ArrayList<Preference> preferences = new ArrayList<Preference>();

                for (int i = 0; i < processedApplicationsList.size(); i++) {
                    final Preference preference = new Preference(getActivity());
                    String applicationName = (String) getActivity().getPackageManager().getApplicationLabel(processedApplicationsList.get(i));
                    String preferenceKey = "__*#ALIAS_A#*__ " + applicationName.toLowerCase();
                    preference.setTitle(applicationName);
                    preference.setKey(preferenceKey);
                    preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(final Preference preference) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            final AppCompatEditText editText = new AppCompatEditText(getActivity());
                            editText.setText(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(preference.getKey(), ""));
                            builder.setView(editText);
                            builder.setTitle(preference.getTitle());
                            builder.setPositiveButton(Translations.getStringResource(getContext(), "ok"), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String[] keysArray = new String[Assistant.syntaxMap.keySet().size()];
                                    Assistant.syntaxMap.keySet().toArray(keysArray);
                                    boolean isProhibited = false;

                                    for (int i = 0; i < keysArray.length && !isProhibited; i++)
                                    {
                                        if (ContainFunctions.contains(Assistant.syntaxMap.get(keysArray[i]), editText.getText().toString())) isProhibited = true;
                                    }

                                    if (!isProhibited) {
                                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(preference.getKey(), editText.getText().toString()).commit();
                                        preference.setSummary(editText.getText().toString());
                                    }
                                    else {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                        builder.setTitle(Translations.getStringResource(getContext(), "prohibited_word"));
                                        builder.setMessage(Translations.getStringResource(getContext(), "prohibited_word_desc"));
                                        builder.setPositiveButton(Translations.getStringResource(getContext(), "ok"), null);
                                        builder.setNegativeButton(null, null);
                                        builder.create().show();
                                    }
                                }
                            });
                            builder.setNegativeButton(Translations.getStringResource(getContext(), Translations.getStringResource(getContext(), "cancel")), null);
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            if (editText.requestFocus()) {
                                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                            }
                            return true;
                        }
                    });
                    preferences.add(preference);
                }
                return preferences;
            } catch (final Exception e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("debug_mode", false))
                            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getActivity(), Translations.getStringResource(getContext(), "app_list_load_error"), Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Preference> result) {
            try {
                if (result != null) {
                    for (int i = 0; i < result.size(); i++) {
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(result.get(i).getKey(), null) != null) {
                            result.get(i).setSummary(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(result.get(i).getKey(), null));
                        }
                        preferenceScreen.addPreference(result.get(i));
                    }
                }
                if (progressDialog != null) progressDialog.dismiss();
            } catch (final Exception e) {
                if (progressDialog != null) progressDialog.dismiss();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("debug_mode", false))
                            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getActivity(), Translations.getStringResource(getContext(), "app_list_load_error"), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    private class LoadContacts extends AsyncTask<Void, Void, ArrayList<String>> {
        private PreferenceScreen preferenceScreen;
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            try {
                preferenceScreen = (PreferenceScreen) findPreference("contacts_aliases");
                preferenceScreen.removeAll();
                progressDialog = ProgressDialog.show(getActivity(), null, Translations.getStringResource(getContext(), "contact_list_loading"));
            } catch (final Exception e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("debug_mode", false))
                            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getActivity(), Translations.getStringResource(getContext(), "contact_list_load_error"), Toast.LENGTH_LONG).show();
                    }
                });
                this.cancel(true);
            }
        }

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            try {
                ArrayList<String> contacts = new ArrayList<String>();
                ContentResolver contentResolver = getActivity().getContentResolver();
                Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0)
                            contacts.add(name);
                    }
                }

                if (cursor != null) cursor.close();

                Collections.sort(contacts, new Comparator<String>() {
                    @Override
                    public int compare(String one, String two) {
                        Collator collator = Collator.getInstance(Locale.getDefault());
                        return collator.compare(one, two);
                    }
                });

                for (int i = 0; i < contacts.size(); i++) {
                    for (int j = 0; j < contacts.size(); j++) {
                        if (i != j) {
                            if (contacts.get(i).equals(contacts.get(j))) {
                                contacts.remove(j);
                                j -= 1;
                            }
                        }
                    }
                }

                return contacts;
            } catch (final Exception e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("debug_mode", false))
                            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getActivity(), Translations.getStringResource(getContext(), "contact_list_load_error"), Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            }
        }

        @Override
        protected void onPostExecute(final ArrayList<String> result) {
            try {
                if (result != null) {
                    for (int i = 0; i < result.size(); i++) {
                        final Preference preference = new Preference(getActivity());
                        preference.setTitle(result.get(i));
                        preference.setKey("__*#ALIAS#*__ " + result.get(i).toLowerCase());
                        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(final Preference preference) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                final AppCompatEditText editText = new AppCompatEditText(getActivity());
                                editText.setText(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(preference.getKey(), ""));
                                builder.setView(editText);
                                builder.setTitle(preference.getTitle());
                                builder.setPositiveButton(Translations.getStringResource(getContext(), "ok"), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String[] keysArray = new String[Assistant.syntaxMap.keySet().size()];
                                        Assistant.syntaxMap.keySet().toArray(keysArray);
                                        boolean isProhibited = false;

                                        for (int i = 0; i < keysArray.length && !isProhibited; i++)
                                        {
                                            if (ContainFunctions.contains(Assistant.syntaxMap.get(keysArray[i]), editText.getText().toString())) isProhibited = true;
                                        }

                                        if (!isProhibited) {
                                            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(preference.getKey(), editText.getText().toString()).commit();
                                            preference.setSummary(editText.getText().toString());
                                        }
                                        else {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                            builder.setTitle(Translations.getStringResource(getContext(), "prohibited_word"));
                                            builder.setMessage(Translations.getStringResource(getContext(), "prohibited_word_desc"));
                                            builder.setPositiveButton(Translations.getStringResource(getContext(), "ok"), null);
                                            builder.setNegativeButton(null, null);
                                            builder.create().show();
                                        }
                                    }
                                });
                                builder.setNegativeButton(Translations.getStringResource(getContext(), "cancel"), null);
                                builder.setNeutralButton(Translations.getStringResource(getContext(), "remove_all_aliases"), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        SharedPreferences preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
                                        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet())
                                        {
                                            if (entry.getKey().contains("ALIAS") && entry.getKey().contains("__*") && (entry.getKey().contains(preference.getTitle().toString().toLowerCase()) || entry.getKey().contains(preference.getTitle().toString())))
                                                preferences.edit().remove(entry.getKey()).apply();
                                        }
                                        Toast.makeText(getContext(), Translations.getStringResource(getContext(), "aliases_removed"), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                                if (editText.requestFocus()) {
                                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                                }
                                return true;
                            }
                        });
                        preferenceScreen.addPreference(preference);
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(preference.getKey(), null) != null) {
                            preference.setSummary(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(preference.getKey(), null));
                        }
                    }
                }
                if (progressDialog != null) progressDialog.dismiss();
            } catch (final Exception e) {
                if (progressDialog != null) progressDialog.dismiss();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("debug_mode", false))
                            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getActivity(), Translations.getStringResource(getContext(), "contact_list_load_error"), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    public void registerSharedPreferencesListener()
    {
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    public void unregisterSharedPreferencesListener()
    {
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference.equals(findPreference("activation"))) {
            Intent intent = new Intent(getActivity(), BackgroundListenerService.class);
            boolean isChecked = (boolean)value;
            if (isChecked) {
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    if (android.provider.Settings.canDrawOverlays(getContext())) {
                        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.DISABLE_KEYGUARD) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WAKE_LOCK, Manifest.permission.DISABLE_KEYGUARD}, 0);
                            return false;
                        }

                        if (!BackgroundListenerService.isWorking) getActivity().startService(intent);

                        RemoteViews remoteViews = new RemoteViews(getActivity().getPackageName(), R.layout.widget);
                        remoteViews.setOnClickPendingIntent(R.id.widgetMicrophoneButton, PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), Widget.class).setAction("MicClicked"), 0));
                        remoteViews.setOnClickPendingIntent(R.id.activationCheckBox, PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), Widget.class).setAction("FieldClicked"), 0));
                        remoteViews.setImageViewResource(R.id.activationCheckBox, R.drawable.checked);

                        AppWidgetManager.getInstance(getActivity()).updateAppWidget(new ComponentName(getActivity(), Widget.class), remoteViews);
                    }
                    else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle(Translations.getStringResource(getContext(), "permissions_required"));
                        builder.setMessage(Translations.getStringResource(getContext(), "overlay_permission_required_desc"));
                        builder.setPositiveButton(Translations.getStringResource(getContext(), "ok"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent();
                                intent.setAction(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                startActivity(intent);
                            }
                        });
                        builder.setNegativeButton(Translations.getStringResource(getContext(), Translations.getStringResource(getContext(), "cancel")), null);
                        builder.create().show();
                        return false;
                    }
                }
                else {
                    if (!BackgroundListenerService.isWorking) getActivity().startService(intent);

                    RemoteViews remoteViews = new RemoteViews(getActivity().getPackageName(), R.layout.widget);
                    remoteViews.setOnClickPendingIntent(R.id.widgetMicrophoneButton, PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), Widget.class).setAction("MicClicked"), 0));
                    remoteViews.setOnClickPendingIntent(R.id.activationCheckBox, PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), Widget.class).setAction("FieldClicked"), 0));
                    remoteViews.setImageViewResource(R.id.activationCheckBox, R.drawable.checked);

                    AppWidgetManager.getInstance(getActivity()).updateAppWidget(new ComponentName(getActivity(), Widget.class), remoteViews);
                }
            } else if (!isChecked) {
                if (BackgroundListenerService.isWorking) getActivity().stopService(intent);

                RemoteViews remoteViews = new RemoteViews(getActivity().getPackageName(), R.layout.widget);
                remoteViews.setOnClickPendingIntent(R.id.widgetMicrophoneButton, PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), Widget.class).setAction("MicClicked"), 0));
                remoteViews.setOnClickPendingIntent(R.id.activationCheckBox, PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), Widget.class).setAction("FieldClicked"), 0));
                remoteViews.setImageViewResource(R.id.activationCheckBox, R.drawable.unchecked);

                AppWidgetManager.getInstance(getActivity()).updateAppWidget(new ComponentName(getActivity(), Widget.class), remoteViews);
            }
        }
        else if (preference.equals(findPreference("bluetooth_headset_support"))) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, 0);
                return false;
            }
        }
        else if (preference.equals(findPreference("receiving_messages_mode"))) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS}, 0);
                return false;
            }
        }
        else if (preference.equals(findPreference("activation_type")))
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setPositiveButton(Translations.getStringResource(getContext(), "ok"), null);
                builder.setNegativeButton(null, null);
                if (value.toString().equals("voice"))
                {
                    thresholdPreference.setEnabled(true);
                    specialCommandPreference.setEnabled(true);
                    builder.setTitle(Translations.getStringResource(getContext(), "voice_activation_desc_title"));
                    builder.setMessage(Translations.getStringResource(getContext(), "voice_activation_desc"));
                }
                else if (value.toString().equals("press_power_button"))
                {
                    thresholdPreference.setEnabled(false);
                    specialCommandPreference.setEnabled(false);
                    builder.setTitle(Translations.getStringResource(getContext(), "press_activation_desc_title"));
                    builder.setMessage(Translations.getStringResource(getContext(), "press_activation_desc"));
                }
                else if (value.toString().equals("wave"))
                {
                    thresholdPreference.setEnabled(false);
                    specialCommandPreference.setEnabled(false);
                    builder.setTitle(Translations.getStringResource(getContext(), "wave_activation_desc_title"));
                    builder.setMessage(Translations.getStringResource(getContext(), "wave_activation_desc"));
                }

                builder.create().show();
            }
            else if (preference.equals(findPreference("activation_phrase"))) findPreference("activation_phrase").setSummary(value.toString());

            if (BackgroundListenerService.isWorking)
            {
                getActivity().stopService(new Intent(getActivity(), BackgroundListenerService.class));
                getActivity().startService(new Intent(getActivity(), BackgroundListenerService.class));
            }

        return true;
    }

    public void preparePreferenceTexts() throws XmlPullParserException, IOException
    {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++)
        {
            Preference preference = preferenceScreen.getPreference(i);

            if (preference.getSummary() != null)
                preference.setSummary(Translations.getStringResource(getContext(), preference.getSummary().toString()));

            preference.setTitle(Translations.getStringResource(getContext(), preference.getTitle().toString()));

            if (preference instanceof EditTextPreference)
            {
                ((EditTextPreference) preference).setDialogTitle(Translations.getStringResource(getContext(), ((EditTextPreference) preference).getDialogTitle().toString()));
            }
            else if (preference instanceof ListPreference)
            {
                ListPreference listPreference = (ListPreference)preference;
                listPreference.setDialogTitle(listPreference.getTitle());
                if (preference.getKey().equals("language")) {
                    listPreference.setEntries(Translations.getLanguageEntries(getContext()));
                    listPreference.setEntryValues(Translations.getLanguageEntryValues(getContext()));
                    listPreference.setSummary("%s");
                }
                else {
                    CharSequence[] entries = listPreference.getEntries();
                    if (entries != null) {
                        for (int j = 0; j < entries.length; j++) {
                            entries[j] = Translations.getStringResource(getContext(), entries[j].toString());
                        }
                        listPreference.setEntries(entries);
                    }
                }
            }
            else if (preference instanceof PreferenceCategory) preparePreferenceTexts((PreferenceCategory)preference);
        }
    }

    public void preparePreferenceTexts(PreferenceCategory preferenceScreen)
    {
        for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++)
        {
            Preference preference = preferenceScreen.getPreference(i);

            if (preference.getSummary() != null)
                preference.setSummary(Translations.getStringResource(getContext(), preference.getSummary().toString()));

            preference.setTitle(Translations.getStringResource(getContext(), preference.getTitle().toString()));

            if (preference instanceof EditTextPreference)
            {
                ((EditTextPreference) preference).setDialogTitle(Translations.getStringResource(getContext(), ((EditTextPreference) preference).getDialogTitle().toString()));
            }
            else if (preference instanceof ListPreference)
            {
                ListPreference listPreference = (ListPreference)preference;
                listPreference.setDialogTitle(listPreference.getTitle());

                    CharSequence[] entries = listPreference.getEntries();
                    if (entries != null) {
                        for (int j = 0; j < entries.length; j++) {
                            entries[j] = Translations.getStringResource(getContext(), entries[j].toString());
                        }
                        listPreference.setEntries(entries);
                    }
            }
        }
    }

    @Override
    public void onCreatePreferences(Bundle b, String key) {
        setPreferencesFromResource(R.xml.preferences, key);

        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

        try {
            if (key == null || (!key.equals("contacts_aliases") && !key.equals("apps_aliases"))) preparePreferenceTexts();

            if (key == null)
            {
                ListPreference languagePreference = (ListPreference)findPreference("language");
                languagePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle(Translations.getStringResource(getContext(), "language_changed_title"));
                        builder.setMessage(Translations.getStringResource(getContext(), "language_changed"));
                        builder.setCancelable(false);
                        builder.setPositiveButton(Translations.getStringResource(getContext(), "ok"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getActivity().finishAffinity();
                            }
                        });
                        builder.setNegativeButton(null, null);
                        builder.create().show();
                        return true;
                    }
                });
            }
            else {
                ((SettingsActivity)getActivity()).getSupportActionBar().setTitle(Translations.getStringResource(getContext(), findPreference(key).getTitle().toString()));

                switch (key)
                {
                    case "contacts_aliases":
                        new LoadContacts().execute();
                        break;

                    case "apps_aliases":
                        new LoadApplications().execute();
                        break;

                    case "activation_screen":
                        ListPreference activationTypePreference = (ListPreference) findPreference("activation_type");
                        activationTypePreference.setOnPreferenceChangeListener(this);

                        specialCommandPreference = (EditTextPreference) findPreference("activation_phrase");
                        specialCommandPreference.setOnPreferenceChangeListener(this);
                        specialCommandPreference.setSummary(specialCommandPreference.getText());
                        if (!activationTypePreference.getValue().equals("voice"))
                            specialCommandPreference.setEnabled(false);

                        thresholdPreference = (ListPreference) findPreference("activation_threshold");
                        thresholdPreference.setOnPreferenceChangeListener(this);
                        if (!activationTypePreference.getValue().equals("voice"))
                            thresholdPreference.setEnabled(false);

                        findPreference("activation").setOnPreferenceChangeListener(this);

                        CheckBoxPreference overlayButtonPreference = (CheckBoxPreference) findPreference("overlay_button");
                        if (AssistantHeadService.isWorking) overlayButtonPreference.setChecked(true);
                        else overlayButtonPreference.setChecked(false);
                        overlayButtonPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference preference, Object o) {
                                if ((boolean) o) {
                                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                                        if (android.provider.Settings.canDrawOverlays(getContext()))
                                            getContext().startService(new Intent(getContext(), AssistantHeadService.class));
                                        else {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                            builder.setTitle(Translations.getStringResource(getContext(), "permissions_required"));
                                            builder.setMessage(Translations.getStringResource(getContext(), "overlay_permission_required_desc"));
                                            builder.setPositiveButton(Translations.getStringResource(getContext(), "ok"), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    Intent intent = new Intent();
                                                    intent.setAction(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                                    startActivity(intent);
                                                }
                                            });
                                            builder.setNegativeButton(Translations.getStringResource(getContext(), Translations.getStringResource(getContext(), "cancel")), null);
                                            builder.create().show();
                                            return false;
                                        }
                                    } else
                                        getContext().startService(new Intent(getContext(), AssistantHeadService.class));
                                } else {
                                    AssistantHeadService.toBeShutDown = true;
                                    getContext().stopService(new Intent(getContext(), AssistantHeadService.class));
                                }
                                return true;
                            }
                        });
                        break;

                    case "functions_screen":
                        findPreference("receiving_messages_mode").setOnPreferenceChangeListener(this);
                        List<ResolveInfo> supportedAppsInfoList = getContext().getPackageManager().queryBroadcastReceivers(new Intent(Intent.ACTION_MEDIA_BUTTON, null), PackageManager.GET_RESOLVED_FILTER);
                        ArrayList<CharSequence> supportedApps = new ArrayList<>();
                        ArrayList<CharSequence> supportedComponents = new ArrayList<>();
                        for (ResolveInfo info : supportedAppsInfoList)
                        {
                            try
                            {
                                supportedApps.add(getContext().getPackageManager().getApplicationLabel(getContext().getPackageManager().getApplicationInfo(info.activityInfo.packageName, 0)));
                                supportedComponents.add(info.activityInfo.packageName + "|" + info.activityInfo.name);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }

                        ListPreference controlledMediaPlayerPreference = (ListPreference)findPreference("controlled_media_player");
                        CharSequence[] entries = new CharSequence[supportedApps.size()];
                        CharSequence[] entryValues = new CharSequence[supportedComponents.size()];
                        supportedApps.toArray(entries);
                        supportedComponents.toArray(entryValues);
                        controlledMediaPlayerPreference.setEntries(entries);
                        controlledMediaPlayerPreference.setEntryValues(entryValues);
                        controlledMediaPlayerPreference.setSummary("%s");
                        break;

                    case "general_screen":
                        findPreference("bluetooth_headset_support").setOnPreferenceChangeListener(this);
                        break;

                    case "appearance_screen":
                        File backgroundFile = new File(getActivity().getFilesDir(), "background");
                        if (backgroundFile.exists()) {
                            findPreference("background").setSummary(Translations.getStringResource(getContext(), "custom"));
                            findPreference("default_background").setEnabled(true);
                        } else {
                            findPreference("background").setSummary(Translations.getStringResource(getContext(), "default"));
                            findPreference("default_background").setEnabled(false);
                        }
                        findPreference("background").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                ((SettingsActivity) getActivity()).setBackgroundImage(findPreference("background"), findPreference("default_background"));
                                return true;
                            }
                        });
                        findPreference("default_background").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                File backgroundFile = new File(getActivity().getFilesDir(), "background");
                                boolean success = true;
                                if (backgroundFile.exists()) success = backgroundFile.delete();
                                if (success) {
                                    findPreference("background").setSummary(Translations.getStringResource(getContext(), "default"));
                                    findPreference("default_background").setEnabled(false);
                                    PreferenceManager.getDefaultSharedPreferences(SettingsFragment.this.getContext()).edit().putBoolean("background_changed", true).apply();
                                    Toast.makeText(getActivity(), Translations.getStringResource(getContext(), "default_background_success"), Toast.LENGTH_SHORT).show();
                                    return true;
                                } else {
                                    Toast.makeText(getActivity(), Translations.getStringResource(getContext(), "default_background_error"), Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                            }
                        });
                        break;

                    case "other_screen":
                        findPreference("clear_last_checked_date").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                getContext().getSharedPreferences("read_news", Context.MODE_PRIVATE).edit().remove("last_checked").commit();
                                Toast.makeText(getContext(), Translations.getStringResource(getContext(), "last_date_cleared"), Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        });
                        break;

                    case "interpunction":
                        Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference preference, Object o) {
                                preference.setSummary(o.toString());
                                return true;
                            }
                        };

                        EditTextPreference dotPreference = (EditTextPreference) findPreference("dot");
                        EditTextPreference commaPreference = (EditTextPreference) findPreference("comma");
                        EditTextPreference questionMarkPreference = (EditTextPreference) findPreference("question_mark");
                        EditTextPreference semicolonPreference = (EditTextPreference) findPreference("semicolon");
                        EditTextPreference exclamationMarkPreference = (EditTextPreference) findPreference("exclamation_mark");
                        EditTextPreference percentPreference = (EditTextPreference) findPreference("percent");
                        EditTextPreference bracketBeginningPreference = (EditTextPreference) findPreference("bracket_beginning");
                        EditTextPreference bracketEndPreference = (EditTextPreference) findPreference("bracket_end");
                        EditTextPreference spacePreference = (EditTextPreference) findPreference("space");
                        EditTextPreference dashPreference = (EditTextPreference) findPreference("dash");
                        EditTextPreference colonPreference = (EditTextPreference) findPreference("colon");
                        EditTextPreference quotationMarkPreference = (EditTextPreference) findPreference("quotation_mark");
                        EditTextPreference slashPreference = (EditTextPreference) findPreference("slash");
                        EditTextPreference atPreference = (EditTextPreference) findPreference("at");

                        dotPreference.setOnPreferenceChangeListener(listener);
                        commaPreference.setOnPreferenceChangeListener(listener);
                        questionMarkPreference.setOnPreferenceChangeListener(listener);
                        semicolonPreference.setOnPreferenceChangeListener(listener);
                        exclamationMarkPreference.setOnPreferenceChangeListener(listener);
                        percentPreference.setOnPreferenceChangeListener(listener);
                        bracketBeginningPreference.setOnPreferenceChangeListener(listener);
                        bracketEndPreference.setOnPreferenceChangeListener(listener);
                        spacePreference.setOnPreferenceChangeListener(listener);
                        dashPreference.setOnPreferenceChangeListener(listener);
                        colonPreference.setOnPreferenceChangeListener(listener);
                        quotationMarkPreference.setOnPreferenceChangeListener(listener);
                        slashPreference.setOnPreferenceChangeListener(listener);
                        atPreference.setOnPreferenceChangeListener(listener);

                        dotPreference.setSummary(dotPreference.getText());
                        commaPreference.setSummary(commaPreference.getText());
                        questionMarkPreference.setSummary(questionMarkPreference.getText());
                        semicolonPreference.setSummary(semicolonPreference.getText());
                        exclamationMarkPreference.setSummary(exclamationMarkPreference.getText());
                        percentPreference.setSummary(percentPreference.getText());
                        bracketBeginningPreference.setSummary(bracketBeginningPreference.getText());
                        bracketEndPreference.setSummary(bracketEndPreference.getText());
                        spacePreference.setSummary(spacePreference.getText());
                        dashPreference.setSummary(dashPreference.getText());
                        colonPreference.setSummary(colonPreference.getText());
                        quotationMarkPreference.setSummary(quotationMarkPreference.getText());
                        slashPreference.setSummary(slashPreference.getText());
                        atPreference.setSummary(atPreference.getText());
                        break;

                    case "command_learning_screen":
                        final SharedPreferences commandPreferences = getContext().getSharedPreferences("commands", Context.MODE_PRIVATE);
                        final PreferenceCategory savedCommandsCategory = (PreferenceCategory)findPreference("saved_commands");

                        class OnCustomCommandPreferenceClickListener implements Preference.OnPreferenceClickListener {
                            private String title;
                            private String command;
                            private String commandReplacement;

                            public OnCustomCommandPreferenceClickListener(String title, String command, String commandReplacement)
                            {
                                this.title = title;
                                this.command = command;
                                this.commandReplacement = commandReplacement;
                            }

                            @Override
                            public boolean onPreferenceClick(final Preference preference) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                View dialogView = View.inflate(getContext(), R.layout.dialog_learn_command, null);
                                final EditText commandToBeLearnt = (EditText)dialogView.findViewById(R.id.commandToBeLearnt);
                                final EditText commandToBeExecuted = (EditText)dialogView.findViewById(R.id.commandToBeExecuted);
                                final EditText commandTitle = (EditText)dialogView.findViewById(R.id.commandTitle);

                                TextView commandToBeLearntTitle = (TextView)dialogView.findViewById(R.id.commandToBeLearntTitle);
                                TextView commandToBeExecutedTitle = (TextView)dialogView.findViewById(R.id.commandToBeExecutedTitle);
                                TextView commandTitleLabel = (TextView)dialogView.findViewById(R.id.commandTitleLabel);

                                commandToBeLearntTitle.setText(Translations.getStringResource(getContext(), commandToBeLearntTitle.getText().toString()));
                                commandToBeExecutedTitle.setText(Translations.getStringResource(getContext(), commandToBeExecutedTitle.getText().toString()));
                                commandTitleLabel.setText(Translations.getStringResource(getContext(), commandTitleLabel.getText().toString()));

                                commandToBeLearntTitle.setHint(Translations.getStringResource(getContext(), commandToBeLearntTitle.getHint().toString()));
                                commandToBeExecutedTitle.setHint(Translations.getStringResource(getContext(), commandToBeExecutedTitle.getHint().toString()));
                                commandTitleLabel.setHint(Translations.getStringResource(getContext(), commandTitleLabel.getHint().toString()));

                                commandToBeLearnt.setText(command);
                                commandToBeExecuted.setText(commandReplacement);
                                commandTitle.setText(title);

                                builder.setView(dialogView);
                                builder.setPositiveButton(Translations.getStringResource(getContext(), "ok"), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String command = commandToBeLearnt.getText().toString();
                                        String commandReplacement = commandToBeExecuted.getText().toString();
                                        String title = commandTitle.getText().toString();
                                        commandPreferences.edit().putString(command.toLowerCase(), title + "||" + commandReplacement).apply();
                                        preference.setTitle(title);
                                        preference.setSummary(command + " = " + commandReplacement);
                                        preference.setKey(command.toLowerCase());
                                        preference.setOnPreferenceClickListener(new OnCustomCommandPreferenceClickListener(title, command, commandReplacement));
                                    }
                                });
                                builder.setNeutralButton("delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        commandPreferences.edit().remove(command).apply();
                                        savedCommandsCategory.removePreference(preference);
                                    }
                                });
                                builder.setNegativeButton(Translations.getStringResource(getContext(), "cancel"), null);
                                builder.setTitle(Translations.getStringResource(getContext(), "command_edit_title"));
                                builder.create().show();

                                return false;
                            }
                        };

                        Set<String> commands = commandPreferences.getAll().keySet();

                        for (final String command : commands)
                        {
                            String commandReplacementFull = commandPreferences.getString(command, "");
                            String[] elements = commandReplacementFull.split("\\|\\|");
                            String title, commandReplacement;
                            if (elements.length == 2) {
                                title = elements[0];
                                commandReplacement = elements[1];
                            }
                            else {
                                title = Translations.getStringResource(getContext(), "no_title");
                                commandReplacement = commandReplacementFull;
                            }

                            Preference preference = new Preference(getContext());
                            preference.setKey(command);
                            preference.setTitle(title);
                            preference.setSummary(command + " = " + commandReplacement);
                            preference.setOnPreferenceClickListener(new OnCustomCommandPreferenceClickListener(title, command, commandReplacement));

                            savedCommandsCategory.addPreference(preference);
                        }

                        findPreference("new_command").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                View dialogView = View.inflate(getContext(), R.layout.dialog_learn_command, null);
                                final EditText commandToBeLearnt = (EditText)dialogView.findViewById(R.id.commandToBeLearnt);
                                final EditText commandToBeExecuted = (EditText)dialogView.findViewById(R.id.commandToBeExecuted);
                                final EditText commandTitle = (EditText)dialogView.findViewById(R.id.commandTitle);

                                TextView commandToBeLearntTitle = (TextView)dialogView.findViewById(R.id.commandToBeLearntTitle);
                                TextView commandToBeExecutedTitle = (TextView)dialogView.findViewById(R.id.commandToBeExecutedTitle);
                                TextView commandTitleLabel = (TextView)dialogView.findViewById(R.id.commandTitleLabel);

                                commandToBeLearntTitle.setText(Translations.getStringResource(getContext(), commandToBeLearntTitle.getText().toString()));
                                commandToBeExecutedTitle.setText(Translations.getStringResource(getContext(), commandToBeExecutedTitle.getText().toString()));
                                commandTitleLabel.setText(Translations.getStringResource(getContext(), commandTitleLabel.getText().toString()));

                                commandToBeLearnt.setHint(Translations.getStringResource(getContext(), commandToBeLearnt.getHint().toString()));
                                commandToBeExecuted.setHint(Translations.getStringResource(getContext(), commandToBeExecuted.getHint().toString()));
                                commandTitle.setHint(Translations.getStringResource(getContext(), commandTitle.getHint().toString()));

                                builder.setView(dialogView);
                                builder.setPositiveButton(Translations.getStringResource(getContext(), "ok"), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String command = commandToBeLearnt.getText().toString();
                                        String commandReplacement = commandToBeExecuted.getText().toString();
                                        String title = commandTitle.getText().toString();
                                        commandPreferences.edit().putString(command.toLowerCase(), title + "||" + commandReplacement).apply();
                                        Preference preference = new Preference(getContext());
                                        preference.setKey(command.toLowerCase());
                                        preference.setTitle(title);
                                        preference.setSummary(command + " = " + commandReplacement);
                                        preference.setOnPreferenceClickListener(new OnCustomCommandPreferenceClickListener(title, command, commandReplacement));
                                        savedCommandsCategory.addPreference(preference);
                                    }
                                });
                                builder.setNegativeButton(Translations.getStringResource(getContext(), "cancel"), null);
                                builder.setTitle(Translations.getStringResource(getContext(), "new_command_title"));
                                builder.create().show();

                                return false;
                            }
                        });
                        break;

                    case "reminders_screen":
                        final SharedPreferences remindersPreferences = getContext().getSharedPreferences("reminders", Context.MODE_PRIVATE);
                        final Set<String> allReminders = remindersPreferences.getAll().keySet();

                        findPreference("delete_all_reminders").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                for (String reminder : allReminders)
                                {
                                    String reminderTitle = remindersPreferences.getString(reminder, "");
                                    long timeInMillis = Long.parseLong(reminder);
                                    GregorianCalendar calendar = new GregorianCalendar();
                                    calendar.setTimeInMillis(timeInMillis);
                                    int reminderIdentifier = (int)(timeInMillis/1000);
                                    PendingIntent pendingIntent = PendingIntent.getActivity(getContext().getApplicationContext(), reminderIdentifier, new Intent(getContext().getApplicationContext(), AlarmActivity.class).putExtra("reminder", true).putExtra("title", reminderTitle), PendingIntent.FLAG_UPDATE_CURRENT);
                                    ((AlarmManager)getContext().getSystemService(Context.ALARM_SERVICE)).cancel(pendingIntent);
                                    remindersPreferences.edit().remove(reminder).apply();
                                    getPreferenceScreen().removePreference(findPreference(reminder));
                                }
                                Toast.makeText(getContext(), Translations.getStringResource(getContext(), "operation_completed"), Toast.LENGTH_SHORT).show();
                                return false;
                            }
                        });

                        for (final String reminder : allReminders)
                        {
                            final String reminderTitle = remindersPreferences.getString(reminder, "");
                            GregorianCalendar calendar = new GregorianCalendar();
                            long timeInMillis = Long.parseLong(reminder);
                            final int reminderIdentifier = (int)(timeInMillis/1000);
                            calendar.setTimeInMillis(timeInMillis);

                            Preference preference = new Preference(getContext());
                            preference.setKey(reminder);
                            preference.setTitle(reminderTitle);
                            preference.setSummary(DateFormat.format("dd/MM/yyyy, HH:mm", calendar.getTime()) + " " + Translations.getStringResource(getContext(), "tap_to_delete"));
                            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference preference) {
                                    PendingIntent pendingIntent = PendingIntent.getActivity(getContext().getApplicationContext(), reminderIdentifier, new Intent(getContext().getApplicationContext(), AlarmActivity.class).putExtra("reminder", true).putExtra("title", reminderTitle), PendingIntent.FLAG_UPDATE_CURRENT);
                                    ((AlarmManager)getContext().getSystemService(Context.ALARM_SERVICE)).cancel(pendingIntent);
                                    remindersPreferences.edit().remove(reminder).apply();
                                    getPreferenceScreen().removePreference(preference);
                                    Toast.makeText(getContext(), Translations.getStringResource(getContext(), "reminder_deleted"), Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                            });

                            getPreferenceScreen().addPreference(preference);
                        }
                        break;

                    case "alarm_screen":
                        final SharedPreferences alarmsPreferences = getContext().getSharedPreferences("alarms", Context.MODE_PRIVATE);
                        final Set<String> allAlarms = alarmsPreferences.getAll().keySet();

                        findPreference("delete_all_alarms").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                for (String alarm : allAlarms)
                                {
                                    long timeInMillis = Long.parseLong(alarm);
                                    GregorianCalendar calendar = new GregorianCalendar();
                                    calendar.setTimeInMillis(timeInMillis);
                                    int alarmIdentifier = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE);
                                    PendingIntent pendingIntent = PendingIntent.getActivity(getContext().getApplicationContext(), alarmIdentifier, new Intent(getContext().getApplicationContext(), AlarmActivity.class).putExtra("alarm", true), PendingIntent.FLAG_UPDATE_CURRENT);
                                    ((AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE)).cancel(pendingIntent);
                                    getPreferenceScreen().removePreference(findPreference(Integer.toString(alarmIdentifier)));
                                }
                                alarmsPreferences.edit().clear().apply();
                                if (android.os.Build.VERSION.SDK_INT < 21) getContext().sendBroadcast(new Intent("android.intent.action.ALARM_CHANGED").putExtra("alarmSet", false));
                                Toast.makeText(getContext(), Translations.getStringResource(getContext(), "operation_completed"), Toast.LENGTH_SHORT).show();
                                return false;
                            }
                        });

                        for (String alarm : allAlarms)
                        {
                            final long timeInMillis = Long.parseLong(alarm);
                            GregorianCalendar calendar = new GregorianCalendar();
                            calendar.setTimeInMillis(timeInMillis);
                            final int alarmIdentifier = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE);

                            Preference preference = new Preference(getContext());
                            preference.setKey(Integer.toString(alarmIdentifier));
                            preference.setTitle(DateFormat.format("HH:mm", calendar.getTime()));
                            preference.setSummary(Translations.getStringResource(getContext(), "tap_to_delete_alarm"));
                            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference preference) {
                                    PendingIntent pendingIntent = PendingIntent.getActivity(getContext().getApplicationContext(), alarmIdentifier, new Intent(getContext().getApplicationContext(), AlarmActivity.class).putExtra("alarm", true), PendingIntent.FLAG_UPDATE_CURRENT);
                                    ((AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE)).cancel(pendingIntent);
                                    getPreferenceScreen().removePreference(preference);
                                    alarmsPreferences.edit().remove(Long.toString(timeInMillis)).apply();
                                    if (android.os.Build.VERSION.SDK_INT < 21 && alarmsPreferences.getAll().keySet().size() == 0) getContext().sendBroadcast(new Intent("android.intent.action.ALARM_CHANGED").putExtra("alarmSet", false));
                                    Toast.makeText(getContext(), Translations.getStringResource(getContext(), "alarm_deleted"), Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                            });

                            getPreferenceScreen().addPreference(preference);
                        }
                        break;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("debug_mode", false))
                Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getActivity(), Translations.getStringResource(getContext(), "settings_screen_load_error"), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onViewCreated(View v, Bundle b) {
        super.onViewCreated(v, b);
        v.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_material_light));
    }
}
