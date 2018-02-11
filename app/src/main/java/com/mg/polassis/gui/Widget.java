package com.mg.polassis.gui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.mg.polassis.R;
import com.mg.polassis.misc.Translations;
import com.mg.polassis.service.BackgroundListenerService;
import com.mg.polassis.service.BackgroundSpeechRecognitionService;

public class Widget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, Widget.class);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        PendingIntent pendingIntentMicClicked = PendingIntent.getBroadcast(context, 0, intent.setAction("MicClicked"), 0);
        PendingIntent pendingIntentFieldClicked = PendingIntent.getBroadcast(context, 0, intent.setAction("FieldClicked"), 0);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        remoteViews.setContentDescription(R.id.widgetMicrophoneButton, Translations.getStringResource(context, "start_recognition"));
        remoteViews.setContentDescription(R.id.activationCheckBox, Translations.getStringResource(context, "tickbox"));
        remoteViews.setContentDescription(R.id.activationLabel, Translations.getStringResource(context, "activation"));
        remoteViews.setTextViewText(R.id.activationLabel, Translations.getStringResource(context, "activation"));
        remoteViews.setOnClickPendingIntent(R.id.widgetMicrophoneButton, pendingIntentMicClicked);
        remoteViews.setOnClickPendingIntent(R.id.activationCheckBox, pendingIntentFieldClicked);
        if (sharedPreferences.getBoolean("activation", false)) remoteViews.setImageViewResource(R.id.activationCheckBox, R.drawable.checked);
        appWidgetManager.updateAppWidget(new ComponentName(context, Widget.class), remoteViews);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
                intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                context.startActivity(intent);
                Toast.makeText(context, Translations.getStringResource(context, "overlay_permission_required_desc2"), Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {

    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);

        if ("MicClicked".equals(intent.getAction())) context.startService(new Intent(context, BackgroundSpeechRecognitionService.class));
        else if ("FieldClicked".equals(intent.getAction()))
        {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
            remoteViews.setOnClickPendingIntent(R.id.widgetMicrophoneButton, PendingIntent.getBroadcast(context, 0, new Intent(context, Widget.class).setAction("MicClicked"), 0));
            remoteViews.setOnClickPendingIntent(R.id.activationCheckBox, PendingIntent.getBroadcast(context, 0, new Intent(context, Widget.class).setAction("FieldClicked"), 0));
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            if (!sharedPreferences.getBoolean("activation", false))
            {
                remoteViews.setImageViewResource(R.id.activationCheckBox, R.drawable.checked);
                sharedPreferences.edit().putBoolean("activation", true).commit();
                if (!BackgroundListenerService.isWorking) context.startService(new Intent(context, BackgroundListenerService.class));
            }
            else
            {
                remoteViews.setImageViewResource(R.id.activationCheckBox, R.drawable.unchecked);
                sharedPreferences.edit().putBoolean("activation", false).commit();
                if (BackgroundListenerService.isWorking) context.stopService(new Intent(context, BackgroundListenerService.class));
            }

            AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, Widget.class), remoteViews);
        }
    }
}


