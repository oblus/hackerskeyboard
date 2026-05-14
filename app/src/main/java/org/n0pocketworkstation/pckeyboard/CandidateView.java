/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import androidx.core.content.ContextCompat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CandidateView extends View {

    private static final int OUT_OF_BOUNDS_WORD_INDEX = -1;
    private static final int OUT_OF_BOUNDS_X_COORD = -1;

    private LatinIME mService;
    private final ArrayList<CharSequence> mSuggestions = new ArrayList<CharSequence>();
    private boolean mShowingCompletions;
    private CharSequence mSelectedString;
    private int mSelectedIndex;
    private int mTouchX = OUT_OF_BOUNDS_X_COORD;
    private final Drawable mSelectionHighlight;
    private boolean mTypedWordValid;
    
    private boolean mHaveMinimalSuggestion;
    
    private Rect mBgPadding;

    private final TextView mPreviewText;
    private final PopupWindow mPreviewPopup;
    private int mCurrentWordIndex;
    private Drawable mDivider;
    
    private static final int MAX_SUGGESTIONS = 32;
    private static final int SCROLL_PIXELS = 20;
    
    private final int[] mWordWidth = new int[MAX_SUGGESTIONS];
    private final int[] mWordX = new int[MAX_SUGGESTIONS];
    private final int[] mWordSource = new int[MAX_SUGGESTIONS];
    private int mPopupPreviewX;
    private int mPopupPreviewY;

    private static final int X_GAP = 10;
    
    private final int mColorNormal;
    private final int mColorRecommended;
    private final int mColorAuto;
    private final int mColorOther;
    private final int mColorHoloBlue;
    private final Paint mPaint;
    private final int mDescent;
    private boolean mScrolled;
    private boolean mShowingAddToDictionary;
    private CharSequence mAddToDictionaryHint;

    private int mTargetScrollX;

    private final int mMinTouchableWidth;

    private int mTotalWidth;
    
    private final GestureDetector mGestureDetector;

    /**
     * Construct a CandidateView for showing suggested words for completion.
     * @param context
     * @param attrs
     */
    public CandidateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSelectionHighlight = ContextCompat.getDrawable(context,
                R.drawable.list_selector_background_pressed);

        LayoutInflater inflate =
            (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Resources res = context.getResources();
        mPreviewPopup = new PopupWindow(context);
        mPreviewText = (TextView) inflate.inflate(R.layout.candidate_preview, null);
        mPreviewPopup.setWidth(LayoutParams.WRAP_CONTENT);
        mPreviewPopup.setHeight(LayoutParams.WRAP_CONTENT);
        mPreviewPopup.setContentView(mPreviewText);
        mPreviewPopup.setBackgroundDrawable(null);
        mPreviewPopup.setAnimationStyle(R.style.KeyPreviewAnimation);
        // Enable clipping for Android P, keep disabled for older versions.
        boolean clippingEnabled = (Build.VERSION.SDK_INT >= 28 /* Build.VERSION_CODES.P */);
        mPreviewPopup.setClippingEnabled(clippingEnabled);
        mColorNormal = ContextCompat.getColor(context, R.color.candidate_normal);
        mColorRecommended = ContextCompat.getColor(context, R.color.candidate_recommended);
        mColorAuto = ContextCompat.getColor(context, R.color.candidate_auto);
        mColorOther = ContextCompat.getColor(context, R.color.candidate_other);
        mColorHoloBlue = 0xFF33B5E5; // Holo Blue Light
        mDivider = ContextCompat.getDrawable(context, R.drawable.keyboard_suggest_strip_divider);
        mAddToDictionaryHint = res.getString(R.string.hint_add_to_dictionary);

        mPaint = new Paint();
        mPaint.setColor(mColorNormal);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mPreviewText.getTextSize() * LatinIME.sKeyboardSettings.candidateScalePref);
        mPaint.setStrokeWidth(0);
        mPaint.setTextAlign(Align.CENTER);
        mDescent = (int) mPaint.descent();
        mMinTouchableWidth = (int)res.getDimension(R.dimen.candidate_min_touchable_width);
        
        mGestureDetector = new GestureDetector(context,
                new CandidateStripGestureListener(mMinTouchableWidth));
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        scrollTo(0, getScrollY());
    }

    private class CandidateStripGestureListener extends GestureDetector.SimpleOnGestureListener {
        private final int mTouchSlopSquare;

        public CandidateStripGestureListener(int touchSlop) {
            // Slightly reluctant to scroll to be able to easily choose the suggestion
            mTouchSlopSquare = touchSlop * touchSlop;
        }

        @Override
        public void onLongPress(MotionEvent me) {
            if (mSuggestions.size() > 0) {
                if (me.getX() + getScrollX() < mWordWidth[0] && getScrollX() < 10) {
                    longPressFirstWord();
                }
            }
        }

        @Override
        public boolean onDown(MotionEvent e) {
            mScrolled = false;
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            if (!mScrolled) {
                // This is applied only when we recognize that scrolling is starting.
                final int deltaX = (int) (e2.getX() - e1.getX());
                final int deltaY = (int) (e2.getY() - e1.getY());
                final int distance = (deltaX * deltaX) + (deltaY * deltaY);
                if (distance < mTouchSlopSquare) {
                    return true;
                }
                mScrolled = true;
            }

            final int width = getWidth();
            mScrolled = true;
            int scrollX = getScrollX();
            scrollX += (int) distanceX;
            if (scrollX < 0) {
                scrollX = 0;
            }
            if (distanceX > 0 && scrollX + width > mTotalWidth) {
                scrollX -= (int) distanceX;
            }
            mTargetScrollX = scrollX;
            scrollTo(scrollX, getScrollY());
            hidePreview();
            invalidate();
            return true;
        }
    }

    /**
     * A connection back to the service to communicate with the text field
     * @param listener
     */
    public void setService(LatinIME listener) {
        mService = listener;
    }
    
    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    /**
     * If the canvas is null, then only touch calculations are performed to pick the target
     * candidate.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
        }
        mTotalWidth = 0;
        
        final int height = getHeight();
        if (mBgPadding == null) {
            mBgPadding = new Rect(0, 0, 0, 0);
            if (getBackground() != null) {
                getBackground().getPadding(mBgPadding);
            }
            mDivider.setBounds(0, 0, mDivider.getIntrinsicWidth(),
                    mDivider.getIntrinsicHeight());
        }

        final int count = mSuggestions.size();
        final Rect bgPadding = mBgPadding;
        final Paint paint = mPaint;
        final int touchX = mTouchX;
        final int scrollX = getScrollX();
        final boolean scrolled = mScrolled;
        final boolean typedWordValid = mTypedWordValid;
        final int y = (int) (height + mPaint.getTextSize() - mDescent) / 2;

        boolean existsAutoCompletion = false;

        int x = 0;
        for (int i = 0; i < count; i++) {
            CharSequence suggestion = mSuggestions.get(i);
            if (suggestion == null) continue;
            final int wordLength = suggestion.length();
            
            // Check if it's punctuation: single char, not letter or digit
            boolean isPunctuation = wordLength == 1 && !Character.isLetterOrDigit(suggestion.charAt(0));
            int source = mWordSource[i];
            
            // Logic for 4-color candidate bar based on state and source
            if (mShowingAddToDictionary || isPunctuation) {
                paint.setColor(mColorOther); // Orange for "Add to dictionary" or punctuation
            } else if (source == Suggest.DIC_AUTO || source == Suggest.DIC_USER) {
                paint.setColor(mColorAuto);  // Yellow: Always for Learned/User dictionary
            } else if (!typedWordValid) {
                // STATE A: Word NOT in dictionary
                if (i == 0) {
                    paint.setColor(mColorNormal); // White: exactly what user typed
                } else if (i == 1) {
                    // Index 1 is the auto-correction candidate. 
                    paint.setColor(mColorAuto);   // Yellow
                } else if (i == 2) {
                    paint.setColor(mColorRecommended); // Blue: main dictionary suggestion
                } else {
                    paint.setColor(mColorOther); // Orange: rest
                }
            } else {
                // STATE B: Word IS in dictionary
                if (i == 0) {
                    // Valid word at index 0 is the active choice.
                    paint.setColor(mColorAuto); // Yellow
                } else if (i == 1) {
                    paint.setColor(mColorRecommended); // Blue: next suggestion
                } else {
                    paint.setColor(mColorOther); // Orange: rest
                }
            }

            boolean isAutoCompletion = mHaveMinimalSuggestion
                    && ((i == 1 && !typedWordValid) || (i == 0 && typedWordValid));

            if (isAutoCompletion) {
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                existsAutoCompletion = true;
            } else {
                paint.setTypeface(Typeface.DEFAULT);
            }
            int wordWidth;
            if ((wordWidth = mWordWidth[i]) == 0) {
                float textWidth =  paint.measureText(suggestion, 0, wordLength);
                wordWidth = Math.max(mMinTouchableWidth, (int) textWidth + X_GAP * 2);
                mWordWidth[i] = wordWidth;
            }

            mWordX[i] = x;

            if (touchX != OUT_OF_BOUNDS_X_COORD && !scrolled
                    && touchX + scrollX >= x && touchX + scrollX < x + wordWidth) {
                if (canvas != null && !mShowingAddToDictionary) {
                    canvas.translate(x, 0);
                    mSelectionHighlight.setBounds(0, bgPadding.top, wordWidth, height);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-x, 0);
                }
                mSelectedString = suggestion;
                mSelectedIndex = i;
            }

            if (canvas != null) {
                canvas.drawText(suggestion, 0, wordLength, x + wordWidth / 2, y, paint);
                paint.setTypeface(Typeface.DEFAULT);
                canvas.translate(x + wordWidth, 0);
                // Draw a divider unless it's after the hint
                if (!(mShowingAddToDictionary && i == 1)) {
                    mDivider.draw(canvas);
                }
                canvas.translate(-x - wordWidth, 0);
            }
            x += wordWidth;
        }
        if (!isInEditMode())
            mService.onAutoCompletionStateChanged(existsAutoCompletion);
        mTotalWidth = x;
        if (mTargetScrollX != scrollX) {
            scrollToTarget();
        }
    }
    
    private void scrollToTarget() {
        int scrollX = getScrollX();
        if (mTargetScrollX > scrollX) {
            scrollX += SCROLL_PIXELS;
            if (scrollX >= mTargetScrollX) {
                scrollX = mTargetScrollX;
                scrollTo(scrollX, getScrollY());
                requestLayout();
            } else {
                scrollTo(scrollX, getScrollY());
            }
        } else {
            scrollX -= SCROLL_PIXELS;
            if (scrollX <= mTargetScrollX) {
                scrollX = mTargetScrollX;
                scrollTo(scrollX, getScrollY());
                requestLayout();
            } else {
                scrollTo(scrollX, getScrollY());
            }
        }
        invalidate();
    }
    
    public void setSuggestions(List<CharSequence> suggestions, boolean completions,
            boolean typedWordValid, boolean haveMinimalSuggestion, int[] sources) {
        boolean changed = false;
        if (suggestions == null) {
            if (!mSuggestions.isEmpty()) {
                changed = true;
            }
        } else if (suggestions.size() != mSuggestions.size()) {
            changed = true;
        } else {
            for (int i = 0; i < suggestions.size(); i++) {
                if (!suggestions.get(i).equals(mSuggestions.get(i))) {
                    changed = true;
                    break;
                }
            }
        }

        clear();
        if (suggestions != null) {
            int insertCount = Math.min(suggestions.size(), MAX_SUGGESTIONS);
            for (int i = 0; i < insertCount; i++) {
                mSuggestions.add(suggestions.get(i));
                if (sources != null && i < sources.length) {
                    mWordSource[i] = sources[i];
                } else {
                    mWordSource[i] = 0; // DIC_USER_TYPED
                }
            }
        }
        mShowingCompletions = completions;
        mTypedWordValid = typedWordValid;
        if (changed && getScrollX() != 0) {
            scrollTo(0, getScrollY());
            mTargetScrollX = 0;
        }
        mHaveMinimalSuggestion = haveMinimalSuggestion;
        // Compute the total width
        onDraw(null);
        invalidate();
        requestLayout();
    }

    public void resetScroll() {
        if (getScrollX() != 0) {
            scrollTo(0, getScrollY());
            mTargetScrollX = 0;
        }
    }

    public boolean isShowingAddToDictionaryHint() {
        return mShowingAddToDictionary;
    }

    public void showAddToDictionaryHint(CharSequence word) {
        ArrayList<CharSequence> suggestions = new ArrayList<CharSequence>();
        suggestions.add(word);
        suggestions.add(mAddToDictionaryHint);
        setSuggestions(suggestions, false, false, false, null);
        mShowingAddToDictionary = true;
    }

    public boolean dismissAddToDictionaryHint() {
        if (!mShowingAddToDictionary) return false;
        mShowingAddToDictionary = false; // Ensure flag is reset first
        clear();
        return true;
    }

    /* package */ List<CharSequence> getSuggestions() {
        return mSuggestions;
    }

    public int getSuggestionsCount() {
        return mSuggestions.size();
    }

    public void clear() {
        // Don't call mSuggestions.clear() because it's being used for logging
        // in LatinIME.pickSuggestionManually().
        mSuggestions.clear();
        mTouchX = OUT_OF_BOUNDS_X_COORD;
        mSelectedString = null;
        mSelectedIndex = -1;
        mShowingAddToDictionary = false;
        invalidate();
        Arrays.fill(mWordWidth, 0);
        Arrays.fill(mWordX, 0);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {

        if (mGestureDetector.onTouchEvent(me)) {
            return true;
        }

        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            invalidate();
            break;
        case MotionEvent.ACTION_MOVE:
            if (y <= 0) {
                // Fling up!?
                if (mSelectedString != null) {
                    // If there are completions from the application, we don't change the state to
                    // STATE_PICKED_SUGGESTION
                    if (!mShowingCompletions) {
                        // This "acceptedSuggestion" will not be counted as a word because
                        // it will be counted in pickSuggestion instead.
                        //TextEntryState.acceptedSuggestion(mSuggestions.get(0), mSelectedString);
                        //TextEntryState.manualTyped(mSelectedString);
                    }
                    mService.pickSuggestionManually(mSelectedIndex, mSelectedString);
                    mSelectedString = null;
                    mSelectedIndex = -1;
                }
            }
            break;
        case MotionEvent.ACTION_UP:
            if (!mScrolled) {
                if (mSelectedString != null) {
                    if (mShowingAddToDictionary) {
                        // Tapping either the word (index 0) or the "Add to dictionary" hint (index 1)
                        // will save the word.
                        longPressFirstWord();
                        clear();
                    } else {
                        mService.pickSuggestionManually(mSelectedIndex, mSelectedString);
                    }
                } else {
                    // Tap on empty space of candidate bar also dismisses the hint
                    if (mShowingAddToDictionary) {
                        dismissAddToDictionaryHint();
                    }
                }
            }
            mSelectedString = null;
            mSelectedIndex = -1;
            // requestLayout(); // Removed to prevent scrolling reset
            hidePreview();
            invalidate();
            break;
        }
        return true;
    }

    private void hidePreview() {
        mTouchX = OUT_OF_BOUNDS_X_COORD;
        mCurrentWordIndex = OUT_OF_BOUNDS_WORD_INDEX;
        mPreviewPopup.dismiss();
    }
    
    private void showPreview(int wordIndex, String altText) {
        int oldWordIndex = mCurrentWordIndex;
        mCurrentWordIndex = wordIndex;
        // If index changed or changing text
        if (oldWordIndex != mCurrentWordIndex || altText != null) {
            if (wordIndex == OUT_OF_BOUNDS_WORD_INDEX) {
                hidePreview();
            } else {
                CharSequence word = altText != null? altText : mSuggestions.get(wordIndex);
                mPreviewText.setText(word);
                mPreviewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int wordWidth = (int) (mPaint.measureText(word, 0, word.length()) + X_GAP * 2);
                final int popupWidth = wordWidth
                        + mPreviewText.getPaddingLeft() + mPreviewText.getPaddingRight();
                final int popupHeight = mPreviewText.getMeasuredHeight();
                //mPreviewText.setVisibility(INVISIBLE);
                mPopupPreviewX = mWordX[wordIndex] - mPreviewText.getPaddingLeft() - getScrollX()
                        + (mWordWidth[wordIndex] - wordWidth) / 2;
                mPopupPreviewY = - popupHeight;
                int [] offsetInWindow = new int[2];
                getLocationInWindow(offsetInWindow);
                if (mPreviewPopup.isShowing()) {
                    mPreviewPopup.update(mPopupPreviewX, mPopupPreviewY + offsetInWindow[1], 
                            popupWidth, popupHeight);
                } else {
                    mPreviewPopup.setWidth(popupWidth);
                    mPreviewPopup.setHeight(popupHeight);
                    mPreviewPopup.showAtLocation(this, Gravity.NO_GRAVITY, mPopupPreviewX, 
                            mPopupPreviewY + offsetInWindow[1]);
                }
                mPreviewText.setVisibility(VISIBLE);
            }
        }
    }

    private void longPressFirstWord() {
        CharSequence word = mSuggestions.get(0);
        if (word.length() < 2) return;
        if (mService.addWordToDictionary(word.toString())) {
            showPreview(0, getContext().getResources().getString(R.string.added_word, word));
        }
    }
    
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        hidePreview();
    }
}
