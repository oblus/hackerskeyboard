/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.n0pocketworkstation.pckeyboard;

import java.util.HashMap;
import java.util.Map;

import android.app.Dialog;
import android.app.backup.BackupManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.text.AutoText;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;
import android.util.Log;
import android.preference.PreferenceManager;

public class LatinIMESettings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        DialogInterface.OnDismissListener {

    private static final String QUICK_FIXES_KEY = "quick_fixes";
    private static final String PREDICTION_SETTINGS_KEY = "prediction_settings";
    private static final String VOICE_SETTINGS_KEY = "voice_mode";
    /* package */ static final String PREF_SETTINGS_KEY = "settings_key";
    static final String INPUT_CONNECTION_INFO = "input_connection_info";    

    private static final String TAG = "LatinIMESettings";

    // Dialog ids
    private static final int VOICE_INPUT_CONFIRM_DIALOG = 0;

    private CheckBoxPreference mQuickFixes;
    private ListPreference mVoicePreference;
    private ListPreference mSettingsKeyPreference;
    private ListPreference mKeyboardModePortraitPreference;
    private ListPreference mKeyboardModeLandscapePreference;
    private Preference mInputConnectionInfo;
    private Preference mLabelVersion;
    private Preference mDetectedDictionaries;

    private boolean mVoiceOn;

