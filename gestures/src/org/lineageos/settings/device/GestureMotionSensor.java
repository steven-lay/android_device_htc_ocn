/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package org.lineageos.settings.device;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lineageos.internal.util.FileUtils;

public class GestureMotionSensor {

    private static final boolean DEBUG = false;
    private static final String TAG = "GestureMotionSensor";

    private static final String CONTROL_PATH = "/sys/class/htc_sensorhub/sensor_hub/gesture_motion";
    private static final String GESTURE_MOTION_SENSOR_NAME = "hTC Gesture_Motion";

    /* Sensor gesture definition used to instantiate GestureMotionSensor, externally usable */
    /* These values also correspond to kernel driver values, so don't change them */
    public static final int SENSOR_GESTURE_SWIPE_UP = 0x4;
    public static final int SENSOR_GESTURE_SWIPE_DOWN = 0x8;
    public static final int SENSOR_GESTURE_SWIPE_LEFT = 0x10;
    public static final int SENSOR_GESTURE_SWIPE_RIGHT = 0x20;
    public static final int SENSOR_GESTURE_CAMERA = 0x40;
    public static final int SENSOR_GESTURE_DOUBLE_TAP = 0x8000;

    /* Corresponds to actual sensor ID, internal use only */
    private static final int SENSOR_TYPE_ANY_MOTION = 65537;
    private static final int SENSOR_TYPE_GESTURE_MOTION = 65538;

    /* Corresponds to sensor event value, internal use only */
    private static final int SENSOR_EVENT_ID_DOUBLE_TAP = 15;
    private static final int SENSOR_EVENT_ID_SWIPE_UP = 2;
    private static final int SENSOR_EVENT_ID_SWIPE_DOWN = 3;
    private static final int SENSOR_EVENT_ID_SWIPE_LEFT = 4;
    private static final int SENSOR_EVENT_ID_SWIPE_RIGHT = 5;
    private static final int SENSOR_EVENT_ID_CAMERA = 6;

    protected static final int BATCH_LATENCY_IN_MS = 100;

    private static GestureMotionSensor sInstance;
    private Context mContext;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mEnabledGestures;
    private List<GestureMotionSensorListener> mListeners;

    private static int sensorEventToGesture(int event) {
        switch (event) {
            case SENSOR_EVENT_ID_DOUBLE_TAP:
                return SENSOR_GESTURE_DOUBLE_TAP;
            case SENSOR_EVENT_ID_SWIPE_UP:
                return SENSOR_GESTURE_SWIPE_UP;
            case SENSOR_EVENT_ID_SWIPE_DOWN:
                return SENSOR_GESTURE_SWIPE_DOWN;
            case SENSOR_EVENT_ID_SWIPE_LEFT:
                return SENSOR_GESTURE_SWIPE_LEFT;
            case SENSOR_EVENT_ID_SWIPE_RIGHT:
                return SENSOR_GESTURE_SWIPE_RIGHT;
            default:
                return -1;
        }
    }

    public interface GestureMotionSensorListener {
        public void onEvent(int gesture, SensorEvent event);
    }

    private GestureMotionSensor(Context context) {
        mContext = context;
        mEnabledGestures = 0;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensor = getGestureMotionSensor();
        mListeners = new ArrayList<GestureMotionSensorListener>();
    }

    public void enableGesture(int gesture) {
        if (DEBUG) Log.d(TAG, "Enabling");

        mEnabledGestures = mEnabledGestures | gesture;
    }

    public void disableGesture(int gesture) {
        if (DEBUG) Log.d(TAG, "Disabling");

        mEnabledGestures = mEnabledGestures & (~gesture);
    }

    public static GestureMotionSensor getInstance(Context context) {
        /* The hTC Gesture_Motion sensor doesn't accept multiple listeners anyway */
        if (sInstance == null) {
            sInstance = new GestureMotionSensor(context);
        }

        return sInstance;
    }

    public void beginListening() {
        if (!FileUtils.isFileReadable(CONTROL_PATH) || !FileUtils.isFileWritable(CONTROL_PATH)) {
            Log.w(TAG, "Control path not accessible, unable to disable sensor");
            return;
        }

        if (!FileUtils.writeLine(CONTROL_PATH, Integer.toHexString(mEnabledGestures))) {
            Log.w(TAG, "Failed to write control path, unable to disable sensor");
            return;
        }

        mSensorManager.registerListener(mSensorEventListener, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL, BATCH_LATENCY_IN_MS * 1000);
    }

    public void stopListening() {
        if (!FileUtils.isFileReadable(CONTROL_PATH) || !FileUtils.isFileWritable(CONTROL_PATH)) {
            Log.w(TAG, "Control path not accessible, unable to disable sensor");
            return;
        }

        if (!FileUtils.writeLine(CONTROL_PATH, Integer.toHexString(0))) {
            Log.w(TAG, "Failed to write control path, unable to disable sensor");
            return;
        }

        mSensorManager.unregisterListener(mSensorEventListener);
    }

    public void registerListener(GestureMotionSensorListener listener) {
        mListeners.add(listener);
    }

    private Sensor getGestureMotionSensor() {
        /* TODO: figure out why
         * mSensorManager.getDefaultSensor(65538);
         * isn't returning a valid sensor.
         */
        Iterator it = mSensorManager.getSensorList(Sensor.TYPE_ALL).iterator();
        while (it.hasNext()) {
            Sensor sensor = (Sensor) it.next();
            if (GESTURE_MOTION_SENSOR_NAME.equals(sensor.getName())) {
                return sensor;
            }
        }
        Log.w(TAG, "Unable to find valid gesture motion sensor");
        return null;
    }

    private void onSensorEvent(int gesture, SensorEvent event) {
        for (GestureMotionSensorListener l : mListeners) {
            l.onEvent(gesture, event);
        }
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (DEBUG) Log.d(TAG, "onSensorChanged: got event: " + (int) event.values[0]);
            int sensorEvent = (int) event.values[0];
            int gesture = sensorEventToGesture(sensorEvent);
            if ((gesture & mEnabledGestures) != 0) {
                /* Only report events which we care about */
                onSensorEvent(gesture, event);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            /* Empty */
        }
    };
}
