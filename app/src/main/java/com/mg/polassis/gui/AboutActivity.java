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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.mg.polassis.R;
import com.mg.polassis.misc.Translations;


public class AboutActivity extends AppCompatActivity {



    public void prepareGUI()
    {
        getSupportActionBar().setTitle(Translations.getStringResource(this, getSupportActionBar().getTitle().toString()));
        ((TextView)findViewById(R.id.appDescription)).setText(Translations.getStringResource(this, "app_description"));
        ((TextView)findViewById(R.id.appDetails)).setText(Translations.getStringResource(this, "app_details"));
        ((Button)findViewById(R.id.exp4jButton)).setText(Translations.getStringResource(this, "exp4j"));
        ((Button)findViewById(R.id.ETIcon)).setText(Translations.getStringResource(this, "ET_icon"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        prepareGUI();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    public void onButtonOneClick(View v)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://projects.congrace.de/exp4j/index.html"));
        startActivity(intent);
    }

    public void onButtonTwoClick(View v)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.flaticon.com/free-icon/mic_10032"));
        startActivity(intent);
    }
}
