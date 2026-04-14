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

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;

public class PrefScreenView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.settings_activity);
        setTitle(R.string.pref_screen_view_title);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        if (icicle == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new ViewFragment())
                    .commit();
        }
    }

    public static class ViewFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener, androidx.preference.DialogPreference.TargetFragment {

        private ListPreference mRenderModePreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.prefs_view, rootKey);
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            if (prefs != null) {
                prefs.registerOnSharedPreferenceChangeListener(this);
            }
            mRenderModePreference = findPreference(LatinIME.PREF_RENDER_MODE);
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
        public void onDestroy() {
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            if (prefs != null) {
                prefs.unregisterOnSharedPreferenceChangeListener(this);
            }
            super.onDestroy();
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            new BackupManager(requireContext()).dataChanged();
        }
    }
}
