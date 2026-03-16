package com.example.gamecatalog.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import java.util.Locale;

public class PreferencesHelper {
    private static final String PREF_NAME = "GameCatalogPrefs";
    private static final String KEY_THEME = "theme";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_FIRST_RUN = "first_run";

    private static final String TAG = "PreferencesHelper";
    private static final String KEY_SORT_BY = "sort_by";
    private static final String KEY_SORT_ORDER = "sort_order";
    private SharedPreferences preferences;
    private Context context;

    public PreferencesHelper(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        checkFirstRun();
    }
    public void setSortBy(String sortBy) {
        preferences.edit().putString(KEY_SORT_BY, sortBy).apply();
        Log.d(TAG, "Sort by set to: " + sortBy);
    }
    public String getSortBy() {
        return preferences.getString(KEY_SORT_BY, "title");
    }
    public void setSortOrder(String sortOrder) {
        preferences.edit().putString(KEY_SORT_ORDER, sortOrder).apply();
        Log.d(TAG, "Sort order set to: " + sortOrder);
    }
    public String getSortOrder() {
        return preferences.getString(KEY_SORT_ORDER, "asc");
    }
    public String getSortOrderSql() {
        String order = getSortOrder();
        return order.equals("desc") ? "DESC" : "ASC";
    }
    private void checkFirstRun() {
        boolean isFirstRun = preferences.getBoolean(KEY_FIRST_RUN, true);
        if (isFirstRun) {
            Log.d(TAG, "checkFirstRun: First application run");

            String systemLanguage = Locale.getDefault().getLanguage();

            if (!systemLanguage.equals("ru") && !systemLanguage.equals("en")) {
                systemLanguage = "en";
            }

            setLanguage(systemLanguage);

            preferences.edit().putBoolean(KEY_FIRST_RUN, false).apply();

            Log.d(TAG, "checkFirstRun: System language detected: " + systemLanguage);
        }
    }


    public void setTheme(boolean isDark) {
        preferences.edit().putBoolean(KEY_THEME, isDark).apply();
        Log.d(TAG, "setTheme: Theme set to " + (isDark ? "dark" : "light"));
    }

    public boolean isDarkTheme() {
        return preferences.getBoolean(KEY_THEME, false);
    }


    public void setLanguage(String languageCode) {
        String oldLanguage = getLanguage();
        preferences.edit().putString(KEY_LANGUAGE, languageCode).apply();

        Log.d(TAG, "setLanguage: Language changed from " + oldLanguage + " to " + languageCode);

        applyLocale(languageCode);
    }

    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE, "en");
    }

    private void applyLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        Log.d(TAG, "applyLocale: Applied locale: " + languageCode);
    }

    public void clearPreferences() {
        preferences.edit().clear().apply();
        Log.d(TAG, "clearPreferences: All preferences cleared");
    }
}