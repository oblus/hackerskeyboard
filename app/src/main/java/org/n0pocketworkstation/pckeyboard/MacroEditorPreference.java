package org.n0pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

public class MacroEditorPreference extends Preference {

    public MacroEditorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_macro_editor);
    }

    public MacroEditorPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        
        holder.itemView.setClickable(false);
        holder.itemView.setFocusable(false);
        
        final EditText editText = (EditText) holder.findViewById(R.id.macro_edit_field);
        final CheckBox checkBox = (CheckBox) holder.findViewById(R.id.macro_checkbox_field);
        
        final String key = getKey();
        if (key == null) return;

        final String index = key.substring(key.lastIndexOf("_") + 1);
        final String contentKey = "macro_content_" + index;
        final String controlKey = "macro_control_" + index;
        
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        
        if (editText != null) {
            // Usuwamy starego watcher'a, żeby uniknąć problemów przy recyclingu (znikający tekst)
            Object oldWatcher = editText.getTag(R.id.macro_edit_field);
            if (oldWatcher instanceof TextWatcher) {
                editText.removeTextChangedListener((TextWatcher) oldWatcher);
            }

            editText.setFocusableInTouchMode(true);
            editText.setText(sp.getString(contentKey, ""));
            
            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    sp.edit().putString(contentKey, s.toString()).apply();
                }
            };
            editText.addTextChangedListener(watcher);
            editText.setTag(R.id.macro_edit_field, watcher); // Zapisujemy watcher w tagu
        }
        
        if (checkBox != null) {
            checkBox.setOnCheckedChangeListener(null); // Czyścimy stary listener
            checkBox.setFocusable(true);
            checkBox.setChecked(sp.getBoolean(controlKey, false));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    sp.edit().putBoolean(controlKey, isChecked).apply();
                }
            });
        }
    }
}
