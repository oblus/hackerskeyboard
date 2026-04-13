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

import android.app.backup.BackupManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import android.text.AutoText;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

public class LatinIMESettings extends AppCompatActivity {
    /* package */ static final String PREF_SETTINGS_KEY = "settings_key";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.settings_activity);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(LatinIMESettings.this, Main.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        if (icicle == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener, androidx.preference.DialogPreference.TargetFragment {

        private static final String QUICK_FIXES_KEY = "quick_fixes";
        private static final String PREDICTION_SETTINGS_KEY = "prediction_settings";
        private static final String VOICE_SETTINGS_KEY = "voice_mode";
        static final String INPUT_CONNECTION_INFO = "input_connection_info";    

        private static final String TAG = "LatinIMESettings";

        private CheckBoxPreference mQuickFixes;
        private ListPreference mVoicePreference;
        private ListPreference mSettingsKeyPreference;
        private ListPreference mKeyboardModePortraitPreference;
        private ListPreference mKeyboardModeLandscapePreference;
        private Preference mInputConnectionInfo;
        private Preference mLabelVersion;
        private Preference mDetectedDictionaries;

        private boolean mVoiceOn;
        private String mVoiceModeOff;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.prefs, rootKey);
            
            mQuickFixes = findPreference(QUICK_FIXES_KEY);
            mVoicePreference = findPreference(VOICE_SETTINGS_KEY);
            mSettingsKeyPreference = findPreference(PREF_SETTINGS_KEY);
            mInputConnectionInfo = findPreference(INPUT_CONNECTION_INFO);
            mLabelVersion = findPreference("label_version");
            mDetectedDictionaries = findPreference("pref_detected_dictionaries");

            mKeyboardModePortraitPreference = findPreference("pref_keyboard_mode_portrait");
            mKeyboardModeLandscapePreference = findPreference("pref_keyboard_mode_landscape");

            if (mSettingsKeyPreference != null) {
                mSettingsKeyPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            }
            if (mVoicePreference != null) {
                mVoicePreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            }
            
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            if (prefs != null) {
                prefs.registerOnSharedPreferenceChangeListener(this);
                mVoiceModeOff = getString(R.string.voice_mode_off);
                mVoiceOn = !(prefs.getString(VOICE_SETTINGS_KEY, mVoiceModeOff).equals(mVoiceModeOff));
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            if (getParentFragmentManager().findFragmentByTag("androidx.preference.PreferenceFragment.DIALOG") != null) {
                return;
            }

            if (preference instanceof SeekBarPreference) {
                PreferenceDialogFragmentCompat dialogFragment = SeekBarPreferenceDialogFragmentCompat.newInstance(preference.getKey());
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            // Note: getListView() might not be available yet or in the same way.
            // In PreferenceFragmentCompat, the list is internal.
            // AutoText.getSize() usually takes a View to get the context/settings.
            int autoTextSize = AutoText.getSize(requireView());
            if (autoTextSize < 1 && mQuickFixes != null) {
                PreferenceGroup group = findPreference(PREDICTION_SETTINGS_KEY);
                if (group != null) {
                    group.removePreference(mQuickFixes);
                }
            }
            
            Log.i(TAG, "compactModeEnabled=" + LatinIME.sKeyboardSettings.compactModeEnabled);
            if (!LatinIME.sKeyboardSettings.compactModeEnabled && mKeyboardModePortraitPreference != null) {
                CharSequence[] oldEntries = mKeyboardModePortraitPreference.getEntries();
                CharSequence[] oldValues = mKeyboardModePortraitPreference.getEntryValues();
                
                if (oldEntries != null && oldEntries.length > 2) {
                    CharSequence[] newEntries = new CharSequence[] { oldEntries[0], oldEntries[2] };
                    CharSequence[] newValues = new CharSequence[] { oldValues[0], oldValues[2] };
                    mKeyboardModePortraitPreference.setEntries(newEntries);
                    mKeyboardModePortraitPreference.setEntryValues(newValues);
                    if (mKeyboardModeLandscapePreference != null) {
                        mKeyboardModeLandscapePreference.setEntries(newEntries);
                        mKeyboardModeLandscapePreference.setEntryValues(newValues);
                    }
                }
            }
            
            updateSummaries();
            updateDetectedDictionaries();

            String version = "";
            try {
                PackageManager pm = requireContext().getPackageManager();
                PackageInfo info;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    info = pm.getPackageInfo(requireContext().getPackageName(), PackageManager.PackageInfoFlags.of(0));
                } else {
                    info = pm.getPackageInfo(requireContext().getPackageName(), 0);
                }
                version = info.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Could not find version info.");
            }

            if (mLabelVersion != null) {
                mLabelVersion.setSummary(version);
            }
        }

        @Override
        public void onDestroy() {
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            if (prefs != null) {
                prefs.unregisterOnSharedPreferenceChangeListener(this);
            }
            super.onDestroy();
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            new BackupManager(requireContext()).dataChanged();
            // If turning on voice input, show dialog
            if (key.equals(VOICE_SETTINGS_KEY) && !mVoiceOn) {
                if (!prefs.getString(VOICE_SETTINGS_KEY, mVoiceModeOff).equals(mVoiceModeOff)) {
                    // showVoiceConfirmation(); // TODO: Implement dialog in Fragment
                }
            }
            mVoiceOn = !(prefs.getString(VOICE_SETTINGS_KEY, mVoiceModeOff).equals(mVoiceModeOff));
            updateVoiceModeSummary();
            updateSummaries();
        }

        private void updateSummaries() {
            if (mInputConnectionInfo != null) {
                mInputConnectionInfo.setSummary(String.format("%s type=%s",
                        LatinIME.sKeyboardSettings.editorPackageName,
                        inputTypeDesc(LatinIME.sKeyboardSettings.editorInputType)
                ));
            }
        }

        private void updateDetectedDictionaries() {
            if (mDetectedDictionaries == null) return;
            
            PackageManager pm = requireContext().getPackageManager();
            java.util.List<CharSequence> dictionaryList = new java.util.ArrayList<CharSequence>();
            
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String selectedLanguages = sp.getString(LatinIME.PREF_SELECTED_LANGUAGES, null);
            String currentLanguage = sp.getString(LatinIME.PREF_INPUT_LANGUAGE, null);
            
            if (selectedLanguages == null || selectedLanguages.length() < 1) {
                currentLanguage = java.util.Locale.getDefault().getLanguage();
            } else if (currentLanguage == null) {
                currentLanguage = selectedLanguages.split(",")[0];
            }

            PluginManager.getPluginDictionaries(requireContext());
            String activePkg = PluginManager.getDictionaryPackageName(currentLanguage);

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
                            int dictSize = 0;
                            BinaryDictionary dict = PluginManager.getDictionary(requireContext(), currentLanguage);
                            if (dict != null) {
                                dictSize = dict.getSize();
                                dict.close();
                            }

                            SpannableStringBuilder ssb = new SpannableStringBuilder(entry);
                            if (dictSize > 100) {
                                ssb.setSpan(new ForegroundColorSpan(0xFF00FF00), 0, entry.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else {
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
                    }
                }
            }

            if (!dictionaryList.isEmpty()) {
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
                PreferenceGroup group = findPreference(PREDICTION_SETTINGS_KEY);
                if (group != null) {
                    group.removePreference(mDetectedDictionaries);
                }
            }
        }

        private void updateVoiceModeSummary() {
            // Summary is now handled by SummaryProvider
        }

        private String inputTypeDesc(int type) {
            int cls = type & 0x0000000f;
            int flags = type & 0x00fff000;
            int var = type &  0x00000ff0;

            StringBuilder out = new StringBuilder();
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

        private void addBit(StringBuilder buf, int bit, String str) {
            if (bit != 0) {
                buf.append("|");
                buf.append(str);
            }
        }

        static final Map<Integer, String> INPUT_CLASSES = new HashMap<>();
        static final Map<Integer, String> DATETIME_VARIATIONS = new HashMap<>();
        static final Map<Integer, String> TEXT_VARIATIONS = new HashMap<>();
        static final Map<Integer, String> NUMBER_VARIATIONS = new HashMap<>();
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
    }
}
