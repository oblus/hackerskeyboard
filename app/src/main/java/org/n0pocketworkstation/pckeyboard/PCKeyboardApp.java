package org.n0pocketworkstation.pckeyboard;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatDelegate;

public class PCKeyboardApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        updateTheme();
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
