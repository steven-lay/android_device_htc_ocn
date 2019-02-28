/*
 * Copyright (c) 2015 The CyanogenMod Project
 *               2017-2018 The LineageOS Project
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

package org.lineageos.settings.doze;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.SystemClock;
import android.util.Log;

public class TiltSensor {

    private static final boolean DEBUG = false;
    private static final String TAG = "TiltSensor";

    private Context mContext;
    private Sensor mPickup;
    private SensorManager mSensorManager;
    private TriggerEventListener mListener;

    public TiltSensor(Context context) {
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mPickup = mSensorManager.getDefaultSensor(Sensor.TYPE_PICK_UP_GESTURE);
        mListener = new TriggerListener();
        mSensorManager.requestTriggerSensor(mListener, mPickup);
    }

    private class TriggerListener extends TriggerEventListener {
        @Override
        public void onTrigger(TriggerEvent event) {
            if (DEBUG) Log.d(TAG, "Pick up detected");
            Utils.launchDozePulse(mContext);
            mSensorManager.cancelTriggerSensor(mListener, mPickup);
            mSensorManager.requestTriggerSensor(mListener, mPickup);
        }
    };

    protected void enable() {
        if (DEBUG) Log.d(TAG, "Enabling");
        mSensorManager.requestTriggerSensor(mListener, mPickup);
    }

    protected void disable() {
        if (DEBUG) Log.d(TAG, "Disabling");
        mSensorManager.cancelTriggerSensor(mListener, mPickup);
    }
}
