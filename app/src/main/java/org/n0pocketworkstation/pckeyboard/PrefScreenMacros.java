package org.n0pocketworkstation.pckeyboard;

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;

public class PrefScreenMacros extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle icicle) {
        PCKeyboardApp.updateLocale(this);
        super.onCreate(icicle);
        setContentView(R.layout.settings_activity);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        if (icicle == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new MacrosFragment())
                    .commit();
        }
    }

    public static class MacrosFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener, androidx.preference.DialogPreference.TargetFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.prefs_macros, rootKey);
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            if (prefs != null) {
                prefs.registerOnSharedPreferenceChangeListener(this);
            }
            updateCategoryTitles();
        }

        private void updateCategoryTitles() {
            for (int i = 1; i <= 5; i++) {
                final String labelKey = "macro_label_" + i;
                final String catKey = "category_macro_" + i;
                
                final EditTextPreference labelPref = findPreference(labelKey);
                final ClickablePreferenceCategory catPref = findPreference(catKey);
                
                if (labelPref != null && catPref != null) {
                    labelPref.setVisible(false);
                    String label = labelPref.getText();
                    if (label == null || label.isEmpty()) label = "M" + i;
                    
                    String baseTitle = "Macro Button Title " + i;
                    String fullTitle = baseTitle + " ( " + label + " )";
                    SpannableString spannable = new SpannableString(fullTitle);
                    int start = baseTitle.length();
                    spannable.setSpan(new ForegroundColorSpan(Color.WHITE), start, fullTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    
                    catPref.setTitle(spannable);
                    
                    catPref.setOnCategoryClickListener(new ClickablePreferenceCategory.OnCategoryClickListener() {
                        @Override
                        public void onCategoryClick(ClickablePreferenceCategory category) {
                            onDisplayPreferenceDialog(labelPref);
                        }
                    });
                }
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
        public void onDestroy() {
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            if (prefs != null) {
                prefs.unregisterOnSharedPreferenceChangeListener(this);
            }
            super.onDestroy();
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key != null && key.startsWith("macro_label_")) {
                updateCategoryTitles();
            }
            new BackupManager(requireContext()).dataChanged();
        }
    }
}
