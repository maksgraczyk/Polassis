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

package com.mg.polassis.gui;

import android.os.Handler;
import android.widget.ImageButton;

import com.mg.polassis.R;

import java.util.Timer;
import java.util.TimerTask;

public class MicrophoneButton {
    private ImageButton microphoneButton;
    private Timer microphoneLoadingTimer;
    private boolean isMicrophoneButtonDarkGray;

    private final Handler handler = new Handler();

    public enum State
    {
        READY,
        INACTIVE,
        INACTIVE_WAITING,
        READY_TO_RECORD,
        RECORDING
    }

    public MicrophoneButton(ImageButton microphoneButton)
    {
        this.microphoneButton = microphoneButton;
    }

    public void changeState(State state)
    {
            cancelLoadingState();

            switch (state)
            {
                case READY:
                    microphoneButton.setImageResource(R.drawable.microphone);
                    microphoneButton.setClickable(true);
                    break;

                case INACTIVE:
                    microphoneButton.setImageResource(R.drawable.mic_inactive);
                    microphoneButton.setClickable(false);
                    break;

                case INACTIVE_WAITING:
                    setMicrophoneButtonAtLoadingState();
                    break;

                case READY_TO_RECORD:
                    microphoneButton.setImageResource(R.drawable.mic_recording);
                    microphoneButton.setClickable(true);
                    break;

                case RECORDING:
                    microphoneButton.setImageResource(R.drawable.mic_saying);
                    microphoneButton.setClickable(true);
                    break;
            }
    }

    public ImageButton getButton()
    {
        return microphoneButton;
    }

    private void setMicrophoneButtonAtLoadingState()
    {
        isMicrophoneButtonDarkGray = false;

        microphoneButton.setImageResource(R.drawable.mic_inactive);
        microphoneButton.setClickable(false);

        microphoneLoadingTimer = new Timer();
        microphoneLoadingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isMicrophoneButtonDarkGray) {
                            microphoneButton.setImageResource(R.drawable.mic_inactive_2);
                            isMicrophoneButtonDarkGray = true;
                        } else {
                            microphoneButton.setImageResource(R.drawable.mic_inactive);
                            isMicrophoneButtonDarkGray = false;
                        }
                    }
                });
            }
        }, 300, 300);
    }

    private void cancelLoadingState()
    {
        try {
            microphoneLoadingTimer.cancel();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        microphoneLoadingTimer = null;
    }
}
