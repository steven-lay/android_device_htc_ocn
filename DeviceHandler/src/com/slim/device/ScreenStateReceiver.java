/*
 * Copyright (C) 2014 SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slim.device;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.util.Log;

import com.slim.device.settings.ScreenOffGesture;

public class ScreenStateReceiver extends BroadcastReceiver {

    public static final String TAG = "ScreenStateReceiver";

    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(
                ScreenOffGesture.GESTURE_SETTINGS, Activity.MODE_PRIVATE);
        if (!prefs.getBoolean(ScreenOffGesture.PREF_GESTURE_ENABLE, true)) return;
        Intent serviceIntent = new Intent(context, SensorService.class);
        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))  {
            context.stopService(serviceIntent);
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            context.startService(serviceIntent);
        }
    }
}
