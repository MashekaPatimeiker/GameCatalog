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
    private static final String KEY_SORT_BY = "sort_by";
    private static final String KEY_SORT_ORDER = "sort_order";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_NOTIFICATION_HOUR = "notification_hour";
    private static final String KEY_NOTIFICATION_MINUTE = "notification_minute";

    private static final int DEFAULT_NOTIFICATION_HOUR = 9;
    private static final int DEFAULT_NOTIFICATION_MINUTE = 0;

    private static final String TAG = "PreferencesHelper";

    private SharedPreferences preferences;
    private Context context;

    public PreferencesHelper(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        checkFirstRun();
    }

    public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
        Log.d(TAG, "Notifications enabled: " + enabled);
    }

    public boolean isNotificationsEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationHour(int hour) {
        preferences.edit().putInt(KEY_NOTIFICATION_HOUR, hour).apply();
        Log.d(TAG, "Notification hour set to: " + hour);
    }

    public int getNotificationHour() {
        return preferences.getInt(KEY_NOTIFICATION_HOUR, DEFAULT_NOTIFICATION_HOUR);
    }

    public void setNotificationMinute(int minute) {
        preferences.edit().putInt(KEY_NOTIFICATION_MINUTE, minute).apply();
        Log.d(TAG, "Notification minute set to: " + minute);
    }

    public int getNotificationMinute() {
        return preferences.getInt(KEY_NOTIFICATION_MINUTE, DEFAULT_NOTIFICATION_MINUTE);
    }

    public void setNotificationTime(int hour, int minute) {
        setNotificationHour(hour);
        setNotificationMinute(minute);
    }

    public String getNotificationTimeString() {
        return String.format(Locale.getDefault(), "%02d:%02d",
                getNotificationHour(), getNotificationMinute());
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

            setNotificationHour(DEFAULT_NOTIFICATION_HOUR);
            setNotificationMinute(DEFAULT_NOTIFICATION_MINUTE);
            setNotificationsEnabled(true);

            preferences.edit().putBoolean(KEY_FIRST_RUN, false).apply();

            Log.d(TAG, "checkFirstRun: System language detected: " + systemLanguage);
            Log.d(TAG, "checkFirstRun: Default notification time set to " +
                    DEFAULT_NOTIFICATION_HOUR + ":" + DEFAULT_NOTIFICATION_MINUTE);
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

    public void resetNotificationSettings() {
        setNotificationHour(DEFAULT_NOTIFICATION_HOUR);
        setNotificationMinute(DEFAULT_NOTIFICATION_MINUTE);
        setNotificationsEnabled(true);
        Log.d(TAG, "resetNotificationSettings: Reset to default values");
    }
}