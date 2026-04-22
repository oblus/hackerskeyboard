/**
 *
 */
package org.n0pocketworkstation.pckeyboard;

import android.content.Context;
import androidx.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

public class AutoSummaryListPreference extends ListPreference {
    private static final String TAG = "HK/AutoSummaryListPreference";

    public AutoSummaryListPreference(Context context) {
        super(context);
    }

    public AutoSummaryListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        String key = getKey();
        if (key != null) {
            try {
                // Ensure it's not a boolean in the preferences
                getSharedPreferences().getString(key, null);
            } catch (ClassCastException e) {
                Log.i(TAG, "Migrating " + key + " from boolean to String");
                boolean oldVal = getSharedPreferences().getBoolean(key, true);
                getSharedPreferences().edit().remove(key).putString(key, oldVal ? "0" : "2").commit();
            }
        }
        super.onSetInitialValue(defaultValue);
    }

    private void trySetSummary() {
        CharSequence entry = null;
        try {
            entry = getEntry();
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.i(TAG, "Malfunctioning ListPreference, can't get entry");
        }
        if (entry != null) {
            //String percent = getResources().getString(R.string.percent);
            String percent = "percent";
            setSummary(entry.toString().replace("%", " " + percent));
        }
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);
        trySetSummary();
    }

    @Override
    public void setEntryValues(CharSequence[] entryValues) {
        super.setEntryValues(entryValues);
        trySetSummary();
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        trySetSummary();
    }
}