    private boolean mOkClicked = false;
    private String mVoiceModeOff;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs);
        mQuickFixes = (CheckBoxPreference) findPreference(QUICK_FIXES_KEY);
        mVoicePreference = (ListPreference) findPreference(VOICE_SETTINGS_KEY);
        mSettingsKeyPreference = (ListPreference) findPreference(PREF_SETTINGS_KEY);
        mInputConnectionInfo = (Preference) findPreference(INPUT_CONNECTION_INFO);
        mLabelVersion = (Preference) findPreference("label_version");
        mDetectedDictionaries = findPreference("pref_detected_dictionaries");


        // TODO(klausw): remove these when no longer needed
        mKeyboardModePortraitPreference = (ListPreference) findPreference("pref_keyboard_mode_portrait");
        mKeyboardModeLandscapePreference = (ListPreference) findPreference("pref_keyboard_mode_landscape");
        
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);

        mVoiceModeOff = getString(R.string.voice_mode_off);
        mVoiceOn = !(prefs.getString(VOICE_SETTINGS_KEY, mVoiceModeOff).equals(mVoiceModeOff));
    }

    @Override
    protected void onResume() {
        super.onResume();
        int autoTextSize = AutoText.getSize(getListView());
        if (autoTextSize < 1) {
            ((PreferenceGroup) findPreference(PREDICTION_SETTINGS_KEY))
                    .removePreference(mQuickFixes);
        }
        
        Log.i(TAG, "compactModeEnabled=" + LatinIME.sKeyboardSettings.compactModeEnabled);
        if (!LatinIME.sKeyboardSettings.compactModeEnabled) {
            CharSequence[] oldEntries = mKeyboardModePortraitPreference.getEntries();
            CharSequence[] oldValues = mKeyboardModePortraitPreference.getEntryValues();
            
            if (oldEntries.length > 2) {
                CharSequence[] newEntries = new CharSequence[] { oldEntries[0], oldEntries[2] };
                CharSequence[] newValues = new CharSequence[] { oldValues[0], oldValues[2] };
                mKeyboardModePortraitPreference.setEntries(newEntries);
                mKeyboardModePortraitPreference.setEntryValues(newValues);
                mKeyboardModeLandscapePreference.setEntries(newEntries);
                mKeyboardModeLandscapePreference.setEntryValues(newValues);
            }
        }
        
        updateSummaries();
        updateDetectedDictionaries();

        String version = "";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            version = info.versionName;
            boolean isOfficial = false;
            for (Signature sig : info.signatures) {
                byte[] b = sig.toByteArray();
                int out = 0;
                for (int i = 0; i < b.length; ++i) {
                    int pos = i % 4;
                    out ^= b[i] << (pos * 4);
                }
                if (out == -466825) {
                    isOfficial = true;
                }
                //version += " [" + Integer.toHexString(out) + "]";
            }
            // version += isOfficial ? " official" : " custom";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find version info.");
        }

        mLabelVersion.setSummary(version);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, Main.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        super.onDestroy();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        (new BackupManager(this)).dataChanged();
        // If turning on voice input, show dialog
        if (key.equals(VOICE_SETTINGS_KEY) && !mVoiceOn) {
            if (!prefs.getString(VOICE_SETTINGS_KEY, mVoiceModeOff)
                    .equals(mVoiceModeOff)) {
                showVoiceConfirmation();
            }
        }
        mVoiceOn = !(prefs.getString(VOICE_SETTINGS_KEY, mVoiceModeOff).equals(mVoiceModeOff));
        updateVoiceModeSummary();
        updateSummaries();
    }

    static Map<Integer, String> INPUT_CLASSES = new HashMap<Integer, String>();
    static Map<Integer, String> DATETIME_VARIATIONS = new HashMap<Integer, String>();
    static Map<Integer, String> TEXT_VARIATIONS = new HashMap<Integer, String>();
    static Map<Integer, String> NUMBER_VARIATIONS = new HashMap<Integer, String>();
    static {
        INPUT_CLASSES.put(0x00000004, "DATETIME");
        INPUT_CLASSES.put(0x00000002, "NUMBER");
        INPUT_CLASSES.put(0x00000003, "PHONE");
        INPUT_CLASSES.put(0x00000001, "TEXT"); 
        INPUT_CLASSES.put(0x00000000, "NULL");
        
        DATETIME_VARIATIONS.put(0x00000010, "DATE");
        DATETIME_VARIATIONS.put(0x00000020, "TIME");

        NUMBER_VARIATIONS.put(0x00000010, "PASSWORD");

        TEXT_VARIATIONS.put(0x00000020, "EMAIL_ADDRESS");
        TEXT_VARIATIONS.put(0x00000030, "EMAIL_SUBJECT");
        TEXT_VARIATIONS.put(0x000000b0, "FILTER");
        TEXT_VARIATIONS.put(0x00000050, "LONG_MESSAGE");
        TEXT_VARIATIONS.put(0x00000080, "PASSWORD");
        TEXT_VARIATIONS.put(0x00000060, "PERSON_NAME");
        TEXT_VARIATIONS.put(0x000000c0, "PHONETIC");
        TEXT_VARIATIONS.put(0x00000070, "POSTAL_ADDRESS");
        TEXT_VARIATIONS.put(0x00000040, "SHORT_MESSAGE");
        TEXT_VARIATIONS.put(0x00000010, "URI");
        TEXT_VARIATIONS.put(0x00000090, "VISIBLE_PASSWORD");
        TEXT_VARIATIONS.put(0x000000a0, "WEB_EDIT_TEXT");
        TEXT_VARIATIONS.put(0x000000d0, "WEB_EMAIL_ADDRESS");
        TEXT_VARIATIONS.put(0x000000e0, "WEB_PASSWORD");

    }
    
    private static void addBit(StringBuffer buf, int bit, String str) {
        if (bit != 0) {
            buf.append("|");
            buf.append(str);
        }
    }

    private static String inputTypeDesc(int type) {
        int cls = type & 0x0000000f; // MASK_CLASS
        int flags = type & 0x00fff000; // MASK_FLAGS
        int var = type &  0x00000ff0; // MASK_VARIATION

        StringBuffer out = new StringBuffer();
        String clsName = INPUT_CLASSES.get(cls);
        out.append(clsName != null ? clsName : "?");
        
        if (cls == InputType.TYPE_CLASS_TEXT) {
            String varName = TEXT_VARIATIONS.get(var);
            if (varName != null) {
                out.append(".");
                out.append(varName);
            }
            addBit(out, flags & 0x00010000, "AUTO_COMPLETE");
            addBit(out, flags & 0x00008000, "AUTO_CORRECT");
            addBit(out, flags & 0x00001000, "CAP_CHARACTERS");
            addBit(out, flags & 0x00004000, "CAP_SENTENCES");
            addBit(out, flags & 0x00002000, "CAP_WORDS");
            addBit(out, flags & 0x00040000, "IME_MULTI_LINE");
            addBit(out, flags & 0x00020000, "MULTI_LINE");
            addBit(out, flags & 0x00080000, "NO_SUGGESTIONS");
        } else if (cls == InputType.TYPE_CLASS_NUMBER) {
            String varName = NUMBER_VARIATIONS.get(var);
            if (varName != null) {
                out.append(".");
                out.append(varName);
            }
            addBit(out, flags & 0x00002000, "DECIMAL");
            addBit(out, flags & 0x00001000, "SIGNED");        
        } else if (cls == InputType.TYPE_CLASS_DATETIME) {
            String varName = DATETIME_VARIATIONS.get(var);
            if (varName != null) {
                out.append(".");
                out.append(varName);
            }
        }
        return out.toString();
    }

    private void updateSummaries() {
        Resources res = getResources();
        mSettingsKeyPreference.setSummary(
                res.getStringArray(R.array.settings_key_modes)
                [mSettingsKeyPreference.findIndexOfValue(mSettingsKeyPreference.getValue())]);

        mInputConnectionInfo.setSummary(String.format("%s type=%s",
                LatinIME.sKeyboardSettings.editorPackageName,
                inputTypeDesc(LatinIME.sKeyboardSettings.editorInputType)
                ));
    }

    private void showVoiceConfirmation() {
        mOkClicked = false;
        showDialog(VOICE_INPUT_CONFIRM_DIALOG);
    }

    private void updateDetectedDictionaries() {
        if (mDetectedDictionaries == null) return;
        
        PackageManager pm = getPackageManager();
        java.util.List<CharSequence> dictionaryList = new java.util.ArrayList<CharSequence>();
        
        // Get current language to see which dictionary is "loaded"
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedLanguages = sp.getString(LatinIME.PREF_SELECTED_LANGUAGES, null);
        String currentLanguage = sp.getString(LatinIME.PREF_INPUT_LANGUAGE, null);
        
        // Fallback logic matching LanguageSwitcher:
        // 1. If no languages selected, use system default
        // 2. If languages selected but no current one set, use first selected
        if (selectedLanguages == null || selectedLanguages.length() < 1) {
            currentLanguage = java.util.Locale.getDefault().getLanguage();
        } else if (currentLanguage == null) {
            currentLanguage = selectedLanguages.split(",")[0];
        }

        PluginManager.getPluginDictionaries(this); // Ensure plugin map is populated
        String activePkg = PluginManager.getDictionaryPackageName(currentLanguage);

        // Scan all installed packages for dictionary patterns
        java.util.List<PackageInfo> packages = pm.getInstalledPackages(0);
        for (PackageInfo pkgInfo : packages) {
            String pkg = pkgInfo.packageName;
            if (pkg.startsWith("org.n0pocketworkstation.dict") || 
                pkg.startsWith("org.pocketworkstation.dict") ||
                pkg.startsWith("com.anysoftkeyboard.languagepack") ||
                pkg.equals("com.menny.anysoftkeyboard.pack.dictionaries")) {
                
                try {
                    String label = pm.getApplicationLabel(pkgInfo.applicationInfo).toString();
                    String entry = label + " [" + pkg + "]";
                    if (pkg.equals(activePkg)) {
                        // Check if the dictionary actually has data
                        int dictSize = 0;
                        BinaryDictionary dict = PluginManager.getDictionary(this, currentLanguage);
                        if (dict != null) {
                            dictSize = dict.getSize();
                            dict.close();
                        }

                        SpannableStringBuilder ssb = new SpannableStringBuilder(entry);
                        if (dictSize > 100) { // arbitrary threshold for a "real" dictionary
                            // Highlight active dictionary in Green
                            ssb.setSpan(new ForegroundColorSpan(0xFF00FF00), 0, entry.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else {
                            // Highlight active but empty/missing dictionary in Orange/Red
                            ssb.setSpan(new ForegroundColorSpan(0xFFFFA500), 0, entry.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            entry += " (Empty/Error)";
                            ssb = new SpannableStringBuilder(entry);
                            ssb.setSpan(new ForegroundColorSpan(0xFFFFA500), 0, entry.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        dictionaryList.add(ssb);
                    } else {
                        dictionaryList.add(entry);
                    }
                } catch (Exception e) {
                    // Skip if error getting label
                }
            }
        }

        if (dictionaryList.size() > 0) {
            // Sort by string representation
            java.util.Collections.sort(dictionaryList, new java.util.Comparator<CharSequence>() {
                @Override
                public int compare(CharSequence a, CharSequence b) {
                    return a.toString().compareTo(b.toString());
                }
            });

            SpannableStringBuilder detected = new SpannableStringBuilder();
            for (int i = 0; i < dictionaryList.size(); i++) {
                if (i > 0) detected.append("\n");
                detected.append(dictionaryList.get(i));
            }
            mDetectedDictionaries.setSummary(detected);
            mDetectedDictionaries.setEnabled(true);
        } else {
            // Remove the preference if no dictionaries are found
            PreferenceGroup group = (PreferenceGroup) findPreference(PREDICTION_SETTINGS_KEY);
            if (group != null) {
                group.removePreference(mDetectedDictionaries);
            }
        }
    }

    private void updateVoiceModeSummary() {
        mVoicePreference.setSummary(
                getResources().getStringArray(R.array.voice_input_modes_summary)
                [mVoicePreference.findIndexOfValue(mVoicePreference.getValue())]);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            default:
                Log.e(TAG, "unknown dialog " + id);
                return null;
        }
    }

    public void onDismiss(DialogInterface dialog) {
        if (!mOkClicked) {
            // This assumes that onPreferenceClick gets called first, and this if the user
            // agreed after the warning, we set the mOkClicked value to true.
            mVoicePreference.setValue(mVoiceModeOff);
        }
    }

    private void updateVoicePreference() {
    }
}
