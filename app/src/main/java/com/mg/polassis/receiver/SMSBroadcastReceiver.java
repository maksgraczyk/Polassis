package com.mg.polassis.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;

public class SMSBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(arg0);
        AudioManager audioManager = (AudioManager)arg0.getSystemService(arg0.AUDIO_SERVICE);
        if (sharedPreferences.getBoolean("receiving_messages_mode", false) && (!sharedPreferences.getBoolean("receiving_messages_when_bluetooth_headset_is_on", false) || (sharedPreferences.getBoolean("receiving_messages_when_bluetooth_headset_is_on", false) && isHeadsetConnected(arg0))) && (!sharedPreferences.getBoolean("do_not_receive_messages_in_silent_mode", true) || (sharedPreferences.getBoolean("do_not_receive_messages_in_silent_mode", true) && (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT && audioManager.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE)))) {
            Intent intent = new Intent();
            intent.setClassName("com.mg.polassis", "com.mg.polassis.Assistant");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle bundle = arg1.getExtras();
            Object[] object = (Object[]) bundle.get("pdus");
            intent.putExtra("sms", object.length);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {

            }
            SmsMessage[] messages = new SmsMessage[object.length];
            for (int i = 0; i < object.length; i++) {
                messages[i] = SmsMessage.createFromPdu((byte[]) object[i]);
            }
            SmsMessage smsMessage = messages[0];
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(smsMessage.getOriginatingAddress()));
            String contact = "";

            ContentResolver contentResolver = arg0.getContentResolver();
            Cursor cursor = contentResolver.query(uri, new String[]{BaseColumns._ID, ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

            try {
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToNext();
                    contact = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) cursor.close();
            }

            String[] message = new String[3];
            if (contact.equals("")) {
                String senderNumber = smsMessage.getOriginatingAddress();
                if (senderNumber.length() == 9) {
                    String processedSenderNumber = "";
                    int howManyDashes = 0;
                    for (int i = 0; i < 9; i++) {
                        if (i != 0 && i % 3 == 0 && (howManyDashes == 0 || (howManyDashes == 1 && i != 3) || (howManyDashes == 2 && i != 6))) {
                            processedSenderNumber += "-";
                            howManyDashes += 1;
                            i--;
                        } else processedSenderNumber += senderNumber.charAt(i);
                    }
                    message[0] = processedSenderNumber;
                } else message[0] = senderNumber;
            } else message[0] = contact;
            if (messages.length == 1) message[1] = smsMessage.getMessageBody();
            else {
                message[1] = "";
                for (int i = 0; i < messages.length; i++) {
                    message[1] += messages[i].getMessageBody();
                }
            }
            message[2] = smsMessage.getOriginatingAddress();
            intent.putExtra("message", message);
            arg0.startActivity(intent);
        }
    }

    public boolean isHeadsetConnected(Context context)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("bluetooth_is_connected", false) && sharedPreferences.getBoolean("bluetooth_headset_support", false);
    }
}
