package org.n0pocketworkstation.pckeyboard;

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrefScreenMacros extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle icicle) {
        PCKeyboardApp.updateLocale(this);
        super.onCreate(icicle);
        setContentView(R.layout.settings_activity);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_revert);
            getSupportActionBar().setTitle(R.string.title_macro_settings);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveAndExit();
            }
        });

        if (icicle == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new MacrosFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            saveAndExit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveAndExit() {
        MacrosFragment fragment = (MacrosFragment) getSupportFragmentManager().findFragmentById(R.id.settings_container);
        if (fragment != null) {
            fragment.handleBackNavigation();
        } else {
            finish();
        }
    }

    public static class MacrosFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener,
            androidx.preference.DialogPreference.TargetFragment,
            MacroEditorPreference.OnMacroInteractionListener {

        private static class MacroData {
            String content;
            boolean enabled;

            MacroData(String content, boolean enabled) {
                this.content = content;
                this.enabled = enabled;
            }
        }

        private Map<Integer, List<MacroData>> mCategoryMacros = new HashMap<>();
        private boolean mHasChanges = false;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.prefs_macros, rootKey);
            loadMacros();
            updateAllCategories();
            mHasChanges = false; // Reset after initial load
            
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            if (prefs != null) {
                prefs.registerOnSharedPreferenceChangeListener(this);
            }
        }

        private void loadMacros() {
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            mCategoryMacros.clear(); // Clear existing
            for (int cat = 1; cat <= 5; cat++) {
                List<MacroData> macros = new ArrayList<>();
                int startIdx = (cat - 1) * 5 + 1;
                for (int i = 0; i < 5; i++) {
                    int globalIdx = startIdx + i;
                    String content = sp.getString("macro_content_" + globalIdx, "");
                    boolean enabled = sp.getBoolean("macro_control_" + globalIdx, false);
                    if (!content.isEmpty() || enabled) {
                        macros.add(new MacroData(content, enabled));
                    }
                }
                mCategoryMacros.put(cat, macros);
            }
        }

        private void updateAllCategories() {
            for (int i = 1; i <= 5; i++) {
                updateCategory(i);
            }
        }

        private void updateCategory(int catIndex) {
            final String catKey = "category_macro_" + catIndex;
            final ClickablePreferenceCategory catPref = findPreference(catKey);
            if (catPref == null) return;

            final String labelKey = "macro_label_" + catIndex;
            final EditTextPreference labelPref = findPreference(labelKey);
            String label = (labelPref != null) ? labelPref.getText() : null;
            if (label == null || label.isEmpty()) label = "M" + catIndex;

            String baseTitle = "Macro Button Title " + catIndex;
            String fullTitle = baseTitle + " ( " + label + " )";
            SpannableString spannable = new SpannableString(fullTitle);
            
            int labelStart = fullTitle.indexOf("(");
            int labelEnd = fullTitle.indexOf(")") + 1;
            
            spannable.setSpan(new ForegroundColorSpan(Color.WHITE), 0, labelStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(0xFFFCAE00), labelStart, labelEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            catPref.setTitle(spannable);
            
            if (labelPref != null) {
                labelPref.setVisible(false);
                labelPref.setDialogTitle("Tile label (max 5 chars)");
                labelPref.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                    @Override
                    public void onBindEditText(@NonNull android.widget.EditText editText) {
                        editText.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(5)});
                    }
                });
                labelPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String newLabel = (String) newValue;
                        if (newLabel.length() > 5) {
                            return false;
                        }
                        return true;
                    }
                });
                catPref.setOnCategoryClickListener(new ClickablePreferenceCategory.OnCategoryClickListener() {
                    @Override
                    public void onCategoryClick(ClickablePreferenceCategory category) {
                        onDisplayPreferenceDialog(labelPref);
                    }
                });
            }

            // Czyszczenie
            catPref.removeAll();
            if (labelPref != null) catPref.addPreference(labelPref);

            List<MacroData> macros = mCategoryMacros.get(catIndex);
            if (macros == null) macros = new ArrayList<>();

            // Jeśli pusta kategoria, dodaj jedno puste pole
            if (macros.isEmpty()) {
                macros.add(new MacroData("", false));
            }

            for (int i = 0; i < macros.size(); i++) {
                addMacroEditor(catPref, catIndex, i, macros.get(i));
            }

            final String infoKey = "macro_control_info_" + catIndex;
            Preference infoPref = new Preference(requireContext()) {
                @Override
                public void onBindViewHolder(PreferenceViewHolder holder) {
                    super.onBindViewHolder(holder);
                    View titleView = holder.findViewById(android.R.id.title);
                    if (titleView instanceof android.widget.TextView) {
                        ((android.widget.TextView) titleView).setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    }
                    View summaryView = holder.findViewById(android.R.id.summary);
                    if (summaryView instanceof android.widget.TextView) {
                        ((android.widget.TextView) summaryView).setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    }
                }
            };
            infoPref.setKey(infoKey);
            infoPref.setTitle(R.string.macro_control_checkbox);
            infoPref.setSummary(R.string.macro_control_summary);
            infoPref.setPersistent(false);
            infoPref.setIconSpaceReserved(false);
            catPref.addPreference(infoPref);
            
            Preference maxMacrosPref = new Preference(requireContext());
            maxMacrosPref.setSummary("Maximum 5 macros per category");
            maxMacrosPref.setPersistent(false);
            maxMacrosPref.setIconSpaceReserved(false);
            catPref.addPreference(maxMacrosPref);
       }

        private void addMacroEditor(ClickablePreferenceCategory catPref, int catIdx, int listIdx, MacroData data) {
            MacroEditorPreference pref = new MacroEditorPreference(requireContext());
            pref.setKey("temp_macro_" + catIdx + "_" + listIdx);
            pref.setPersistent(false);
            pref.setMacroData(data.content, data.enabled);
            pref.setPosition(listIdx);
            pref.setOnMacroInteractionListener(this);
            catPref.addPreference(pref);
        }

        public void handleBackNavigation() {
            if (!mHasChanges) {
                requireActivity().finish();
                return;
            }

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Save changes?")
                    .setMessage("You have unsaved changes. Do you want to save them before exiting?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        performSave();
                        requireActivity().finish();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        requireActivity().finish();
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        }

        private void performSave() {
            SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
            for (int cat = 1; cat <= 5; cat++) {
                List<MacroData> macros = mCategoryMacros.get(cat);
                int startIdx = (cat - 1) * 5 + 1;
                
                List<MacroData> filtered = new ArrayList<>();
                if (macros != null) {
                    for (MacroData md : macros) {
                        if (md.content != null && !md.content.trim().isEmpty() || md.enabled) {
                            filtered.add(md);
                        }
                    }
                }

                for (int i = 0; i < 5; i++) {
                    int globalIdx = startIdx + i;
                    if (i < filtered.size()) {
                        MacroData d = filtered.get(i);
                        editor.putString("macro_content_" + globalIdx, d.content);
                        editor.putBoolean("macro_control_" + globalIdx, d.enabled);
                    } else {
                        editor.remove("macro_content_" + globalIdx);
                        editor.remove("macro_control_" + globalIdx);
                    }
                }
            }
            editor.apply();
            new BackupManager(requireContext()).dataChanged();
        }

        @Override
        public void onMoveUp(MacroEditorPreference pref) {
            String key = pref.getKey();
            if (key == null || !key.startsWith("temp_macro_")) return;
            String[] parts = key.split("_");
            if (parts.length < 4) return;
            int catIdx = Integer.parseInt(parts[2]);
            int listIdx = Integer.parseInt(parts[3]);

            List<MacroData> macros = mCategoryMacros.get(catIdx);
            if (macros != null && listIdx >= 0 && listIdx < macros.size()) {
                macros.remove(listIdx);
                mHasChanges = true;
                if (macros.isEmpty()) {
                    macros.add(new MacroData("", false));
                }
                updateCategory(catIdx);
            }
        }

        @Override
        public void onMoveDown(MacroEditorPreference pref) {
            String key = pref.getKey();
            if (key == null || !key.startsWith("temp_macro_")) return;
            String[] parts = key.split("_");
            if (parts.length < 4) return;
            int catIdx = Integer.parseInt(parts[2]);
            int listIdx = Integer.parseInt(parts[3]);
            
            List<MacroData> macros = mCategoryMacros.get(catIdx);
            if (macros != null && macros.size() < 5) {
                macros.add(listIdx + 1, new MacroData("", false));
                mHasChanges = true;
                updateCategory(catIdx);
            }
        }

        @Override
        public void onMacroChanged(MacroEditorPreference pref, String content, boolean enabled) {
            String key = pref.getKey();
            if (key == null || !key.startsWith("temp_macro_")) return;
            String[] parts = key.split("_");
            if (parts.length < 4) return;
            int catIdx = Integer.parseInt(parts[2]);
            int listIdx = Integer.parseInt(parts[3]);
            
            List<MacroData> macros = mCategoryMacros.get(catIdx);
            if (macros != null) {
                if (listIdx < macros.size()) {
                    MacroData md = macros.get(listIdx);
                    if (!content.equals(md.content) || enabled != md.enabled) {
                        md.content = content;
                        md.enabled = enabled;
                        mHasChanges = true;
                    }
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
                updateAllCategories();
            }
        }
    }
}
