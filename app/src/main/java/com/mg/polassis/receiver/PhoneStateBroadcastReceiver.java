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

package com.mg.polassis.receiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.mg.polassis.misc.Assistant;
import com.mg.polassis.misc.Translations;
import com.mg.polassis.service.BackgroundListenerService;

public class PhoneStateBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent)
    {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK || telephonyManager.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("call_speaker_mode", false) && Assistant.isCalling) {
                            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                            audioManager.setMode(AudioManager.MODE_IN_CALL);
                            audioManager.setSpeakerphoneOn(true);
                        }
                    }
                }, 500);
            }

            if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK && BackgroundListenerService.isWorking)
                context.stopService(new Intent(context, BackgroundListenerService.class));
            else if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE && PreferenceManager.getDefaultSharedPreferences(context).getBoolean("activation", false) && !BackgroundListenerService.isWorking)
                context.startService(new Intent(context, BackgroundListenerService.class));

            if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                Assistant.isCalling = false;
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(AudioManager.MODE_NORMAL);
            }
        }
        else {
            Toast.makeText(context, Translations.getStringResource(context, "read_phone_state_permission_required_part1"), Toast.LENGTH_LONG).show();
            Toast.makeText(context, Translations.getStringResource(context, "read_phone_state_permission_required_part1"), Toast.LENGTH_LONG).show();
            Toast.makeText(context, Translations.getStringResource(context, "read_phone_state_permission_required_part2"), Toast.LENGTH_LONG).show();
            Toast.makeText(context, Translations.getStringResource(context, "read_phone_state_permission_required_part2"), Toast.LENGTH_LONG).show();
        }
    }
}
