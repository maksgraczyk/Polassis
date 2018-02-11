package com.mg.polassis.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.mg.polassis.receiver.BluetoothBroadcastReceiver;
import com.mg.polassis.receiver.SMSBroadcastReceiver;

public class BluetoothAndSMSListenerService extends Service {
    private IntentFilter smsIntentFilter;
    private IntentFilter bluetoothIntentFilter;
    private BroadcastReceiver smsBroadcastReceiver;
    private BroadcastReceiver bluetoothBroadcastReceiver;

    public static boolean isWorking = false;

    @Override
    public void onCreate()
    {
        smsIntentFilter = new IntentFilter();
        bluetoothIntentFilter = new IntentFilter();
        smsBroadcastReceiver = new SMSBroadcastReceiver();
        bluetoothBroadcastReceiver = new BluetoothBroadcastReceiver();

        smsIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        smsIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        bluetoothIntentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        bluetoothIntentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED");
        bluetoothIntentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        isWorking = true;
        registerReceiver(smsBroadcastReceiver, smsIntentFilter);
        registerReceiver(bluetoothBroadcastReceiver, bluetoothIntentFilter);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy()
    {
        isWorking = false;
        unregisterReceiver(smsBroadcastReceiver);
        unregisterReceiver(bluetoothBroadcastReceiver);
    }
}
