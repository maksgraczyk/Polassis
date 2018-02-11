package com.mg.polassis.gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.mg.polassis.R;
import com.mg.polassis.misc.Translations;

public class PossibilitiesActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_possibilities);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getSupportActionBar().setTitle(Translations.getStringResource(this, getSupportActionBar().getTitle().toString()));
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
}
