/*
 * Copyright (C) 2008-2009 Google Inc.
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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

public class InputLanguageSelection extends AppCompatActivity {
    private static final String TAG = "PCKeyboardILS";
    private LanguageSelectionFragment mFragment;

    // Languages for which auto-caps should be disabled
    public static final Set<String> NOCAPS_LANGUAGES = new HashSet<String>();
    static {
        NOCAPS_LANGUAGES.add("ar");
        NOCAPS_LANGUAGES.add("iw");
        NOCAPS_LANGUAGES.add("th");
    }

    // Languages which should not use dead key logic.
    public static final Set<String> NODEADKEY_LANGUAGES = new HashSet<String>();
    static {
        NODEADKEY_LANGUAGES.add("ar");
        NODEADKEY_LANGUAGES.add("iw");
        NODEADKEY_LANGUAGES.add("th");
    }

    // Languages which should not auto-add space after completions
    public static final Set<String> NOAUTOSPACE_LANGUAGES = new HashSet<String>();
    static {
        NOAUTOSPACE_LANGUAGES.add("th");
    }

    @Override
    protected void onCreate(Bundle icicle) {
        PCKeyboardApp.updateLocale(this);
        super.onCreate(icicle);
        setContentView(R.layout.language_selection);

        if (icicle == null) {
            mFragment = new LanguageSelectionFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.preference_container, mFragment)
                    .commit();
        } else {
            mFragment = (LanguageSelectionFragment) getSupportFragmentManager().findFragmentById(R.id.preference_container);
        }

        EditText searchField = (EditText) findViewById(R.id.search_field);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mFragment != null) {
                    mFragment.buildPreferenceList(s.toString());
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFragment != null) {
            mFragment.savePreferences();
        }
    }

    public static class LanguageSelectionFragment extends PreferenceFragmentCompat {
        private ArrayList<Loc> mAvailableLanguages = new ArrayList<Loc>();
        private Set<String> mSelectedLanguages = new HashSet<String>();
        private static final String[] BLACKLIST_LANGUAGES = {
            "ko", "ja", "zh"
        };

        private static final String[] KBD_LOCALIZATIONS = {
            "ar", "bg", "bg_ST", "ca", "cs", "cs_QY", "da", "de", "de_NE",
            "el", "en", "en_CX", "en_DV", "en_GB", "es", "es_LA", "es_US",
            "fa", "fi", "fr", "fr_CA", "he", "hr", "hu", "hu_QY", "hy", "in",
            "it", "iw", "ja", "ka", "ko", "lo", "lt", "lv", "nb", "nl", "pl",
            "pt", "pt_PT", "rm", "ro", "ru", "ru_PH", "si", "sk", "sk_QY", "sl",
            "sr", "sv", "ta", "th", "tl", "tr", "uk", "vi", "zh_CN", "zh_TW"
        };

        private static final String[] KBD_5_ROW = {
            "ar", "bg", "bg_ST", "cs", "cs_QY", "da", "de", "de_NE", "el",
            "en", "en_CX", "en_DV", "en_GB", "es", "es_LA", "fa", "fi", "fr",
            "fr_CA", "he", "hr", "hu", "hu_QY", "hy", "it", "iw", "lo", "lt",
            "nb", "pt_PT", "ro", "ru", "ru_PH", "si", "sk", "sk_QY", "sl",
            "sr", "sv", "ta", "th", "tr", "uk"
        };

        private static final String[] KBD_4_ROW = {
            "ar", "bg", "bg_ST", "cs", "cs_QY", "da", "de", "de_NE", "el",
            "en", "en_CX", "en_DV", "es", "es_LA", "es_US", "fa", "fr", "fr_CA",
            "he", "hr", "hu", "hu_QY", "iw", "nb", "ru", "ru_PH", "sk", "sk_QY",
            "sl", "sr", "sv", "tr", "uk"
        };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.language_prefs, rootKey);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String selectedLanguagePref = sp.getString(LatinIME.PREF_SELECTED_LANGUAGES, "");
            String[] languageList = selectedLanguagePref.split(",");

            mAvailableLanguages = getUniqueLocales();

            Set<String> availableLanguages = new HashSet<String>();
            for (int i = 0; i < mAvailableLanguages.size(); i++) {
                Locale locale = mAvailableLanguages.get(i).locale;
                availableLanguages.add(get5Code(locale));
            }
            mSelectedLanguages.clear();
            for (String spec : languageList) {
                if (availableLanguages.contains(spec)) {
                    mSelectedLanguages.add(spec);
                } else if (spec.length() > 2) {
                    String lang = spec.substring(0, 2);
                    if (availableLanguages.contains(lang)) mSelectedLanguages.add(lang);
                }
            }

            Collections.sort(mAvailableLanguages, new Comparator<Loc>() {
                @Override
                public int compare(Loc lhs, Loc rhs) {
                    String lCode = get5Code(lhs.locale);
                    String rCode = get5Code(rhs.locale);
                    boolean lChecked = mSelectedLanguages.contains(lCode);
                    boolean rChecked = mSelectedLanguages.contains(rCode);
                    if (lChecked != rChecked) {
                        return lChecked ? -1 : 1;
                    }
                    return lhs.compareTo(rhs);
                }
            });

            buildPreferenceList(null);
        }

        public void buildPreferenceList(String query) {
            PreferenceGroup parent = getPreferenceScreen();
            if (parent == null) return;
            parent.removeAll();
            String lowerQuery = query != null ? query.toLowerCase(Locale.getDefault()) : "";
            for (Loc loc : mAvailableLanguages) {
                if (!lowerQuery.isEmpty()) {
                    String label = loc.label.toLowerCase(Locale.getDefault());
                    String code = get5Code(loc.locale).toLowerCase(Locale.getDefault());
                    if (!label.contains(lowerQuery) && !code.contains(lowerQuery)) {
                        continue;
                    }
                }
                addLanguagePreference(loc, parent);
            }
        }

        private void addLanguagePreference(Loc loc, PreferenceGroup parent) {
            Locale locale = loc.locale;
            String fivecode = get5Code(locale);
            String language = locale.getLanguage();

            CheckBoxPreference pref = new CheckBoxPreference(requireContext());
            pref.setIconSpaceReserved(false);
            pref.setTitle(loc.label + " [" + locale.toString() + "]");
            pref.setKey(fivecode);
            pref.setChecked(mSelectedLanguages.contains(fivecode));
            pref.setPersistent(false);
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    boolean checked = (Boolean) newValue;
                    if (checked) {
                        mSelectedLanguages.add(preference.getKey());
                    } else {
                        mSelectedLanguages.remove(preference.getKey());
                    }
                    return true;
                }
            });

            boolean has4Row = arrayContains(KBD_4_ROW, fivecode) || arrayContains(KBD_4_ROW, language);
            boolean has5Row = arrayContains(KBD_5_ROW, fivecode) || arrayContains(KBD_5_ROW, language);
            List<String> summaries = new ArrayList<>(3);
            if (has5Row) summaries.add("5-row");
            if (has4Row) summaries.add("4-row");
            if (hasDictionary(locale)) {
                summaries.add(getResources().getString(R.string.has_dictionary));
            }
            if (!summaries.isEmpty()) {
                StringBuilder summary = new StringBuilder();
                for (int j = 0; j < summaries.size(); ++j) {
                    if (j > 0) summary.append(", ");
                    summary.append(summaries.get(j));
                }
                pref.setSummary(summary.toString());
            }
            parent.addPreference(pref);
        }

        private boolean hasDictionary(Locale locale) {
            Context context = getContext();
            if (context == null) return false;
            
            // Re-enable plugin lookup safely
            try {
                BinaryDictionary plug = PluginManager.getDictionary(context.getApplicationContext(), locale.getLanguage());
                if (plug != null) {
                    plug.close();
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking plugin dictionary", e);
            }
            return false;
        }

        public void savePreferences() {
            StringBuilder checkedLanguages = new StringBuilder();
            for (Loc loc : mAvailableLanguages) {
                String fiveCode = get5Code(loc.locale);
                if (mSelectedLanguages.contains(fiveCode)) {
                    if (checkedLanguages.length() > 0) checkedLanguages.append(",");
                    checkedLanguages.append(fiveCode);
                }
            }
            String result = checkedLanguages.length() > 0 ? checkedLanguages.toString() : null;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
            Editor editor = sp.edit();
            editor.putString(LatinIME.PREF_SELECTED_LANGUAGES, result);
            editor.apply();
        }

        private String get5Code(Locale locale) {
            String country = locale.getCountry();
            return locale.getLanguage()
                    + (TextUtils.isEmpty(country) ? "" : "_" + country);
        }

        ArrayList<Loc> getUniqueLocales() {
            Set<String> localeSet = new HashSet<>();
            Set<String> langSet = new HashSet<>();

            for (String kl : KBD_LOCALIZATIONS) {
                if (kl.length() == 2 && langSet.contains(kl)) continue;
                if (kl.length() == 6) kl = kl.substring(0, 2) + "_" + kl.substring(4, 6);
                localeSet.add(kl);
            }

            String[] locales = localeSet.toArray(new String[0]);
            Arrays.sort(locales);
            
            ArrayList<Loc> uniqueLocales = new ArrayList<>();
            final int origSize = locales.length;
            Loc[] preprocess = new Loc[origSize];
            int finalSize = 0;
            for (String s : locales) {
                int len = s.length();
                if (len == 2 || len == 5 || len == 6) {
                    String language = s.substring(0, 2);
                    Locale l;
                    if (len == 5) {
                        String country = s.substring(3, 5);
                        l = new Locale.Builder().setLanguage(language).setRegion(country).build();
                    } else if (len == 6) {
                        l = new Locale.Builder().setLanguage(language).setRegion(s.substring(4, 6)).build();
                    } else {
                        l = new Locale.Builder().setLanguage(language).build();
                    }

                    if (arrayContains(BLACKLIST_LANGUAGES, language)) continue;

                    if (finalSize == 0) {
                        preprocess[finalSize++] =
                                new Loc(LanguageSwitcher.toTitleCase(l.getDisplayName(l)), l);
                    } else {
                        if (preprocess[finalSize - 1].locale.getLanguage().equals(language)) {
                            preprocess[finalSize - 1].label = getLocaleName(preprocess[finalSize - 1].locale);
                            preprocess[finalSize++] = new Loc(getLocaleName(l), l);
                        } else {
                            preprocess[finalSize++] = new Loc(getLocaleName(l), l);
                        }
                    }
                }
            }
            for (int i = 0; i < finalSize ; i++) {
                uniqueLocales.add(preprocess[i]);
            }
            return uniqueLocales;
        }

        private boolean arrayContains(String[] array, String value) {
            for (String s : array) {
                if (s.equalsIgnoreCase(value)) return true;
            }
            return false;
        }

        private static String getLocaleName(Locale l) {
            String lang = l.getLanguage();
            String country = l.getCountry();
            if (lang.equals("en") && country.equals("DV")) {
                return "English (Dvorak)";
            } else if (lang.equals("en") && country.equals("EX")) {
                return "English (4x11)";
            } else if (lang.equals("en") && country.equals("CX")) {
                return "English (Carpalx)";
            } else if (lang.equals("es") && country.equals("LA")) {
                return "Español (Latinoamérica)";
            } else if (lang.equals("cs") && country.equals("QY")) {
                return "Čeština (QWERTY)";
            } else if (lang.equals("de") && country.equals("NE")) {
                return "Deutsch (Neo2)";
            } else if (lang.equals("hu") && country.equals("QY")) {
                return "Magyar (QWERTY)";
            } else if (lang.equals("sk") && country.equals("QY")) {
                return "Slovenčina (QWERTY)";
            } else if (lang.equals("ru") && country.equals("PH")) {
                return "Русский (Phonetic)";
            } else if (lang.equals("bg")) {
                if (country.equals("ST")) {
                    return "български език (Standard)";
                } else {
                    return "български език (Phonetic)";
                }
            } else {
                return LanguageSwitcher.toTitleCase(l.getDisplayName(l));
            }
        }
    }

    private static class Loc implements Comparable<Object> {
        static Collator sCollator = Collator.getInstance();
        String label;
        Locale locale;
        public Loc(String label, Locale locale) {
            this.label = label;
            this.locale = locale;
        }
        @Override
        public String toString() { return this.label; }
        @Override
        public int compareTo(@NonNull Object o) { return sCollator.compare(this.label, ((Loc) o).label); }
    }
}
