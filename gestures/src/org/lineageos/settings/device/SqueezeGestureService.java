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
import android.app.ActivityManager;
import android.content.ComponentName;
import android.app.Instrumentation;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ApplicationInfo;
import android.view.MotionEvent;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.graphics.PixelFormat;
import android.view.Display;
import android.view.Gravity;

import java.util.Iterator;
import java.lang.Runtime;
import java.io.IOException;

import lineageos.providers.LineageSettings;
import org.lineageos.internal.util.FileUtils;

import java.util.List;

public class SqueezeGestureService extends Service {

    private static final boolean DEBUG = false;

    private static final int SHORTSQUEEZE = 100;
    private static final int LONGSQUEEZE = 101;

    private static final String TAG = "SqueezeService";
    private static final String HTC_EDGESENSOR = "hTC Edge Sensor";
    private static final String HTC_EDGEGESTURESENSOR = "hTC Edge Gesture";
    private static final String GESTURE_WAKEUP_REASON = "squeeze-gesture-wakeup";

    private static final String EDGE_THRESHOLD_PATH = "/sys/class/htc_sensorhub/sensor_hub/edge_thd";

    private static final String SQUEEZE_SHORT_ACTION = "squeeze_short";
    private static final String SQUEEZE_LONG_ACTION = "squeeze_long";
    private static final String SQUEEZE_GESTURE_ENABLE = "squeeze_enabled";
    private static final String SQUEEZE_FORCE = "squeeze_force";
    private static final String SQUEEZE_HAPTIC_FEEDBACK_ENABLED = "squeeze_haptic_feedback";
    private static final String SQUEEZE_LONG_SQUEEZE_DURATION = "long_squeeze_duration";
    private static final String HAPTIC_FEEDBACK_IGNORE_RINGER = "haptic_ignore_ringer";

    private static final int SQUEEZE_FORCE_DEFAULT = 30;
    private static final int SQUEEZE_FORCE_MULTIPLIER = 6;
    private static final int GESTURE_WAKELOCK_DURATION = 500;
    private static final int LONG_SQUEEZE_DURATION_DEFAULT = 700;

    private static final int ACTION_TAKE_SCREENSHOT = 12;

    private Context mContext;
    private PowerManager mPowerManager;
    private AudioManager mAudioManager;
    private WakeLock mGestureWakeLock;
    private CameraManager mCameraManager;
    private Vibrator mVibrator;
    private ScreenStateReceiver mScreenStateReceiver;
    private SensorManager mSensorManager;
    private WindowManager windowManager;
    private ImageView squeezeForceView;

    private String mRearCameraId;
    private boolean mTorchEnabled;
    private int mShortSqueezeAction;
    private int mLongSqueezeAction;
    private int mLongSqueezeDuration = 700;
    private long mHoldDownTime;
    private boolean mSqueezeEnabled;
    private boolean mHapticFeedbackEnabled;
    private boolean mHapticIgnoreRinger;
    private boolean mLongSqueezeHandled = false;
    private boolean mSqueezedDown = false;

    private Sensor mEdgeSensor = null;
    private Sensor mEdgeGestureSensor = null;
    private int mForcePref = 0;
    private String[] mIntStrings = new String[10];

    private HtcEdgeSensorEventListener mEdgeSensorEventListener = new HtcEdgeSensorEventListener();
    private HtcEdgeGestureSensorEventListener mEdgeGestureSensorEventListener = new HtcEdgeGestureSensorEventListener();

