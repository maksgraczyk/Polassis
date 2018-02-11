package com.mg.polassis.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.mg.polassis.service.AssistantHeadService;
import com.mg.polassis.service.BackgroundListenerService;
import com.mg.polassis.service.BluetoothAndSMSListenerService;

public class OnBootCompletedBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context arg0, Intent arg1)
    {
        if (PreferenceManager.getDefaultSharedPreferences(arg0).getBoolean("activation", false)) arg0.startService(new Intent(arg0, BackgroundListenerService.class));
        if (PreferenceManager.getDefaultSharedPreferences(arg0).getBoolean("overlay_button", false)) arg0.startService(new Intent(arg0, AssistantHeadService.class));
        arg0.startService(new Intent(arg0, BluetoothAndSMSListenerService.class));
    }
}
