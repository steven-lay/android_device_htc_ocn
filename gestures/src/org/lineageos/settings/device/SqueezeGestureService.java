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
import android.app.ActivityManager;
import android.content.ComponentName;
import android.app.Instrumentation;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ApplicationInfo;
import android.view.MotionEvent;
import android.util.Log;
import android.widget.ImageView;
import android.graphics.PixelFormat;
import android.view.Display;
import android.view.Gravity;
import android.os.Handler;

import java.util.Iterator;
import java.lang.Runtime;
import java.io.IOException;

import lineageos.providers.LineageSettings;
import org.lineageos.internal.util.FileUtils;

import java.util.List;

public class SqueezeGestureService extends HTCSuperGestures {

    private static final boolean DEBUG = false;

    private static final int SHORTSQUEEZE = 100;
    private static final int LONGSQUEEZE = 101;

    private static final String TAG = "SqueezeService";
    private static final String HTC_EDGEGESTURESENSOR = "hTC Edge Gesture";
    private static final String GESTURE_WAKEUP_REASON = "squeeze-gesture-wakeup";

    private static final String EDGE_THRESHOLD_PATH = "/sys/class/htc_sensorhub/sensor_hub/edge_thd";

    private static final String SQUEEZE_FORCE = "squeeze_force";
    private static final String SQUEEZE_GESTURE_ENABLE = "squeeze_enabled";
    private static final String SQUEEZE_LONG_ACTION = "squeeze_long";
    private static final String SQUEEZE_LONG_SQUEEZE_DURATION = "long_squeeze_duration";
    private static final String SQUEEZE_SHORT_ACTION = "squeeze_short";

    private static final String HAPTIC_FEEDBACK_ENABLED = "squeeze_haptic_feedback";
    private static final String HAPTIC_FEEDBACK_IGNORE_RINGER = "squeeze_haptic_ignore_ringer";

    private static final int SQUEEZE_FORCE_DEFAULT = 150;
    private static final int LONG_SQUEEZE_DURATION_DEFAULT = 700;

    private static final int ACTION_TAKE_SCREENSHOT = 12;
    private static final int ACTION_TURN_SCREEN_ON_OFF = 13;

    private Handler mHandler;
    private ImageView squeezeForceView;
    private Sensor mEdgeGestureSensor = null;
    private SensorManager mSensorManager;

    private boolean mSqueezeEnabled;
    private final int mForcePrefMin = 100;
    private int mForcePref = 0;
    private int mLongSqueezeAction;
    private int mLongSqueezeDuration = 700;
    private int mShortSqueezeAction;
    private long mHoldDownTime;

    private boolean mHapticIgnoreRinger;
    private boolean mHapticFeedbackEnabled;

    private final int LONG_SQUEEZE_ACTION = 1;
    private final int SHORT_SQUEEZE_VIBRATION = 2;

