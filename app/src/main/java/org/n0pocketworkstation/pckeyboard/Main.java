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
import java.util.Locale;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;
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
        try {
            InputStream is = getAssets().open("README.md");
            BufferedReader br = new BufferedReader(new java.io.InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        PCKeyboardApp.updateLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        String readmeContent = readReadme();
        String html;
        if (readmeContent != null && !readmeContent.trim().isEmpty()) {
            // Very basic Markdown to HTML conversion for display
            html = readmeContent.replace("\n", "<br/>");
            // Highlight headers
            html = html.replaceAll("(?m)^# (.*)$", "<h1>$1</h1>");
            html = html.replaceAll("(?m)^## (.*)$", "<h2>$1</h2>");
            // Bold
            html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
            // Links
            html = html.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "<a href='$2'>$1</a>");
        } else {
            html = getString(R.string.main_body);
        }

        html += "<p><i>Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")</i></p>";
        Spanned content = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY);
        TextView description = (TextView) findViewById(R.id.main_description);
        description.setMovementMethod(LinkMovementMethod.getInstance());
        description.setText(content, BufferType.SPANNABLE);


        final Button setup1 = (Button) findViewById(R.id.main_setup_btn_configure_imes);
        setup1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });

        final Button setup2 = (Button) findViewById(R.id.main_setup_btn_set_ime);
        setup2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.showInputMethodPicker();
            }
        });
        
        final Activity that = this;

        final Button setup4 = (Button) findViewById(R.id.main_setup_btn_input_lang);
        setup4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(that, InputLanguageSelection.class));
            }
        });

        final Button setup3 = (Button) findViewById(R.id.main_setup_btn_get_dicts);
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

        final Button setup5 = (Button) findViewById(R.id.main_setup_btn_settings);
        setup5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(that, LatinIMESettings.class));
            }
        });

        final Button uiLangBtn = (Button) findViewById(R.id.main_setup_btn_ui_language);
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

        testFieldsToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (testFieldsContainer.getVisibility() == View.VISIBLE) {
                    testFieldsContainer.setVisibility(View.GONE);
                    testFieldsArrow.setRotation(0);
                    testFieldsClear.setVisibility(View.GONE);
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
                if (testFieldsContainer instanceof ViewGroup) {
                    ViewGroup container = (ViewGroup) testFieldsContainer;
                    for (int i = 0; i < container.getChildCount(); i++) {
                        View child = container.getChildAt(i);
                        if (child instanceof EditText) {
                            ((EditText) child).setText("");
                            ((EditText) child).clearFocus();
                        }
                    }
                    // Request focus on the container or a neutral view to avoid jumping
                    testFieldsContainer.requestFocus();
                }
            }
        });
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

        for (int i = 0; i < languages.length; i++) {
            popup.getMenu().add(0, i, i, languages[i][0]);
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

