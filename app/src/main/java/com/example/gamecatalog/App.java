package com.example.gamecatalog;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.gamecatalog.utils.LocaleManager;
import com.example.gamecatalog.utils.PreferencesHelper;

import java.util.Locale;

public class App extends Application {

    private static final String TAG = "App";
    private static App instance;
    private PreferencesHelper preferencesHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        preferencesHelper = new PreferencesHelper(this);

        Log.d(TAG, "onCreate: Application started");
        Log.d(TAG, "onCreate: Default locale = " + Locale.getDefault().getLanguage());
        Log.d(TAG, "onCreate: Saved language = " + preferencesHelper.getLanguage());
    }

    @Override
    protected void attachBaseContext(Context base) {
        PreferencesHelper prefs = new PreferencesHelper(base);
        String language = prefs.getLanguage();

        Log.d(TAG, "attachBaseContext: Setting base context with language: " + language);

        super.attachBaseContext(LocaleManager.setLocale(base, language));
    }

    public static App getInstance() {
        return instance;
    }

    public PreferencesHelper getPreferencesHelper() {
        return preferencesHelper;
    }
}