/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.text.HtmlCompat;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.view.MenuItem;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;
import android.text.Html;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.content.ClipboardManager;
import android.content.ClipData;
import java.io.InputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Main extends AppCompatActivity {

    // Original Market URI for Klaus Weidner's dictionary packs
    // private final static String MARKET_URI = "market://search?q=pub:\"Klaus Weidner\"";
    private final static String MARKET_URI = "https://github.com/AnySoftKeyboard/AnySoftKeyboard/blob/main/addons/languages/PACKS.md";

    private String readReadme() {
        // Try to read README.md from the application's data directory or similar
        // Since README.md is usually at the project root, we'll look for it
        // in a few common places. On a real device, it might not be accessible
        // unless it's bundled in assets.
        
        // For now, let's assume it might be in assets or we fall back to R.string.main_body
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getAssets().open("README.md");
             BufferedReader br = new BufferedReader(new java.io.InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInputConnectionInfo();
    }

    private void updateInputConnectionInfo() {
        TextView packageText = findViewById(R.id.main_input_package_name);
        TextView typeText = findViewById(R.id.main_input_type);
        
        if (packageText == null || typeText == null) return;

        String pkg = LatinIME.sKeyboardSettings.editorPackageName;
        int inputType = LatinIME.sKeyboardSettings.editorInputType;

        if (pkg == null || pkg.isEmpty()) {
            packageText.setText("[No active connection]");
        } else {
            packageText.setText("[" + pkg + "]");
        }
        
        typeText.setText(getTechnicalInputType(inputType));
    }

    private String getTechnicalInputType(int type) {
        if (type == InputType.TYPE_NULL) return "TYPE_NULL";
        
        List<String> flags = new ArrayList<>();
        int cls = type & InputType.TYPE_MASK_CLASS;
        int variation = type & InputType.TYPE_MASK_VARIATION;
        int flagsMask = type & InputType.TYPE_MASK_FLAGS;

        if (cls == InputType.TYPE_CLASS_TEXT) {
            flags.add("TYPE_CLASS_TEXT");
            switch (variation) {
                case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS: flags.add("TYPE_TEXT_VARIATION_EMAIL_ADDRESS"); break;
                case InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT: flags.add("TYPE_TEXT_VARIATION_EMAIL_SUBJECT"); break;
                case InputType.TYPE_TEXT_VARIATION_FILTER: flags.add("TYPE_TEXT_VARIATION_FILTER"); break;
                case InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE: flags.add("TYPE_TEXT_VARIATION_LONG_MESSAGE"); break;
                case InputType.TYPE_TEXT_VARIATION_NORMAL: flags.add("TYPE_TEXT_VARIATION_NORMAL"); break;
                case InputType.TYPE_TEXT_VARIATION_PASSWORD: flags.add("TYPE_TEXT_VARIATION_PASSWORD"); break;
                case InputType.TYPE_TEXT_VARIATION_PERSON_NAME: flags.add("TYPE_TEXT_VARIATION_PERSON_NAME"); break;
                case InputType.TYPE_TEXT_VARIATION_PHONETIC: flags.add("TYPE_TEXT_VARIATION_PHONETIC"); break;
                case InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS: flags.add("TYPE_TEXT_VARIATION_POSTAL_ADDRESS"); break;
                case InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE: flags.add("TYPE_TEXT_VARIATION_SHORT_MESSAGE"); break;
                case InputType.TYPE_TEXT_VARIATION_URI: flags.add("TYPE_TEXT_VARIATION_URI"); break;
                case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD: flags.add("TYPE_TEXT_VARIATION_VISIBLE_PASSWORD"); break;
                case InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT: flags.add("TYPE_TEXT_VARIATION_WEB_EDIT_TEXT"); break;
                case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS: flags.add("TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS"); break;
                case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD: flags.add("TYPE_TEXT_VARIATION_WEB_PASSWORD"); break;
            }
            if ((flagsMask & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) flags.add("TYPE_TEXT_FLAG_AUTO_COMPLETE");
            if ((flagsMask & InputType.TYPE_TEXT_FLAG_AUTO_CORRECT) != 0) flags.add("TYPE_TEXT_FLAG_AUTO_CORRECT");
            if ((flagsMask & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) flags.add("TYPE_TEXT_FLAG_CAP_CHARACTERS");
            if ((flagsMask & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0) flags.add("TYPE_TEXT_FLAG_CAP_SENTENCES");
            if ((flagsMask & InputType.TYPE_TEXT_FLAG_CAP_WORDS) != 0) flags.add("TYPE_TEXT_FLAG_CAP_WORDS");
            if ((flagsMask & InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE) != 0) flags.add("TYPE_TEXT_FLAG_IME_MULTI_LINE");
            if ((flagsMask & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0) flags.add("TYPE_TEXT_FLAG_MULTI_LINE");
            if ((flagsMask & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) flags.add("TYPE_TEXT_FLAG_NO_SUGGESTIONS");
        } else if (cls == InputType.TYPE_CLASS_NUMBER) {
            flags.add("TYPE_CLASS_NUMBER");
            switch (variation) {
                case InputType.TYPE_NUMBER_VARIATION_NORMAL: flags.add("TYPE_NUMBER_VARIATION_NORMAL"); break;
                case InputType.TYPE_NUMBER_VARIATION_PASSWORD: flags.add("TYPE_NUMBER_VARIATION_PASSWORD"); break;
            }
            if ((flagsMask & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0) flags.add("TYPE_NUMBER_FLAG_DECIMAL");
            if ((flagsMask & InputType.TYPE_NUMBER_FLAG_SIGNED) != 0) flags.add("TYPE_NUMBER_FLAG_SIGNED");
        } else if (cls == InputType.TYPE_CLASS_PHONE) {
            flags.add("TYPE_CLASS_PHONE");
        } else if (cls == InputType.TYPE_CLASS_DATETIME) {
            flags.add("TYPE_CLASS_DATETIME");
            switch (variation) {
                case InputType.TYPE_DATETIME_VARIATION_NORMAL: flags.add("TYPE_DATETIME_VARIATION_NORMAL"); break;
                case InputType.TYPE_DATETIME_VARIATION_DATE: flags.add("TYPE_DATETIME_VARIATION_DATE"); break;
                case InputType.TYPE_DATETIME_VARIATION_TIME: flags.add("TYPE_DATETIME_VARIATION_TIME"); break;
            }
        }

        if (flags.isEmpty()) return "0x" + Integer.toHexString(type);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < flags.size(); i++) {
            if (i > 0) sb.append(" | ");
            sb.append(flags.get(i));
        }
        return sb.toString();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        PCKeyboardApp.updateLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        String readmeContent = readReadme();
        String html;
        if (readmeContent != null && !readmeContent.trim().isEmpty()) {
            String htmlContent = readmeContent;
            String syntaxColor = "#808080"; // Gray for Markdown symbols
            String headerColor = "#D19A66"; // Peach/Orange for hashes (AS style)

            // 1. Headers (Color symbols and text in AS-style)
            htmlContent = htmlContent.replaceAll("(?m)^### (.*?) ?(###)?$", "<font color='" + headerColor + "'>### <b>$1</b> $2</font>");
            htmlContent = htmlContent.replaceAll("(?m)^## (.*?) ?(##)?$", "<font color='" + headerColor + "'>## <b>$1</b> $2</font>");
            htmlContent = htmlContent.replaceAll("(?m)^# (.*?) ?(#)?$", "<font color='" + headerColor + "'># <b>$1</b> $2</font>");

            // 2. Bullet points
            htmlContent = htmlContent.replaceAll("(?m)^([ \t]*)- (.*)$", "$1<font color='" + syntaxColor + "'>-</font> $2");

            // 3. Bold and Italic (Keep symbols)
            htmlContent = htmlContent.replaceAll("\\*\\*\\*(.*?)\\*\\*\\*", "<font color='" + syntaxColor + "'>***</font><b><i>$1</i></b><font color='" + syntaxColor + "'>***</font>");
            htmlContent = htmlContent.replaceAll("\\*\\*(.*?)\\*\\*", "<font color='" + syntaxColor + "'>**</font><b>$1</b><font color='" + syntaxColor + "'>**</font>");
            htmlContent = htmlContent.replaceAll("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)", "<font color='" + syntaxColor + "'>*</font><i>$1</i><font color='" + syntaxColor + "'>*</font>");

            // 4. Inline Code
            htmlContent = htmlContent.replaceAll("`(.*?)`", "<font color='#A9B7C6' face='monospace'>$1</font>");

            // 5. Images (Convert Markdown image syntax to HTML img tag)
            // Process images BEFORE links so the link regex doesn't eat the image syntax
            // Use double quotes and ensure we don't match links
            htmlContent = htmlContent.replaceAll("!\\[(.*?)\\]\\((.*?)\\)", "<br/><img src=\"$2\" alt=\"$1\"/><br/>");

            // 6. Links (Hide URL, show blue text in gray brackets)
            // Use negative lookbehind to avoid matching image syntax if it somehow survived
            htmlContent = htmlContent.replaceAll("(?<!\\!)\\[(.*?)\\]\\((.*?)\\)",
                    "<font color='" + syntaxColor + "'>[</font><a href=\"$2\">$1</a><font color='" + syntaxColor + "'>]</font>");

            // 7. Line breaks
            html = htmlContent.replace("\n", "<br/>");
        } else {
            html = getString(R.string.main_body);
        }

        html += "<p><i>Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")</i></p>";
        
        // Use ImageGetter to load images from assets
        Spanned content = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY, new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                if (source == null) return null;
                try {
                    InputStream is = getAssets().open(source);
                    Drawable d = Drawable.createFromStream(is, source);
                    if (d != null) {
                        int intrinsicWidth = d.getIntrinsicWidth();
                        int intrinsicHeight = d.getIntrinsicHeight();
                        if (intrinsicWidth <= 0) intrinsicWidth = 500;
                        if (intrinsicHeight <= 0) intrinsicHeight = 300;

                        // Scale image to fit width while maintaining aspect ratio
                        int screenWidth = getResources().getDisplayMetrics().widthPixels - 100; // padding
                        double ratio = (double) intrinsicWidth / (double) intrinsicHeight;
                        int width = screenWidth; // Force stretch to screen width as requested
                        int height = (int) (width / ratio);
                        d.setBounds(0, 0, width, height);
                    }
                    return d;
                } catch (java.io.IOException e) {
                    return null;
                }
            }
        }, null);
        
        TextView description = findViewById(R.id.main_description);
        description.setLinkTextColor(Color.parseColor("#58a6ff"));
        description.setMovementMethod(LinkMovementMethod.getInstance());
        description.setText(content, BufferType.SPANNABLE);


        final Button setup1 = findViewById(R.id.main_setup_btn_configure_imes);
        setup1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });

        final Button setup2 = findViewById(R.id.main_setup_btn_set_ime);
        setup2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.showInputMethodPicker();
            }
        });
        
        final Activity that = this;

        final Button setup4 = findViewById(R.id.main_setup_btn_input_lang);
        setup4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(that, InputLanguageSelection.class));
            }
        });

        final Button setup3 = findViewById(R.id.main_setup_btn_get_dicts);
        setup3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI));
                try {
                	startActivity(it);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(
                            		R.string.no_market_warning), Toast.LENGTH_LONG)
                            .show();
                }
            }
        });
        // PluginManager.getPluginDictionaries(getApplicationContext()); // why?

        final Button setup5 = findViewById(R.id.main_setup_btn_settings);
        setup5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(that, LatinIMESettings.class));
            }
        });

        final Button uiLangBtn = findViewById(R.id.main_setup_btn_ui_language);
        uiLangBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageMenu(v);
            }
        });

        final View testFieldsToggle = findViewById(R.id.test_fields_toggle);
        final View testFieldsContainer = findViewById(R.id.test_fields_container);
        final View testFieldsArrow = findViewById(R.id.test_fields_arrow);
        final View testFieldsClear = findViewById(R.id.test_fields_clear);
        final View copyBtn = findViewById(R.id.main_input_copy);
        if (copyBtn != null) {
            copyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView pkgTv = findViewById(R.id.main_input_package_name);
                    TextView typeTv = findViewById(R.id.main_input_type);
                    
                    if (pkgTv == null || typeTv == null) return;

                    String data = "Package: " + pkgTv.getText() + "\nInput Type: " + typeTv.getText();
                    
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("InputConnectionDetails", data);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(Main.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        testFieldsToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (testFieldsContainer.getVisibility() == View.VISIBLE) {
                    testFieldsContainer.setVisibility(View.GONE);
                    testFieldsArrow.setRotation(0);
                    testFieldsClear.setVisibility(View.INVISIBLE);
                } else {
                    testFieldsContainer.setVisibility(View.VISIBLE);
                    testFieldsArrow.setRotation(180);
                    testFieldsClear.setVisibility(View.VISIBLE);
                }
            }
        });

        testFieldsClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Focus the toggle header to keep the ScrollView anchored here and prevent jumping
                testFieldsToggle.requestFocus();

                if (testFieldsContainer instanceof ViewGroup) {
                    ViewGroup container = (ViewGroup) testFieldsContainer;
                    // Briefly hide the container to prevent multiple layout passes and jumps during mass clearing
                    container.setVisibility(View.INVISIBLE);
                    for (int i = 0; i < container.getChildCount(); i++) {
                        View child = container.getChildAt(i);
                        if (child instanceof EditText) {
                            ((EditText) child).setText("");
                        }
                    }
                    container.setVisibility(View.VISIBLE);
                }
                
                // Small delay to allow the keyboard/UI to settle before updating connection info
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateInputConnectionInfo();
                    }
                }, 100);
            }
        });
        
        // Add listeners to test fields to update info when they get focus
        if (testFieldsContainer instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) testFieldsContainer;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof EditText) {
                    child.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (hasFocus) {
                                // Small delay to allow LatinIME to update sKeyboardSettings
                                v.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateInputConnectionInfo();
                                    }
                                }, 200);
                            }
                        }
                    });
                }
            }
        }
    }

    private void showLanguageMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        
        // Define supported UI languages (matching available translations in res/values-*)
        final String[][] languages = {
            {"System Default", ""},
            {"English", "en"},
            {"العربية", "ar"},
            {"Български", "bg"},
            {"Català", "ca"},
            {"Čeština", "cs"},
            {"Dansk", "da"},
            {"Deutsch", "de"},
            {"Ελληνικά", "el"},
            {"Español", "es"},
            {"فارسی", "fa"},
            {"Suomi", "fi"},
            {"Français", "fr"},
            {"עברית", "he"},
            {"Hrvatski", "hr"},
            {"Magyar", "hu"},
            {"Հայերեն", "hy"},
            {"Indonesia", "in"},
            {"Italiano", "it"},
            {"日本語", "ja"},
            {"한국어", "ko"},
            {"ລາວ", "lo"},
            {"Lietuvių", "lt"},
            {"Latviešu", "lv"},
            {"Norsk bokmål", "nb"},
            {"Nederlands", "nl"},
            {"Polski", "pl"},
            {"Português", "pt"},
            {"Română", "ro"},
            {"Русский", "ru"},
            {"සිංහල", "si"},
            {"Slovenčina", "sk"},
            {"Slovenščina", "sl"},
            {"Српски", "sr"},
            {"Svenska", "sv"},
            {"தமிழ்", "ta"},
            {"ไทย", "th"},
            {"Türkçe", "tr"},
            {"Українська", "uk"},
            {"Tiếng Việt", "vi"},
            {"中文 (简体)", "zh_CN"},
            {"中文 (繁體)", "zh_TW"}
        };

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String currentLang = sp.getString("pref_ui_language", "");
        // HEX Color: #FF33B5E5 (Holo Blue)
        int blueColor = 0xFF33B5E5;

        for (int i = 0; i < languages.length; i++) {
            CharSequence title = languages[i][0];
            if (languages[i][1].equals(currentLang)) {
                android.text.SpannableString s = new android.text.SpannableString(title);
                s.setSpan(new android.text.style.ForegroundColorSpan(blueColor), 0, s.length(), 0);
                title = s;
            }
            MenuItem item = popup.getMenu().add(0, i, i, title);
            item.setCheckable(true);
            if (languages[i][1].equals(currentLang)) {
                item.setChecked(true);
            }
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                String langCode = languages[id][1];
                setLocale(langCode);
                return true;
            }
        });
        popup.show();
    }

    private void setLocale(String langCode) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putString("pref_ui_language", langCode).apply();
        
        PCKeyboardApp.updateLocale(this);
        
        // Restart activity to apply changes
        recreate();
    }
}

