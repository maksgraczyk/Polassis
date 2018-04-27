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
