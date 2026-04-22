package org.n0pocketworkstation.pckeyboard;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;

public class PCKeyboardApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        updateLocale(this);
        updateTheme();
    }

    public static void updateLocale(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String langCode = sp.getString("pref_ui_language", "");
        if (!langCode.isEmpty()) {
            // Replace underscores with hyphens for BCP 47 language tags used by LocaleListCompat
            String languageTag = langCode.replace('_', '-');
            androidx.core.os.LocaleListCompat appLocales = androidx.core.os.LocaleListCompat.forLanguageTags(languageTag);
            if (!appLocales.equals(AppCompatDelegate.getApplicationLocales())) {
                AppCompatDelegate.setApplicationLocales(appLocales);
            }
        }
    }

    public void updateTheme() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean revert = sp.getBoolean("pref_revert_theme_color", false);
        if (revert) {
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