    @Override
    public void onCreate() {
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

        Iterator iterator = mSensorManager.getSensorList(-1).iterator();
        while (iterator.hasNext()) {
            Sensor sensor = (Sensor) iterator.next();
            if (sensor.getName().equals(HTC_EDGESENSOR)) {
                if (DEBUG) Log.d(TAG, "found Edge sensor");
                mEdgeSensor = sensor;
                if (!FileUtils.writeLine(EDGE_THRESHOLD_PATH, Integer.toString(mForcePref))) {
                    Log.w(TAG, "Failed to write force threshold sysfs path");
                }
            }
            if (sensor.getName().equals(HTC_EDGEGESTURESENSOR)) {
                if (DEBUG) Log.d(TAG, "found Edge sensor");
                mEdgeGestureSensor = sensor;
            }
        }
        if (mEdgeSensor != null) {
            if (mSqueezeEnabled) {
                mSensorManager.registerListener(mEdgeSensorEventListener,
                        mEdgeSensor, SensorManager.SENSOR_DELAY_FASTEST);
                if (DEBUG) Log.d(TAG, "Registered Edge Sensor Listener");
            }
        }
        if (mEdgeGestureSensor != null) {
            if (mSqueezeEnabled) {
                mSensorManager.registerListener(mEdgeGestureSensorEventListener,
                        mEdgeGestureSensor, SensorManager.SENSOR_DELAY_FASTEST);
                if (DEBUG) Log.d(TAG, "Registered Edge Gesture Sensor Listener");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(mEdgeSensorEventListener);
        mSensorManager.unregisterListener(mEdgeGestureSensorEventListener);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

	/**
	 *  This sensor will be responsible for handling long squeeze actions. averageValue should
	 *  be equal (or almost equal) to the value of sensorEvent.values[1] for HtcEdgeGestureSensor.
	 *  For when we're in deep sleep, this sensor relies on the other to hold a wakelock to process
	 *  the incoming sensor events.
	 */
    private class HtcEdgeSensorEventListener implements SensorEventListener {

        public final void onAccuracyChanged(Sensor sensor, int i) {
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
			float averageValue = ((sensorEvent.values[8] + sensorEvent.values[9])/2);
			if (mSqueezedDown && (averageValue > mForcePref)) {
				if ((SystemClock.elapsedRealtime() - mHoldDownTime) > mLongSqueezeDuration) {
                    int action = gestureToAction(LONGSQUEEZE);
                    if (action > -1) {
                        handleGestureAction(action);
                        mSqueezedDown = false;
					}
				}
			}
        }
    }

	/**
	 *  This sensor will be responsible for handling short squeeze actions, as well as holding a
	 *  wakelock for the other sensor to work in deep sleep.
	 *  sensorEvent.values[0] = 1.0f the moment the edge sensors are squeezed down
	 *  sensorEvent.values[0] = 2.0f the moment the squeezed edge sensors are released
	 *  sensorEvent.values[1] holds the value of the squeeze force
	 */
    private class HtcEdgeGestureSensorEventListener implements SensorEventListener {

        public final void onAccuracyChanged(Sensor sensor, int i) {
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            float value = sensorEvent.values[0];
            if (value == 1.0f) {
                mGestureWakeLock.acquire(5000);
                mHoldDownTime = SystemClock.elapsedRealtime();
                mSqueezedDown = true;
            } else {
                float force = sensorEvent.values[1];
                long SqueezeReleaseTime = SystemClock.elapsedRealtime() - mHoldDownTime;
                if (SqueezeReleaseTime > 100 && SqueezeReleaseTime < mLongSqueezeDuration && (force > mForcePref)) {
                    int action = gestureToAction(SHORTSQUEEZE);
                    if (action > -1)
                        handleGestureAction(action);
                } else if (mGestureWakeLock.isHeld()) {
                    mGestureWakeLock.release();
				}
                mSqueezedDown = false;
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
            mForcePref = SQUEEZE_FORCE_MULTIPLIER * (mForcePref + 1);
	    mHapticFeedbackEnabled = sharedPreferences.getBoolean(SQUEEZE_HAPTIC_FEEDBACK_ENABLED, true);
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
                    if(mSqueezeEnabled){
                        mSensorManager.registerListener(mEdgeSensorEventListener,
                            mEdgeSensor, SensorManager.SENSOR_DELAY_FASTEST);
                        mSensorManager.registerListener(mEdgeGestureSensorEventListener,
                            mEdgeGestureSensor, SensorManager.SENSOR_DELAY_FASTEST);
                    } else {
                        mSensorManager.unregisterListener(mEdgeSensorEventListener);
                        mSensorManager.unregisterListener(mEdgeGestureSensorEventListener);
                    }
                } else if (SQUEEZE_FORCE.equals(key)) {
                    mForcePref = sharedPreferences.getInt(SQUEEZE_FORCE, SQUEEZE_FORCE_DEFAULT);
                    mForcePref = SQUEEZE_FORCE_MULTIPLIER * (mForcePref + 1);
                } else if (SQUEEZE_HAPTIC_FEEDBACK_ENABLED.equals(key)) {
		    mHapticFeedbackEnabled = sharedPreferences.getBoolean(SQUEEZE_HAPTIC_FEEDBACK_ENABLED, true);
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

    private void takeScreenshot() {
	if (ScreenStateReceiver.isScreenOn()) {
            simulateKey(KeyEvent.KEYCODE_SYSRQ);
	    doHapticFeedback();
	} else {
	    Log.d(TAG, "Cannot take screenshot while screen is off");
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
        if (mVibrator == null || !mVibrator.hasVibrator() || !mHapticFeedbackEnabled) {
            return;
        }
	if (mHapticIgnoreRinger) {
	    mVibrator.vibrate(50);
	} else if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
	    mVibrator.vibrate(50);
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

    public static String getForegroundApp(Context context){
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List< ActivityManager.RunningTaskInfo > runningTaskInfo = manager.getRunningTasks(1); 

        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        PackageManager packageManager = context.getPackageManager();

        String appName = "empty";
        try {
			return appName = (String)packageManager.getApplicationLabel(packageManager.getApplicationInfo(
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
