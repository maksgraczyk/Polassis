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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

import com.mg.polassis.R;
import com.mg.polassis.misc.Assistant;
import com.mg.polassis.misc.Translations;

public class LanguageSelectionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        final AsyncTask<String, Void, Exception> downloadLanguageFiles = new AsyncTask<String, Void, Exception>() {
            private ProgressDialog progressDialog;

            @Override
            public void onPreExecute()
            {
                progressDialog = ProgressDialog.show(LanguageSelectionActivity.this, getString(R.string.app_name), "Downloading language files...");
            }

            @Override
            protected Exception doInBackground(String... params) {
                try {
                    Translations.setLanguage(LanguageSelectionActivity.this, params[0], null, false);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    return e;
                }
                return null;
            }

            @Override
            public void onPostExecute(Exception result)
            {
                progressDialog.cancel();
                if (result == null)
                {
                    Intent intent = new Intent(LanguageSelectionActivity.this, Assistant.class);
                    startActivity(intent);
                    finish();
                }
                else
                {
                    Toast.makeText(LanguageSelectionActivity.this, "An error occurred when preparing the language. " + getString(R.string.app_name) + " will now close.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        };

        if (!Translations.isLanguageSet(this))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Language");
            builder.setCancelable(false);
            builder.setMessage("What language do you want to choose? You can install additional languages later if available.");
            builder.setPositiveButton("Polski", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    downloadLanguageFiles.execute("pl-PL");
                }
            });
            builder.setNegativeButton("English (UK)", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    downloadLanguageFiles.execute("en-GB");
                }
            });
            builder.show();
        }
        else downloadLanguageFiles.execute(Translations.getLanguageSet(this));
    }
}
