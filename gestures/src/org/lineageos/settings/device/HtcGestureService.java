/*
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
import android.hardware.SensorEvent;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraAccessException;
import android.Manifest;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import lineageos.providers.LineageSettings;

import java.util.List;

public class HtcGestureService extends Service {

    private static final boolean DEBUG = false;

    public static final String TAG = "GestureService";
    private static final String GESTURE_WAKEUP_REASON = "gesture-wakeup";

    private static final String KEY_SWIPE_UP = "swipe_up_action_key";
    private static final String KEY_SWIPE_DOWN = "swipe_down_action_key";
    private static final String KEY_SWIPE_LEFT = "swipe_left_action_key";
    private static final String KEY_SWIPE_RIGHT = "swipe_right_action_key";

    private static final int SENSOR_WAKELOCK_DURATION = 500;

    private static final int ACTION_NONE = 0;
    private static final int ACTION_CAMERA = 1;
    private static final int ACTION_TORCH = 2;
    private static final int ACTION_BROWSER = 3;
    private static final int ACTION_DIALER = 4;
    private static final int ACTION_EMAIL = 5;
    private static final int ACTION_MESSAGES = 6;

    private Context mContext;
    private GestureMotionSensor mGestureSensor;
    private PowerManager mPowerManager;
    private AudioManager mAudioManager;
    private WakeLock mSensorWakeLock;
    private CameraManager mCameraManager;
    private Vibrator mVibrator;

    private String mRearCameraId;
    private boolean mTorchEnabled;

    private int mSwipeUpAction;
    private int mSwipeDownAction;
    private int mSwipeLeftAction;
    private int mSwipeRightAction;

    private GestureMotionSensor.GestureMotionSensorListener mListener =
        new GestureMotionSensor.GestureMotionSensorListener() {
        @Override
        public void onEvent(int type, SensorEvent event) {
            if (DEBUG) Log.d(TAG, "Received event: " + type);
            switch (type) {
                case GestureMotionSensor.SENSOR_GESTURE_DOUBLE_TAP:
                    mPowerManager.wakeUp(SystemClock.uptimeMillis());
                    break;
                case GestureMotionSensor.SENSOR_GESTURE_SWIPE_UP:
                case GestureMotionSensor.SENSOR_GESTURE_SWIPE_DOWN:
                case GestureMotionSensor.SENSOR_GESTURE_SWIPE_LEFT:
                case GestureMotionSensor.SENSOR_GESTURE_SWIPE_RIGHT:
                    handleGestureAction(gestureToAction(type));
                    break;
                case GestureMotionSensor.SENSOR_GESTURE_CAMERA:
                    launchCamera();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service");
        super.onCreate();

        mContext = this;
        mGestureSensor = GestureMotionSensor.getInstance(mContext);
        mGestureSensor.registerListener(mListener);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        loadPreferences(sharedPrefs);
        sharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mSensorWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HtcGestureWakeLock");
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        mCameraManager.registerTorchCallback(new TorchModeCallback(), null);
    }


    private class TorchModeCallback extends CameraManager.TorchCallback {
        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (!cameraId.equals(mRearCameraId)) return;
            mTorchEnabled = enabled;
        }

        @Override
        public void onTorchModeUnavailable(String cameraId) {
            if (!cameraId.equals(mRearCameraId)) return;
            mTorchEnabled = false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onDisplayOn() {
        if (DEBUG) Log.d(TAG, "Display on");
        if (isDoubleTapEnabled()) {
            mGestureSensor.disableGesture(GestureMotionSensor.SENSOR_GESTURE_DOUBLE_TAP);
        }
        if (mSwipeUpAction != ACTION_NONE) {
            mGestureSensor.disableGesture(GestureMotionSensor.SENSOR_GESTURE_SWIPE_UP);
        }
        if (mSwipeDownAction != ACTION_NONE) {
            mGestureSensor.disableGesture(GestureMotionSensor.SENSOR_GESTURE_SWIPE_DOWN);
        }
        if (mSwipeLeftAction != ACTION_NONE) {
            mGestureSensor.disableGesture(GestureMotionSensor.SENSOR_GESTURE_SWIPE_LEFT);
        }
        if (mSwipeRightAction != ACTION_NONE) {
            mGestureSensor.disableGesture(GestureMotionSensor.SENSOR_GESTURE_SWIPE_RIGHT);
        }
        mGestureSensor.stopListening();
    }

    private void onDisplayOff() {
        if (DEBUG) Log.d(TAG, "Display off");
        if (isDoubleTapEnabled()) {
            mGestureSensor.enableGesture(GestureMotionSensor.SENSOR_GESTURE_DOUBLE_TAP);
        }
        if (mSwipeUpAction != ACTION_NONE) {
            mGestureSensor.enableGesture(GestureMotionSensor.SENSOR_GESTURE_SWIPE_UP);
        }
        if (mSwipeDownAction != ACTION_NONE) {
            mGestureSensor.enableGesture(GestureMotionSensor.SENSOR_GESTURE_SWIPE_DOWN);
        }
        if (mSwipeLeftAction != ACTION_NONE) {
            mGestureSensor.enableGesture(GestureMotionSensor.SENSOR_GESTURE_SWIPE_LEFT);
        }
        if (mSwipeRightAction != ACTION_NONE) {
            mGestureSensor.enableGesture(GestureMotionSensor.SENSOR_GESTURE_SWIPE_RIGHT);
        }
        mGestureSensor.beginListening();
    }

    private boolean isDoubleTapEnabled() {
        return (Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.DOUBLE_TAP_TO_WAKE, 0) != 0);
    }

    private void handleGestureAction(int action) {
        if (DEBUG) Log.d(TAG, "Performing gesture action: " + action);
        switch (action) {
            case ACTION_CAMERA:
                launchCamera();
                break;
            case ACTION_TORCH:
                toggleFlashlight();
                break;
            case ACTION_BROWSER:
                launchBrowser();
                break;
            case ACTION_DIALER:
                launchDialer();
                break;
            case ACTION_EMAIL:
                launchEmail();
                break;
            case ACTION_MESSAGES:
                launchMessages();
                break;
            case ACTION_NONE:
            default:
                break;
        }
    }

    private void launchCamera() {
        mSensorWakeLock.acquire(SENSOR_WAKELOCK_DURATION);
        final Intent intent = new Intent(lineageos.content.Intent.ACTION_SCREEN_CAMERA_GESTURE);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT,
                Manifest.permission.STATUS_BAR_SERVICE);
        doHapticFeedback();
    }

    private void toggleFlashlight() {
        String rearCameraId = getRearCameraId();
        if (rearCameraId != null) {
            mSensorWakeLock.acquire(SENSOR_WAKELOCK_DURATION);
            try {
                mCameraManager.setTorchMode(rearCameraId, !mTorchEnabled);
                mTorchEnabled = !mTorchEnabled;
            } catch (CameraAccessException e) {
                // Ignore
            }
            doHapticFeedback();
            onDisplayOn(); // Reset Gestures without turning screen on
            onDisplayOff(); // Re-enable gestures
        }
    }

    private void launchBrowser() {
        mSensorWakeLock.acquire(SENSOR_WAKELOCK_DURATION);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), GESTURE_WAKEUP_REASON);
        final Intent intent = getLaunchableIntent(
                new Intent(Intent.ACTION_VIEW, Uri.parse("http:")));
        startActivitySafely(intent);
        doHapticFeedback();
    }

    private void launchDialer() {
        mSensorWakeLock.acquire(SENSOR_WAKELOCK_DURATION);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), GESTURE_WAKEUP_REASON);
        final Intent intent = new Intent(Intent.ACTION_DIAL, null);
        startActivitySafely(intent);
        doHapticFeedback();
    }

    private void launchEmail() {
        mSensorWakeLock.acquire(SENSOR_WAKELOCK_DURATION);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), GESTURE_WAKEUP_REASON);
        final Intent intent = getLaunchableIntent(
                new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:")));
        startActivitySafely(intent);
        doHapticFeedback();
    }

    private void launchMessages() {
        mSensorWakeLock.acquire(SENSOR_WAKELOCK_DURATION);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), GESTURE_WAKEUP_REASON);
        final String defaultApplication = Settings.Secure.getString(
                mContext.getContentResolver(), "sms_default_application");
        final PackageManager pm = mContext.getPackageManager();
        final Intent intent = pm.getLaunchIntentForPackage(defaultApplication);
        if (intent != null) {
            startActivitySafely(intent);
            doHapticFeedback();
        }
    }

    private void startActivitySafely(final Intent intent) {
        if (intent == null) {
            Log.w(TAG, "No intent passed to startActivitySafely");
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            final UserHandle user = new UserHandle(UserHandle.USER_CURRENT);
            mContext.startActivityAsUser(intent, null, user);
        } catch (ActivityNotFoundException e) {
            // Ignore
        }
    }

    private void doHapticFeedback() {
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            return;
        }

        if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            final boolean enabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                    LineageSettings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, 1) != 0;
            if (enabled) {
                mVibrator.vibrate(50);
            }
        }
    }

    private String getRearCameraId() {
        if (mRearCameraId == null) {
            try {
                for (final String cameraId : mCameraManager.getCameraIdList()) {
                    final CameraCharacteristics characteristics =
                            mCameraManager.getCameraCharacteristics(cameraId);
                    final int orientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (orientation == CameraCharacteristics.LENS_FACING_BACK) {
                        mRearCameraId = cameraId;
                        break;
                    }
                }
            } catch (CameraAccessException e) {
                // Ignore
            }
        }
        return mRearCameraId;
    }

    private Intent getLaunchableIntent(Intent intent) {
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> resInfo = pm.queryIntentActivities(intent, 0);
        if (resInfo.isEmpty()) {
            return null;
        }
        return pm.getLaunchIntentForPackage(resInfo.get(0).activityInfo.packageName);
    }

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                onDisplayOff();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                onDisplayOn();
            }
        }
    };

    private int gestureToAction(int gesture) {
        switch (gesture) {
            case GestureMotionSensor.SENSOR_GESTURE_SWIPE_UP:
                return mSwipeUpAction;
            case GestureMotionSensor.SENSOR_GESTURE_SWIPE_DOWN:
                return mSwipeDownAction;
            case GestureMotionSensor.SENSOR_GESTURE_SWIPE_LEFT:
                return mSwipeLeftAction;
            case GestureMotionSensor.SENSOR_GESTURE_SWIPE_RIGHT:
                return mSwipeRightAction;
            default:
                return -1;
        }
    }

    private void loadPreferences(SharedPreferences sharedPreferences) {
        try {
            mSwipeUpAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_UP,
                        Integer.toString(ACTION_NONE)));
            mSwipeDownAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_DOWN,
                        Integer.toString(ACTION_NONE)));
            mSwipeLeftAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_LEFT,
                    Integer.toString(ACTION_NONE)));
            mSwipeRightAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_RIGHT,
                        Integer.toString(ACTION_NONE)));
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
                                Integer.toString(ACTION_NONE)));
                } else if (KEY_SWIPE_DOWN.equals(key)) {
                    mSwipeDownAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_DOWN,
                                Integer.toString(ACTION_NONE)));
                } else if (KEY_SWIPE_LEFT.equals(key)) {
                    mSwipeLeftAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_LEFT,
                                Integer.toString(ACTION_NONE)));
                } else if (KEY_SWIPE_RIGHT.equals(key)) {
                    mSwipeRightAction = Integer.parseInt(sharedPreferences.getString(KEY_SWIPE_RIGHT,
                                Integer.toString(ACTION_NONE)));
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error loading preferences");
            }
        }
    };
}
