package org.n0pocketworkstation.pckeyboard;

import android.provider.Settings;
import android.graphics.Color;
import android.text.SpannableString;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class LatinIMESettings extends AppCompatActivity {
    public static final String PREF_SETTINGS_KEY = "settings_key";
    @Override
    protected void onCreate(Bundle icicle) {
        PCKeyboardApp.updateLocale(this);
        super.onCreate(icicle);
        setContentView(R.layout.settings_activity);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (icicle == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private static final String TAG = "LatinIMESettings";
        private final ActivityResultLauncher<Intent> exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        saveSettingsToFile(result.getData().getData());
                    }
                }
        );

        private final ActivityResultLauncher<Intent> importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        loadSettingsFromFile(result.getData().getData());
                    }
                }
        );

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Migration for auto_cap from boolean to String
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String key = "auto_cap_mode";
            String oldKey = "auto_cap";

            // 1. Migrate old key to new key if needed
            if (!sp.contains(key) && sp.contains(oldKey)) {
                try {
                    boolean oldVal = sp.getBoolean(oldKey, true);
                    sp.edit().putString(key, oldVal ? "0" : "2").commit();
                } catch (ClassCastException e) {
                    // Already converted or weird state
                }
            }

            // 2. Ensure new key is a String to avoid ClassCastException in ListPreference
            try {
                sp.getString(key, "0");
            } catch (ClassCastException e) {
                Log.i(TAG, "Migrating auto_cap_mode from boolean to String");
                boolean oldVal = sp.getBoolean(key, true);
                sp.edit().remove(key).commit();
                sp.edit().putString(key, oldVal ? "0" : "2").commit();
            }
            addPreferencesFromResource(R.xml.prefs);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            updateSummaries();
        }


        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            if (getParentFragmentManager().findFragmentByTag("androidx.preference.PreferenceFragment.DIALOG") != null) {
                return;
            }

            if (preference instanceof SeekBarPreference) {
                androidx.fragment.app.DialogFragment f = SeekBarPreferenceDialogFragmentCompat.newInstance(preference.getKey());
                // Explicitly set target fragment for AndroidX compatibility
                f.setTargetFragment(this, 0);
                f.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if ("pref_revert_theme_color".equals(key)) {
                ((PCKeyboardApp) requireActivity().getApplication()).updateTheme();
            } else if ("pref_ui_language".equals(key)) {
                PCKeyboardApp.updateLocale(requireContext());
                requireActivity().recreate();
            }
            new BackupManager(requireContext()).dataChanged();
            updateSummaries();
        }

        @Override
        public boolean onPreferenceTreeClick(@NonNull Preference preference) {
            if ("export_settings".equals(preference.getKey())) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "hkg_settings.json");
                exportLauncher.launch(intent);
                return true;
            } else if ("import_settings".equals(preference.getKey())) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                importLauncher.launch(intent);
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }

        private void saveSettingsToFile(Uri uri) {
            try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                if (os == null) return;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                org.json.JSONObject json = new org.json.JSONObject();
                for (java.util.Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                    json.put(entry.getKey(), entry.getValue());
                }
                os.write(json.toString(4).getBytes(StandardCharsets.UTF_8));
                android.widget.Toast.makeText(requireContext(), "Settings exported", android.widget.Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Export failed", e);
            }
        }

        private void loadSettingsFromFile(Uri uri) {
            try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
                if (is == null) return;
                byte[] bytes = new byte[is.available()];
                is.read(bytes);
                String content = new String(bytes, StandardCharsets.UTF_8);
                org.json.JSONObject json = new org.json.JSONObject(content);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                java.util.Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object val = json.get(key);
                    if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                    else if (val instanceof Integer) editor.putInt(key, (Integer) val);
                    else if (val instanceof Long) editor.putLong(key, (Long) val);
                    else if (val instanceof Float) editor.putFloat(key, (Float) val);
                    else if (val instanceof String) editor.putString(key, (String) val);
                }
                editor.apply();
                android.widget.Toast.makeText(requireContext(), "Settings imported. Restarting...", android.widget.Toast.LENGTH_SHORT).show();
                requireActivity().recreate();
            } catch (Exception e) {
                Log.e(TAG, "Import failed", e);
            }
        }

        private void updateSummaries() {
            Preference info = findPreference("input_connection_info");
            if (info != null) {
                info.setSummary(String.format("%s type=%s",
                        LatinIME.sKeyboardSettings.editorPackageName,
                        inputTypeDesc(LatinIME.sKeyboardSettings.editorInputType)));
            }
            updateDetectedDictionaries();
            Preference voice = findPreference("voice_mode");
            if (voice instanceof androidx.preference.ListPreference) {
                voice.setSummary(((androidx.preference.ListPreference) voice).getEntry());
            }
        }

        private void updateDetectedDictionaries() {
            Preference mDetectedDictionaries = findPreference("pref_detected_dictionaries");
            if (mDetectedDictionaries == null) return;
            
            PackageManager pm = requireContext().getPackageManager();
            List<CharSequence> dictionaryList = new ArrayList<CharSequence>();
            
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

            List<PackageInfo> packages = pm.getInstalledPackages(0);
            for (PackageInfo pkgInfo : packages) {
                String pkg = pkgInfo.packageName;
                if (pkg.startsWith("org.n0pocketworkstation.dict") || 
                    pkg.startsWith("org.pocketworkstation.dict") ||
                    pkg.startsWith("com.anysoftkeyboard.languagepack") ||
                    pkg.equals("com.menny.android.anysoftkeyboard") ||
                    pkg.equals("com.menny.anysoftkeyboard.pack.dictionaries")) {
                    
                    try {
                        String label = pm.getApplicationLabel(pkgInfo.applicationInfo).toString();
                        String entry = label + " [" + pkg + "]";
                        if (pkg.equals("com.menny.android.anysoftkeyboard")) {
                            entry = "English from " + entry;
                        }
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
                                entry += " (Empty/Error)";
                                ssb = new SpannableStringBuilder(entry);
                                ssb.setSpan(new ForegroundColorSpan(0xFFFFA500), 0, entry.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            dictionaryList.add(ssb);
                        } else {
                            dictionaryList.add(entry);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing dictionary package: " + pkg, e);
                    }
                }
            }

            if (!dictionaryList.isEmpty()) {
                java.util.Collections.sort(dictionaryList, (a, b) -> a.toString().compareTo(b.toString()));

                SpannableStringBuilder detected = new SpannableStringBuilder();
                for (int i = 0; i < dictionaryList.size(); i++) {
                    if (i > 0) detected.append("\n");
                    detected.append(dictionaryList.get(i));
                }
                mDetectedDictionaries.setSummary(detected);
                mDetectedDictionaries.setEnabled(true);
            } else {
                mDetectedDictionaries.setSummary("None");
            }
        }

        private String inputTypeDesc(int type) {
            int mask = type & android.text.InputType.TYPE_MASK_CLASS;
            if (mask == android.text.InputType.TYPE_CLASS_TEXT) return "text";
            if (mask == android.text.InputType.TYPE_CLASS_NUMBER) return "number";
            if (mask == android.text.InputType.TYPE_CLASS_PHONE) return "phone";
            if (mask == android.text.InputType.TYPE_CLASS_DATETIME) return "datetime";
            return "unknown";
        }
    }
}