    private HtcEdgeGestureSensorEventListener mEdgeGestureSensorEventListener = new HtcEdgeGestureSensorEventListener();

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        loadPreferences(sharedPrefs);
        sharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Iterator iterator = mSensorManager.getSensorList(-1).iterator();
        while (iterator.hasNext()) {
            Sensor sensor = (Sensor) iterator.next();
            if (sensor.getName().equals(HTC_EDGEGESTURESENSOR)) {
                if (DEBUG) Log.d(TAG, "found Edge Gesture sensor");
                mEdgeGestureSensor = sensor;
                if (!FileUtils.writeLine(EDGE_THRESHOLD_PATH, Integer.toString(mForcePref))) {
                    Log.w(TAG, "Failed to write force threshold sysfs path");
                }
            }
        }
        if (mEdgeGestureSensor != null) {
            if (mSqueezeEnabled) {
                mSensorManager.registerListener(mEdgeGestureSensorEventListener,
                    mEdgeGestureSensor, SensorManager.SENSOR_DELAY_GAME);
                if (DEBUG) Log.d(TAG, "Registered Edge Gesture Sensor Listener");
            }
        }

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                case LONG_SQUEEZE_ACTION:
                    tryHapticFeedback();
                    int action = gestureToAction(LONGSQUEEZE);
                    if (action > -1)
                        handleGestureAction(action);
                    break;
                case SHORT_SQUEEZE_VIBRATION:
                    tryHapticFeedback();
                    break;
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(mEdgeGestureSensorEventListener);
    }

    private void tryHapticFeedback() {
        if (mHapticIgnoreRinger && mHapticFeedbackEnabled)
            doHapticFeedback();
        if (mHapticFeedbackEnabled && mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT)
            doHapticFeedback();
    }

    /**
     *  sensorEvent.values[0] = 1.0f the moment the edge sensors are squeezed down
     *  sensorEvent.values[0] = 2.0f the moment the squeezed edge sensors are released
     *  sensorEvent.values[0] = 3.0f squeezed down for too long or gesture cancelled
     *  sensorEvent.values[1] holds the value of the squeeze force
     */
    private class HtcEdgeGestureSensorEventListener implements SensorEventListener {

        public final void onAccuracyChanged(Sensor sensor, int i) {}

        public void onSensorChanged(SensorEvent sensorEvent) {
            float value = sensorEvent.values[0];
            if (DEBUG) Log.d(TAG, "HtcEdgeGestureSensor value = " + value);
            if (value == 1.0f) {
                mHoldDownTime = SystemClock.elapsedRealtime();
                if (mLongSqueezeAction != TouchscreenGestureConstants.ACTION_DO_NOTHING)
                    mHandler.sendEmptyMessageDelayed(LONG_SQUEEZE_ACTION, mLongSqueezeDuration);
                if (mShortSqueezeAction != TouchscreenGestureConstants.ACTION_DO_NOTHING)
                    mHandler.sendEmptyMessageDelayed(SHORT_SQUEEZE_VIBRATION, 100);
                if (!ScreenStateReceiver.isScreenOn())
                    mGestureWakeLock.acquire(5000);
            } else if (value == 2.0f) {
                long SqueezeReleaseTime = SystemClock.elapsedRealtime() - mHoldDownTime;
                if (SqueezeReleaseTime > 100 && SqueezeReleaseTime < mLongSqueezeDuration) {
                    mHandler.removeMessages(LONG_SQUEEZE_ACTION);
                    if (getForegroundApp(mContext).equals("Camera")) {
                        simulateKey(KeyEvent.KEYCODE_VOLUME_UP);
                    } else {
                        int action = gestureToAction(SHORTSQUEEZE);
                        if (action > -1)
                            handleGestureAction(action);
                    }
                } else if (SqueezeReleaseTime < 100) {
                    mHandler.removeMessages(LONG_SQUEEZE_ACTION);
                    mHandler.removeMessages(SHORT_SQUEEZE_VIBRATION);
                    if (mGestureWakeLock.isHeld())
                        mGestureWakeLock.release();
                }
            } else if (value == 3.0f) {
                mHandler.removeMessages(LONG_SQUEEZE_ACTION);
                if (mGestureWakeLock.isHeld())
                    mGestureWakeLock.release();
            }
        }
    }

    private int gestureToAction(int gesture) {
        if (DEBUG) Log.d(TAG, "Gesture to action: " + gesture);
        switch (gesture) {
            case SHORTSQUEEZE:
                return mShortSqueezeAction;
            case LONGSQUEEZE:
                return mLongSqueezeAction;
            default:
                return -1;
        }
    }

    private void handleGestureAction(int action) {
        if (DEBUG) Log.d(TAG, "Performing gesture action: " + action);
        switch (action) {
            case TouchscreenGestureConstants.ACTION_CAMERA:
                launchCamera();
                break;
            case TouchscreenGestureConstants.ACTION_FLASHLIGHT:
                toggleFlashlight();
                break;
            case TouchscreenGestureConstants.ACTION_BROWSER:
                launchBrowser();
                break;
            case TouchscreenGestureConstants.ACTION_DIALER:
                launchDialer();
                break;
            case TouchscreenGestureConstants.ACTION_EMAIL:
                launchEmail();
                break;
            case TouchscreenGestureConstants.ACTION_MESSAGES:
                launchMessages();
                break;
            case TouchscreenGestureConstants.ACTION_PLAY_PAUSE_MUSIC:
                playPauseMusic();
                break;
            case TouchscreenGestureConstants.ACTION_PREVIOUS_TRACK:
                previousTrack();
                break;
            case TouchscreenGestureConstants.ACTION_NEXT_TRACK:
                nextTrack();
                break;
            case TouchscreenGestureConstants.ACTION_VOLUME_DOWN:
                volumeDown();
                break;
            case TouchscreenGestureConstants.ACTION_VOLUME_UP:
                volumeUp();
                break;
            case ACTION_TAKE_SCREENSHOT:
                takeScreenshot();
                break;
            case ACTION_TURN_SCREEN_ON_OFF:
                turnScreenOnOff();
                break;
        }
    }

    private void loadPreferences(SharedPreferences sharedPreferences) {
        try {
            mShortSqueezeAction = Integer.parseInt(sharedPreferences.getString(SQUEEZE_SHORT_ACTION,
                Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
            mLongSqueezeAction = Integer.parseInt(sharedPreferences.getString(SQUEEZE_LONG_ACTION,
                Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
            mSqueezeEnabled = sharedPreferences.getBoolean(SQUEEZE_GESTURE_ENABLE, true);
            mForcePref = sharedPreferences.getInt(SQUEEZE_FORCE, SQUEEZE_FORCE_DEFAULT);
            mForcePref += mForcePrefMin;
            mHapticFeedbackEnabled = sharedPreferences.getBoolean(HAPTIC_FEEDBACK_ENABLED, true);
            mHapticIgnoreRinger = sharedPreferences.getBoolean(HAPTIC_FEEDBACK_IGNORE_RINGER, true);
            mLongSqueezeDuration = Integer.parseInt(sharedPreferences.getString(SQUEEZE_LONG_SQUEEZE_DURATION,
                Integer.toString(LONG_SQUEEZE_DURATION_DEFAULT)));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error loading preferences");
        }
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                try {
                    if (SQUEEZE_SHORT_ACTION.equals(key)) {
                        mShortSqueezeAction = Integer.parseInt(sharedPreferences.getString(SQUEEZE_SHORT_ACTION,
                            Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
                    } else if (SQUEEZE_LONG_ACTION.equals(key)) {
                        mLongSqueezeAction = Integer.parseInt(sharedPreferences.getString(SQUEEZE_LONG_ACTION,
                            Integer.toString(TouchscreenGestureConstants.ACTION_DO_NOTHING)));
                    } else if (SQUEEZE_GESTURE_ENABLE.equals(key)) {
                        mSqueezeEnabled = sharedPreferences.getBoolean(SQUEEZE_GESTURE_ENABLE, true);
                        if (mSqueezeEnabled) {
                            mSensorManager.registerListener(mEdgeGestureSensorEventListener,
                                mEdgeGestureSensor, SensorManager.SENSOR_DELAY_GAME);
                        } else {
                            mSensorManager.unregisterListener(mEdgeGestureSensorEventListener);
                        }
                    } else if (SQUEEZE_FORCE.equals(key)) {
                        mForcePref = sharedPreferences.getInt(SQUEEZE_FORCE, SQUEEZE_FORCE_DEFAULT);
                        mForcePref += mForcePrefMin;
                        if (!FileUtils.writeLine(EDGE_THRESHOLD_PATH, Integer.toString(mForcePref))) {
                            Log.w(TAG, "Failed to write force threshold sysfs path");
                        }
                    } else if (HAPTIC_FEEDBACK_ENABLED.equals(key)) {
                        mHapticFeedbackEnabled = sharedPreferences.getBoolean(HAPTIC_FEEDBACK_ENABLED, true);
                    } else if (SQUEEZE_LONG_SQUEEZE_DURATION.equals(key)) {
                        mLongSqueezeDuration = Integer.parseInt(sharedPreferences.getString(SQUEEZE_LONG_SQUEEZE_DURATION,
                            Integer.toString(LONG_SQUEEZE_DURATION_DEFAULT)));
                    } else if (HAPTIC_FEEDBACK_IGNORE_RINGER.equals(key)) {
                        mHapticIgnoreRinger = sharedPreferences.getBoolean(HAPTIC_FEEDBACK_IGNORE_RINGER, true);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error loading preferences");
                }
            }
        };

    private void takeScreenshot() {
        if (ScreenStateReceiver.isScreenOn()) {
            simulateKey(KeyEvent.KEYCODE_SYSRQ);
        } else {
            Log.d(TAG, "Cannot take screenshot while screen is off");
        }
    }

    private void turnScreenOnOff() {
        simulateKey(KeyEvent.KEYCODE_POWER);
    }

    public static String getForegroundApp(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List < ActivityManager.RunningTaskInfo > runningTaskInfo = manager.getRunningTasks(1);

        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        PackageManager packageManager = context.getPackageManager();

        String appName = "empty";
        try {
            return appName = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(
                componentInfo.getPackageName(), PackageManager.GET_META_DATA));
        } catch (NameNotFoundException e) {
            Log.e("Application not found ", e.toString());
            return "empty";
        }

    }

    public static void simulateKey(final int KeyCode) {

        new Thread() {
            @Override
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(KeyCode);
                } catch (Exception e) {
                    Log.e("Exception when sendKeyDownUpSync", e.toString());
                }
            }

        }.start();
    }

}
