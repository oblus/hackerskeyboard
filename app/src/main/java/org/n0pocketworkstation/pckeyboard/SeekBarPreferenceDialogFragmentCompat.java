package org.n0pocketworkstation.pckeyboard;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

public class SeekBarPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    public static SeekBarPreferenceDialogFragmentCompat newInstance(String key) {
        SeekBarPreferenceDialogFragmentCompat fragment = new SeekBarPreferenceDialogFragmentCompat();
        Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        SeekBarPreference preference = (SeekBarPreference) getPreference();
        preference.onBindDialogView(view);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        SeekBarPreference preference = (SeekBarPreference) getPreference();
        preference.onDialogClosed(positiveResult);
    }
}
