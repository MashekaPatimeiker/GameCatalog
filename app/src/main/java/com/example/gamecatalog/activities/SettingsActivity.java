package com.example.gamecatalog.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.gamecatalog.R;

import java.util.Locale;

public class SettingsActivity extends BaseActivity {

    private Switch themeSwitch;
    private RadioGroup languageGroup;
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Button btnSortSettings = findViewById(R.id.btnSortSettings);
        btnSortSettings.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, SortSettingsActivity.class);
            startActivity(intent);
        });
        Log.d(TAG, "onCreate: Current locale = " + Locale.getDefault().getLanguage());

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        languageGroup = findViewById(R.id.languageGroup);
    }

    private void loadSettings() {
        themeSwitch.setChecked(preferencesHelper.isDarkTheme());

        String currentLanguage = preferencesHelper.getLanguage();
        Log.d(TAG, "loadSettings: Saved language = " + currentLanguage);

        if (currentLanguage.equals("ru")) {
            languageGroup.check(R.id.radioRussian);
        } else {
            languageGroup.check(R.id.radioEnglish);
        }
    }

    private void setupListeners() {
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Theme switch changed to: " + isChecked);

            preferencesHelper.setTheme(isChecked);

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            recreate();
        });

        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newLanguage = checkedId == R.id.radioRussian ? "ru" : "en";
            String oldLanguage = preferencesHelper.getLanguage();

            Log.d(TAG, "Language selection changed. Old: " + oldLanguage + ", New: " + newLanguage);

            if (!newLanguage.equals(oldLanguage)) {
                changeLanguage(newLanguage);
            }
        });
    }

    private void restartApp() {
        Log.d(TAG, "restartApp: Restarting entire application");

        Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        String newLocale = newConfig.locale.getLanguage();
        String savedLocale = preferencesHelper.getLanguage();

        Log.d(TAG, "onConfigurationChanged: newConfig locale = " + newLocale);
        Log.d(TAG, "onConfigurationChanged: saved locale = " + savedLocale);

        if (!newLocale.equals(savedLocale)) {
            Log.d(TAG, "onConfigurationChanged: Updating UI with saved locale");
            loadSettings();
        }
    }
}