/*
 * Copyright (C) 2014 Slimroms
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

package com.slim.device.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import slim.utils.AppHelper;
import slim.action.ActionsArray;
import slim.action.ActionConstants;

import com.slim.device.R;
import com.slim.device.util.ShortcutPickerHelper;

public class ScreenOffGesture extends PreferenceFragment implements
        OnPreferenceChangeListener, OnPreferenceClickListener,
        ShortcutPickerHelper.OnPickListener {

    private static final String SLIM_METADATA_NAME = "slim.framework";

    public static final String GESTURE_SETTINGS = "screen_off_gesture_settings";

    public static final String PREF_GESTURE_ENABLE = "enable_gestures";
    public static final String PREF_SWIPE_UP = "gesture_swipe_up";
    public static final String PREF_SWIPE_DOWN = "gesture_swipe_down";
    public static final String PREF_SWIPE_LEFT = "gesture_swipe_left";
    public static final String PREF_SWIPE_RIGHT = "gesture_swipe_right";
    public static final String PREF_DOUBLE_TAP = "gesture_double_tap";
    public static final String PREF_CAMERA = "gesture_camera";

    private static final int DLG_SHOW_ACTION_DIALOG  = 0;
    private static final int DLG_RESET_TO_DEFAULT    = 1;

    private static final int MENU_RESET = Menu.FIRST;

    private Preference mSwipeUp;
    private Preference mSwipeDown;
    private Preference mSwipeLeft;
    private Preference mSwipeRight;
    private Preference mDoubleTap;
    private Preference mCamera;
    private SwitchPreference mEnableGestures;

    private boolean mCheckPreferences;
    private SharedPreferences mPrefs;

    private ShortcutPickerHelper mPicker;
    private String mPendingSettingsKey;
    private ActionsArray mActionsArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPicker = new ShortcutPickerHelper(getActivity(), this);

        mPrefs = getActivity().getSharedPreferences(
                GESTURE_SETTINGS, Activity.MODE_PRIVATE);

        mActionsArray = new ActionsArray(getActivity(), true);

        // Attach final settings screen.
        reloadSettings();

        setHasOptionsMenu(true);
    }

    private PreferenceScreen reloadSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.screen_off_gesture);
        prefs = getPreferenceScreen();

        mEnableGestures = (SwitchPreference) prefs.findPreference(PREF_GESTURE_ENABLE);

        mSwipeUp = (Preference) prefs.findPreference(PREF_SWIPE_UP);
        mSwipeDown = (Preference) prefs.findPreference(PREF_SWIPE_DOWN);
        mSwipeLeft = (Preference) prefs.findPreference(PREF_SWIPE_LEFT);
        mSwipeRight = (Preference) prefs.findPreference(PREF_SWIPE_RIGHT);
        mDoubleTap = (Preference) prefs.findPreference(PREF_DOUBLE_TAP);
        mCamera = (Preference) prefs.findPreference(PREF_CAMERA);

        setupOrUpdatePreference(mSwipeDown, mPrefs
                .getString(PREF_SWIPE_DOWN, ActionConstants.ACTION_MEDIA_PLAY_PAUSE));
        setupOrUpdatePreference(mSwipeLeft, mPrefs
                .getString(PREF_SWIPE_LEFT, ActionConstants.ACTION_MEDIA_PREVIOUS));
        setupOrUpdatePreference(mSwipeRight, mPrefs
                .getString(PREF_SWIPE_RIGHT, ActionConstants.ACTION_MEDIA_NEXT));
        setupOrUpdatePreference(mSwipeUp, mPrefs
                .getString(PREF_SWIPE_UP, ActionConstants.ACTION_TORCH));
        setupOrUpdatePreference(mDoubleTap, mPrefs
                .getString(PREF_DOUBLE_TAP, ActionConstants.ACTION_WAKE_DEVICE));
        setupOrUpdatePreference(mCamera, mPrefs
                .getString(PREF_CAMERA, ActionConstants.ACTION_CAMERA));

        boolean enableGestures =
                mPrefs.getBoolean(PREF_GESTURE_ENABLE, true);
        mEnableGestures.setChecked(enableGestures);
        mEnableGestures.setOnPreferenceChangeListener(this);

        mCheckPreferences = true;
        return prefs;
    }

    private void setupOrUpdatePreference(Preference preference, String action) {
        if (preference == null || action == null) {
            return;
        }

        if (action.startsWith("**")) {
            preference.setSummary(getDescription(action));
        } else {
            preference.setSummary(AppHelper.getFriendlyNameForUri(
                    getActivity(), getActivity().getPackageManager(), action));
        }
        preference.setOnPreferenceClickListener(this);
    }

    private String getDescription(String action) {
        if (mActionsArray == null || action == null) {
            return null;
        }
        int i = 0;
        for (String actionValue : mActionsArray.getValues()) {
            if (action.equals(actionValue)) {
                return mActionsArray.getEntries()[i];
            }
            i++;
        }
        return null;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String settingsKey = null;
        int dialogTitle = 0;
        if (preference == mSwipeUp) {
            settingsKey = PREF_SWIPE_UP;
            dialogTitle = R.string.swipe_up_title;
        } else if (preference == mSwipeDown) {
            settingsKey = PREF_SWIPE_DOWN;
            dialogTitle = R.string.swipe_down_title;
        } else if (preference == mSwipeLeft) {
            settingsKey = PREF_SWIPE_LEFT;
            dialogTitle = R.string.swipe_left_title;
        } else if (preference == mSwipeRight) {
            settingsKey = PREF_SWIPE_RIGHT;
            dialogTitle = R.string.swipe_right_title;
        } else if (preference == mDoubleTap) {
            settingsKey = PREF_DOUBLE_TAP;
            dialogTitle = R.string.double_tap_title;
        } else if (preference == mCamera) {
            settingsKey = PREF_CAMERA;
            dialogTitle = R.string.camera_title;
        }
        if (settingsKey != null) {
            showDialogInner(DLG_SHOW_ACTION_DIALOG, settingsKey, dialogTitle);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mEnableGestures) {
            mPrefs.edit()
                    .putBoolean(PREF_GESTURE_ENABLE, (Boolean) newValue).commit();
            return true;
        }
        return false;
    }

    // Reset all entries to default.
    private void resetToDefault() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_GESTURE_ENABLE, true);
        editor.putString(PREF_SWIPE_UP, ActionConstants.ACTION_TORCH);
        editor.putString(PREF_SWIPE_DOWN, ActionConstants.ACTION_MEDIA_PLAY_PAUSE);
        editor.putString(PREF_SWIPE_LEFT, ActionConstants.ACTION_MEDIA_PREVIOUS);
        editor.putString(PREF_SWIPE_RIGHT, ActionConstants.ACTION_MEDIA_NEXT);
        editor.putString(PREF_DOUBLE_TAP, ActionConstants.ACTION_WAKE_DEVICE);
        editor.putString(PREF_CAMERA, ActionConstants.ACTION_CAMERA);
        editor.commit();
        reloadSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void shortcutPicked(String action,
                String description, Bitmap bmp, boolean isApplication) {
        if (mPendingSettingsKey == null || action == null) {
            return;
        }
        mPrefs.edit().putString(mPendingSettingsKey, action).commit();
        reloadSettings();
        mPendingSettingsKey = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            }
        } else {
            mPendingSettingsKey = null;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                    showDialogInner(DLG_RESET_TO_DEFAULT, null, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    private void showDialogInner(int id, String settingsKey, int dialogTitle) {
        DialogFragment newFragment =
                MyAlertDialogFragment.newInstance(id, settingsKey, dialogTitle);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(
                int id, String settingsKey, int dialogTitle) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putString("settingsKey", settingsKey);
            args.putInt("dialogTitle", dialogTitle);
            frag.setArguments(args);
            return frag;
        }

        ScreenOffGesture getOwner() {
            return (ScreenOffGesture) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final String settingsKey = getArguments().getString("settingsKey");
            int dialogTitle = getArguments().getInt("dialogTitle");
            switch (id) {
                case DLG_SHOW_ACTION_DIALOG:
                    if (getOwner().mActionsArray == null) {
                        return null;
                    }
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(dialogTitle)
                    .setNegativeButton(R.string.cancel, null)
                    .setItems(getOwner().mActionsArray.getEntries(),
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            if (getOwner().mActionsArray.getValues()[item]
                                    .equals(ActionConstants.ACTION_APP)) {
                                if (getOwner().mPicker != null) {
                                    getOwner().mPendingSettingsKey = settingsKey;
                                    getOwner().mPicker.pickShortcut(getOwner().getId());
                                }
                            } else {
                                getOwner().mPrefs.edit()
                                        .putString(settingsKey,
                                        getOwner().mActionsArray.getValues()[item]).commit();
                                getOwner().reloadSettings();
                            }
                        }
                    })
                    .create();
                case DLG_RESET_TO_DEFAULT:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().resetToDefault();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
        }
    }

}
