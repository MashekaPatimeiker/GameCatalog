package com.example.gamecatalog.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.gamecatalog.utils.LocaleManager;
import com.example.gamecatalog.utils.PreferencesHelper;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    protected PreferencesHelper preferencesHelper;
    private static final String TAG = "BaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferencesHelper = new PreferencesHelper(this);

        applyTheme();

        applyLanguage();

        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Current locale = " + Locale.getDefault().getLanguage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLanguageConsistency();
    }


    private void applyTheme() {
        if (preferencesHelper.isDarkTheme()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void applyLanguage() {
        String savedLanguage = preferencesHelper.getLanguage();
        Locale locale = new Locale(savedLanguage);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        Log.d(TAG, "applyLanguage: Applied language = " + savedLanguage);
    }

    private void checkLanguageConsistency() {
        String savedLanguage = preferencesHelper.getLanguage();
        String currentLanguage = Locale.getDefault().getLanguage();

        if (!savedLanguage.equals(currentLanguage)) {
            Log.d(TAG, "checkLanguageConsistency: Language mismatch! Saved: " + savedLanguage +
                    ", Current: " + currentLanguage + " - Reapplying...");
            applyLanguage();
            recreate();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        String savedLanguage = preferencesHelper.getLanguage();
        String newConfigLanguage = newConfig.locale.getLanguage();

        Log.d(TAG, "onConfigurationChanged: newConfig language = " + newConfigLanguage);
        Log.d(TAG, "onConfigurationChanged: saved language = " + savedLanguage);
        if (!savedLanguage.equals(newConfigLanguage)) {
            applyLanguage();
        }
    }

    public void changeLanguage(String languageCode) {
        preferencesHelper.setLanguage(languageCode);

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        recreate();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        PreferencesHelper prefs = new PreferencesHelper(newBase);
        String language = prefs.getLanguage();

        Context context = LocaleManager.setLocale(newBase, language);
        super.attachBaseContext(context);

        Log.d(TAG, "attachBaseContext: Setting locale to " + language);
    }
}