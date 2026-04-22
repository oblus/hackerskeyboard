package org.n0pocketworkstation.pckeyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

public class ClickablePreferenceCategory extends PreferenceCategory {
    
    public interface OnCategoryClickListener {
        void onCategoryClick(ClickablePreferenceCategory category);
    }

    private OnCategoryClickListener mOnCategoryClickListener;

    public ClickablePreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectable(true);
    }

    public ClickablePreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSelectable(true);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        mOnCategoryClickListener = listener;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        
        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
        
        TypedValue outValue = new TypedValue();
        if (getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            holder.itemView.setBackgroundResource(outValue.resourceId);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnCategoryClickListener != null) {
                    mOnCategoryClickListener.onCategoryClick(ClickablePreferenceCategory.this);
                }
            }
        });
    }
}
