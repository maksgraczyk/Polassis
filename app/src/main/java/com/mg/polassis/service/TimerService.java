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

package com.mg.polassis.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.mg.polassis.R;
import com.mg.polassis.misc.Translations;
import com.mg.polassis.gui.AlarmActivity;

public class TimerService extends Service {
    public static boolean isWorking = false;

    private CountDownTimer countdownTimer;
    private int remainingSecondsToBeSet = -1;
    private int secondsLeft;
    private int minutesLeft;
    private int hoursLeft;
    private NotificationManager notificationManager;
    private PowerManager.WakeLock wakeLock;
    private NotificationCompat.Builder notificationBuilder;
    private boolean isPaused;

    public class TimerServiceBinder extends Binder
    {
        public TimerService getService()
        {
            return TimerService.this;
        }
    }

    private final TimerServiceBinder timerServiceBinder = new TimerServiceBinder();

    @Override
    public void onCreate()
    {
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        wakeLock = ((PowerManager)getSystemService(Service.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Timer");
    }

    public int getHoursLeft()
    {
        return hoursLeft;
    }

    public int getMinutesLeft()
    {
        return minutesLeft;
    }

    public int getSecondsLeft()
    {
        return secondsLeft;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent.hasExtra("cancel"))
        {
            stopSelf();
            return START_NOT_STICKY;
        }
        else {
            isWorking = true;
            wakeLock.acquire();
            notificationBuilder = new NotificationCompat.Builder(this).setContentTitle(Translations.getStringResource(this, "timer_notify_title")).setSmallIcon(R.drawable.timer).addAction(new NotificationCompat.Action(R.drawable.close_black, Translations.getStringResource(this, "cancel"), PendingIntent.getService(this, 0, new Intent(this, TimerService.class).putExtra("cancel", true), PendingIntent.FLAG_UPDATE_CURRENT)));
            if (remainingSecondsToBeSet == -1)
                remainingSecondsToBeSet = intent.getIntExtra("s", 0);
            secondsLeft = remainingSecondsToBeSet % 60;
            minutesLeft = (remainingSecondsToBeSet % 3600) / 60;
            hoursLeft = remainingSecondsToBeSet / 3600;
            notificationBuilder.setContentText((hoursLeft < 10 ? "0" + hoursLeft : hoursLeft) + ":" + (minutesLeft < 10 ? "0" + minutesLeft : minutesLeft) + ":" + (secondsLeft < 10 ? "0" + secondsLeft : secondsLeft));
            Notification notification = notificationBuilder.build();
            notification.flags = Notification.FLAG_ONGOING_EVENT;
            startForeground(1, notification);
            countdownTimer = new CountDownTimer(remainingSecondsToBeSet * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    remainingSecondsToBeSet -= 1;

                    if (secondsLeft - 1 < 0) {
                        secondsLeft = 59;
                        if (minutesLeft - 1 < 0) {
                            minutesLeft = 59;
                            hoursLeft -= 1;
                        } else minutesLeft -= 1;
                    } else secondsLeft -= 1;

                    notificationBuilder.setContentText((hoursLeft < 10 ? "0" + hoursLeft : hoursLeft) + ":" + (minutesLeft < 10 ? "0" + minutesLeft : minutesLeft) + ":" + (secondsLeft < 10 ? "0" + secondsLeft : secondsLeft));
                    Notification notification = notificationBuilder.build();
                    notification.flags = Notification.FLAG_ONGOING_EVENT;
                    notificationManager.notify(1, notification);
                    sendBroadcast(new Intent().setAction("polassis_timer").putExtra("notes_count", (hoursLeft < 10 ? "0" + hoursLeft : hoursLeft) + ":" + (minutesLeft < 10 ? "0" + minutesLeft : minutesLeft) + ":" + (secondsLeft < 10 ? "0" + secondsLeft : secondsLeft)));
                }

                @Override
                public void onFinish() {
                    notificationBuilder.setContentText("00:00:00");
                    Notification notification = notificationBuilder.build();
                    notification.flags = Notification.FLAG_ONGOING_EVENT;
                    notificationManager.notify(1, notification);
                    Intent intent = new Intent(TimerService.this, AlarmActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    stopSelf();
                }
            };

            countdownTimer.start();

            return START_REDELIVER_INTENT;
        }
    }

    public void pauseTimer()
    {
        isPaused = true;
        countdownTimer.cancel();
        countdownTimer = null;
    }

    public boolean isPaused()
    {
        return isPaused;
    }

    public void resumeTimer()
    {
        if (isPaused) {
            countdownTimer = new CountDownTimer(remainingSecondsToBeSet * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    remainingSecondsToBeSet -= 1;

                    if (secondsLeft - 1 < 0) {
                        secondsLeft = 59;
                        if (minutesLeft - 1 < 0) {
                            minutesLeft = 59;
                            hoursLeft -= 1;
                        } else minutesLeft -= 1;
                    } else secondsLeft -= 1;

                    notificationBuilder.setContentText((hoursLeft < 10 ? "0" + hoursLeft : hoursLeft) + ":" + (minutesLeft < 10 ? "0" + minutesLeft : minutesLeft) + ":" + (secondsLeft < 10 ? "0" + secondsLeft : secondsLeft));
                    Notification notification = notificationBuilder.build();
                    notification.flags = Notification.FLAG_ONGOING_EVENT;
                    notificationManager.notify(1, notification);
                    sendBroadcast(new Intent().setAction("polassis_timer").putExtra("notes_count", (hoursLeft < 10 ? "0" + hoursLeft : hoursLeft) + ":" + (minutesLeft < 10 ? "0" + minutesLeft : minutesLeft) + ":" + (secondsLeft < 10 ? "0" + secondsLeft : secondsLeft)));
                }

                @Override
                public void onFinish() {
                    notificationBuilder.setContentText("00:00:00");
                    Notification notification = notificationBuilder.build();
                    notification.flags = Notification.FLAG_ONGOING_EVENT;
                    notificationManager.notify(1, notification);
                    Intent intent = new Intent(TimerService.this, AlarmActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    stopSelf();
                }
            };

            countdownTimer.start();
            isPaused = false;
        }
    }

    @Override
    public void onDestroy()
    {
        wakeLock.release();
        if (countdownTimer != null) countdownTimer.cancel();
        stopForeground(true);
        isWorking = false;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return timerServiceBinder;
    }
}
