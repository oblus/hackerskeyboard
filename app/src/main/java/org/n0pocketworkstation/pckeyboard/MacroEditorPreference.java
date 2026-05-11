package org.n0pocketworkstation.pckeyboard;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class MacroEditorPreference extends Preference {

    private OnMacroInteractionListener mListener;
    private String mContent = "";
    private boolean mEnabled = false;
    private boolean mIsUpdating = false;

    private int mListIdx = 0;

    public MacroEditorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_macro_editor);
    }

    public MacroEditorPreference(Context context) {
        this(context, null);
    }

    public void setPosition(int listIdx) {
        mListIdx = listIdx;
        notifyChanged();
    }

    public void setMacroData(String content, boolean enabled) {
        mContent = (content == null) ? "" : content;
        mEnabled = enabled;
        notifyChanged();
    }

    public String getContent() { return mContent; }
    public boolean isMacroEnabled() { return mEnabled; }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        
        final EditText editField = (EditText) holder.findViewById(R.id.macro_edit_field);
        final CheckBox checkBox = (CheckBox) holder.findViewById(R.id.macro_checkbox_field);
        final View btnUp = holder.findViewById(R.id.macro_move_up);
        final View btnDown = holder.findViewById(R.id.macro_move_down);

        // Zapobieganie teleportacji danych - usuwamy starego słuchacza przed bindowaniem nowego
        if (editField != null) {
            Object oldWatcher = editField.getTag();
            if (oldWatcher instanceof TextWatcher) {
                editField.removeTextChangedListener((TextWatcher) oldWatcher);
            }
            editField.setTag(null);
        }
        if (checkBox != null) {
            checkBox.setOnCheckedChangeListener(null);
        }

        mIsUpdating = true;
        if (editField != null) {
            editField.setText(mContent);
        }
        if (checkBox != null) {
            checkBox.setChecked(mEnabled);
        }
        mIsUpdating = false;

        if (editField != null) {
            TextWatcher watcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (mIsUpdating) return;
                    mContent = s.toString();
                    if (mListener != null) {
                        mListener.onMacroChanged(MacroEditorPreference.this, mContent, mEnabled);
                    }
                }
            };
            editField.addTextChangedListener(watcher);
            editField.setTag(watcher);
        }

        if (checkBox != null) {
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mIsUpdating) return;
                mEnabled = isChecked;
                if (mListener != null) {
                    mListener.onMacroChanged(MacroEditorPreference.this, mContent, mEnabled);
                }
            });
        }

        if (btnUp != null) {
            // Strzałka w górę (usuwanie): Widoczna dla pól 2, 3, 4, 5 (indeksy 1-4)
            btnUp.setVisibility(mListIdx > 0 ? View.VISIBLE : View.INVISIBLE);
            btnUp.setOnClickListener(v -> {
                if (mListener != null) mListener.onMoveUp(MacroEditorPreference.this);
            });
        }

        if (btnDown != null) {
            // Strzałka w dół (dodawanie): Widoczna dla pól 1, 2, 3, 4 (indeksy 0-3)
            btnDown.setVisibility(mListIdx < 4 ? View.VISIBLE : View.INVISIBLE);
            btnDown.setOnClickListener(v -> {
                if (mListener != null) mListener.onMoveDown(MacroEditorPreference.this);
            });
        }
    }

    public interface OnMacroInteractionListener {
        void onMoveUp(MacroEditorPreference pref);
        void onMoveDown(MacroEditorPreference pref);
        void onMacroChanged(MacroEditorPreference pref, String content, boolean enabled);
    }

    public void setOnMacroInteractionListener(OnMacroInteractionListener listener) {
        mListener = listener;
    }
}
