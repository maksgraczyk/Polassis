package com.mg.polassis.gui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.mg.polassis.R;
import com.mg.polassis.misc.Translations;
import com.mg.polassis.service.BackgroundSpeechRecognitionService;

public class SmallWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, SmallWidget.class);
        PendingIntent pendingIntentMicClicked = PendingIntent.getBroadcast(context, 1, intent.setAction("MicClicked"), 0);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_small);
        remoteViews.setContentDescription(R.id.widgetButton, Translations.getStringResource(context, "start_recognition"));
        remoteViews.setOnClickPendingIntent(R.id.widgetButton, pendingIntentMicClicked);
        appWidgetManager.updateAppWidget(new ComponentName(context, SmallWidget.class), remoteViews);
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
    }
}


