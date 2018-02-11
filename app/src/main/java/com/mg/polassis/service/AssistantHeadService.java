package com.mg.polassis.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.mg.polassis.R;
import com.mg.polassis.misc.Translations;

public class AssistantHeadService extends Service {
    private WindowManager windowManager;
    private ImageView microphoneHead;
    private WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.FILL_PARENT,
            WindowManager.LayoutParams.FILL_PARENT,
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);

    public static boolean isWorking = false;
    public static boolean toBeShutDown = false;

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (!toBeShutDown) {

            microphoneHead = new ImageView(AssistantHeadService.this);
            microphoneHead.setImageResource(R.drawable.logo);
            microphoneHead.setScaleType(ImageView.ScaleType.FIT_XY);
            microphoneHead.setMaxHeight(64);
            microphoneHead.setMaxWidth(64);
            microphoneHead.setClickable(true);
            microphoneHead.setContentDescription(Translations.getStringResource(AssistantHeadService.this, "assistant"));
            microphoneHead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startService(new Intent(AssistantHeadService.this, BackgroundSpeechRecognitionService.class));
                }
            });

            layoutParams.gravity = getSharedPreferences("head_position", MODE_PRIVATE).getInt("gravity", Gravity.START | Gravity.CENTER_VERTICAL);

            layoutParams.height = (int) (0.1 * windowManager.getDefaultDisplay().getHeight());
            layoutParams.width = layoutParams.height;

            windowManager.addView(microphoneHead, layoutParams);

            isWorking = true;

            return START_STICKY;
        }
        else {
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (toBeShutDown) {
            if (microphoneHead != null) {
                windowManager.removeView(microphoneHead);
                microphoneHead = null;
            }
            toBeShutDown = false;
            isWorking = false;
        }
    }
}
