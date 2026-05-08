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

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.os.VibrationEffect;
import androidx.core.app.NotificationCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import android.util.TypedValue;
import android.widget.Button;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
//import androidx.preference.PreferenceActivity;


import org.n0pocketworkstation.pckeyboard.LatinIMEUtil.RingCharBuffer;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Input method implementation for Qwerty'ish keyboard.
 */
public class LatinIME extends InputMethodService implements
        ComposeSequencing,
        LatinKeyboardBaseView.OnKeyboardActionListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static GlobalKeyboardSettings getSettingsSafe() {
        return LatinIME.sKeyboardSettings;
    }

    private static final String TAG = "PCKeyboardIME";
    private static final String NOTIFICATION_CHANNEL_ID = "PCKeyboard";
    private static final int NOTIFICATION_ONGOING_ID = 1001;
    static Map<Integer, String> ESC_SEQUENCES;
    static Map<Integer, Integer> CTRL_SEQUENCES;

    private static final String PREF_VIBRATE_ON = "vibrate_on";
    static final String PREF_VIBRATE_LEN = "vibrate_len";
    private static final String PREF_SOUND_ON = "sound_on";
    private static final String PREF_POPUP_ON = "popup_on";
    private static final String PREF_AUTO_CAP = "auto_cap_mode";
    private static final String PREF_SHOW_SUGGESTIONS = "show_suggestions";
    private static final String PREF_MIN_LETTERS_SUGGESTION = "pref_min_letters_suggestion";
    private static final String PREF_AUTO_CORRECTION_MODE = "auto_correction_mode";
    private static final String PREF_AUTO_PUNCTUATE = "auto_punctuate";
    // private static final String PREF_BIGRAM_SUGGESTIONS =
    // "bigram_suggestion";
    private static final String PREF_VOICE_MODE = "voice_mode";

    // The private IME option used to indicate that no microphone should be
    // shown for a
    // given text field. For instance this is specified by the search dialog
    // when the
    // dialog is already showing a voice search button.
    private static final String IME_OPTION_NO_MICROPHONE = "nm";

    public static final String PREF_SELECTED_LANGUAGES = "selected_languages";
    public static final String PREF_INPUT_LANGUAGE = "input_language";
    private static final String PREF_RECORRECTION_ENABLED = "recorrection_enabled";
    static final String PREF_FULLSCREEN_OVERRIDE = "fullscreen_override";
    static final String PREF_FORCE_KEYBOARD_ON = "force_keyboard_on";
    static final String PREF_KEYBOARD_NOTIFICATION = "keyboard_notification";
    static final String PREF_CONNECTBOT_TAB_HACK = "connectbot_tab_hack";
    static final String PREF_FULL_KEYBOARD_IN_PORTRAIT = "full_keyboard_in_portrait";
    static final String PREF_SUGGESTIONS_IN_LANDSCAPE = "suggestions_in_landscape";
    static final String PREF_MACROS_IN_LANDSCAPE = "macros_in_landscape";
    static final String PREF_HEIGHT_PORTRAIT = "settings_height_portrait";
    static final String PREF_HEIGHT_LANDSCAPE = "settings_height_landscape";
    static final String PREF_HINT_MODE = "pref_hint_mode";
    static final String PREF_LONGPRESS_TIMEOUT = "pref_long_press_duration";
    static final String PREF_RENDER_MODE = "pref_render_mode";
    static final String PREF_SWIPE_UP = "pref_swipe_up";
    static final String PREF_SWIPE_DOWN = "pref_swipe_down";
    static final String PREF_SWIPE_LEFT = "pref_swipe_left";
    static final String PREF_SWIPE_RIGHT = "pref_swipe_right";
    static final String PREF_VOL_UP = "pref_vol_up";
    static final String PREF_VOL_DOWN = "pref_vol_down";

    private static final int MSG_UPDATE_SUGGESTIONS = 0;
    private static final int MSG_START_TUTORIAL = 1;
    private static final int MSG_UPDATE_SHIFT_STATE = 2;
    private static final int MSG_VOICE_RESULTS = 3;
    private static final int MSG_UPDATE_OLD_SUGGESTIONS = 4;
    private static final int MSG_ABORT_RECORRECTION = 5;

    // How many continuous deletes at which to start deleting at a higher speed.
    private static final int DELETE_ACCELERATE_AT = 20;
    // Key events coming any faster than this are long-presses.
    private static final int QUICK_PRESS = 200;

    static final int ASCII_ENTER = '\n';
    static final int ASCII_SPACE = ' ';
    static final int ASCII_PERIOD = '.';

    // Contextual menu positions
    private static final int POS_METHOD = 0;
    private static final int POS_SETTINGS = 1;

    // private LatinKeyboardView mInputView;
    private LinearLayout mCandidateViewContainer;
    private CandidateView mCandidateView;
    private View mMacroBar;
    private Suggest mSuggest;
    private CompletionInfo[] mCompletions;

    private AlertDialog mOptionsDialog;

    /* package */KeyboardSwitcher mKeyboardSwitcher;

    private UserDictionary mUserDictionary;
    private UserBigramDictionary mUserBigramDictionary;
    //private ContactsDictionary mContactsDictionary;
    private AutoDictionary mAutoDictionary;

    private Resources mResources;

    private String mInputLocale;
    private String mSystemLocale;
    private LanguageSwitcher mLanguageSwitcher;

    private StringBuilder mComposing = new StringBuilder();
    private WordComposer mWord = new WordComposer();
    private int mCommittedLength;
    private boolean mPredicting;
    private long mLastWordTouchTime; // GEM FIX: Anti-flicker hysteresis
    private boolean mEnableVoiceButton;
    private CharSequence mBestWord;
    private boolean mPredictionOnForMode;
    private boolean mPredictionOnPref;    
    private boolean mCompletionOn;
    private boolean mHasDictionary;
    private boolean mIgnoreNextUpdateSelection;
    private boolean mAutoSpace;
    private boolean mJustAddedAutoSpace;
    private boolean mAutoCorrectEnabled;
    private int mAutoCorrectionMode;
    private boolean mAutoPunctuate;
    private boolean mReCorrectionEnabled;
    // Bigram Suggestion is disabled in this version.
    private final boolean mBigramSuggestionEnabled = false;
    private boolean mAutoCorrectOn;
    // TODO move this state variable outside LatinIME
    private boolean mModCtrl;
    private boolean mModAlt;
    private boolean mModMeta;
    private boolean mModFn;
    private boolean mIsAutoShift;
    private boolean mShiftManualOverride;
    // Saved shift state when leaving alphabet mode, or when applying multitouch shift
    private int mSavedShiftState;
    private boolean mPasswordText;
    private boolean mVibrateOn;
    private int mVibrateLen;
    private boolean mSoundOn;
    private boolean mPopupOn;
    private int mAutoCapPref;
    private boolean mAutoCapActive;
    private boolean mDeadKeysActive;
    private boolean mShowSuggestions;
    private boolean mIsShowingHint;
    private boolean mConnectbotTabHack;
    private boolean mFullscreenOverride;
    private boolean mForceKeyboardOn;
    private boolean mKeyboardNotification;
    private boolean mSuggestionsInLandscape;
    private boolean mMacrosInLandscape;
    private boolean mSuggestionForceOn;
    private boolean mSuggestionForceOff;
    private String mSwipeUpAction;
    private String mSwipeDownAction;
    private String mSwipeLeftAction;
    private String mSwipeRightAction;
    private String mVolUpAction;
    private String mVolDownAction;

    public static final GlobalKeyboardSettings sKeyboardSettings = new GlobalKeyboardSettings(); 
    static LatinIME sInstance;
    
    private int mHeightPortrait;
    private int mHeightLandscape;
    private int mNumKeyboardModes = 3;
    private int mKeyboardModeOverridePortrait;
    private int mKeyboardModeOverrideLandscape;
    private int mCorrectionMode;
    private boolean mEnableVoice = true;
    private boolean mVoiceOnPrimary;
    private int mOrientation;
    private List<CharSequence> mSuggestPuncList;
    // Keep track of the last selection range to decide if we need to show word
    // alternatives
    private int mLastSelectionStart;
    private int mLastSelectionEnd;

    // Input type is such that we should not auto-correct
    private boolean mInputTypeNoAutoCorrect;

    // Indicates whether the suggestion strip is to be on in landscape
    private boolean mJustAccepted;
    private CharSequence mJustRevertedSeparator;
    private int mDeleteCount;
    private long mLastKeyTime;
    private long mLastSpaceTime;

    // Modifier keys state
    private ModifierKeyState mShiftKeyState = new ModifierKeyState();
    private ModifierKeyState mSymbolKeyState = new ModifierKeyState();
    private ModifierKeyState mCtrlKeyState = new ModifierKeyState();
    private ModifierKeyState mAltKeyState = new ModifierKeyState();
    private ModifierKeyState mMetaKeyState = new ModifierKeyState();
    private ModifierKeyState mFnKeyState = new ModifierKeyState();

    // Compose sequence handling
    private boolean mComposeMode = false;
    private ComposeSequence mComposeBuffer = new ComposeSequence(this);
    private ComposeSequence mDeadAccentBuffer = new DeadAccentSequence(this);

    private AudioManager mAudioManager;
    // Align sound effect volume on music volume
    private final float FX_VOLUME = -1.0f;
    private final float FX_VOLUME_RANGE_DB = 72.0f;
    private boolean mSilentMode;

    /* package */String mWordSeparators;
    private String mSentenceSeparators;
    private boolean mConfigurationChanging;

    // Keeps track of most recently inserted text (multi-character key) for
    // reverting
    private CharSequence mEnteredText;
    private boolean mRefreshKeyboardRequired;

    // For each word, a list of potential replacements, usually from voice.
    private Map<String, List<CharSequence>> mWordToSuggestions = new HashMap<String, List<CharSequence>>();

    private ArrayList<WordAlternatives> mWordHistory = new ArrayList<WordAlternatives>();
    
    private PluginManager mPluginManager;
    private NotificationReceiver mNotificationReceiver;

    //private VoiceRecognitionTrigger mVoiceRecognitionTrigger;
    //Object mVoiceRecognitionTrigger;
    private SpeechRecognizer mSpeechRecognizer;

    public abstract static class WordAlternatives {
        protected CharSequence mChosenWord;

        public WordAlternatives() {
            // Nothing
        }

        public WordAlternatives(CharSequence chosenWord) {
            mChosenWord = chosenWord;
        }

        @Override
        public int hashCode() {
            return mChosenWord.hashCode();
        }

        public abstract CharSequence getOriginalWord();

        public CharSequence getChosenWord() {
            return mChosenWord;
        }

        public abstract List<CharSequence> getAlternatives();
    }

    public class TypedWordAlternatives extends WordAlternatives {
        private WordComposer word;

        public TypedWordAlternatives() {
            // Nothing
        }

        public TypedWordAlternatives(CharSequence chosenWord,
                WordComposer wordComposer) {
            super(chosenWord);
            word = wordComposer;
        }

        @Override
        public CharSequence getOriginalWord() {
            return word.getTypedWord();
        }

        @Override
        public List<CharSequence> getAlternatives() {
            return getTypedSuggestions(word);
        }
    }

    /* package */Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_SUGGESTIONS:
                updateSuggestions();
                break;
            case MSG_UPDATE_OLD_SUGGESTIONS:
                setOldSuggestions();
                break;
            case MSG_ABORT_RECORRECTION:
                abortCorrection(true);
                break;
            case MSG_UPDATE_SHIFT_STATE:
                updateShiftKeyState(getCurrentInputEditorInfo());
                break;
            }
        }
    };

    @Override
    public void onCreate() {
        PCKeyboardApp.updateLocale(this);
        Log.i("PCKeyboard", "onCreate(), os.version=" + System.getProperty("os.version"));
        KeyboardSwitcher.init(this);
        super.onCreate();
        sInstance = this;
        // setStatusIcon(R.drawable.ime_qwerty);
        mResources = getResources();
        final Configuration conf = mResources.getConfiguration();
        mOrientation = conf.orientation;
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        mLanguageSwitcher = new LanguageSwitcher(this);
        mLanguageSwitcher.loadLocales(prefs);
        mKeyboardSwitcher = KeyboardSwitcher.getInstance();
        mKeyboardSwitcher.setLanguageSwitcher(mLanguageSwitcher);
        Locale systemLocale = ConfigurationCompat.getLocales(conf).get(0);
        if (systemLocale == null) {
            systemLocale = Locale.getDefault();
        }
        mSystemLocale = systemLocale.toString();
        mLanguageSwitcher.setSystemLocale(systemLocale);
        String inputLanguage = mLanguageSwitcher.getInputLanguage();
        if (inputLanguage == null) {
            inputLanguage = systemLocale.toString();
        }
        Resources res = getResources();
        mReCorrectionEnabled = prefs.getBoolean(PREF_RECORRECTION_ENABLED,
                res.getBoolean(R.bool.default_recorrection_enabled));
        mConnectbotTabHack = prefs.getBoolean(PREF_CONNECTBOT_TAB_HACK,
                res.getBoolean(R.bool.default_connectbot_tab_hack));
        mFullscreenOverride = prefs.getBoolean(PREF_FULLSCREEN_OVERRIDE,
                res.getBoolean(R.bool.default_fullscreen_override));
        mForceKeyboardOn = prefs.getBoolean(PREF_FORCE_KEYBOARD_ON,
                res.getBoolean(R.bool.default_force_keyboard_on));
        mKeyboardNotification = prefs.getBoolean(PREF_KEYBOARD_NOTIFICATION,
                res.getBoolean(R.bool.default_keyboard_notification));
        mSuggestionsInLandscape = prefs.getBoolean(PREF_SUGGESTIONS_IN_LANDSCAPE,
                res.getBoolean(R.bool.default_suggestions_in_landscape));
        mMacrosInLandscape = prefs.getBoolean(PREF_MACROS_IN_LANDSCAPE, true);
        mHeightPortrait = getHeight(prefs, PREF_HEIGHT_PORTRAIT, res.getString(R.string.default_height_portrait));
        mHeightLandscape = getHeight(prefs, PREF_HEIGHT_LANDSCAPE, res.getString(R.string.default_height_landscape));
        getSettingsSafe().hintMode = Integer.parseInt(prefs.getString(PREF_HINT_MODE, res.getString(R.string.default_hint_mode)));
        getSettingsSafe().longpressTimeout = getPrefInt(prefs, PREF_LONGPRESS_TIMEOUT, res.getString(R.string.default_long_press_duration));
        getSettingsSafe().renderMode = getPrefInt(prefs, PREF_RENDER_MODE, res.getString(R.string.default_render_mode));
        mSwipeUpAction = prefs.getString(PREF_SWIPE_UP, res.getString(R.string.default_swipe_up));
        mSwipeDownAction = prefs.getString(PREF_SWIPE_DOWN, res.getString(R.string.default_swipe_down));
        mSwipeLeftAction = prefs.getString(PREF_SWIPE_LEFT, res.getString(R.string.default_swipe_left));
        mSwipeRightAction = prefs.getString(PREF_SWIPE_RIGHT, res.getString(R.string.default_swipe_right));
        mVolUpAction = prefs.getString(PREF_VOL_UP, res.getString(R.string.default_vol_up));
        mVolDownAction = prefs.getString(PREF_VOL_DOWN, res.getString(R.string.default_vol_down));
        sKeyboardSettings.initPrefs(prefs, res);

        //mVoiceRecognitionTrigger = new VoiceRecognitionTrigger(this);
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mSpeechRecognizer.setRecognitionListener(new VoiceRecognitionListener());
        }
        
        updateKeyboardOptions();

        PluginManager.getPluginDictionaries(getApplicationContext());
        mPluginManager = new PluginManager(this);
        final IntentFilter pFilter = new IntentFilter();
        pFilter.addDataScheme("package");
        pFilter.addAction("android.intent.action.PACKAGE_ADDED");
        pFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        pFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mPluginManager, pFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mPluginManager, pFilter);
        }

        LatinIMEUtil.GCUtils.getInstance().reset();
        boolean tryGC = true;
        for (int i = 0; i < LatinIMEUtil.GCUtils.GC_TRY_LOOP_MAX && tryGC; ++i) {
            try {
                initSuggest(inputLanguage);
                tryGC = false;
            } catch (OutOfMemoryError e) {
                tryGC = LatinIMEUtil.GCUtils.getInstance().tryGCOrWait(
                        inputLanguage, e);
            }
        }

        mOrientation = conf.orientation;

        // register to receive ringer mode changes for silent mode
        IntentFilter filter = new IntentFilter(
                AudioManager.RINGER_MODE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mReceiver, filter);
        }
        prefs.registerOnSharedPreferenceChangeListener(this);
        setNotification(mKeyboardNotification);
    }

    private int getKeyboardModeNum(int origMode, int override) {
        if (mNumKeyboardModes == 2 && origMode == 2) origMode = 1; // skip "compact". FIXME!
        int num = (origMode + override) % mNumKeyboardModes;
        if (mNumKeyboardModes == 2 && num == 1) num = 2; // skip "compact". FIXME!
        return num;
    }
    
    private void updateKeyboardOptions() {
        //Log.i(TAG, "setFullKeyboardOptions " + fullInPortrait + " " + heightPercentPortrait + " " + heightPercentLandscape);
        boolean isPortrait = isPortrait();
        int kbMode;
        mNumKeyboardModes = sKeyboardSettings.compactModeEnabled ? 3 : 2; // FIXME!
        if (isPortrait) {
            kbMode = getKeyboardModeNum(sKeyboardSettings.keyboardModePortrait, mKeyboardModeOverridePortrait);
        } else {
            kbMode = getKeyboardModeNum(sKeyboardSettings.keyboardModeLandscape, mKeyboardModeOverrideLandscape);
        }
        // Convert overall keyboard height to per-row percentage
        int screenHeightPercent = isPortrait ? mHeightPortrait : mHeightLandscape;
        getSettingsSafe().keyboardMode = kbMode;
        getSettingsSafe().keyboardHeightPercent = (float) screenHeightPercent;
    }

    private void setNotification(boolean visible) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (visible && mNotificationReceiver == null) {
            mNotificationReceiver = new NotificationReceiver(this);
            final IntentFilter pFilter = new IntentFilter(NotificationReceiver.ACTION_SHOW);
            pFilter.addAction(NotificationReceiver.ACTION_SETTINGS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mNotificationReceiver, pFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(mNotificationReceiver, pFilter);
            }

            int piFlags = PendingIntent.FLAG_IMMUTABLE;

            Intent notificationIntent = new Intent(NotificationReceiver.ACTION_SHOW);
            PendingIntent contentIntent = PendingIntent.getBroadcast(getApplicationContext(), 1,
                    notificationIntent, piFlags);

            Intent configIntent = new Intent(NotificationReceiver.ACTION_SETTINGS);
            PendingIntent configPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 2,
                    configIntent, piFlags);

            String title = "Show Hacked Keyboard GEM";
            String body = "Select this to open the keyboard. Disable in settings.";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                        "Hacked Keyboard GEM", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Keyboard status notification");
                mNotificationManager.createNotificationChannel(channel);
            }

            Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon_hk_notification)
                    .setTicker("Keyboard notification enabled.")
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(contentIntent)
                    .setOngoing(true)
                    .addAction(R.drawable.icon_hk_notification, getString(R.string.notification_action_settings),
                            configPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            mNotificationManager.notify(NOTIFICATION_ONGOING_ID, notification);

        } else if (mNotificationReceiver != null) {
            if (mNotificationManager != null) {
                mNotificationManager.cancel(NOTIFICATION_ONGOING_ID);
            }
            unregisterReceiver(mNotificationReceiver);
            mNotificationReceiver = null;
        }
    }
    
    private boolean isPortrait() {
        return (mOrientation == Configuration.ORIENTATION_PORTRAIT);
    }

    private boolean suggestionsDisabled() {
        if (mSuggestionForceOff) return true;
        if (mSuggestionForceOn) return false;
        return !(mSuggestionsInLandscape || isPortrait());
    }

    /**
     * Loads a dictionary or multiple separated dictionary
     *
     * @return returns array of dictionary resource ids
     */
    /* package */static int[] getDictionary(Resources res) {
        String packageName = LatinIME.class.getPackage().getName();
        XmlResourceParser xrp = res.getXml(R.xml.dictionary);
        ArrayList<Integer> dictionaries = new ArrayList<Integer>();

        try {
            int current = xrp.getEventType();
            while (current != XmlResourceParser.END_DOCUMENT) {
                if (current == XmlResourceParser.START_TAG) {
                    String tag = xrp.getName();
                    if (tag != null) {
                        if (tag.equals("part")) {
                            String dictFileName = xrp.getAttributeValue(null,
                                    "name");
                            dictionaries.add(res.getIdentifier(dictFileName,
                                    "raw", packageName));
                        }
                    }
                }
                xrp.next();
                current = xrp.getEventType();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Dictionary XML parsing failure");
        } catch (IOException e) {
            Log.e(TAG, "Dictionary XML IOException");
        }

        int count = dictionaries.size();
        int[] dict = new int[count];
        for (int i = 0; i < count; i++) {
            dict[i] = dictionaries.get(i);
        }

        return dict;
    }

    @SuppressWarnings("deprecation")
    private void initSuggest(String localeStr) {
        mInputLocale = localeStr;

        Resources res = getResources();
        Configuration conf = new Configuration(res.getConfiguration());
        
        Locale locale;
        if (localeStr.contains("_")) {
            String[] parts = localeStr.split("_", 3);
            if (parts.length == 3) {
                locale = new Locale(parts[0], parts[1], parts[2]);
            } else if (parts.length == 2) {
                locale = new Locale(parts[0], parts[1]);
            } else {
                locale = new Locale(parts[0]);
            }
        } else {
            locale = new Locale(localeStr);
        }
        
        conf.setLocale(locale);
        
        // CRITICAL: Update global resources configuration BEFORE creating dictionaries
        // so that openRawResourceFd finds the correct localized version.
        res.updateConfiguration(conf, res.getDisplayMetrics());

        // Use a localized context just for resource lookup to avoid deprecation
        Context localizedContext = createConfigurationContext(conf);
        Resources localizedRes = localizedContext.getResources();

        if (mSuggest != null) {
            mSuggest.close();
        }
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);

        int[] dictionaries = getDictionary(localizedRes);
        // CRITICAL: Pass 'this' (the Service context), NOT localizedContext, 
        // because external dictionary plugins need the main application context
        // to resolve their own resources/providers.
        mSuggest = new Suggest(this, dictionaries);
        
        Locale systemLocale = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        if (systemLocale == null) {
            systemLocale = Locale.getDefault();
        }
        updateAutoTextEnabled(systemLocale);

        if (mUserDictionary != null)
            mUserDictionary.close();
        mUserDictionary = new UserDictionary(this, mInputLocale);

        if (mAutoDictionary != null) {
            mAutoDictionary.close();
        }
        mAutoDictionary = new AutoDictionary(this, this, mInputLocale,
                Suggest.DIC_AUTO);
        if (mUserBigramDictionary != null) {
            mUserBigramDictionary.close();
        }
        mUserBigramDictionary = new UserBigramDictionary(this, this,
                mInputLocale, Suggest.DIC_USER);
        mSuggest.setUserBigramDictionary(mUserBigramDictionary);
        mSuggest.setUserDictionary(mUserDictionary);

        mSuggest.setAutoDictionary(mAutoDictionary);
        updateCorrectionMode();
        mWordSeparators = res.getString(R.string.word_separators);
        mSentenceSeparators = res
                .getString(R.string.sentence_separators);
        initSuggestPuncList();
    }

    @Override
    public void onDestroy() {
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
        if (mUserDictionary != null) {
            mUserDictionary.close();
            mUserDictionary = null;
        }
        if (mAutoDictionary != null) {
            mAutoDictionary.close();
            mAutoDictionary = null;
        }
        if (mUserBigramDictionary != null) {
            mUserBigramDictionary.close();
            mUserBigramDictionary = null;
        }
        if (mSuggest != null) {
            mSuggest.close();
            mSuggest = null;
        }
        unregisterReceiver(mReceiver);
        unregisterReceiver(mPluginManager);
        if (mNotificationReceiver != null) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager != null) {
                mNotificationManager.cancel(NOTIFICATION_ONGOING_ID);
            }
            unregisterReceiver(mNotificationReceiver);
            mNotificationReceiver = null;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        if (mKeyboardSwitcher != null) {
            mKeyboardSwitcher.onDestroy();
        }
        sInstance = null;
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration conf) {
        Log.i("PCKeyboard", "onConfigurationChanged()");
        // If the system locale changes and is different from the saved
        // locale (mSystemLocale), then reload the input locale list from the
        // latin ime settings (shared prefs) and reset the input locale
        // to the first one.
        final Locale locale = ConfigurationCompat.getLocales(conf).get(0);
        final String systemLocaleStr = locale != null ? locale.toString() : Locale.getDefault().toString();
        if (!TextUtils.equals(systemLocaleStr, mSystemLocale)) {
            mSystemLocale = systemLocaleStr;
            if (mLanguageSwitcher != null) {
                mLanguageSwitcher.loadLocales(PreferenceManager
                        .getDefaultSharedPreferences(this));
                mLanguageSwitcher.setSystemLocale(locale);
                toggleLanguage(true, true);
            } else {
                reloadKeyboards();
            }
        }
        // If orientation changed while predicting, commit the change
        if (conf.orientation != mOrientation) {
            InputConnection ic = getCurrentInputConnection();
            commitTyped(ic, true);
            if (ic != null)
                ic.finishComposingText(); // For voice input
            mOrientation = conf.orientation;
            
            // Force re-inflation of candidate views on orientation change to update resources/dimensions
            mCandidateViewContainer = null;
            mCandidateView = null;
            mMacroBar = null;
            setCandidatesViewShown(isCandidateStripVisible());
            
            reloadKeyboards();
        }
        mConfigurationChanging = true;
        super.onConfigurationChanged(conf);
        mConfigurationChanging = false;
    }

    @Override
    public View onCreateInputView() {
        if (mKeyboardSwitcher.getInputView() == null) {
            mKeyboardSwitcher.recreateInputView();
            mKeyboardSwitcher.makeKeyboards(true);
            mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT, 0,
                    shouldShowVoiceButton(getCurrentInputEditorInfo()));
        }
        return mKeyboardSwitcher.getInputView();
    }

    private void initCandidateViewContainer() {
        if (mCandidateViewContainer == null) {
            mCandidateViewContainer = (LinearLayout) getLayoutInflater().inflate(
                    R.layout.candidates, null);
            mCandidateView = (CandidateView) mCandidateViewContainer
                    .findViewById(R.id.candidates);
            mCandidateView.setPadding(0, 0, 0, 0);
            mCandidateView.setService(this);

            View macroToggle = mCandidateViewContainer.findViewById(R.id.macro_toggle);
            if (macroToggle != null) {
                macroToggle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleMacroBar();
                    }
                });
            }
        }

        if (mCandidateViewContainer != null && mCandidateViewContainer.findViewById(R.id.macro_bar_container) == null) {
            mMacroBar = getLayoutInflater().inflate(R.layout.macro_bar, null);
            mMacroBar.setVisibility(View.GONE);
            mCandidateViewContainer.addView(mMacroBar, 0); 
            setupMacroButtons();
        }
    }

    @Override
    public View onCreateCandidatesView() {
        initCandidateViewContainer();
        return mCandidateViewContainer;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AbstractInputMethodImpl onCreateInputMethodInterface() {
    	return new MyInputMethodImpl();
    }
    
    IBinder mToken;
    public class MyInputMethodImpl extends InputMethodImpl {
    	@Override
    	public void attachToken(IBinder token) {
    		super.attachToken(token);
    		Log.i(TAG, "attachToken " + token);
    		if (mToken == null) {
    			mToken = token;
    		}
    	}
    }

    private void toggleMacroBar() {
        if (mCandidateViewContainer == null) {
            initCandidateViewContainer();
        }
        if (mMacroBar == null) {
             mMacroBar = mCandidateViewContainer.findViewById(R.id.macro_bar_container);
        }
        if (mMacroBar == null) return;

        boolean currentlyVisible = mMacroBar.getVisibility() == View.VISIBLE;
        if (currentlyVisible) {
            mMacroBar.setVisibility(View.GONE);
            mSuggestionForceOn = false;
        } else {
            setupMacroButtons();
            mMacroBar.setVisibility(View.VISIBLE);
            // mMacroBar.setBackgroundColor(0xFFFF0000); // RED FOR TESTING (Commented out)
            
            // Adjust this number to change the height of the M1-M5 bar
            int barHeight = (int) (30 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mMacroBar.getLayoutParams();
            if (lp != null) {
                lp.height = barHeight;
                mMacroBar.setLayoutParams(lp);
            }
            mSuggestionForceOn = true;
        }
        
        // Toggle the macro bar visibility, considering landscape constraints
        boolean canShowMacros = isPortrait() || mMacrosInLandscape;
        if (!canShowMacros && !currentlyVisible) {
             // If we tried to enable it but it's restricted in landscape
             mMacroBar.setVisibility(View.GONE);
             mSuggestionForceOn = false;
        }

        // Avoid full bar restart if possible
        setCandidatesViewShown(isCandidateStripVisible());

        if (mCandidateViewContainer != null) {
            mCandidateViewContainer.requestLayout();
        }
    }

    private void setupMacroButtons() {
        if (mCandidateViewContainer == null) return;
        if (mMacroBar == null) {
            mMacroBar = mCandidateViewContainer.findViewById(R.id.macro_bar_container);
        }
        if (mMacroBar == null) return;

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        for (int i = 1; i <= 5; i++) {
            final int groupIndex = i;
            int resId = getResources().getIdentifier("macro_tile_" + i, "id", getPackageName());
            View button = mMacroBar.findViewById(resId);
            if (button instanceof Button) {
                Button macroBtn = (Button) button;
                String label = sp.getString("macro_label_" + groupIndex, "M" + groupIndex);
                macroBtn.setText(label);
                
                // FORCE button style updates in code - aggressive reset
                macroBtn.setBackgroundResource(0); // Remove background to kill insets
                macroBtn.setPadding(0, 0, 0, 0);
                macroBtn.setMinHeight(0);
                macroBtn.setMinimumHeight(0);
                macroBtn.setMinWidth(0);
                macroBtn.setMinimumWidth(0);
                
                // Re-add a simple selector background
                macroBtn.setBackgroundResource(android.R.drawable.list_selector_background);

                // Adjust font size to match suggestion bar style
                macroBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * sKeyboardSettings.candidateScalePref);
                macroBtn.setTypeface(android.graphics.Typeface.DEFAULT);
                macroBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMacroMenu(v, groupIndex);
                    }
                });
            }
        }
    }

    private void showMacroMenu(View v, int groupIndex) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int startIdx = (groupIndex - 1) * 5 + 1;

        // Use PopupWindow with focusable=false to avoid stealing focus from keyboard (prevents flickering)
        LinearLayout layout = new LinearLayout(new ContextThemeWrapper(this, R.style.MacroPopupTheme));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF111111); // Dark background
        layout.setPadding(4, 4, 4, 4);

        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                layout,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                false); // Focusable = false, critical to avoid IME reload

        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);

        for (int i = 0; i < 5; i++) {
            final int macroIdx = startIdx + i;
            String content = sp.getString("macro_content_" + macroIdx, "");
            if (content.isEmpty()) continue; // Skip empty macros
            String menuTitle = content.length() > 30 ? content.substring(0, 27) + "..." : content;

            Button btn = new Button(new ContextThemeWrapper(this, R.style.MacroPopupTheme));
            btn.setText(menuTitle);
            btn.setAllCaps(false);
            // Macro Menu - Adjust font size to match suggestion bar style
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * sKeyboardSettings.candidateScalePref);
            btn.setTypeface(android.graphics.Typeface.DEFAULT);
            // Example: Set a specific height for every row in the popup
            //btn.setMinHeight((int) (28 * getResources().getDisplayMetrics().density));
            btn.setPadding(20, 8, 20, 8);
            btn.setMinHeight(0);
            btn.setMinimumHeight(0);
            
            // Set minimum width to approx 5 characters
            int minWidth = (int) (btn.getPaint().measureText("00000") + 40);
            btn.setMinWidth(minWidth);
            
            // Force the layout params to WRAP_CONTENT for height
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            btn.setLayoutParams(lp);

            btn.setBackgroundResource(android.R.drawable.list_selector_background);
            btn.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    executeMacro(macroIdx);
                    popupWindow.dismiss();
                }
            });
            layout.addView(btn);
        }

        // Measure size to know how much to offset upwards
        layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int height = layout.getMeasuredHeight();

        // Show ABOVE the button (anchor v)
        popupWindow.showAsDropDown(v, 0, -(height + v.getHeight()));
    }

    private void executeMacro(int index) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String content = sp.getString("macro_content_" + index, "");
        boolean isControl = sp.getBoolean("macro_control_" + index, false);

        if (content.isEmpty()) return;

        if (isControl) {
            sendMacroSequence(content);
        } else {
            android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                commitTyped(ic, true);
                ic.commitText(content, 1);
            }
        }
    }

    private void sendMacroSequence(String sequence) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        // Simple parser for {TAG}
        String[] parts = sequence.split("(?=\\{)|(?<=\\})");
        for (String part : parts) {
            if (part.startsWith("{") && part.endsWith("}")) {
                String tag = part.substring(1, part.length() - 1).toUpperCase();
                switch (tag) {
                    case "ENTER": sendKeyChar('\n'); break;
                    case "TAB": sendDownUpKeyEvents(KeyEvent.KEYCODE_TAB); break;
                    case "ESC": sendDownUpKeyEvents(KeyEvent.KEYCODE_ESCAPE); break;
                    case "CTRL+C":
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_C));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                        break;
                    case "CTRL+V":
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_V));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_V));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                        break;
                    case "CTRL+X":
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_X));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                        break;
                    case "CTRL+A":
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                        break;
                }
            } else {
                ic.commitText(part, 1);
            }
        }
    }

    @Override
    public void sendDownUpKeyEvents(int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }

    private void removeCandidateViewContainer() {
        mCandidateViewContainer = null;
        mCandidateView = null;
        mMacroBar = null;
        resetPrediction();
    }

    private void resetPrediction() {
        mComposing.setLength(0);
        mPredicting = false;
        mDeleteCount = 0;
        mJustAddedAutoSpace = false;
    }
    
    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        sKeyboardSettings.editorPackageName = attribute.packageName;
        sKeyboardSettings.editorFieldName = attribute.fieldName;
        sKeyboardSettings.editorFieldId = attribute.fieldId;
        sKeyboardSettings.editorInputType = attribute.inputType;

        //Log.i("PCKeyboard", "onStartInputView " + attribute + ", inputType= " + Integer.toHexString(attribute.inputType) + ", restarting=" + restarting);
        LatinKeyboardView inputView = mKeyboardSwitcher.getInputView();
        // In landscape mode, this method gets called without the input view
        // being created.
        if (inputView == null) {
            return;
        }

        if (mRefreshKeyboardRequired) {
            mRefreshKeyboardRequired = false;
            toggleLanguage(true, true);
        }

        mKeyboardSwitcher.makeKeyboards(false);

        TextEntryState.newSession(this);

        // Most such things we decide below in the switch statement, but we need to know
        // now whether this is a password text field, because we need to know now (before
        // the switch statement) whether we want to enable the voice button.
        mPasswordText = false;
        int variation = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;
        if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                || variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == 0xe0 /* EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD */
        ) {
            if ((attribute.inputType & EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT) {
                mPasswordText = true;
            }
        }

        mEnableVoiceButton = shouldShowVoiceButton(attribute);
        final boolean enableVoiceButton = mEnableVoiceButton && mEnableVoice;

        // Dismiss "Touch again to save" hint if user touches text / starts new input
        if (mCandidateView != null && mCandidateView.isShowingAddToDictionaryHint()) {
            mCandidateView.dismissAddToDictionaryHint();
            setCandidatesViewShown(isCandidateStripVisible());
            postUpdateSuggestions();
        }

        mInputTypeNoAutoCorrect = false;
        mPredictionOnForMode = false;
        mCompletionOn = false;
        mCompletions = null;
        mModCtrl = false;
        mModAlt = false;
        mModMeta = false;
        mModFn = false;
        mEnteredText = null;
        mSuggestionForceOn = false;
        mSuggestionForceOff = false;
        mKeyboardModeOverridePortrait = 0;
        mKeyboardModeOverrideLandscape = 0;
        // sKeyboardSettings.useExtension = false; // Removed to prevent state loss

        loadSettings(); // Load settings before applying logic based on them

        switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
        case EditorInfo.TYPE_CLASS_NUMBER:
        case EditorInfo.TYPE_CLASS_DATETIME:
            // fall through
            // NOTE: For now, we use the phone keyboard for NUMBER and DATETIME
            // until we get
            // a dedicated number entry keypad.
            // TODO: Use a dedicated number entry keypad here when we get one.
        case EditorInfo.TYPE_CLASS_PHONE:
            mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_PHONE,
                    attribute.imeOptions, enableVoiceButton);
            break;
        case EditorInfo.TYPE_CLASS_TEXT:
            mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT,
                    attribute.imeOptions, enableVoiceButton);
            mPredictionOnForMode = true;

            if (mPasswordText) {
                mPredictionOnForMode = false;
            }

            boolean isEmail = variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    || variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS;
            boolean isUri = variation == EditorInfo.TYPE_TEXT_VARIATION_URI;
            boolean isFilter = variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER;
            boolean isWeb = variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT;

            if (isEmail || variation == EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME
                    || !mLanguageSwitcher.allowAutoSpace()) {
                mAutoSpace = false;
            } else {
                mAutoSpace = true;
            }

            if (isEmail) {
                mPredictionOnForMode = false;
                mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_EMAIL,
                        attribute.imeOptions, enableVoiceButton);
            } else if (isUri) {
                mPredictionOnForMode = false;
                mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_URL,
                        attribute.imeOptions, enableVoiceButton);
            } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
                mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_IM,
                        attribute.imeOptions, enableVoiceButton);
            } else if (isFilter) {
                mPredictionOnForMode = false;
            } else if (isWeb) {
                mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_WEB,
                        attribute.imeOptions, enableVoiceButton);
            }

            // Apply Auto-correction mode logic
            int variationMasked = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;
            if (mAutoCorrectionMode == 0) { // Off
                mInputTypeNoAutoCorrect = true;
            } else if (mAutoCorrectionMode == 1) { // Balanced
                // Disable in Web, Filter, Email, URI, or if it's a password
                if (isWeb || isFilter || isEmail || isUri || mPasswordText) {
                    mInputTypeNoAutoCorrect = true;
                } else if (variationMasked == EditorInfo.TYPE_TEXT_VARIATION_PHONETIC) {
                    mInputTypeNoAutoCorrect = true;
                } else if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT) == 0
                        && (attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) == 0) {
                    // Respect system "no autocorrect" for non-multiline fields in Balanced mode
                    mInputTypeNoAutoCorrect = true;
                }
            } else if (mAutoCorrectionMode == 2) { // Full
                // Only disable for strictly sensitive fields (Passwords, Emails, URIs)
                if (mPasswordText || isEmail || isUri) {
                    mInputTypeNoAutoCorrect = true;
                }
            }

            // If NO_SUGGESTIONS is set, don't do prediction.
            if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
                mPredictionOnForMode = false;
                mInputTypeNoAutoCorrect = true;
            }

            if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                mPredictionOnForMode = false;
                mCompletionOn = isFullscreenMode();
            }
            break;
        default:
            mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT,
                    attribute.imeOptions, enableVoiceButton);
        }
        // if (!restarting) {
        //    inputView.closing();
        // }
        resetPrediction();
        mAutoCapActive = (mAutoCapPref != 2) && mLanguageSwitcher.allowAutoCap();
        updateShiftKeyState(attribute);

        mPredictionOnPref = (mCorrectionMode > 0 || mShowSuggestions);
        setCandidatesViewShownInternal(isCandidateStripVisible()
                || mCompletionOn, false /* needsInputViewShown */);
        updateSuggestions();

        // If the dictionary is not big enough, don't auto correct
        mHasDictionary = mSuggest.hasMainDictionary();

        updateCorrectionMode();

        inputView.setPreviewEnabled(mPopupOn);
        inputView.setProximityCorrectionEnabled(true);
        // If we just entered a text field, maybe it has some old text that
        // requires correction
        checkReCorrectionOnStart();
    }

    private boolean shouldShowVoiceButton(EditorInfo attribute) {
        // TODO Auto-generated method stub
        return true;
    }

    private void checkReCorrectionOnStart() {
        if (mReCorrectionEnabled && isPredictionOn()) {
            // First get the cursor position. This is required by
            // setOldSuggestions(), so that
            // it can pass the correct range to setComposingRegion(). At this
            // point, we don't
            // have valid values for mLastSelectionStart/Stop because
            // onUpdateSelection() has
            // not been called yet.
            InputConnection ic = getCurrentInputConnection();
            if (ic == null)
                return;
            ExtractedTextRequest etr = new ExtractedTextRequest();
            etr.token = 0; // anything is fine here
            ExtractedText et = ic.getExtractedText(etr, 0);
            if (et == null)
                return;

            mLastSelectionStart = et.startOffset + et.selectionStart;
            mLastSelectionEnd = et.startOffset + et.selectionEnd;

            // Then look for possible corrections in a delayed fashion
            if (!TextUtils.isEmpty(et.text) && isCursorInsideWord()) {
                postUpdateOldSuggestions();
            }
        }
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();

        onAutoCompletionStateChanged(false);

        if (mKeyboardSwitcher.getInputView() != null) {
            mKeyboardSwitcher.getInputView().closing();
        }
        if (mAutoDictionary != null)
            mAutoDictionary.flushPendingWrites();
        if (mUserBigramDictionary != null)
            mUserBigramDictionary.flushPendingWrites();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        // Remove penging messages related to update suggestions
        mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
        mHandler.removeMessages(MSG_UPDATE_OLD_SUGGESTIONS);
    }

    @Override
    public void onUpdateExtractedText(int token, ExtractedText text) {
        super.onUpdateExtractedText(token, text);
        InputConnection ic = getCurrentInputConnection();
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd, int candidatesStart,
            int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        // added to use for Dismiss "Touch again to save" hint if user touches text
        if (mIgnoreNextUpdateSelection) {
            mIgnoreNextUpdateSelection = false;
            return;
        }

        // Dismiss "Touch again to save" hint if user touches text
        if (mCandidateView != null && mCandidateView.isShowingAddToDictionaryHint()) {
            mCandidateView.dismissAddToDictionaryHint();
            mPredicting = false;
            setNextSuggestions();
            // Force a refresh
            setCandidatesViewShown(isCandidateStripVisible());
            // Notify UI
            Log.i(TAG, "Hint dismissed via onUpdateSelection");
        }

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if ((((mComposing.length() > 0 && mPredicting))
                && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd) && mLastSelectionStart != newSelStart)) {
            mComposing.setLength(0);
            mPredicting = false;
            postUpdateSuggestions();
            TextEntryState.reset();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        } else if (!mPredicting && !mJustAccepted) {
            switch (TextEntryState.getState()) {
            case ACCEPTED_DEFAULT:
                TextEntryState.reset();
                // fall through
            case SPACE_AFTER_PICKED:
                mJustAddedAutoSpace = false; // The user moved the cursor.
                break;
            }
        }
        mJustAccepted = false;
        postUpdateShiftKeyState();

        // Make a note of the cursor position
        mLastSelectionStart = newSelStart;
        mLastSelectionEnd = newSelEnd;

        if (mReCorrectionEnabled && mShowSuggestions) {
            // Don't look for corrections if the keyboard is not visible
            if (mKeyboardSwitcher != null
                    && mKeyboardSwitcher.getInputView() != null
                    && mKeyboardSwitcher.getInputView().isShown()) {
                // Check if we should go in or out of correction mode.
                // GEM FIX: Stop the feedback loop by only triggering on actual user cursor movement
                if (isPredictionOn()
                        && mJustRevertedSeparator == null
                        && (newSelStart != oldSelStart || newSelEnd != oldSelEnd || candidatesStart == candidatesEnd)
                        && (newSelStart < newSelEnd - 1 || (!mPredicting))) {
                    if (isCursorInsideWord()
                            || (newSelStart < newSelEnd)) {
                        postUpdateOldSuggestions();
                    } else {
                        abortCorrection(false);
                        postUpdateSuggestions();
                    }
                }
            }
        }
    }

    /**
     * This is called when the user has clicked on the extracted text view, when
     * running in fullscreen mode. The default implementation hides the
     * candidates view when this happens, but only if the extracted text editor
     * has a vertical scroll bar because its text doesn't fit. Here we override
     * the behavior due to the possibility that a re-correction could cause the
     * candidate strip to disappear and re-appear.
     */
    @Override
    public void onExtractedTextClicked() {
        if (mReCorrectionEnabled && isPredictionOn())
            return;

        super.onExtractedTextClicked();
    }

    /**
     * This is called when the user has performed a cursor movement in the
     * extracted text view, when it is running in fullscreen mode. The default
     * implementation hides the candidates view when a vertical movement
     * happens, but only if the extracted text editor has a vertical scroll bar
     * because its text doesn't fit. Here we override the behavior due to the
     * possibility that a re-correction could cause the candidate strip to
     * disappear and re-appear.
     */
    @Override
    public void onExtractedCursorMovement(int dx, int dy) {
        if (mReCorrectionEnabled && isPredictionOn())
            return;

        super.onExtractedCursorMovement(dx, dy);
    }

    @Override
    public void hideWindow() {
        onAutoCompletionStateChanged(false);

        if (mOptionsDialog != null && mOptionsDialog.isShowing()) {
            mOptionsDialog.dismiss();
            mOptionsDialog = null;
        }
        mWordToSuggestions.clear();
        mWordHistory.clear();
        super.hideWindow();
        TextEntryState.endSession();
    }

    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                clearSuggestions();
                return;
            }

            List<CharSequence> stringList = new ArrayList<CharSequence>();
            for (int i = 0; i < (completions != null ? completions.length : 0); i++) {
                CompletionInfo ci = completions[i];
                if (ci != null)
                    stringList.add(ci.getText());
            }
            // When in fullscreen mode, show completions generated by the
            // application
            setSuggestions(stringList, true, true, true);
            mBestWord = null;
            setCandidatesViewShown(true);
        }
    }

    private void setCandidatesViewShownInternal(boolean shown,
            boolean needsInputViewShown) {
        // The bar is visible if the system wants it OR if we have word suggestions enabled in settings.
        // We use isCandidateStripVisible() to ensure the macro toggle is available even when suggestions are hidden.
        boolean visible = (shown || isCandidateStripVisible())
        && onEvaluateInputViewShown()
        && mKeyboardSwitcher.getInputView() != null
        && (needsInputViewShown
                ? mKeyboardSwitcher.getInputView().isShown()
                        : true);
        if (visible) {
            if (mCandidateViewContainer == null) {
                initCandidateViewContainer();
                setNextSuggestions();
            }
        } else {
            if (mCandidateViewContainer != null) {
                removeCandidateViewContainer();
                commitTyped(getCurrentInputConnection(), true);
            }
        }
        // Call our overridden method to handle view visibility within InputView
        setCandidatesViewShown(visible);
    }

    @Override
    public void onFinishCandidatesView(boolean finishingInput) {
        //Log.i(TAG, "onFinishCandidatesView(), mCandidateViewContainer=" + mCandidateViewContainer);
        super.onFinishCandidatesView(finishingInput);
        // Removed removeCandidateViewContainer() because we don't want to destroy the view when changing fields
        if (mCandidateViewContainer != null) {
            mCandidateViewContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onEvaluateInputViewShown() {
    	boolean parent = super.onEvaluateInputViewShown();
    	boolean wanted = mForceKeyboardOn || parent;
    	//Log.i(TAG, "OnEvaluateInputViewShown, parent=" + parent + " + " wanted=" + wanted);
    	return wanted;
    }
    
    @Override
    public void setCandidatesViewShown(boolean shown) {
        // Log.i(TAG, "setCandidatesViewShown(" + shown + ")");
        boolean macroVisible = (mMacroBar != null && mMacroBar.getVisibility() == View.VISIBLE) && (isPortrait() || mMacrosInLandscape);
        boolean suggestionAllowedInMode = (isPortrait() || mSuggestionsInLandscape);
        boolean suggestionVisible = shown && suggestionAllowedInMode;
        
        // The toggle icon (macro_toggle) should be visible if suggestions OR macros are allowed in this mode
        boolean toggleAllowed = (isPortrait() || mSuggestionsInLandscape || mMacrosInLandscape);
        
        // The whole container (CandidateViewContainer) must be visible if:
        // 1. We want to show suggestions (and they are allowed for this mode)
        // 2. Macros are open (and they are allowed for this mode)
        // 3. We want to show the macro toggle (toggleAllowed) - but only if suggestions are also allowed
        // 4. Suggestions are forced by the UI (mSuggestionForceOn)
        
        // REFINED LOGIC: If suggestions are disabled in landscape, but macros are enabled,
        // we should show the macro bar directly if it's active, OR show the toggle if it's not.
        // HOWEVER, the user wants the macro bar ALWAYS visible if suggestions are off.
        
        boolean macrosAllowedInMode = isPortrait() || mMacrosInLandscape;
        
        // If macros are allowed and suggestions are NOT, force the macro bar to be open
        if (!suggestionAllowedInMode && macrosAllowedInMode) {
            macroVisible = true;
            if (mMacroBar != null) {
                setupMacroButtons();
                mMacroBar.setVisibility(View.VISIBLE);
                // mMacroBar.setBackgroundColor(0xFFFF0000); // RED FOR TESTING (Commented out)
                
                // Adjust this number to change the height of the M1-M5 bar (Landscape)
                int barHeight = (int) (32 * getResources().getDisplayMetrics().density);
                ViewGroup.LayoutParams lp = mMacroBar.getLayoutParams();
                if (lp != null) {
                    lp.height = barHeight;
                    mMacroBar.setLayoutParams(lp);
                }
            }
        }

        boolean forceVisible = suggestionVisible || mSuggestionForceOn || (macroVisible && macrosAllowedInMode) || (toggleAllowed && suggestionAllowedInMode);
        
        super.setCandidatesViewShown(forceVisible);
        
        if (mCandidateViewContainer != null) {
            mCandidateViewContainer.setVisibility(forceVisible ? View.VISIBLE : View.GONE);
            
            View strip = mCandidateViewContainer.findViewById(R.id.suggestion_strip_container);
            if (strip != null) {
                // 1. Determine if words should be VISIBLE
                boolean wordsVisible = suggestionAllowedInMode && (shown || mSuggestionForceOn);
                
                // Set word view visibility
                if (mCandidateView != null) {
                    mCandidateView.setVisibility(wordsVisible ? View.VISIBLE : View.GONE);
                }
                
                // 2. Determine if the macro toggle is needed
                // It's needed only if both suggestions and macros are allowed, so we can switch.
                // If only macros are allowed, we don't need the toggle (pencil), just the macro bar.
                boolean toggleNeeded = suggestionAllowedInMode && macrosAllowedInMode;
                View toggle = strip.findViewById(R.id.macro_toggle);
                if (toggle != null) {
                    toggle.setVisibility(toggleNeeded ? View.VISIBLE : View.GONE);
                }

                // The entire strip container (containing words and toggle) should be GONE if:
                // Words are not visible AND the toggle is not visible
                boolean stripVisible = wordsVisible || toggleNeeded;
                
                strip.setVisibility(stripVisible ? View.VISIBLE : View.GONE);
            }

            if (mMacroBar != null) {
                mMacroBar.setVisibility((macroVisible && macrosAllowedInMode) ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void onComputeInsets(InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (!isFullscreenMode()) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float displayHeight = dm.heightPixels;
        // If the display is more than X inches high, don't go to fullscreen
        // mode
        float dimen = getResources().getDimension(
                R.dimen.max_height_for_fullscreen);
        if (displayHeight > dimen || mFullscreenOverride || isConnectbot()) {
            return false;
        } else {
            return super.onEvaluateFullscreenMode();
        }
    }

    public boolean isKeyboardVisible() {
        return (mKeyboardSwitcher != null
                && mKeyboardSwitcher.getInputView() != null
                && mKeyboardSwitcher.getInputView().isShown());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            if (event.getRepeatCount() == 0
                    && mKeyboardSwitcher.getInputView() != null) {
                if (mKeyboardSwitcher.getInputView().handleBack()) {
                    return true;
                }
            }
            break;
        case KeyEvent.KEYCODE_VOLUME_UP:
            if (!mVolUpAction.equals("none") && isKeyboardVisible()) {
                return true;
            }
            break;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (!mVolDownAction.equals("none") && isKeyboardVisible()) {
                return true;
            }
            break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            LatinKeyboardView inputView = mKeyboardSwitcher.getInputView();
            // Enable shift key and DPAD to do selections
            if (inputView != null && inputView.isShown()
                    && inputView.getShiftState() == Keyboard.SHIFT_ON) {
                event = new KeyEvent(event.getDownTime(), event.getEventTime(),
                        event.getAction(), event.getKeyCode(), event
                                .getRepeatCount(), event.getDeviceId(), event
                                .getScanCode(), KeyEvent.META_SHIFT_LEFT_ON
                                | KeyEvent.META_SHIFT_ON);
                InputConnection ic = getCurrentInputConnection();
                if (ic != null)
                    ic.sendKeyEvent(event);
                return true;
            }
            break;
        case KeyEvent.KEYCODE_VOLUME_UP:
            if (!mVolUpAction.equals("none") && isKeyboardVisible()) {
                return doSwipeAction(mVolUpAction);
            }
            break;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (!mVolDownAction.equals("none") && isKeyboardVisible()) {
                return doSwipeAction(mVolDownAction);
            }
            break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void reloadKeyboards() {
        mKeyboardSwitcher.setLanguageSwitcher(mLanguageSwitcher);
        if (mKeyboardSwitcher.getInputView() != null
                && mKeyboardSwitcher.getKeyboardMode() != KeyboardSwitcher.MODE_NONE) {
            mKeyboardSwitcher.setVoiceMode(mEnableVoice && mEnableVoiceButton,
                    mVoiceOnPrimary);
        }
        updateKeyboardOptions();
        mKeyboardSwitcher.makeKeyboards(true);
    }

    private void commitTyped(InputConnection inputConnection, boolean manual) {
        if (mPredicting) {
            mPredicting = false;
            if (mComposing.length() > 0) {
                if (inputConnection != null) {
                    inputConnection.commitText(mComposing, 1);
                }
                mCommittedLength = mComposing.length();
                if (manual) {
                    TextEntryState.manualTyped(mComposing);
                } else {
                    TextEntryState.acceptedTyped(mComposing);
                }
                addToDictionaries(mComposing,
                        AutoDictionary.FREQUENCY_FOR_TYPED);
            }
            updateSuggestions();
        }
    }

    private void postUpdateShiftKeyState() {
        mHandler.removeMessages(MSG_UPDATE_SHIFT_STATE);
        // Klaus: Delay is necessary because the text buffer is not always updated immediately
        // after a key press. 50ms is enough for most cases.
        mHandler.sendMessageDelayed(mHandler
                .obtainMessage(MSG_UPDATE_SHIFT_STATE), 50);
    }

    public void updateShiftKeyState(EditorInfo attr) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && attr != null && mKeyboardSwitcher.isAlphabetMode()) {
            int oldState = getShiftState();
            
            // If user manually locked shift/caps, don't auto-change it
            if (oldState == Keyboard.SHIFT_CAPS_LOCKED || oldState == Keyboard.SHIFT_LOCKED) {
                return;
            }

            if (mShiftKeyState.isChording()) {
                return;
            }

            int caps = getCursorCapsMode(ic, attr);
            int newState = Keyboard.SHIFT_OFF;
            boolean isAutoCapRecommended = false;
            if ((caps & TextUtils.CAP_MODE_CHARACTERS) != 0) {
                newState = getCapsOrShiftLockState();
            } else if (caps != 0) {
                newState = Keyboard.SHIFT_ON;
                isAutoCapRecommended = true; //Auto-capitalization - turn off by Shift if no need
            }
            
            // Apply manual override if active - Auto-capitalization - turn off by Shift if no need
            if (mShiftManualOverride && isAutoCapRecommended) {
                newState = Keyboard.SHIFT_OFF;
            }

            if (oldState != newState) {
                mKeyboardSwitcher.setShiftState(newState);
            }
            
            // Update auto-shift tracking - Auto-capitalization - turn off by Shift if no need
            mIsAutoShift = isAutoCapRecommended && (newState == Keyboard.SHIFT_ON);
        }
        if (ic != null) {
            // Clear modifiers other than shift, to avoid them getting stuck
            int states = 
                KeyEvent.META_FUNCTION_ON
                | KeyEvent.META_ALT_MASK
                | KeyEvent.META_CTRL_MASK
                | KeyEvent.META_META_MASK
                | KeyEvent.META_SYM_ON;
            ic.clearMetaKeyStates(states);
        }
    }

    private int getShiftState() {
        if (mKeyboardSwitcher != null) {
            LatinKeyboardView view = mKeyboardSwitcher.getInputView();
            if (view != null) {
                return view.getShiftState();
            }
        }
        return Keyboard.SHIFT_OFF;
    }

    private boolean isShiftCapsMode() {
        if (mKeyboardSwitcher != null) {
            LatinKeyboardView view = mKeyboardSwitcher.getInputView();
            if (view != null) {
                return view.isShiftCaps();
            }
        }
        return false;
    }

    private int getCursorCapsMode(InputConnection ic, EditorInfo attr) {
        int caps = 0;
        if (mAutoCapActive && attr != null && attr.inputType != EditorInfo.TYPE_NULL) {
            int mode = mAutoCapPref;
            int variation = attr.inputType & EditorInfo.TYPE_MASK_VARIATION;
            int clazz = attr.inputType & EditorInfo.TYPE_MASK_CLASS;

            if (clazz != EditorInfo.TYPE_CLASS_TEXT) {
                return 0;
            }

            // Standard exclusions for auto-cap
            if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                    || variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                    || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER
                    || variation == 0xd0 /* TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS */
                    || variation == 0xe0 /* TYPE_TEXT_VARIATION_WEB_PASSWORD */) {
                return 0;
            }

            // Determine what modes to check for.
            int mask = attr.inputType & (TextUtils.CAP_MODE_CHARACTERS 
                    | TextUtils.CAP_MODE_WORDS 
                    | TextUtils.CAP_MODE_SENTENCES);
            
            // "Smart always" (1) logic:
            // If the app explicitly requested words or chars, use that.
            // Otherwise, ALWAYS add sentences.
            if (mode == 1) {
                if ((mask & (TextUtils.CAP_MODE_WORDS | TextUtils.CAP_MODE_CHARACTERS)) == 0) {
                    mask |= TextUtils.CAP_MODE_SENTENCES;
                }
            } else if (mask == 0) {
                // "Default" (0) logic: only add sentences for known normal text variations
                mask |= TextUtils.CAP_MODE_SENTENCES;
            }
            
            if (mask != 0) {
                caps = ic.getCursorCapsMode(mask);
            }
        }
        return caps;
    }


    private void updateShiftKeyStateDelayed() {
        mHandler.removeMessages(MSG_UPDATE_SHIFT_STATE);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_SHIFT_STATE, 50);
    }

    private void swapPunctuationAndSpace() {
        final InputConnection ic = getCurrentInputConnection();
        if (ic == null)
            return;
        CharSequence lastTwo = ic.getTextBeforeCursor(2, 0);
        if (lastTwo != null && lastTwo.length() == 2
                && lastTwo.charAt(0) == ASCII_SPACE
                && isSentenceSeparator(lastTwo.charAt(1))) {
            ic.beginBatchEdit();
            ic.deleteSurroundingText(2, 0);
            ic.commitText(lastTwo.charAt(1) + " ", 1);
            ic.endBatchEdit();
            updateShiftKeyStateDelayed();
            mJustAddedAutoSpace = true;
        }
    }

    private void reswapPeriodAndSpace() {
        final InputConnection ic = getCurrentInputConnection();
        if (ic == null)
            return;
        CharSequence lastThree = ic.getTextBeforeCursor(3, 0);
        if (lastThree != null && lastThree.length() == 3
                && lastThree.charAt(0) == ASCII_PERIOD
                && lastThree.charAt(1) == ASCII_SPACE
                && lastThree.charAt(2) == ASCII_PERIOD) {
            ic.beginBatchEdit();
            ic.deleteSurroundingText(3, 0);
            ic.commitText(" ..", 1);
            ic.endBatchEdit();
            updateShiftKeyStateDelayed();
        }
    }

    private void doubleSpace() {
        if (!mAutoPunctuate) return;
        if (!mAutoCorrectEnabled) return;
        if (mCorrectionMode == Suggest.CORRECTION_NONE)
            return;

        final long now = SystemClock.uptimeMillis();
        // Only trigger if the second space was pressed quickly after something else.
        // For a true "double-tap space", we would need to track the previous space time.
        if (now - mLastSpaceTime > 500) { // 500ms threshold for "double-tap"
            return;
        }

        final InputConnection ic = getCurrentInputConnection();
        if (ic == null)
            return;
        CharSequence lastThree = ic.getTextBeforeCursor(3, 0);
        if (lastThree != null && lastThree.length() == 3
                && Character.isLetterOrDigit(lastThree.charAt(0))
                && lastThree.charAt(1) == ASCII_SPACE
                && lastThree.charAt(2) == ASCII_SPACE) {
            ic.beginBatchEdit();
            ic.deleteSurroundingText(2, 0);
            ic.commitText(". ", 1);
            ic.endBatchEdit();
            updateShiftKeyStateDelayed();
            mJustAddedAutoSpace = true;
        }
    }

    private void maybeRemovePreviousPeriod(CharSequence text) {
        final InputConnection ic = getCurrentInputConnection();
        if (ic == null || text.length() == 0)
            return;

        // When the text's first character is '.', remove the previous period
        // if there is one.
        CharSequence lastOne = ic.getTextBeforeCursor(1, 0);
        if (lastOne != null && lastOne.length() == 1
                && lastOne.charAt(0) == ASCII_PERIOD
                && text.charAt(0) == ASCII_PERIOD) {
            ic.deleteSurroundingText(1, 0);
        }
    }

    private void removeTrailingSpace() {
        final InputConnection ic = getCurrentInputConnection();
        if (ic == null)
            return;

        CharSequence lastOne = ic.getTextBeforeCursor(1, 0);
        if (lastOne != null && lastOne.length() == 1
                && lastOne.charAt(0) == ASCII_SPACE) {
            ic.deleteSurroundingText(1, 0);
        }
    }

    public boolean addWordToDictionary(String word) {
        mUserDictionary.addWord(word, 128);
        // Suggestion strip should be updated after the operation of adding word
        // to the
        // user dictionary
        postUpdateSuggestions();
        return true;
    }

    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    private void showInputMethodPicker() {
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .showInputMethodPicker();
    }

    private void onOptionKeyPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Use our helper method which includes the FLAG_ACTIVITY_NEW_TASK
            launchSettings();
        } else {
            if (!isShowingOptionDialog()) {
                showOptionsMenu();
            }
        }
    }

    private void onOptionKeyLongPressed() {
        if (!isShowingOptionDialog()) {
            showInputMethodPicker();
        }
    }

    private boolean isShowingOptionDialog() {
        return mOptionsDialog != null && mOptionsDialog.isShowing();
    }

    private boolean isConnectbot() {
        EditorInfo ei = getCurrentInputEditorInfo();
        String pkg = ei.packageName;
        if (ei == null || pkg == null) return false;
        return ((pkg.equalsIgnoreCase("org.connectbot")
            || pkg.equalsIgnoreCase("org.woltage.irssiconnectbot")
            || pkg.equalsIgnoreCase("com.pslib.connectbot")
            || pkg.equalsIgnoreCase("sk.vx.connectbot")
        ) && ei.inputType == 0); // FIXME
    }

    private int getMetaState(boolean shifted) {
        int meta = 0;
        if (shifted) meta |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
        if (mModCtrl) meta |= KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
        if (mModAlt) meta |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
        if (mModMeta) meta |= KeyEvent.META_META_ON | KeyEvent.META_META_LEFT_ON;
        return meta;
    }

    private void sendKeyDown(InputConnection ic, int key, int meta) {
        long now = System.currentTimeMillis();
        if (ic != null) ic.sendKeyEvent(new KeyEvent(
                now, now, KeyEvent.ACTION_DOWN, key, 0, meta));
    }

    private void sendKeyUp(InputConnection ic, int key, int meta) {
        long now = System.currentTimeMillis();
        if (ic != null) ic.sendKeyEvent(new KeyEvent(
                now, now, KeyEvent.ACTION_UP, key, 0, meta));
    }

    private void sendModifiedKeyDownUp(int key, boolean shifted) {
        InputConnection ic = getCurrentInputConnection();
        int meta = getMetaState(shifted);
        sendModifierKeysDown(shifted);
        sendKeyDown(ic, key, meta);
        sendKeyUp(ic, key, meta);
        sendModifierKeysUp(shifted);
    }

    private boolean isShiftMod() {
        if (mShiftKeyState.isChording()) return true;
        if (mKeyboardSwitcher != null) {
            LatinKeyboardView kb = mKeyboardSwitcher.getInputView();
            if (kb != null) return kb.isShiftAll();
        }
        return false;
    }

    private boolean delayChordingCtrlModifier() {
        return sKeyboardSettings.chordingCtrlKey == 0;
    }

    private boolean delayChordingAltModifier() {
        return sKeyboardSettings.chordingAltKey == 0;
    }

    private boolean delayChordingMetaModifier() {
        return sKeyboardSettings.chordingMetaKey == 0;
    }

    private void sendModifiedKeyDownUp(int key) {
        sendModifiedKeyDownUp(key, isShiftMod());
    }

    private void sendShiftKey(InputConnection ic, boolean isDown) {
        int key = KeyEvent.KEYCODE_SHIFT_LEFT;
        int meta = KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
        if (isDown) {
            sendKeyDown(ic, key, meta);
        } else {
            sendKeyUp(ic, key, meta);
        }
    }

    private void sendCtrlKey(InputConnection ic, boolean isDown, boolean chording) {
        if (chording && delayChordingCtrlModifier()) return;

        int key = sKeyboardSettings.chordingCtrlKey;
        if (key == 0) key = KeyEvent.KEYCODE_CTRL_LEFT;
        int meta = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
        if (isDown) {
            sendKeyDown(ic, key, meta);
        } else {
            sendKeyUp(ic, key, meta);
        }
    }

    private void sendAltKey(InputConnection ic, boolean isDown, boolean chording) {
        if (chording && delayChordingAltModifier()) return;

        int key = sKeyboardSettings.chordingAltKey;
        if (key == 0) key = KeyEvent.KEYCODE_ALT_LEFT;
        int meta = KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
        if (isDown) {
            sendKeyDown(ic, key, meta);
        } else {
            sendKeyUp(ic, key, meta);
        }
    }

    private void sendMetaKey(InputConnection ic, boolean isDown, boolean chording) {
        if (chording && delayChordingMetaModifier()) return;

        int key = sKeyboardSettings.chordingMetaKey;
        if (key == 0) key = KeyEvent.KEYCODE_META_LEFT;
        int meta = KeyEvent.META_META_ON | KeyEvent.META_META_LEFT_ON;
        if (isDown) {
            sendKeyDown(ic, key, meta);
        } else {
            sendKeyUp(ic, key, meta);
        }
    }

    private void sendModifierKeysDown(boolean shifted) {
        InputConnection ic = getCurrentInputConnection();
        if (shifted) {
            //Log.i(TAG, "send SHIFT down");
            sendShiftKey(ic, true);
        }
        if (mModCtrl && (!mCtrlKeyState.isChording() || delayChordingCtrlModifier())) {
            sendCtrlKey(ic, true, false);
        }
        if (mModAlt && (!mAltKeyState.isChording() || delayChordingAltModifier())) {
            sendAltKey(ic, true, false);
        }
        if (mModMeta && (!mMetaKeyState.isChording() || delayChordingMetaModifier())) {
            sendMetaKey(ic, true, false);
        }
    }

    private void handleModifierKeysUp(boolean shifted, boolean sendKey) {
        InputConnection ic = getCurrentInputConnection();
        if (mModMeta && (!mMetaKeyState.isChording() || delayChordingMetaModifier())) {
            if (sendKey) sendMetaKey(ic, false, false);
            if (!mMetaKeyState.isChording()) setModMeta(false);
        }
        if (mModAlt && (!mAltKeyState.isChording() || delayChordingAltModifier())) {
            if (sendKey) sendAltKey(ic, false, false);
            if (!mAltKeyState.isChording()) setModAlt(false);
        }
        if (mModCtrl && (!mCtrlKeyState.isChording()  || delayChordingCtrlModifier())) {
            if (sendKey) sendCtrlKey(ic, false, false);
            if (!mCtrlKeyState.isChording()) setModCtrl(false);
        }
        if (shifted) {
            //Log.i(TAG, "send SHIFT up");
            if (sendKey) sendShiftKey(ic, false);
            int shiftState = getShiftState();
            if (!(mShiftKeyState.isChording() || shiftState == Keyboard.SHIFT_LOCKED)) {
                resetShift();
            }
        }
    }

    private void sendModifierKeysUp(boolean shifted) {
        handleModifierKeysUp(shifted, true);
    }

    private void sendSpecialKey(int code) {
        if (!isConnectbot()) {
            commitTyped(getCurrentInputConnection(), true);
            sendModifiedKeyDownUp(code);
            return;
        }

        // TODO(klausw): properly support xterm sequences for Ctrl/Alt modifiers?
        // See http://slackware.osuosl.org/slackware-12.0/source/l/ncurses/xterm.terminfo
        // and the output of "$ infocmp -1L". Support multiple sets, and optional 
        // true numpad keys?
        if (ESC_SEQUENCES == null) {
            ESC_SEQUENCES = new HashMap<Integer, String>();
            CTRL_SEQUENCES = new HashMap<Integer, Integer>();

            // VT escape sequences without leading Escape
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_HOME, "[1~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_END, "[4~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_PAGE_UP, "[5~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_PAGE_DOWN, "[6~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F1, "OP");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F2, "OQ");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F3, "OR");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F4, "OS");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F5, "[15~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F6, "[17~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F7, "[18~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F8, "[19~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F9, "[20~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F10, "[21~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F11, "[23~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F12, "[24~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FORWARD_DEL, "[3~");
            ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_INSERT, "[2~");

            // Special ConnectBot hack: Ctrl-1 to Ctrl-0 for F1-F10.
            CTRL_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F1, KeyEvent.KEYCODE_1);
            CTRL_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F2, KeyEvent.KEYCODE_2);
            CTRL_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F3, KeyEvent.KEYCODE_3);
            CTRL_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F4, KeyEvent.KEYCODE_4);
            CTRL_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F5, KeyEvent.KEYCODE_5);
            CTRL_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F6, KeyEvent.KEYCODE_6);
            CTRL_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F7, KeyEvent.KEYCODE_7);
            CTRL_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F8, KeyEvent.KEYCODE_8);
            CTRL_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F9, KeyEvent.KEYCODE_9);
            CTRL_SEQUENCES.put(-LatinKeyboardView.KEYCODE_FKEY_F10, KeyEvent.KEYCODE_0);

            // Natively supported by ConnectBot
            // ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_DPAD_UP, "OA");
            // ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_DPAD_DOWN, "OB");
            // ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_DPAD_LEFT, "OD");
            // ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_DPAD_RIGHT, "OC");

            // No VT equivalents?
            // ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_DPAD_CENTER, "");
            // ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_SYSRQ, "");
            // ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_BREAK, "");
            // ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_NUM_LOCK, "");
            // ESC_SEQUENCES.put(-LatinKeyboardView.KEYCODE_SCROLL_LOCK, "");
        }
        InputConnection ic = getCurrentInputConnection();
        Integer ctrlseq = null;
        if (mConnectbotTabHack) {
            ctrlseq = CTRL_SEQUENCES.get(code);
        }
        String seq = ESC_SEQUENCES.get(code);

        if (ctrlseq != null) {
            if (mModAlt) {
                // send ESC prefix for "Alt"
                ic.commitText(Character.toString((char) 27), 1);
            }
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DPAD_CENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_DPAD_CENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                    ctrlseq));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
                    ctrlseq));
        } else if (seq != null) {
            if (mModAlt) {
                // send ESC prefix for "Alt"
                ic.commitText(Character.toString((char) 27), 1);
            }
            // send ESC prefix of escape sequence
            ic.commitText(Character.toString((char) 27), 1);
            ic.commitText(seq, 1);
        } else {
            // send key code, let connectbot handle it
            sendDownUpKeyEvents(code);
        }
        handleModifierKeysUp(false, false);
    }

    private final static int asciiToKeyCode[] = new int[127];
    private final static int KF_MASK = 0xffff;
    private final static int KF_SHIFTABLE = 0x10000;
    private final static int KF_UPPER = 0x20000;
    private final static int KF_LETTER = 0x40000;

    {
        // Include RETURN in this set even though it's not printable.
        // Most other non-printable keys get handled elsewhere.
        asciiToKeyCode['\n'] = KeyEvent.KEYCODE_ENTER | KF_SHIFTABLE;

        // Non-alphanumeric ASCII codes which have their own keys
        // (on some keyboards)
        asciiToKeyCode[' '] = KeyEvent.KEYCODE_SPACE | KF_SHIFTABLE;
        //asciiToKeyCode['!'] = KeyEvent.KEYCODE_;
        //asciiToKeyCode['"'] = KeyEvent.KEYCODE_;
        asciiToKeyCode['#'] = KeyEvent.KEYCODE_POUND;
        //asciiToKeyCode['$'] = KeyEvent.KEYCODE_;
        //asciiToKeyCode['%'] = KeyEvent.KEYCODE_;
        //asciiToKeyCode['&'] = KeyEvent.KEYCODE_;
        asciiToKeyCode['\''] = KeyEvent.KEYCODE_APOSTROPHE;
        //asciiToKeyCode['('] = KeyEvent.KEYCODE_;
        //asciiToKeyCode[')'] = KeyEvent.KEYCODE_;
        asciiToKeyCode['*'] = KeyEvent.KEYCODE_STAR;
        asciiToKeyCode['+'] = KeyEvent.KEYCODE_PLUS;
        asciiToKeyCode[','] = KeyEvent.KEYCODE_COMMA;
        asciiToKeyCode['-'] = KeyEvent.KEYCODE_MINUS;
        asciiToKeyCode['.'] = KeyEvent.KEYCODE_PERIOD;
        asciiToKeyCode['/'] = KeyEvent.KEYCODE_SLASH;
        //asciiToKeyCode[':'] = KeyEvent.KEYCODE_;
        asciiToKeyCode[';'] = KeyEvent.KEYCODE_SEMICOLON;
        //asciiToKeyCode['<'] = KeyEvent.KEYCODE_;
        asciiToKeyCode['='] = KeyEvent.KEYCODE_EQUALS;
        //asciiToKeyCode['>'] = KeyEvent.KEYCODE_;
        //asciiToKeyCode['?'] = KeyEvent.KEYCODE_;
        asciiToKeyCode['@'] = KeyEvent.KEYCODE_AT;
        asciiToKeyCode['['] = KeyEvent.KEYCODE_LEFT_BRACKET;
        asciiToKeyCode['\\'] = KeyEvent.KEYCODE_BACKSLASH;
        asciiToKeyCode[']'] = KeyEvent.KEYCODE_RIGHT_BRACKET;
        //asciiToKeyCode['^'] = KeyEvent.KEYCODE_;
        //asciiToKeyCode['_'] = KeyEvent.KEYCODE_;
        asciiToKeyCode['`'] = KeyEvent.KEYCODE_GRAVE;
        //asciiToKeyCode['{'] = KeyEvent.KEYCODE_;
        //asciiToKeyCode['|'] = KeyEvent.KEYCODE_;
        //asciiToKeyCode['}'] = KeyEvent.KEYCODE_;
        //asciiToKeyCode['~'] = KeyEvent.KEYCODE_;


        for (int i = 0; i <= 25; ++i) {
            asciiToKeyCode['a' + i] = KeyEvent.KEYCODE_A + i | KF_LETTER;
            asciiToKeyCode['A' + i] = KeyEvent.KEYCODE_A + i | KF_UPPER | KF_LETTER;
        }

        for (int i = 0; i <= 9; ++i) {
            asciiToKeyCode['0' + i] = KeyEvent.KEYCODE_0 + i;
        }
    }

    public void sendModifiableKeyChar(char ch) {
        // Support modified key events
        boolean modShift = isShiftMod();
        if ((modShift || mModCtrl || mModAlt || mModMeta) && ch > 0 && ch < 127) {
            InputConnection ic = getCurrentInputConnection();
            if (isConnectbot()) {
                if (mModAlt) {
                    // send ESC prefix
                    ic.commitText(Character.toString((char) 27), 1);
                }
                if (mModCtrl) {
                    int code = ch & 31;
                    if (code == 9) {
                        sendTab();
                    } else {
                        ic.commitText(Character.toString((char) code), 1);
                    }
                } else {
                    ic.commitText(Character.toString(ch), 1);
                }
                handleModifierKeysUp(false, false);
                return;
            }

            // Non-ConnectBot

            // Restrict Shift modifier to ENTER and SPACE, supporting Shift-Enter etc.
            // Note that most special keys such as DEL or cursor keys aren't handled
            // by this charcode-based method.

            int combinedCode = asciiToKeyCode[ch];
            if (combinedCode > 0) {
                int code = combinedCode & KF_MASK;
                boolean shiftable = (combinedCode & KF_SHIFTABLE) > 0;
                boolean upper = (combinedCode & KF_UPPER) > 0;
                boolean letter = (combinedCode & KF_LETTER) > 0;
                boolean shifted = modShift && (upper || shiftable);
                if (letter && !mModCtrl && !mModAlt && !mModMeta) {
                    // Try workaround for issue 179 where letters don't get upcased
                    ic.commitText(Character.toString(ch), 1);
                    handleModifierKeysUp(false, false);
                } else if ((ch == 'a' || ch == 'A') && mModCtrl) {
                    // Special case for Ctrl-A to work around accidental select-all-then-replace.
                    if (sKeyboardSettings.ctrlAOverride == 0) {
                        // Ignore Ctrl-A, treat Ctrl-Alt-A as Ctrl-A.
                        if (mModAlt) {
                            boolean isChordingAlt = mAltKeyState.isChording();
                            setModAlt(false);
                            sendModifiedKeyDownUp(code, shifted);
                            if (isChordingAlt) setModAlt(true);
                        } else {
                            Toast.makeText(getApplicationContext(),
                                getResources()
                                .getString(R.string.toast_ctrl_a_override_info), Toast.LENGTH_LONG)
                                .show();
                            // Clear the Ctrl modifier (and others)
                            sendModifierKeysDown(shifted);
                            sendModifierKeysUp(shifted);
                            return;  // ignore the key
                        }

                    } else if (sKeyboardSettings.ctrlAOverride == 1) {
                        // Clear the Ctrl modifier (and others)
                        sendModifierKeysDown(shifted);
                        sendModifierKeysUp(shifted);
                        return;  // ignore the key
                    } else {
                        // Standard Ctrl-A behavior.
                        sendModifiedKeyDownUp(code, shifted);
                    }
                } else {
                    sendModifiedKeyDownUp(code, shifted);
                }
                return;
            }
        }

        if (ch >= '0' && ch <= '9') {
            //WIP
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.clearMetaKeyStates(KeyEvent.META_SHIFT_ON | KeyEvent.META_ALT_ON | KeyEvent.META_SYM_ON);
            }
            //EditorInfo ei = getCurrentInputEditorInfo();
            //Log.i(TAG, "capsmode=" + ic.getCursorCapsMode(ei.inputType));
            //sendModifiedKeyDownUp(KeyEvent.KEYCODE_0 + ch - '0');
            //return;
        }

        // Default handling for anything else, including unmodified ENTER and SPACE.
        sendKeyChar(ch);
    }
    
    private void sendTab() {
        InputConnection ic = getCurrentInputConnection();
        boolean tabHack = isConnectbot() && mConnectbotTabHack;

        // FIXME: tab and ^I don't work in connectbot, hackish workaround
        if (tabHack) {
            if (mModAlt) {
                // send ESC prefix
                ic.commitText(Character.toString((char) 27), 1);
            }
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DPAD_CENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_DPAD_CENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_I));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_I));
        } else {
            sendModifiedKeyDownUp(KeyEvent.KEYCODE_TAB);
        }
    }

    private void sendEscape() {
        if (isConnectbot()) {
            sendKeyChar((char) 27);
        } else {
            sendModifiedKeyDownUp(111 /*KeyEvent.KEYCODE_ESCAPE */);
        }
    }

    private boolean processMultiKey(int primaryCode) {
        if (mDeadAccentBuffer.composeBuffer.length() > 0) {
            //Log.i(TAG, "processMultiKey: pending DeadAccent, length=" + mDeadAccentBuffer.composeBuffer.length());
            mDeadAccentBuffer.execute(primaryCode);
            mDeadAccentBuffer.clear();
            return true;
        }
        if (mComposeMode) {
            mComposeMode = mComposeBuffer.execute(primaryCode);
            return true;
        }
        return false;
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes, int x, int y) {
        mIgnoreNextUpdateSelection = false;
        // Always dismiss the "Touch again to save" message on any key press
        if (mCandidateView != null && mCandidateView.dismissAddToDictionaryHint()) {
            mPredicting = false;
            setNextSuggestions();
            // If we just dismissed the hint, we should NOT process this key as a word separator/space
            // to avoid inserting unintended punctuation (like a dot).
            return;
        }

        long when = SystemClock.uptimeMillis();
        if (primaryCode != Keyboard.KEYCODE_SHIFT) { // Auto-capitalization - turn off by Shift when no need
            mShiftManualOverride = false;
        }
        if (primaryCode != Keyboard.KEYCODE_DELETE
                || when > mLastKeyTime + QUICK_PRESS) {
            mDeleteCount = 0;
        }
        mLastKeyTime = when;
        if (primaryCode == ASCII_SPACE) {
            mLastSpaceTime = when; //when we will be ready to add doubleSpace() to HK
        }
        final boolean distinctMultiTouch = mKeyboardSwitcher
                .hasDistinctMultitouch();
        switch (primaryCode) {
        case Keyboard.KEYCODE_DELETE:
            if (processMultiKey(primaryCode)) {
                break;
            }
            handleBackspace();
            mDeleteCount++;
            break;
        case Keyboard.KEYCODE_SHIFT:
            // Shift key is handled in onPress() when device has distinct
            // multi-touch panel.
            if (!distinctMultiTouch)
                handleShift();
            break;
        case Keyboard.KEYCODE_MODE_CHANGE:
            // Symbol key is handled in onPress() when device has distinct
            // multi-touch panel.
            if (!distinctMultiTouch)
                changeKeyboardMode();
            break;
        case LatinKeyboardView.KEYCODE_CTRL_LEFT:
            // Ctrl key is handled in onPress() when device has distinct
            // multi-touch panel.
            if (!distinctMultiTouch)
                setModCtrl(!mModCtrl);
            break;
        case LatinKeyboardView.KEYCODE_ALT_LEFT:
            // Alt key is handled in onPress() when device has distinct
            // multi-touch panel.
            if (!distinctMultiTouch)
                setModAlt(!mModAlt);
            break;
        case LatinKeyboardView.KEYCODE_META_LEFT:
            // Meta key is handled in onPress() when device has distinct
            // multi-touch panel.
            if (!distinctMultiTouch)
                setModMeta(!mModMeta);
            break;
        case LatinKeyboardView.KEYCODE_FN:
            if (!distinctMultiTouch)
                setModFn(!mModFn);
            break;
        case Keyboard.KEYCODE_CANCEL:
            if (!isShowingOptionDialog()) {
                handleClose();
            }
            break;
        case LatinKeyboardView.KEYCODE_OPTIONS:
            onOptionKeyPressed();
            break;
        case LatinKeyboardView.KEYCODE_OPTIONS_LONGPRESS:
            onOptionKeyLongPressed();
            break;
        case LatinKeyboardView.KEYCODE_COMPOSE:
            mComposeMode = !mComposeMode;
            mComposeBuffer.clear();
            break;
        case LatinKeyboardView.KEYCODE_NEXT_LANGUAGE:
            toggleLanguage(false, true);
            break;
        case LatinKeyboardView.KEYCODE_PREV_LANGUAGE:
            toggleLanguage(false, false);
            break;
        case LatinKeyboardView.KEYCODE_VOICE:
            triggerVoiceInput();
            break;
        case 9 /* Tab */:
            if (processMultiKey(primaryCode)) {
                break;
            }
            sendTab();
            break;
        case LatinKeyboardView.KEYCODE_ESCAPE:
            if (processMultiKey(primaryCode)) {
                break;
            }
            sendEscape();
            break;
        case LatinKeyboardView.KEYCODE_DPAD_UP:
        case LatinKeyboardView.KEYCODE_DPAD_DOWN:
        case LatinKeyboardView.KEYCODE_DPAD_LEFT:
        case LatinKeyboardView.KEYCODE_DPAD_RIGHT:
        case LatinKeyboardView.KEYCODE_DPAD_CENTER:
        case LatinKeyboardView.KEYCODE_HOME:
        case LatinKeyboardView.KEYCODE_END:
        case LatinKeyboardView.KEYCODE_PAGE_UP:
        case LatinKeyboardView.KEYCODE_PAGE_DOWN:
        case LatinKeyboardView.KEYCODE_FKEY_F1:
        case LatinKeyboardView.KEYCODE_FKEY_F2:
        case LatinKeyboardView.KEYCODE_FKEY_F3:
        case LatinKeyboardView.KEYCODE_FKEY_F4:
        case LatinKeyboardView.KEYCODE_FKEY_F5:
        case LatinKeyboardView.KEYCODE_FKEY_F6:
        case LatinKeyboardView.KEYCODE_FKEY_F7:
        case LatinKeyboardView.KEYCODE_FKEY_F8:
        case LatinKeyboardView.KEYCODE_FKEY_F9:
        case LatinKeyboardView.KEYCODE_FKEY_F10:
        case LatinKeyboardView.KEYCODE_FKEY_F11:
        case LatinKeyboardView.KEYCODE_FKEY_F12:
        case LatinKeyboardView.KEYCODE_FORWARD_DEL:
        case LatinKeyboardView.KEYCODE_INSERT:
        case LatinKeyboardView.KEYCODE_SYSRQ:
        case LatinKeyboardView.KEYCODE_BREAK:
        case LatinKeyboardView.KEYCODE_NUM_LOCK:
        case LatinKeyboardView.KEYCODE_SCROLL_LOCK:
            if (processMultiKey(primaryCode)) {
                break;
            }
            // send as plain keys, or as escape sequence if needed
            sendSpecialKey(-primaryCode);
            break;
        default:
            if (!mComposeMode && mDeadKeysActive && Character.getType(primaryCode) == Character.NON_SPACING_MARK) {
                //Log.i(TAG, "possible dead character: " + primaryCode);
                if (!mDeadAccentBuffer.execute(primaryCode)) {
                    //Log.i(TAG, "double dead key");
                    break; // pressing a dead key twice produces spacing equivalent
                }
                updateShiftKeyStateDelayed();
                break;
            }
            if (processMultiKey(primaryCode)) {
                break;
            }
            if (primaryCode != ASCII_ENTER) {
                mJustAddedAutoSpace = false;
            }
            RingCharBuffer.getInstance().push((char) primaryCode, x, y);
            if (isWordSeparator(primaryCode)) {
                handleSeparator(primaryCode);
            } else {
                handleCharacter(primaryCode, keyCodes);
            }
            // Cancel the just reverted state
            mJustRevertedSeparator = null;
        }
        mKeyboardSwitcher.onKey(primaryCode);
        // Reset after any single keystroke
        mEnteredText = null;
        //mDeadAccentBuffer.clear();  // FIXME
    }

    public void onText(CharSequence text) {
        mShiftManualOverride = false; //Auto-capitalization - turn off by Shift if no need
        //mDeadAccentBuffer.clear();  // FIXME
        InputConnection ic = getCurrentInputConnection();
        if (ic == null)
            return;
        if (mPredicting && text.length() == 1) {
            // If adding a single letter, treat it as a regular keystroke so
            // that completion works as expected.
            int c = text.charAt(0);
            if (!isWordSeparator(c)) {
                int[] codes = {c};
                handleCharacter(c, codes);
                return;
            }
        }
        abortCorrection(false);
        ic.beginBatchEdit();
        if (mPredicting) {
            commitTyped(ic, true);
        }
        maybeRemovePreviousPeriod(text);
        ic.commitText(text, 1);
        ic.endBatchEdit();
        updateShiftKeyStateDelayed();
        mKeyboardSwitcher.onKey(0); // dummy key code.
        mJustRevertedSeparator = null;
        mJustAddedAutoSpace = false;
        mEnteredText = text;
    }

    public void onCancel() {
        // User released a finger outside any key
        mKeyboardSwitcher.onCancelInput();
    }

    private void handleBackspace() {
        boolean deleteChar = false;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null)
            return;

        ic.beginBatchEdit();

        if (mPredicting) {
            final int length = mComposing.length();
            if (length > 0) {
                mComposing.delete(length - 1, length);
                mWord.deleteLast();
                ic.setComposingText(mComposing, 1);
                if (mComposing.length() == 0) {
                    mPredicting = false;
                }
                postUpdateSuggestions();
            } else {
                ic.deleteSurroundingText(1, 0);
            }
        } else {
            deleteChar = true;
        }
        postUpdateShiftKeyState();
        TextEntryState.backspace();
        if (TextEntryState.getState() == TextEntryState.State.UNDO_COMMIT) {
            revertLastWord(deleteChar);
            ic.endBatchEdit();
            return;
        } else if (mEnteredText != null
                && sameAsTextBeforeCursor(ic, mEnteredText)) {
            ic.deleteSurroundingText(mEnteredText.length(), 0);
        } else if (deleteChar) {
            if (mCandidateView != null
                    && mCandidateView.dismissAddToDictionaryHint()) {
                // Go back to the suggestion mode if the user canceled the
                // "Touch again to save".
                // NOTE: In gerenal, we don't revert the word when backspacing
                // from a manual suggestion pick. We deliberately chose a
                // different behavior only in the case of picking the first
                // suggestion (typed word). It's intentional to have made this
                // inconsistent with backspacing after selecting other
                // suggestions.
                revertLastWord(deleteChar);
            } else {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                if (mDeleteCount > DELETE_ACCELERATE_AT) {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                }
            }
        }
        mJustRevertedSeparator = null;
        ic.endBatchEdit();
    }

    private void setModCtrl(boolean val) {
        // Log.i("LatinIME", "setModCtrl "+ mModCtrl + "->" + val + ", chording=" + mCtrlKeyState.isChording());
        mKeyboardSwitcher.setCtrlIndicator(val);
        mModCtrl = val;
    }

    private void setModAlt(boolean val) {
        //Log.i("LatinIME", "setModAlt "+ mModAlt + "->" + val + ", chording=" + mAltKeyState.isChording());
        mKeyboardSwitcher.setAltIndicator(val);
        mModAlt = val;
    }

    private void setModMeta(boolean val) {
        //Log.i("LatinIME", "setModMeta "+ mModMeta + "->" + val + ", chording=" + mMetaKeyState.isChording());
        mKeyboardSwitcher.setMetaIndicator(val);
        mModMeta = val;
    }

    private void setModFn(boolean val) {
        //Log.i("LatinIME", "setModFn " + mModFn + "->" + val + ", chording=" + mFnKeyState.isChording());
        mModFn = val;
        mKeyboardSwitcher.setFn(val);
        mKeyboardSwitcher.setCtrlIndicator(mModCtrl);
        mKeyboardSwitcher.setAltIndicator(mModAlt);
        mKeyboardSwitcher.setMetaIndicator(mModMeta);
    }

    private void startMultitouchShift() {
        int newState = Keyboard.SHIFT_ON;
        if (mKeyboardSwitcher.isAlphabetMode()) {
            mSavedShiftState = getShiftState();
            // Auto-capitalization - turn off by Shift if no need
            // If currently in auto-shift, we might want to toggle it off
            if (mIsAutoShift && mSavedShiftState == Keyboard.SHIFT_ON) {
                newState = Keyboard.SHIFT_ON; // Keep it on during press
            } else if (mSavedShiftState == Keyboard.SHIFT_LOCKED) {
                newState = Keyboard.SHIFT_CAPS;
            }
        }
        handleShiftInternal(true, newState);
    }

    private void commitMultitouchShift() {
        if (mKeyboardSwitcher.isAlphabetMode()) {
            int newState;
            // Auto-capitalization - turn off by Shift when no need
            // Check if we were in auto-shift state before the press
            if (mIsAutoShift && mSavedShiftState == Keyboard.SHIFT_ON) {
                newState = Keyboard.SHIFT_OFF;
                mShiftManualOverride = true;
                mIsAutoShift = false;
            } else {
                newState = nextShiftState(mSavedShiftState, true);
                mShiftManualOverride = false;
                mIsAutoShift = false;
            }
            handleShiftInternal(true, newState);
        }
    }

    private void resetMultitouchShift() {
        int newState = Keyboard.SHIFT_OFF;
        if (mSavedShiftState == Keyboard.SHIFT_CAPS_LOCKED || mSavedShiftState == Keyboard.SHIFT_LOCKED) {
            newState = mSavedShiftState;
        }
        handleShiftInternal(true, newState);
    }

    private void resetShift() {
        handleShiftInternal(true, Keyboard.SHIFT_OFF);
    }

    private void handleShift() {
        handleShiftInternal(false, -1);
    }

    private static int getCapsOrShiftLockState() {
        return sKeyboardSettings.capsLock ? Keyboard.SHIFT_CAPS_LOCKED : Keyboard.SHIFT_LOCKED;
    }
    
    // Rotate through shift states by successively pressing and releasing the Shift key.
    private static int nextShiftState(int prevState, boolean allowCapsLock) {
        if (allowCapsLock) {
            if (prevState == Keyboard.SHIFT_OFF) {
                return Keyboard.SHIFT_ON;
            } else if (prevState == Keyboard.SHIFT_ON) {
                return getCapsOrShiftLockState();
            } else {
                return Keyboard.SHIFT_OFF;
            }
        } else {
            // currently unused, see toggleShift()
            if (prevState == Keyboard.SHIFT_OFF) {
                return Keyboard.SHIFT_ON;
            } else {
                return Keyboard.SHIFT_OFF;
            }
        }
    }

    private void handleShiftInternal(boolean forceState, int newState) {
        //Log.i(TAG, "handleShiftInternal forceNormal=" + forceNormal);
        mHandler.removeMessages(MSG_UPDATE_SHIFT_STATE);
        KeyboardSwitcher switcher = mKeyboardSwitcher;
        if (switcher.isAlphabetMode()) {
            if (forceState) {
                switcher.setShiftState(newState);
            } else {
                int currentState = getShiftState(); //Auto-capitalization - turn off by Shift if no need
                if (mIsAutoShift && currentState == Keyboard.SHIFT_ON) {
                    // Manual override of auto-cap
                    mShiftManualOverride = true;
                    mIsAutoShift = false;
                    switcher.setShiftState(Keyboard.SHIFT_OFF);
                } else {
                    int nextState = nextShiftState(currentState, true);
                    mShiftManualOverride = false;
                    mIsAutoShift = false;
                    switcher.setShiftState(nextState);
                }
            }
        } else {
            switcher.toggleShift();
        }
    }

    private void abortCorrection(boolean force) {
        if (force || TextEntryState.isCorrecting()) {
            getCurrentInputConnection().finishComposingText();
            clearSuggestions();
        }
    }

    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (mLastSelectionStart == mLastSelectionEnd
                && TextEntryState.isCorrecting()) {
            abortCorrection(false);
        }

        if (isAlphabet(primaryCode) && isPredictionOn()
                && !mModCtrl && !mModAlt && !mModMeta
                && !isCursorInsideWord()) {
            if (!mPredicting) {
                mPredicting = true;
                mComposing.setLength(0);
                saveWordInHistory(mBestWord);
                mWord.reset();
            }
        }

        if (mModCtrl || mModAlt || mModMeta) {
            commitTyped(getCurrentInputConnection(), true); // sets mPredicting=false
        }

        if (mPredicting) {
            if (isShiftCapsMode()
                    && mKeyboardSwitcher.isAlphabetMode()
                    && mComposing.length() == 0) {
                // Show suggestions with initial caps if starting out shifted,
                // could be either auto-caps or manual shift.
                mWord.setFirstCharCapitalized(true);
            }
            mComposing.append((char) primaryCode);
            mWord.add(primaryCode, keyCodes);
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                // If it's the first letter, make note of auto-caps state
                if (mWord.size() == 1) {
                    mWord.setAutoCapitalized(getCursorCapsMode(ic,
                            getCurrentInputEditorInfo()) != 0);
                }
                ic.setComposingText(mComposing, 1);
            }
            postUpdateSuggestions();
        } else {
            sendModifiableKeyChar((char) primaryCode);
        }
        updateShiftKeyStateDelayed();
        TextEntryState.typedCharacter((char) primaryCode,
                isWordSeparator(primaryCode));
    }

    private void handleSeparator(int primaryCode) {

        // Should dismiss the "Touch again to save" message when handling
        // separator
        if (mCandidateView != null
                && mCandidateView.dismissAddToDictionaryHint()) {
            postUpdateSuggestions();
        }

        boolean pickedDefault = false;
        // Handle separator
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.beginBatchEdit();
            abortCorrection(false);
        }
        if (mPredicting) {
            // In certain languages where single quote is a separator, it's
            // better
            // not to auto correct, but accept the typed word. For instance,
            // in Italian dov' should not be expanded to dove' because the
            // elision
            // requires the last vowel to be removed.
            if (mAutoCorrectOn
                    && primaryCode != '\''
                    && (mJustRevertedSeparator == null
                            || mJustRevertedSeparator.length() == 0
                            || mJustRevertedSeparator.charAt(0) != primaryCode)) {
                pickedDefault = pickDefaultSuggestion();
                if (!pickedDefault) {
                    commitTyped(ic, true);
                }
                // Picked the suggestion by the space key. We consider this
                // as "added an auto space" in autocomplete mode, but as manually
                // typed space in "quick fixes" mode.
                if (primaryCode == ASCII_SPACE) {
                    if (mAutoCorrectEnabled) {
                        mJustAddedAutoSpace = true;
                    } else {
                        TextEntryState.manualTyped("");
                    }
                }
            } else {
                commitTyped(ic, true);
            }
        }
        if (mAutoCorrectEnabled && mJustAddedAutoSpace && primaryCode == ASCII_ENTER) {
            // Remove trailing space if we just added one and the user hits Enter.
            // This is part of auto-correction behavior to avoid trailing spaces at end of lines.
            removeTrailingSpace();
            mJustAddedAutoSpace = false;
        }
        sendModifiableKeyChar((char) primaryCode);

        // Handle the case of ". ." -> " .." with auto-space if necessary
        // before changing the TextEntryState.
        if (TextEntryState.getState() == TextEntryState.State.PUNCTUATION_AFTER_ACCEPTED
                && primaryCode == ASCII_PERIOD) {
            reswapPeriodAndSpace();
        }

        TextEntryState.typedCharacter((char) primaryCode, true);
        if (mAutoPunctuate && TextEntryState.getState() == TextEntryState.State.PUNCTUATION_AFTER_ACCEPTED
                && primaryCode != ASCII_ENTER) {
            swapPunctuationAndSpace();
        } else if (isPredictionOn() && primaryCode == ASCII_SPACE) {
            // doubleSpace() is currently disabled because it triggers on any second space,
            // not just fast double-taps. Even with the timing check, it can be intrusive.
            // If you want to use double-tap space for period, you must first ensure
            // the logic is solid and doesn't interfere with manual text editing.
            // doubleSpace();
        }
        if (pickedDefault) {
            TextEntryState.backToAcceptedDefault(mWord.getTypedWord());
        }
        if (ic != null) {
            ic.endBatchEdit();
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
        updateShiftKeyStateDelayed();
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection(), true);
        requestHideSelf(0);
        if (mKeyboardSwitcher != null) {
            LatinKeyboardView inputView = mKeyboardSwitcher.getInputView();
            if (inputView != null) {
                inputView.closing();
            }
        }
        TextEntryState.endSession();
    }

    private void saveWordInHistory(CharSequence result) {
        if (mWord.size() <= 1) {
            mWord.reset();
            return;
        }
        // Skip if result is null. It happens in some edge case.
        if (TextUtils.isEmpty(result)) {
            return;
        }

        // Make a copy of the CharSequence, since it is/could be a mutable
        // CharSequence
        final String resultCopy = result.toString();
        TypedWordAlternatives entry = new TypedWordAlternatives(resultCopy,
                new WordComposer(mWord));
        mWordHistory.add(entry);
    }

    private void postUpdateSuggestions() {
        mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
        mHandler.removeMessages(MSG_ABORT_RECORRECTION); // Clear safety brake if user is active
        mHandler.sendMessageDelayed(mHandler
                .obtainMessage(MSG_UPDATE_SUGGESTIONS), 100);
    }

    private void postUpdateOldSuggestions() {
        mHandler.removeMessages(MSG_UPDATE_OLD_SUGGESTIONS);
        mHandler.sendMessageDelayed(mHandler
                .obtainMessage(MSG_UPDATE_OLD_SUGGESTIONS), 300);
    }

    private boolean isPredictionOn() {
        return mPredictionOnForMode && isPredictionWanted();
    }

    private boolean isPredictionWanted() {
        return (mShowSuggestions || mSuggestionForceOn) && !suggestionsDisabled();
    }

    private boolean isCandidateStripVisible() {
        boolean suggestionsVisible = (mShowSuggestions || mSuggestionForceOn) && !suggestionsDisabled();
        boolean macrosVisible = (mMacroBar != null && mMacroBar.getVisibility() == View.VISIBLE) && (isPortrait() || mMacrosInLandscape);
        
        // Pasek jest wymagany jeśli cokolwiek w nim chcemy pokazać, LUB jeśli chcemy mieć dostęp do ołówka
        // Ołówek powinien być dostępny jeśli sugestie LUB makra są dozwolone w danym trybie
        boolean toggleAllowed = (isPortrait() || mSuggestionsInLandscape || mMacrosInLandscape);
        
        return suggestionsVisible || macrosVisible || mSuggestionForceOn || toggleAllowed;
    }

    private void switchToKeyboardView() {
        mHandler.post(new Runnable() {
            public void run() {
                // Odświeżamy cały widok wejściowy, aby zachować stos z paskami
                setInputView(onCreateInputView());
                setCandidatesViewShown(mShowSuggestions || mSuggestionForceOn);
                updateInputViewShown();
                postUpdateSuggestions();
            }
        });
    }

    private void clearSuggestions() {
        setSuggestions(null, false, false, false);
    }

    private void setSuggestions(List<CharSequence> suggestions,
            boolean completions, boolean typedWordValid,
            boolean haveMinimalSuggestion) {
        setSuggestions(suggestions, completions, typedWordValid, haveMinimalSuggestion, null);
    }

    private void setSuggestions(List<CharSequence> suggestions,
            boolean completions, boolean typedWordValid,
            boolean haveMinimalSuggestion, int[] sources) {

        if (mIsShowingHint) {
            setCandidatesViewShown(true);
            mIsShowingHint = false;
        }

        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions,
                    typedWordValid, haveMinimalSuggestion, sources);
        }
    }

    private void updateSuggestions() {
        LatinKeyboardView inputView = mKeyboardSwitcher.getInputView();
        ((LatinKeyboard) inputView.getKeyboard()).setPreferredLetters(null);

        // Check if we have a suggestion engine attached.
        if ((mSuggest == null || !isPredictionOn())) {
            // Nawet jeśli predykcja jest wyłączona (np. w Termux), 
            // chcemy pokazać pasek z interpunkcją, jeśli sugestie są włączone.
            if (isCandidateStripVisible()) {
                setNextSuggestions();
            }
            return;
        }

        if (!mPredicting) {
            // GEM FIX: Prevent flicker by not jumping to punctuation if cursor is still inside a word.
            if (isCursorInsideWord()) {
                return;
            }
            setNextSuggestions();
            return;
        }

        if (mWord.size() < sKeyboardSettings.minLettersSuggestion) {
            showSuggestions(null, mWord.getTypedWord(), false, false, null);
            return;
        }

        showSuggestions(mWord);
    }

    private List<CharSequence> getTypedSuggestions(WordComposer word) {
        List<CharSequence> stringList = mSuggest.getSuggestions(
                mKeyboardSwitcher.getInputView(), word, false, null);
        return stringList;
    }

    private void showCorrections(WordAlternatives alternatives) {
        List<CharSequence> stringList = alternatives.getAlternatives();
        int[] sources = null;
        if (alternatives instanceof TypedWordAlternatives) {
            sources = mSuggest.getSuggestionSources();
        }
        ((LatinKeyboard) mKeyboardSwitcher.getInputView().getKeyboard())
                .setPreferredLetters(null);
        showSuggestions(stringList, alternatives.getOriginalWord(), false,
                false, sources);
    }

    private void showSuggestions(WordComposer word) {
        // long startTime = System.currentTimeMillis(); // TIME MEASUREMENT!
        // TODO Maybe need better way of retrieving previous word
        CharSequence prevWord = EditingUtil.getPreviousWord(
                getCurrentInputConnection(), mWordSeparators);
        List<CharSequence> stringList = mSuggest.getSuggestions(
                mKeyboardSwitcher.getInputView(), word, false, prevWord);
        // long stopTime = System.currentTimeMillis(); // TIME MEASUREMENT!
        // Log.d("LatinIME","Suggest Total Time - " + (stopTime - startTime));

        // int[] nextLettersFrequencies = mSuggest.getNextLettersFrequencies();

        // ((LatinKeyboard) mKeyboardSwitcher.getInputView().getKeyboard())
        //        .setPreferredLetters(nextLettersFrequencies);

        boolean correctionAvailable = !mInputTypeNoAutoCorrect
                && mSuggest.hasMinimalCorrection();
        // || mCorrectionMode == mSuggest.CORRECTION_FULL;
        CharSequence typedWord = word.getTypedWord();
        // If we're in basic correct
        boolean typedWordValid = mSuggest.isValidWord(typedWord)
                || (preferCapitalization() && mSuggest.isValidWord(typedWord
                        .toString().toLowerCase()));
        if (mCorrectionMode == Suggest.CORRECTION_FULL
                || mCorrectionMode == Suggest.CORRECTION_FULL_BIGRAM) {
            correctionAvailable |= typedWordValid;
        }
        // Don't auto-correct words with multiple capital letter
        correctionAvailable &= !word.isMostlyCaps();
        correctionAvailable &= !TextEntryState.isCorrecting();

        // Special handling for dictionary sources
        int[] sources = mSuggest.getSuggestionSources();
        if (sources != null && sources.length > 1 && correctionAvailable && !typedWordValid) {
            int topSource = sources[1]; // Index 1 is the first real suggestion after typed word at 0
            // Only auto-correct if it comes from a reliable source (Main or Contacts)
            if (topSource == Suggest.DIC_AUTO || topSource == Suggest.DIC_USER) {
                // For learned words, maybe be more conservative?
                // For now, let's keep it enabled but we could add a check here.
            }
        }

        showSuggestions(stringList, typedWord, typedWordValid,
                correctionAvailable, sources);
    }

    private void showSuggestions(List<CharSequence> stringList,
            CharSequence typedWord, boolean typedWordValid,
            boolean correctionAvailable, int[] sources) {
        setSuggestions(stringList, false, typedWordValid, correctionAvailable, sources);
        setCandidatesViewShown(isCandidateStripVisible() || mSuggestionForceOn || mCompletionOn || (mMacroBar != null && mMacroBar.getVisibility() == View.VISIBLE));
        if (stringList != null && stringList.size() > 0) {
            if (correctionAvailable && !typedWordValid && stringList.size() > 1) {
                CharSequence suggestion = stringList.get(1);
                // Don't auto-correct lowercase to capitalized unless auto-caps is on
                if (typedWord.length() > 0 && Character.isLowerCase(typedWord.charAt(0))
                        && Character.isUpperCase(suggestion.charAt(0))
                        && !mWord.isAutoCapitalized()) {
                    mBestWord = typedWord;
                } else {
                    mBestWord = suggestion;
                }
            } else {
                mBestWord = typedWord;
            }
        } else {
            mBestWord = typedWord;
        }
        setCandidatesViewShown(isCandidateStripVisible() || mCompletionOn);
    }

    private boolean pickDefaultSuggestion() {
        // Complete any pending candidate query first
        if (mHandler.hasMessages(MSG_UPDATE_SUGGESTIONS)) {
            mHandler.removeMessages(MSG_UPDATE_SUGGESTIONS);
            updateSuggestions();
        }
        if (mBestWord != null && mBestWord.length() > 0) {
            TextEntryState.acceptedDefault(mWord.getTypedWord(), mBestWord);
            mJustAccepted = true;
            pickSuggestion(mBestWord, false, true);
            // Add the word to the auto dictionary if it's not a known word
            addToDictionaries(mBestWord, AutoDictionary.FREQUENCY_FOR_TYPED);
            return true;

        }
        return false;
    }

    public void pickSuggestionManually(int index, CharSequence suggestion) {
        List<CharSequence> suggestions = mCandidateView.getSuggestions();

        final boolean correcting = TextEntryState.isCorrecting();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.beginBatchEdit();
        }
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            if (ic != null) {
                ic.commitCompletion(ci);
            }
            mCommittedLength = suggestion.length();
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyStateDelayed();
            if (ic != null) {
                ic.endBatchEdit();
            }
            return;
        }

        // If this is a punctuation, apply it through the normal key press
        if (suggestion.length() == 1
                && (isWordSeparator(suggestion.charAt(0)) || isSuggestedPunctuation(suggestion
                        .charAt(0)))) {
            final char primaryCode = suggestion.charAt(0);
            onKey(primaryCode, new int[] { primaryCode },
                    LatinKeyboardBaseView.NOT_A_TOUCH_COORDINATE,
                    LatinKeyboardBaseView.NOT_A_TOUCH_COORDINATE);
            if (ic != null) {
                ic.endBatchEdit();
            }
            return;
        }
        // Hint should be shown if it is not a known word.
        final boolean showingAddToDictionaryHint = index == 0
                && mSuggest != null
                && !mSuggest.isValidWord(suggestion)
                && !mSuggest.isValidWord(suggestion.toString().toLowerCase());

        mJustAccepted = true;
        mIgnoreNextUpdateSelection = showingAddToDictionaryHint;
        pickSuggestion(suggestion, correcting, !showingAddToDictionaryHint);

        if (showingAddToDictionaryHint) {
            mCandidateView.showAddToDictionaryHint(suggestion);
        } else {
            if (index == 0) {
                addToDictionaries(suggestion, AutoDictionary.FREQUENCY_FOR_PICKED);
            } else {
                addToBigramDictionary(suggestion, 1);
            }
            TextEntryState.acceptedSuggestion(mComposing.toString(), suggestion);
            // Follow it with a space
            if (mAutoSpace && !correcting) {
                sendSpace();
                mJustAddedAutoSpace = true;
            }

            if (!correcting) {
                // Fool the state watcher so that a subsequent backspace will not do
                // a revert, unless
                // we just did a correction, in which case we need to stay in
                // TextEntryState.State.PICKED_SUGGESTION state.
                TextEntryState.typedCharacter((char) ASCII_SPACE, true);
                setNextSuggestions();
            } else {
                // If we're not showing the "Touch again to save", then show
                // corrections again.
                // In case the cursor position doesn't change, make sure we show the
                // suggestions again.
                clearSuggestions();
                postUpdateOldSuggestions();
            }
        }
        if (ic != null) {
            ic.endBatchEdit();
        }
    }

    private void rememberReplacedWord(CharSequence suggestion) {
    }

    /**
     * Commits the chosen word to the text field and saves it for later
     * retrieval.
     *
     * @param suggestion
     *            the suggestion picked by the user to be committed to the text
     *            field
     * @param correcting
     *            whether this is due to a correction of an existing word.
     */
    private void pickSuggestion(CharSequence suggestion, boolean correcting, boolean showNextSuggestions) {
        LatinKeyboardView inputView = mKeyboardSwitcher.getInputView();
        int shiftState = getShiftState();
        if (shiftState == Keyboard.SHIFT_LOCKED || shiftState == Keyboard.SHIFT_CAPS_LOCKED) {
            suggestion = suggestion.toString().toUpperCase(); // all UPPERCASE
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            rememberReplacedWord(suggestion);
            ic.commitText(suggestion, 1);
        }
        saveWordInHistory(suggestion);
        mPredicting = false;
        mCommittedLength = suggestion.length();
        ((LatinKeyboard) inputView.getKeyboard()).setPreferredLetters(null);
        // If we just corrected a word, then don't show punctuations
        if (!correcting && showNextSuggestions) {
            setNextSuggestions();
        }
        updateShiftKeyStateDelayed();
    }

    /**
     * Tries to apply any typed alternatives for the word if we have any cached
     * alternatives, otherwise tries to find new corrections and completions for
     * the word.
     *
     * @param touching
     *            The word that the cursor is touching, with position
     *            information
     * @return true if an alternative was found, false otherwise.
     */
    private boolean applyTypedAlternatives(EditingUtil.SelectedWord touching) {
        // If we didn't find a match, search for result in typed word history
        WordComposer foundWord = null;
        WordAlternatives alternatives = null;
        for (WordAlternatives entry : mWordHistory) {
            if (TextUtils.equals(entry.getChosenWord(), touching.word)) {
                if (entry instanceof TypedWordAlternatives) {
                    foundWord = ((TypedWordAlternatives) entry).word;
                }
                alternatives = entry;
                break;
            }
        }
        // If we didn't find a match, at least suggest completions
        if (foundWord == null
                && (mSuggest.isValidWord(touching.word) || mSuggest
                        .isValidWord(touching.word.toString().toLowerCase()))) {
            foundWord = new WordComposer();
            for (int i = 0; i < touching.word.length(); i++) {
                foundWord.add(touching.word.charAt(i),
                        new int[] { touching.word.charAt(i) });
            }
            foundWord.setFirstCharCapitalized(Character
                    .isUpperCase(touching.word.charAt(0)));
        }
        // Found a match, show suggestions
        if (foundWord != null || alternatives != null) {
            if (alternatives == null) {
                alternatives = new TypedWordAlternatives(touching.word,
                        foundWord);
            }
            showCorrections(alternatives);
            if (foundWord != null) {
                mWord = new WordComposer(foundWord);
            } else {
                mWord.reset();
            }
            return true;
        }
        return false;
    }

    private void setOldSuggestions() {
        if (mCandidateView != null
                && mCandidateView.isShowingAddToDictionaryHint()) {
            return;
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic == null)
            return;
        if (!mPredicting) {
            // Extract the selected or touching text
            EditingUtil.SelectedWord touching = EditingUtil
                    .getWordAtCursorOrSelection(ic, mLastSelectionStart,
                            mLastSelectionEnd, mWordSeparators);

            if (touching != null) {
                abortCorrection(true);
                if (applyTypedAlternatives(touching)) {
                    TextEntryState.selectedForCorrection();
                    EditingUtil.underlineWord(ic, touching);
                    // GEM FIX: Start safety brake timer (5 seconds) to hide suggestions if unused
                    mHandler.removeMessages(MSG_ABORT_RECORRECTION);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_ABORT_RECORRECTION), 5000);
                    return;
                }
            }

            abortCorrection(true);
            setNextSuggestions(); // Show the punctuation suggestions list
        } else {
            abortCorrection(true);
        }
    }

    private void setNextSuggestions() {
        setSuggestions(mSuggestPuncList, false, false, false);
        //int[] sources = new int[mSuggestPuncList.size()];
        // Fill sources with 0 (DIC_USER_TYPED) to ensure they fall into the 'Orange' category
        //java.util.Arrays.fill(sources, Suggest.DIC_USER_TYPED);
        //setSuggestions(mSuggestPuncList, false, false, false, sources);
    }

    private void addToDictionaries(CharSequence suggestion, int frequencyDelta) {
        checkAddToDictionary(suggestion, frequencyDelta, false);
    }

    private void addToBigramDictionary(CharSequence suggestion,
            int frequencyDelta) {
        checkAddToDictionary(suggestion, frequencyDelta, true);
    }

    /**
     * Adds to the UserBigramDictionary and/or AutoDictionary
     *
     * @param addToBigramDictionary
     *            true if it should be added to bigram dictionary if possible
     */
    private void checkAddToDictionary(CharSequence suggestion,
            int frequencyDelta, boolean addToBigramDictionary) {
        if (suggestion == null || suggestion.length() < 1)
            return;
        // Only auto-add to dictionary if auto-correct is ON. Otherwise we'll be
        // adding words in situations where the user or application really
        // didn't
        // want corrections enabled or learned.
        if (!(mCorrectionMode == Suggest.CORRECTION_FULL || mCorrectionMode == Suggest.CORRECTION_FULL_BIGRAM)) {
            return;
        }
        if (suggestion != null) {
            if (!addToBigramDictionary
                    && mAutoDictionary.isValidWord(suggestion)
                    || (!mSuggest.isValidWord(suggestion.toString()) && !mSuggest
                            .isValidWord(suggestion.toString().toLowerCase()))) {
                mAutoDictionary.addWord(suggestion.toString(), frequencyDelta);
            }

            if (mUserBigramDictionary != null) {
                CharSequence prevWord = EditingUtil.getPreviousWord(
                        getCurrentInputConnection(), mSentenceSeparators);
                if (!TextUtils.isEmpty(prevWord)) {
                    mUserBigramDictionary.addBigrams(prevWord.toString(),
                            suggestion.toString());
                }
            }
        }
    }

    private boolean isCursorInsideWord() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return false;
        CharSequence toLeft = ic.getTextBeforeCursor(1, 0);
        CharSequence toRight = ic.getTextAfterCursor(1, 0);
        if (TextUtils.isEmpty(toLeft) || TextUtils.isEmpty(toRight)) return false;
        
        char l = toLeft.charAt(0);
        char r = toRight.charAt(0);
        
        // Strict check: must be a letter on both sides to be "inside"
        return Character.isLetter(l) && Character.isLetter(r);
    }

    private boolean sameAsTextBeforeCursor(InputConnection ic, CharSequence text) {
        CharSequence beforeText = ic.getTextBeforeCursor(text.length(), 0);
        return TextUtils.equals(text, beforeText);
    }

    public void revertLastWord(boolean deleteChar) {
        final int length = mComposing.length();
        if (!mPredicting && length > 0) {
            final InputConnection ic = getCurrentInputConnection();
            mPredicting = true;
            mJustRevertedSeparator = ic.getTextBeforeCursor(1, 0);
            if (deleteChar)
                ic.deleteSurroundingText(1, 0);
            int toDelete = mCommittedLength;
            CharSequence toTheLeft = ic
                    .getTextBeforeCursor(mCommittedLength, 0);
            if (toTheLeft != null && toTheLeft.length() > 0
                    && isWordSeparator(toTheLeft.charAt(0))) {
                toDelete--;
            }
            ic.deleteSurroundingText(toDelete, 0);
            ic.setComposingText(mComposing, 1);
            TextEntryState.backspace();
            postUpdateSuggestions();
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            mJustRevertedSeparator = null;
        }
    }

    protected String getWordSeparators() {
        return mWordSeparators;
    }

    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char) code));
    }

    private boolean isSentenceSeparator(int code) {
        return mSentenceSeparators.contains(String.valueOf((char) code));
    }

    private void sendSpace() {
        sendModifiableKeyChar((char) ASCII_SPACE);
        updateShiftKeyStateDelayed();
        // onKey(KEY_SPACE[0], KEY_SPACE);
    }

    public boolean preferCapitalization() {
        return mWord.isFirstCharCapitalized();
    }

    void toggleLanguage(boolean reset, boolean next) {
        int oldShiftState = getShiftState();
        if (reset) {
            mLanguageSwitcher.reset();
        } else {
            if (next) {
                mLanguageSwitcher.next();
            } else {
                mLanguageSwitcher.prev();
            }
        }
        int currentKeyboardMode = mKeyboardSwitcher.getKeyboardMode();
        reloadKeyboards();
        if (isInputViewShown()) {
            mKeyboardSwitcher.makeKeyboards(true);
        }
        EditorInfo ei = getCurrentInputEditorInfo();
        mKeyboardSwitcher.setKeyboardMode(currentKeyboardMode, (ei != null) ? ei.imeOptions : 0,
                mEnableVoiceButton && mEnableVoice);
        initSuggest(mLanguageSwitcher.getInputLanguage());
        mLanguageSwitcher.persist();
        mAutoCapActive = (mAutoCapPref != 2) && mLanguageSwitcher.allowAutoCap();
        mDeadKeysActive = mLanguageSwitcher.allowDeadKeys();
        if (oldShiftState == Keyboard.SHIFT_OFF) {
            mKeyboardSwitcher.setShiftState(Keyboard.SHIFT_OFF);
        } else {
            updateShiftKeyState(getCurrentInputEditorInfo());
        }
        if (isInputViewShown()) {
            setCandidatesViewShown(isPredictionOn());
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        Log.i("PCKeyboard", "onSharedPreferenceChanged()");
        boolean needReload = false;
        Resources res = getResources();
        
        // Apply globally handled shared prefs
        sKeyboardSettings.sharedPreferenceChanged(sharedPreferences, key);
        boolean needReloadFlag = sKeyboardSettings.hasFlag(GlobalKeyboardSettings.FLAG_PREF_NEED_RELOAD);
        boolean newPuncList = sKeyboardSettings.hasFlag(GlobalKeyboardSettings.FLAG_PREF_NEW_PUNC_LIST);
        boolean recreateInputView = sKeyboardSettings.hasFlag(GlobalKeyboardSettings.FLAG_PREF_RECREATE_INPUT_VIEW);
        boolean resetModeOverride = sKeyboardSettings.hasFlag(GlobalKeyboardSettings.FLAG_PREF_RESET_MODE_OVERRIDE);
        boolean resetKeyboards = sKeyboardSettings.hasFlag(GlobalKeyboardSettings.FLAG_PREF_RESET_KEYBOARDS);

        if (needReloadFlag) {
            needReload = true;
        }
        if (newPuncList) {
            initSuggestPuncList();
        }
        
        if (isInputViewShown()) {
            if (recreateInputView) {
                mKeyboardSwitcher.recreateInputView();
            }
            if (resetModeOverride) {
                mKeyboardModeOverrideLandscape = 0;
                mKeyboardModeOverridePortrait = 0;
            }
            if (resetKeyboards) {
                toggleLanguage(true, true);
            }
        } else {
            if (recreateInputView || resetModeOverride || resetKeyboards || needReloadFlag) {
                mRefreshKeyboardRequired = true;
            }
        }

        int unhandledFlags = sKeyboardSettings.unhandledFlags();
        if (unhandledFlags != GlobalKeyboardSettings.FLAG_PREF_NONE) {
            Log.w(TAG, "Not all flag settings handled, remaining=" + unhandledFlags);
        }

        if (PREF_SELECTED_LANGUAGES.equals(key)) {
            mLanguageSwitcher.loadLocales(sharedPreferences);
            mRefreshKeyboardRequired = true;
        } else if (PREF_RECORRECTION_ENABLED.equals(key)) {
            mReCorrectionEnabled = sharedPreferences.getBoolean(
                    PREF_RECORRECTION_ENABLED, res
                            .getBoolean(R.bool.default_recorrection_enabled));
            //if (mReCorrectionEnabled) {
                // It doesn't work right on pre-Gingerbread phones.
            //    Toast.makeText(getApplicationContext(),
            //            res.getString(R.string.recorrect_warning), Toast.LENGTH_LONG)
            //            .show();
            //}
        } else if (PREF_CONNECTBOT_TAB_HACK.equals(key)) {
            mConnectbotTabHack = sharedPreferences.getBoolean(
                    PREF_CONNECTBOT_TAB_HACK, res
                            .getBoolean(R.bool.default_connectbot_tab_hack));
        } else if (PREF_FULLSCREEN_OVERRIDE.equals(key)) {
            mFullscreenOverride = sharedPreferences.getBoolean(
                    PREF_FULLSCREEN_OVERRIDE, res
                            .getBoolean(R.bool.default_fullscreen_override));
            needReload = true;
        } else if (PREF_FORCE_KEYBOARD_ON.equals(key)) {
            mForceKeyboardOn = sharedPreferences.getBoolean(
                    PREF_FORCE_KEYBOARD_ON, res
                            .getBoolean(R.bool.default_force_keyboard_on));
            needReload = true;
        } else if (PREF_KEYBOARD_NOTIFICATION.equals(key)) {
            mKeyboardNotification = sharedPreferences.getBoolean(
                    PREF_KEYBOARD_NOTIFICATION, res
                            .getBoolean(R.bool.default_keyboard_notification));
            setNotification(mKeyboardNotification);
        } else if (PREF_SUGGESTIONS_IN_LANDSCAPE.equals(key)) {
            mSuggestionsInLandscape = sharedPreferences.getBoolean(
                    PREF_SUGGESTIONS_IN_LANDSCAPE, res
                            .getBoolean(R.bool.default_suggestions_in_landscape));
            mSuggestionForceOff = false;
            mSuggestionForceOn = false;
            needReload = true;
        } else if (PREF_MACROS_IN_LANDSCAPE.equals(key)) {
            mMacrosInLandscape = sharedPreferences.getBoolean(PREF_MACROS_IN_LANDSCAPE, true);
            // No direct call to setCandidatesViewShown here.
            needReload = true;
        } else if (PREF_SHOW_SUGGESTIONS.equals(key)) {
            mShowSuggestions = sharedPreferences.getBoolean(
                    PREF_SHOW_SUGGESTIONS, res.getBoolean(R.bool.default_suggestions));
            mSuggestionForceOff = false;
            mSuggestionForceOn = false;
            if (!mShowSuggestions) {
                abortCorrection(true);
                mHandler.removeMessages(MSG_ABORT_RECORRECTION);
            }
            // setCandidatesViewShown(mShowSuggestions); // Removed to prevent hang in settings
            needReload = true;
        } else if (PREF_MIN_LETTERS_SUGGESTION.equals(key)) {
            // Already handled by sKeyboardSettings.sharedPreferenceChanged
        } else if (PREF_HEIGHT_PORTRAIT.equals(key)) {
            mHeightPortrait = getHeight(sharedPreferences,
                    PREF_HEIGHT_PORTRAIT, res.getString(R.string.default_height_portrait));
            needReload = true;
        } else if (PREF_HEIGHT_LANDSCAPE.equals(key)) {
            mHeightLandscape = getHeight(sharedPreferences,
                    PREF_HEIGHT_LANDSCAPE, res.getString(R.string.default_height_landscape));
            needReload = true;
        } else if (PREF_HINT_MODE.equals(key)) {
            getSettingsSafe().hintMode = Integer.parseInt(sharedPreferences.getString(PREF_HINT_MODE,
                    res.getString(R.string.default_hint_mode)));
            needReload = true;
        } else if (PREF_LONGPRESS_TIMEOUT.equals(key)) {
               getSettingsSafe().longpressTimeout = getPrefInt(sharedPreferences, PREF_LONGPRESS_TIMEOUT,
                       res.getString(R.string.default_long_press_duration));
        } else if (PREF_RENDER_MODE.equals(key)) {
            getSettingsSafe().renderMode = getPrefInt(sharedPreferences, PREF_RENDER_MODE,
                    res.getString(R.string.default_render_mode));
            needReload = true;
        } else if (PREF_SWIPE_UP.equals(key)) {
            mSwipeUpAction = sharedPreferences.getString(PREF_SWIPE_UP, res.getString(R.string.default_swipe_up));
        } else if (PREF_SWIPE_DOWN.equals(key)) {
            mSwipeDownAction = sharedPreferences.getString(PREF_SWIPE_DOWN, res.getString(R.string.default_swipe_down));
        } else if (PREF_SWIPE_LEFT.equals(key)) {
            mSwipeLeftAction = sharedPreferences.getString(PREF_SWIPE_LEFT, res.getString(R.string.default_swipe_left));
        } else if (PREF_SWIPE_RIGHT.equals(key)) {
            mSwipeRightAction = sharedPreferences.getString(PREF_SWIPE_RIGHT, res.getString(R.string.default_swipe_right));
        } else if (PREF_VOL_UP.equals(key)) {
            mVolUpAction = sharedPreferences.getString(PREF_VOL_UP, res.getString(R.string.default_vol_up));
        } else if (PREF_VOL_DOWN.equals(key)) {
            mVolDownAction = sharedPreferences.getString(PREF_VOL_DOWN, res.getString(R.string.default_vol_down));
        } else if (PREF_VIBRATE_LEN.equals(key)) {
            mVibrateLen = getPrefInt(sharedPreferences, PREF_VIBRATE_LEN, getResources().getString(R.string.vibrate_duration_ms));
        }

        updateKeyboardOptions();
        if (needReload) {
            if (!isInputViewShown()) {
                mRefreshKeyboardRequired = true;
            } else {
                mKeyboardSwitcher.makeKeyboards(true);
            }
        }
    }

    private void triggerVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "Grant microphone permission for voice input", Toast.LENGTH_LONG).show();
            return;
        }
        if (mSpeechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(this)) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mSpeechRecognizer.setRecognitionListener(new VoiceRecognitionListener());
        }
        if (mSpeechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            String locale = mInputLocale;
            if (locale == null) {
                locale = Locale.getDefault().toString();
            }
            locale = locale.replace('_', '-'); // SpeechRecognizer prefers BCP 47
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale);
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
            try {
                mSpeechRecognizer.startListening(intent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to start speech recognizer", e);
            }
        }
    }

    private boolean doSwipeAction(String action) {
        //Log.i(TAG, "doSwipeAction + " + action);
        if (action == null || action.equals("") || action.equals("none")) {
            return false;
        } else if (action.equals("close")) {
            handleClose();
        } else if (action.equals("settings")) {
            launchSettings();
        } else if (action.equals("suggestions")) {
            if (mSuggestionForceOn) {
                mSuggestionForceOn = false;
                mSuggestionForceOff = true;
            } else if (mSuggestionForceOff) {
                mSuggestionForceOn = true;
                mSuggestionForceOff = false;                
            } else if (isPredictionWanted()) {
                mSuggestionForceOff = true;
            } else {
                mSuggestionForceOn = true;
            }
            setCandidatesViewShown(isPredictionOn());
        } else if (action.equals("lang_prev")) {
            toggleLanguage(false, false);
        } else if (action.equals("lang_next")) {
            toggleLanguage(false, true);
        } else if (action.equals("full_mode")) {
            if (isPortrait()) {
                mKeyboardModeOverridePortrait = (mKeyboardModeOverridePortrait + 1) % mNumKeyboardModes;
            } else {
                mKeyboardModeOverrideLandscape = (mKeyboardModeOverrideLandscape + 1) % mNumKeyboardModes;
            }
            toggleLanguage(true, true);
        } else if (action.equals("extension")) {
            sKeyboardSettings.useExtension = !sKeyboardSettings.useExtension;
            reloadKeyboards();
        } else if (action.equals("height_up")) {
            if (isPortrait()) {
                mHeightPortrait += 5;
                if (mHeightPortrait > 70) mHeightPortrait = 70;
            } else {
                mHeightLandscape += 5;
                if (mHeightLandscape > 70) mHeightLandscape = 70;                
            }
            toggleLanguage(true, true);
        } else if (action.equals("voice_input")) {
            triggerVoiceInput();
        } else if (action.equals("height_down")) {
            if (isPortrait()) {
                mHeightPortrait -= 5;
                if (mHeightPortrait < 15) mHeightPortrait = 15;
            } else {
                mHeightLandscape -= 5;
                if (mHeightLandscape < 15) mHeightLandscape = 15;                
            }
            toggleLanguage(true, true);
        } else {
            Log.i(TAG, "Unsupported swipe action config: " + action);
        }
        return true;
    }

    public boolean swipeRight() {
        return doSwipeAction(mSwipeRightAction);
    }

    public boolean swipeLeft() {
        return doSwipeAction(mSwipeLeftAction);
    }

    public boolean swipeDown() {
        return doSwipeAction(mSwipeDownAction);
    }

    public boolean swipeUp() {
        return doSwipeAction(mSwipeUpAction);
    }

    public void onPress(int primaryCode) {
        InputConnection ic = getCurrentInputConnection();
        if (mKeyboardSwitcher.isVibrateAndSoundFeedbackRequired() || !mVibrateOn) {
            vibrate();
            if (mKeyboardSwitcher.isVibrateAndSoundFeedbackRequired()) {
                playKeyClick(primaryCode);
            }
        }
        final boolean distinctMultiTouch = mKeyboardSwitcher
                .hasDistinctMultitouch();
        if (distinctMultiTouch && primaryCode == Keyboard.KEYCODE_SHIFT) {
            mShiftKeyState.onPress();
            startMultitouchShift();
        } else if (distinctMultiTouch
                && primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            changeKeyboardMode();
            mSymbolKeyState.onPress();
            mKeyboardSwitcher.setAutoModeSwitchStateMomentary();
        } else if (distinctMultiTouch
                && primaryCode == LatinKeyboardView.KEYCODE_CTRL_LEFT) {
            setModCtrl(!mModCtrl);
            mCtrlKeyState.onPress();
            sendCtrlKey(ic, true, true);
        } else if (distinctMultiTouch
                && primaryCode == LatinKeyboardView.KEYCODE_ALT_LEFT) {
            setModAlt(!mModAlt);
            mAltKeyState.onPress();
            sendAltKey(ic, true, true);
        } else if (distinctMultiTouch
                && primaryCode == LatinKeyboardView.KEYCODE_META_LEFT) {
            setModMeta(!mModMeta);
            mMetaKeyState.onPress();
            sendMetaKey(ic, true, true);
        } else if (distinctMultiTouch
                && primaryCode == LatinKeyboardView.KEYCODE_FN) {
            setModFn(!mModFn);
            mFnKeyState.onPress();
        } else {
            mShiftKeyState.onOtherKeyPressed();
            mSymbolKeyState.onOtherKeyPressed();
            mCtrlKeyState.onOtherKeyPressed();
            mAltKeyState.onOtherKeyPressed();
            mMetaKeyState.onOtherKeyPressed();
            mFnKeyState.onOtherKeyPressed();
        }
    }

    public void onRelease(int primaryCode) {
        // Reset any drag flags in the keyboard
        ((LatinKeyboard) mKeyboardSwitcher.getInputView().getKeyboard())
                .keyReleased();
        // vibrate();
        final boolean distinctMultiTouch = mKeyboardSwitcher
                .hasDistinctMultitouch();
        InputConnection ic = getCurrentInputConnection();
        if (distinctMultiTouch && primaryCode == Keyboard.KEYCODE_SHIFT) {
            if (mShiftKeyState.isChording()) {
                resetMultitouchShift();
            } else {
                commitMultitouchShift();
            }
            mShiftKeyState.onRelease();
        } else if (distinctMultiTouch
                && primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            // Snap back to the previous keyboard mode if the user chords the
            // mode change key and
            // other key, then released the mode change key.
            if (mKeyboardSwitcher.isInChordingAutoModeSwitchState())
                changeKeyboardMode();
            mSymbolKeyState.onRelease();
        } else if (distinctMultiTouch
                && primaryCode == LatinKeyboardView.KEYCODE_CTRL_LEFT) {
            if (mCtrlKeyState.isChording()) {
                setModCtrl(false);
            }
            sendCtrlKey(ic, false, true);
            mCtrlKeyState.onRelease();
        } else if (distinctMultiTouch
                && primaryCode == LatinKeyboardView.KEYCODE_ALT_LEFT) {
            if (mAltKeyState.isChording()) {
                setModAlt(false);
            }
            sendAltKey(ic, false, true);
            mAltKeyState.onRelease();
        } else if (distinctMultiTouch
                && primaryCode == LatinKeyboardView.KEYCODE_META_LEFT) {
            if (mMetaKeyState.isChording()) {
                setModMeta(false);
            }
            sendMetaKey(ic, false, true);
            mMetaKeyState.onRelease();
        } else if (distinctMultiTouch
                && primaryCode == LatinKeyboardView.KEYCODE_FN) {
            if (mFnKeyState.isChording()) {
                setModFn(false);
            }
            mFnKeyState.onRelease();
        }
        // WARNING: Adding a chording modifier key? Make sure you also
        // edit PointerTracker.isModifierInternal(), otherwise it will
        // force a release event instead of chording.
    }

    // receive ringer mode changes to detect silent mode
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateRingerMode();
        }
    };

    // update flags for silent mode
    private void updateRingerMode() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        if (mAudioManager != null) {
            mSilentMode = (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
        }
    }

    private float getKeyClickVolume() {
        if (mAudioManager == null) return 0.0f; // shouldn't happen
        
        // The volume calculations are poorly documented, this is the closest I could
        // find for explaining volume conversions:
        // http://developer.android.com/reference/android/media/MediaPlayer.html#setAuxEffectSendLevel(float)
        //
        //   Note that the passed level value is a raw scalar. UI controls should be scaled logarithmically:
        //   the gain applied by audio framework ranges from -72dB to 0dB, so an appropriate conversion 
        //   from linear UI input x to level is: x == 0 -> level = 0 0 < x <= R -> level = 10^(72*(x-R)/20/R)
        
        int method = sKeyboardSettings.keyClickMethod; // See click_method_values in strings.xml
        if (method == 0) return FX_VOLUME;
        
        float targetVol = sKeyboardSettings.keyClickVolume;

        if (method > 1) {
            // TODO(klausw): on some devices the media volume controls the click volume?
            // If that's the case, try to set a relative target volume.
            int mediaMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int mediaVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            //Log.i(TAG, "getKeyClickVolume relative, media vol=" + mediaVol + "/" + mediaMax);
            float channelVol = (float) mediaVol / mediaMax;
            if (method == 2) {
                targetVol *= channelVol;
            } else if (method == 3) {
                if (channelVol == 0) return 0.0f; // Channel is silent, won't get audio
                targetVol = Math.min(targetVol / channelVol, 1.0f); // Cap at 1.0
            }
        }
        // Set absolute volume, treating the percentage as a logarithmic control
        float vol = (float) Math.pow(10.0, FX_VOLUME_RANGE_DB * (targetVol - 1) / 20);
        //Log.i(TAG, "getKeyClickVolume absolute, target=" + targetVol + " amp=" + vol);
        return vol;
    }
    
    private void playKeyClick(int primaryCode) {
        // if mAudioManager is null, we don't have the ringer state yet
        // mAudioManager will be set by updateRingerMode
        if (mAudioManager == null) {
            if (mKeyboardSwitcher.getInputView() != null) {
                updateRingerMode();
            }
        }
        if (mSoundOn && !mSilentMode) {
            // FIXME: Volume and enable should come from UI settings
            // FIXME: These should be triggered after auto-repeat logic
            int sound = AudioManager.FX_KEYPRESS_STANDARD;
            switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                sound = AudioManager.FX_KEYPRESS_DELETE;
                break;
            case ASCII_ENTER:
                sound = AudioManager.FX_KEYPRESS_RETURN;
                break;
            case ASCII_SPACE:
                sound = AudioManager.FX_KEYPRESS_SPACEBAR;
                break;
            }
            mAudioManager.playSoundEffect(sound, getKeyClickVolume());
        }
    }

    private void vibrate() {
        if (!mVibrateOn) {
            // Even if disabled, try haptic feedback
            vibrate(-1);
            return;
        }
        vibrate(mVibrateLen);
    }

    @SuppressWarnings("deprecation")
    void vibrate(int len) {
        // App-controlled haptic feedback setting
        boolean isVibrateSettingsOn = mVibrateOn;
        if (mKeyboardSwitcher.getInputView() != null) {
            // Respect app master override: if off, disable all feedback
            mKeyboardSwitcher.getInputView().setHapticFeedbackEnabled(isVibrateSettingsOn);
        }

        if (len <= 0) {
            if (isVibrateSettingsOn && mKeyboardSwitcher.getInputView() != null) {
                // If setting is ON, force trigger vibration even if system-wide is disabled
                mKeyboardSwitcher.getInputView().performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            return;
        }

        Vibrator v = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.os.VibratorManager vm = (android.os.VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                v = vm.getDefaultVibrator();
            }
        }
        if (v == null) {
            v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(len, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate((long) len);
            }
        }
    }
    
    /* package */void promoteToUserDictionary(String word, int frequency) {
        if (mUserDictionary.isValidWord(word))
            return;
        mUserDictionary.addWord(word, frequency);
    }

    /* package */WordComposer getCurrentWord() {
        return mWord;
    }

    /* package */boolean getPopupOn() {
        return mPopupOn;
    }

    private void updateCorrectionMode() {
        mHasDictionary = mSuggest != null ? mSuggest.hasMainDictionary()
                : false;
        mAutoCorrectOn = mAutoCorrectEnabled
                && !mInputTypeNoAutoCorrect && mHasDictionary;
        mCorrectionMode = (mAutoCorrectOn && mAutoCorrectEnabled) ? Suggest.CORRECTION_FULL
                : (mAutoCorrectOn ? Suggest.CORRECTION_BASIC
                        : Suggest.CORRECTION_NONE);
        mCorrectionMode = (mBigramSuggestionEnabled && mAutoCorrectOn && mAutoCorrectEnabled) ? Suggest.CORRECTION_FULL_BIGRAM
                : mCorrectionMode;
        if (suggestionsDisabled()) {
            mAutoCorrectOn = false;
            mCorrectionMode = Suggest.CORRECTION_NONE;
        }
        if (mSuggest != null) {
            mSuggest.setCorrectionMode(mCorrectionMode);
        }
    }

    private void updateAutoTextEnabled(Locale systemLocale) {
        // AutoText removed
    }

    // Version 1: The "Simple" one that gets called by the swipe actions
    protected void launchSettings() {
        launchSettings(LatinIMESettings.class);
    }

    // Version 2: The "Worker" one that handles the Intent
    protected void launchSettings(Class<? extends android.app.Activity> settingsClass) {
        // Break the infinite loop by NOT calling handleClose() here.
        // The keyboard will naturally close itself when the new Activity takes over the screen.

        Intent intent = new Intent();
        intent.setClass(this, settingsClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void loadSettings() {
        // Get the settings preferences
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        mVibrateOn = sp.getBoolean(PREF_VIBRATE_ON, false);
        mVibrateLen = getPrefInt(sp, PREF_VIBRATE_LEN, getResources().getString(R.string.vibrate_duration_ms));
        mSoundOn = sp.getBoolean(PREF_SOUND_ON, false);
        mPopupOn = sp.getBoolean(PREF_POPUP_ON, mResources
                .getBoolean(R.bool.default_popup_preview));
        // Migration for auto_cap from boolean to String
        try {
            sp.getString(PREF_AUTO_CAP, "0");
        } catch (ClassCastException e) {
            Log.i(TAG, "Migrating auto_cap from boolean to String in LatinIME");
            boolean oldVal = sp.getBoolean(PREF_AUTO_CAP, true);
            sp.edit().remove(PREF_AUTO_CAP).commit();
            sp.edit().putString(PREF_AUTO_CAP, oldVal ? "0" : "2").commit();
        }
        mAutoCapPref = Integer.parseInt(sp.getString(PREF_AUTO_CAP, "0"));

        mShowSuggestions = sp.getBoolean(PREF_SHOW_SUGGESTIONS, mResources
                .getBoolean(R.bool.default_suggestions));

        final String voiceMode = sp.getString(PREF_VOICE_MODE,
                getString(R.string.voice_mode_main));
        boolean enableVoice = !voiceMode
                .equals(getString(R.string.voice_mode_off))
                && mEnableVoiceButton;
        boolean voiceOnPrimary = voiceMode
                .equals(getString(R.string.voice_mode_main));
        if (mKeyboardSwitcher != null
                && (enableVoice != mEnableVoice || voiceOnPrimary != mVoiceOnPrimary)) {
            mKeyboardSwitcher.setVoiceMode(enableVoice, voiceOnPrimary);
        }
        mEnableVoice = enableVoice;
        mVoiceOnPrimary = voiceOnPrimary;

        try {
            mAutoCorrectionMode = Integer.parseInt(sp.getString(PREF_AUTO_CORRECTION_MODE, "1"));
        } catch (NumberFormatException e) {
            mAutoCorrectionMode = 1;
        }
        mAutoCorrectEnabled = (mAutoCorrectionMode > 0) && mShowSuggestions;
        mAutoPunctuate = sp.getBoolean(PREF_AUTO_PUNCTUATE, true);
        // mBigramSuggestionEnabled = sp.getBoolean(
        // PREF_BIGRAM_SUGGESTIONS, true) & mShowSuggestions;
        updateCorrectionMode();
        Locale locale = ConfigurationCompat.getLocales(mResources.getConfiguration()).get(0);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        updateAutoTextEnabled(locale);
        mLanguageSwitcher.loadLocales(sp);
        mAutoCapActive = (mAutoCapPref != 2) && mLanguageSwitcher.allowAutoCap();
        mDeadKeysActive = mLanguageSwitcher.allowDeadKeys();

        mSuggestionsInLandscape = sp.getBoolean(PREF_SUGGESTIONS_IN_LANDSCAPE, 
                mResources.getBoolean(R.bool.default_suggestions_in_landscape));
        mMacrosInLandscape = sp.getBoolean(PREF_MACROS_IN_LANDSCAPE, true);
    }

    private void initSuggestPuncList() {
        mSuggestPuncList = new ArrayList<CharSequence>();
        String suggestPuncs = sKeyboardSettings.suggestedPunctuation;
        String defaultPuncs = getResources().getString(R.string.suggested_punctuations_default);
        if (suggestPuncs.equals(defaultPuncs) || suggestPuncs.equals("")) {
            // Not user-configured, load the language-specific default.
            suggestPuncs = getResources().getString(R.string.suggested_punctuations);
        }
        if (suggestPuncs != null) {
            for (int i = 0; i < suggestPuncs.length(); i++) {
                mSuggestPuncList.add(suggestPuncs.subSequence(i, i + 1));
            }
        }
        setNextSuggestions();
    }

    private boolean isSuggestedPunctuation(int code) {
        return sKeyboardSettings.suggestedPunctuation.contains(String.valueOf((char) code));
    }

    private void showOptionsMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setIcon(R.drawable.ic_dialog_keyboard);
        builder.setNegativeButton(android.R.string.cancel, null);
        CharSequence itemSettings = getString(R.string.english_ime_settings);
        CharSequence itemInputMethod = getString(R.string.selectInputMethod);
        builder.setItems(new CharSequence[] { itemInputMethod, itemSettings },
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface di, int position) {
                        di.dismiss();
                        switch (position) {
                        case POS_SETTINGS:
                            launchSettings();
                            break;
                        case POS_METHOD:
                            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                                    .showInputMethodPicker();
                            break;
                        }
                    }
                });
        builder.setTitle(mResources
                .getString(R.string.english_ime_input_options));
        mOptionsDialog = builder.create();
        Window window = mOptionsDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = mKeyboardSwitcher.getInputView().getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mOptionsDialog.show();
    }

    public void changeKeyboardMode() {
        KeyboardSwitcher switcher = mKeyboardSwitcher;
        if (switcher.isAlphabetMode()) {
            mSavedShiftState = getShiftState();
        }
        switcher.toggleSymbols();
        if (switcher.isAlphabetMode()) {
            switcher.setShiftState(mSavedShiftState);
        }

        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    @SafeVarargs
    public static <E> ArrayList<E> newArrayList(E... elements) {
        int capacity = (elements.length * 110) / 100 + 5;
        ArrayList<E> list = new ArrayList<E>(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        super.dump(fd, fout, args);

        final Printer p = new PrintWriterPrinter(fout);
        p.println("LatinIME state :");
        p.println("  Keyboard mode = " + mKeyboardSwitcher.getKeyboardMode());
        p.println("  mComposing=" + mComposing.toString());
        p.println("  mPredictionOnForMode=" + mPredictionOnForMode);
        p.println("  mCorrectionMode=" + mCorrectionMode);
        p.println("  mPredicting=" + mPredicting);
        p.println("  mAutoCorrectOn=" + mAutoCorrectOn);
        p.println("  mAutoSpace=" + mAutoSpace);
        p.println("  mCompletionOn=" + mCompletionOn);
        p.println("  TextEntryState.state=" + TextEntryState.getState());
        p.println("  mSoundOn=" + mSoundOn);
        p.println("  mVibrateOn=" + mVibrateOn);
        p.println("  mPopupOn=" + mPopupOn);
    }

    // Characters per second measurement

    private long mLastCpsTime;
    private static final int CPS_BUFFER_SIZE = 16;
    private long[] mCpsIntervals = new long[CPS_BUFFER_SIZE];
    private int mCpsIndex;
    private static Pattern NUMBER_RE = Pattern.compile("(\\d+).*");

    private void measureCps() {
        long now = System.currentTimeMillis();
        if (mLastCpsTime == 0)
            mLastCpsTime = now - 100; // Initial
        mCpsIntervals[mCpsIndex] = now - mLastCpsTime;
        mLastCpsTime = now;
        mCpsIndex = (mCpsIndex + 1) % CPS_BUFFER_SIZE;
        long total = 0;
        for (int i = 0; i < CPS_BUFFER_SIZE; i++)
            total += mCpsIntervals[i];
        System.out.println("CPS = " + ((CPS_BUFFER_SIZE * 1000f) / total));
    }

    public void onAutoCompletionStateChanged(boolean isAutoCompletion) {
        mKeyboardSwitcher.onAutoCompletionStateChanged(isAutoCompletion);
    }

    static int getIntFromString(String val, int defVal) {
        Matcher num = NUMBER_RE.matcher(val);
        if (!num.matches()) return defVal;
        return Integer.parseInt(num.group(1));
    }

    static int getPrefInt(SharedPreferences prefs, String prefName, int defVal) {
        String prefVal = prefs.getString(prefName, Integer.toString(defVal));
        //Log.i("PCKeyboard", "getPrefInt " + prefName + " = " + prefVal + ", default " + defVal);
        return getIntFromString(prefVal, defVal);
    }

    static int getPrefInt(SharedPreferences prefs, String prefName, String defStr) {
        int defVal = getIntFromString(defStr, 0);
        return getPrefInt(prefs, prefName, defVal);
    }

    static int getHeight(SharedPreferences prefs, String prefName, String defVal) {
        int val = getPrefInt(prefs, prefName, defVal);
        if (val < 15)
            val = 15;
        if (val > 75)
            val = 75;
        return val;
    }

    private class VoiceRecognitionListener implements RecognitionListener {
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(matches.get(0), 1);
                }
            }
        }
        @Override public void onReadyForSpeech(Bundle params) {}
        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}
        @Override public void onEndOfSpeech() {}
        @Override public void onError(int error) { Log.e(TAG, "Voice Error: " + error); }
        @Override public void onPartialResults(Bundle partialResults) {}
        @Override public void onEvent(int eventType, Bundle params) {}
    }
}
