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

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.preference.PreferenceManager;

import com.mg.polassis.misc.Assistant;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        BluetoothDevice bluetoothDevice = arg1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (bluetoothDevice.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
            AudioManager audioManager = (AudioManager)arg0.getSystemService(Context.AUDIO_SERVICE);
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(arg1.getAction())) {
                PreferenceManager.getDefaultSharedPreferences(arg0).edit().putBoolean("bluetooth_is_connected", true).apply();
                if (Assistant.inForeground && audioManager.getMode() != AudioManager.MODE_IN_COMMUNICATION && PreferenceManager.getDefaultSharedPreferences(arg0).getBoolean("bluetooth_headset_support", false))
                {
                    Intent intent = new Intent(arg0, Assistant.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("bluetooth_connected", true);
                    arg0.startActivity(intent);
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(arg1.getAction()) || BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(arg1.getAction())) {
                PreferenceManager.getDefaultSharedPreferences(arg0).edit().putBoolean("bluetooth_is_connected", false).apply();
                if (Assistant.inForeground && audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION && PreferenceManager.getDefaultSharedPreferences(arg0).getBoolean("bluetooth_headset_support", false))
                {
                    Intent intent = new Intent(arg0, Assistant.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("bluetooth_disconnected", true);
                    arg0.startActivity(intent);
                }
            }
        }
    }
}

