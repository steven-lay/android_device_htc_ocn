/*
 * Copyright (C) 2018 The LineageOS Project
 * Copyright (C) 2016 The CyanogenMod Project
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

package org.lineageos.settings.device;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.Manifest;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.KeyEvent;
import android.util.Log;

import java.util.Iterator;

import lineageos.providers.LineageSettings;
import org.lineageos.internal.util.FileUtils;

import java.util.List;

public class ScreenGestureService extends HTCSuperGestures implements SensorEventListener {

    private static final boolean DEBUG = true;

    private static final String HAPTIC_FEEDBACK_ENABLED = "screen_haptic_feedback";
    private static final String HAPTIC_FEEDBACK_IGNORE_RINGER = "screen_haptic_ignore_ringer";

    private static final String HTC_GESTURES = "hTC Gesture_Motion";

    private static final String KEY_DOUBLE_SWIPE_DOWN = "double_swipe_down_action_key";
    private static final String KEY_SWIPE_LEFT = "swipe_left_action_key";
    private static final String KEY_SWIPE_RIGHT = "swipe_right_action_key";
    private static final String KEY_SWIPE_UP = "swipe_up_action_key";

    // Gestures
    private static final int DOUBLE_SWIPE_DOWN = 6;
    private static final int DOUBLE_TAP = 15;
    private static final int SWIPE_DOWN = 3;
    private static final int SWIPE_LEFT = 4;
    private static final int SWIPE_RIGHT = 5;
    private static final int SWIPE_UP = 2;

    private static final String CONTROL_PATH =
        "/sys/class/htc_sensorhub/sensor_hub/gesture_motion";

    /* Sensor gesture definition used to instantiate GestureMotionSensor, externally usable */
    /* These values also correspond to kernel driver values, so don't change them */
    public static final int SENSOR_GESTURE_ALL = 0x807C;
    public static final int SENSOR_GESTURE_CAMERA = 0x40;
    public static final int SENSOR_GESTURE_DOUBLE_TAP = 0x8000;
    public static final int SENSOR_GESTURE_SWIPE_DOWN = 0x8;
    public static final int SENSOR_GESTURE_SWIPE_LEFT = 0x10;
    public static final int SENSOR_GESTURE_SWIPE_RIGHT = 0x20;
    public static final int SENSOR_GESTURE_SWIPE_UP = 0x4;

    private Sensor mSensor = null;
    private SensorEventListener mSensorEventListener;
    private SensorManager mSensorManager;

    private int mSwipeDownAction;
    private int mSwipeLeftAction;
    private int mSwipeRightAction;
    private int mSwipeUpAction;

    private boolean mHapticIgnoreRinger;
    private boolean mHapticFeedbackEnabled;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        loadPreferences(sharedPrefs);
        sharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mSensorEventListener = this;

        Iterator iterator = mSensorManager.getSensorList(-1).iterator();
        while (iterator.hasNext()) {
            Sensor sensor = (Sensor) iterator.next();
            if (sensor.getName().equals(HTC_GESTURES)) {
                if (DEBUG) Log.d(TAG, "found gesture sensor");
                mSensor = sensor;
                if (!FileUtils.writeLine(CONTROL_PATH, Integer.toHexString(SENSOR_GESTURE_ALL))) {
                    Log.w(TAG, "Failed to write control path, unable to disable sensor");
                }
            }
        }
        if (mSensor != null) {
            mSensorManager.registerListener(mSensorEventListener,
                mSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }

    private void tryHapticFeedback() {
        if (mHapticIgnoreRinger && mHapticFeedbackEnabled)
            doHapticFeedback();
        if (mHapticFeedbackEnabled && mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT)
            doHapticFeedback();
    }

    public final void onAccuracyChanged(Sensor sensor, int i) {}

    public final void onSensorChanged(SensorEvent sensorEvent) {
        mSensorManager.unregisterListener(mSensorEventListener);
        float gesture = sensorEvent.values[0];
        if (DEBUG) Log.d(TAG, "Sensor type=" + sensorEvent.sensor.getType() +
            "," + sensorEvent.values[0] + "," + sensorEvent.values[1]);
        int action = gestureToAction((int) gesture);
        if (action > -1) {
            handleGestureAction(action);
        }
    }

    private boolean isDoubleTapEnabled() {
        return (Settings.Secure.getInt(mContext.getContentResolver(),
            Settings.Secure.DOUBLE_TAP_TO_WAKE, 0) != 0);
    }

    private int gestureToAction(int gesture) {
        if (DEBUG) Log.d(TAG, "Gesture to action: " + gesture);
        switch (gesture) {
            case DOUBLE_TAP:
                if (!isDoubleTapEnabled()) {
                    mSensorManager.registerListener(mSensorEventListener,
                        mSensor, SensorManager.SENSOR_DELAY_GAME);
                    return -1;
                }
                tryHapticFeedback();
                mPowerManager.wakeUp(SystemClock.uptimeMillis());
                return -1;
            case SWIPE_UP:
                return mSwipeUpAction;
            case DOUBLE_SWIPE_DOWN:
                return mSwipeDownAction;
            case SWIPE_LEFT:
                return mSwipeLeftAction;
            case SWIPE_RIGHT:
                return mSwipeRightAction;
            default:
                return -1;
        }
    }

    private void handleGestureAction(int action) {
        if (DEBUG) Log.d(TAG, "Performing gesture action: " + action);
        switch (action) {
            case TouchscreenGestureConstants.ACTION_CAMERA:
                launchCamera();
                tryHapticFeedback();
                break;
            case TouchscreenGestureConstants.ACTION_FLASHLIGHT:
                toggleFlashlight();
                tryHapticFeedback();
                break;
            case TouchscreenGestureConstants.ACTION_BROWSER:
                launchBrowser();
                tryHapticFeedback();
                break;
            case TouchscreenGestureConstants.ACTION_DIALER:
                launchDialer();
                tryHapticFeedback();
                break;
            case TouchscreenGestureConstants.ACTION_EMAIL:
                launchEmail();
                tryHapticFeedback();
                break;
            case TouchscreenGestureConstants.ACTION_MESSAGES:
                launchMessages();
                tryHapticFeedback();
                break;
            case TouchscreenGestureConstants.ACTION_PLAY_PAUSE_MUSIC:
                playPauseMusic();
                tryHapticFeedback();
                break;
            case TouchscreenGestureConstants.ACTION_PREVIOUS_TRACK:
                previousTrack();
                tryHapticFeedback();
                break;
            case TouchscreenGestureConstants.ACTION_NEXT_TRACK:
                nextTrack();
                tryHapticFeedback();
                break;
            case TouchscreenGestureConstants.ACTION_VOLUME_DOWN:
                volumeDown();
                tryHapticFeedback();
                break;
            case TouchscreenGestureConstants.ACTION_VOLUME_UP:
                volumeUp();
                tryHapticFeedback();
                break;
        }
        mSensorManager.registerListener(mSensorEventListener,
            mSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    private void loadPreferences(SharedPreferences sharedPreferences) {
        try {
            mSwipeUpAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_UP,
                Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
            mSwipeDownAction = Integer.parseInt(sharedPreferences.getString(KEY_DOUBLE_SWIPE_DOWN,
                Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
            mSwipeLeftAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_LEFT,
                Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
            mSwipeRightAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_RIGHT,
                Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
            mHapticIgnoreRinger = sharedPreferences.getBoolean(HAPTIC_FEEDBACK_IGNORE_RINGER, true);
            mHapticFeedbackEnabled = sharedPreferences.getBoolean(HAPTIC_FEEDBACK_ENABLED, true);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error loading preferences");
        }
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                try {
                    if (KEY_SWIPE_UP.equals(key)) {
                        mSwipeUpAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_UP,
                            Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
                    } else if (KEY_DOUBLE_SWIPE_DOWN.equals(key)) {
                        mSwipeDownAction = Integer.parseInt(sharedPreferences.getString(KEY_DOUBLE_SWIPE_DOWN,
                            Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
                    } else if (KEY_SWIPE_LEFT.equals(key)) {
                        mSwipeLeftAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_LEFT,
                            Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
                    } else if (KEY_SWIPE_RIGHT.equals(key)) {
                        mSwipeRightAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_RIGHT,
                            Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
                    } else if (HAPTIC_FEEDBACK_IGNORE_RINGER.equals(key)) {
                        mHapticIgnoreRinger = sharedPreferences.getBoolean(HAPTIC_FEEDBACK_IGNORE_RINGER, true);
                    } else if (HAPTIC_FEEDBACK_ENABLED.equals(key)) {
                        mHapticFeedbackEnabled = sharedPreferences.getBoolean(HAPTIC_FEEDBACK_ENABLED, true);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error loading preferences");
                }
            }
        };

}
