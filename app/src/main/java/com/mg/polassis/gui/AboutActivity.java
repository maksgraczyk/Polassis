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
