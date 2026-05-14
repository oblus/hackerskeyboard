package org.n0pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.preference.EditTextPreference;
import android.util.AttributeSet;

public class AutoSummaryEditTextPreference extends EditTextPreference {

    public AutoSummaryEditTextPreference(Context context) {
        super(context);
    }

    public AutoSummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AutoSummaryEditTextPreference(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoSummaryEditTextPreference);
        boolean isVisible = a.getBoolean(R.styleable.AutoSummaryEditTextPreference_isPreferenceVisible, true);
        setVisible(isVisible);
        a.recycle();
    }

    // Note: We deliberately do NOT override setText(String) here to set the summary.
    // In this project, punctuation settings (suggested_punctuation, punctuation_swap_list)
    // require custom descriptive summaries (e.g., "Description: value") which are
    // managed manually in LatinIMESettings.updateSummaries().
    // Overriding setText to call setSummary(text) would overwrite those custom summaries
    // with raw values whenever the preference is updated.
}
