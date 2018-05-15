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
import android.os.VibrationEffect;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.KeyEvent;
import android.util.Log;

import java.util.Iterator;

import lineageos.providers.LineageSettings;
import org.lineageos.internal.util.FileUtils;

import java.util.List;

public class ScreenGestureService extends Service implements SensorEventListener {

    private static final boolean DEBUG = true;

    private static final String TAG = "GestureService";
    private static final String GESTURE_WAKEUP_REASON = "gesture-wakeup";
    private static final String HTC_GESTURES = "hTC Gesture_Motion";

    private static final String KEY_SWIPE_UP = "swipe_up_action_key";
    private static final String KEY_DOUBLE_SWIPE_DOWN = "double_swipe_down_action_key";
    private static final String KEY_SWIPE_LEFT = "swipe_left_action_key";
    private static final String KEY_SWIPE_RIGHT = "swipe_right_action_key";

    private static final int GESTURE_WAKELOCK_DURATION = 500;

    // Gestures
    private static final int DOUBLE_TAP = 15;
    private static final int SWIPE_UP = 2;
    private static final int SWIPE_DOWN = 3;
    private static final int SWIPE_LEFT = 4;
    private static final int SWIPE_RIGHT = 5;
    private static final int DOUBLE_SWIPE_DOWN = 6;

    private static final String CONTROL_PATH =
            "/sys/class/htc_sensorhub/sensor_hub/gesture_motion";

    /* Sensor gesture definition used to instantiate GestureMotionSensor, externally usable */
    /* These values also correspond to kernel driver values, so don't change them */
    public static final int SENSOR_GESTURE_SWIPE_UP = 0x4;
    public static final int SENSOR_GESTURE_SWIPE_DOWN = 0x8;
    public static final int SENSOR_GESTURE_SWIPE_LEFT = 0x10;
    public static final int SENSOR_GESTURE_SWIPE_RIGHT = 0x20;
    public static final int SENSOR_GESTURE_CAMERA = 0x40;
    public static final int SENSOR_GESTURE_DOUBLE_TAP = 0x8000;
    public static final int SENSOR_GESTURE_ALL = 0x807C;

    private Context mContext;
    private PowerManager mPowerManager;
    private AudioManager mAudioManager;
    private WakeLock mGestureWakeLock;
    private CameraManager mCameraManager;
    private Vibrator mVibrator;
    private ScreenStateReceiver mScreenStateReceiver;
    private SensorManager mSensorManager;
    private Sensor mSensor = null;
    private SensorEventListener mSensorEventListener;

    private String mRearCameraId;
    private boolean mTorchEnabled;

    private int mSwipeUpAction;
    private int mSwipeDownAction;
    private int mSwipeLeftAction;
    private int mSwipeRightAction;

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service");
        super.onCreate();

        mContext = this;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        loadPreferences(sharedPrefs);
        sharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mGestureWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HtcGestureWakeLock");
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        mCameraManager.registerTorchCallback(new TorchModeCallback(), null);

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
                    mSensor, SensorManager.SENSOR_DELAY_FASTEST);
            mScreenStateReceiver = new ScreenStateReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(mScreenStateReceiver, intentFilter);
        }
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        unregisterReceiver(mScreenStateReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public final void onAccuracyChanged(Sensor sensor, int i) {
    }

    public final void onSensorChanged(SensorEvent sensorEvent) {
        mSensorManager.unregisterListener(mSensorEventListener);
        float gesture = sensorEvent.values[0];
        if (DEBUG) Log.d(TAG, "Sensor type=" + sensorEvent.sensor.getType()
                + "," + sensorEvent.values[0] + "," + sensorEvent.values[1]);
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
                            mSensor, SensorManager.SENSOR_DELAY_FASTEST);
                        return -1;
                    }
                    mPowerManager.wakeUp(SystemClock.uptimeMillis());
                    doHapticFeedback();
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
        }
        mSensorManager.registerListener(mSensorEventListener,
                mSensor, SensorManager.SENSOR_DELAY_FASTEST);
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
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error loading preferences");
            }
        }
    };

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

    private void launchCamera() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        final Intent intent = new Intent(lineageos.content.Intent.ACTION_SCREEN_CAMERA_GESTURE);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT,
                Manifest.permission.STATUS_BAR_SERVICE);
        doHapticFeedback();
    }

    private void launchBrowser() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), GESTURE_WAKEUP_REASON);
        final Intent intent = getLaunchableIntent(
                new Intent(Intent.ACTION_VIEW, Uri.parse("http:")));
        startActivitySafely(intent);
        doHapticFeedback();
    }

    private void launchDialer() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), GESTURE_WAKEUP_REASON);
        final Intent intent = new Intent(Intent.ACTION_DIAL, null);
        startActivitySafely(intent);
        doHapticFeedback();
    }

    private void launchEmail() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), GESTURE_WAKEUP_REASON);
        final Intent intent = getLaunchableIntent(
                new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:")));
        startActivitySafely(intent);
        doHapticFeedback();
    }

    private void launchMessages() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
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

    private void toggleFlashlight() {
        String rearCameraId = getRearCameraId();
        if (rearCameraId != null) {
            mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
            try {
                mCameraManager.setTorchMode(rearCameraId, !mTorchEnabled);
                mTorchEnabled = !mTorchEnabled;
            } catch (CameraAccessException e) {
                // Ignore
            }
            doHapticFeedback();
        }
    }

    private void playPauseMusic() {
        dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        doHapticFeedback();
    }

    private void previousTrack() {
        dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        doHapticFeedback();
    }

    private void nextTrack() {
        dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_NEXT);
        doHapticFeedback();
    }

    private void volumeDown() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
        doHapticFeedback();
    }

    private void volumeUp() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
        doHapticFeedback();
    }

    private void dispatchMediaKeyWithWakeLockToMediaSession(final int keycode) {
        final MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(mContext);
        if (helper == null) {
            Log.w(TAG, "Unable to send media key event");
            return;
        }
        KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keycode, 0);
        helper.sendMediaButtonEvent(event, true);
        event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
        helper.sendMediaButtonEvent(event, true);
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
            final boolean enabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                    LineageSettings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, 1) != 0;
            if (enabled) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(VibrationEffect.createOneShot(50,VibrationEffect.DEFAULT_AMPLITUDE));
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

}
