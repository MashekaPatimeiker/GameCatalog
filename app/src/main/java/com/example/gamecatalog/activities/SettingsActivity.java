package com.example.gamecatalog.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.gamecatalog.App;
import com.example.gamecatalog.R;
import com.example.gamecatalog.utils.PreferencesHelper;
import com.example.gamecatalog.workers.GameNotificationWorker;

public class SettingsActivity extends BaseActivity {

    private Switch themeSwitch;
    private RadioGroup languageGroup;
    private Switch switchNotifications;
    private Button btnTestNotification;
    private TimePicker timePicker;
    private PreferencesHelper preferencesHelper;
    private App app;
    private Button btnTestIn10Seconds;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        app = (App) getApplication();
        preferencesHelper = new PreferencesHelper(this);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        btnTestIn10Seconds = findViewById(R.id.btnTestIn10Seconds);
        languageGroup = findViewById(R.id.languageGroup);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnTestNotification = findViewById(R.id.btnTestNotification);
        timePicker = findViewById(R.id.timePicker);

        timePicker.setIs24HourView(true);
    }

    private void loadSettings() {
        themeSwitch.setChecked(preferencesHelper.isDarkTheme());

        String currentLanguage = preferencesHelper.getLanguage();
        if (currentLanguage.equals("ru")) {
            languageGroup.check(R.id.radioRussian);
        } else {
            languageGroup.check(R.id.radioEnglish);
        }

        boolean notificationsEnabled = preferencesHelper.isNotificationsEnabled();
        switchNotifications.setChecked(notificationsEnabled);

        int hour = preferencesHelper.getNotificationHour();
        int minute = preferencesHelper.getNotificationMinute();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
        } else {
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);
        }
    }

    private void setupListeners() {
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesHelper.setTheme(isChecked);

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            recreate();
        });
        btnTestIn10Seconds.setOnClickListener(v -> {
            app.scheduleTestNotificationIn10Seconds();
            Toast.makeText(this, "Test notification scheduled in 10 seconds", Toast.LENGTH_SHORT).show();
        });
        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newLanguage = checkedId == R.id.radioRussian ? "ru" : "en";
            String oldLanguage = preferencesHelper.getLanguage();

            if (!newLanguage.equals(oldLanguage)) {
                changeLanguage(newLanguage);
            }
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesHelper.setNotificationsEnabled(isChecked);

            if (isChecked) {
                int hour, minute;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    hour = timePicker.getHour();
                    minute = timePicker.getMinute();
                } else {
                    hour = timePicker.getCurrentHour();
                    minute = timePicker.getCurrentMinute();
                }

                preferencesHelper.setNotificationTime(hour, minute);

                app.cancelNotifications();
                app.scheduleNotifications(hour, minute);
                Toast.makeText(this, R.string.notifications_enabled, Toast.LENGTH_SHORT).show();
            } else {
                app.cancelNotifications();
                Toast.makeText(this, R.string.notifications_disabled, Toast.LENGTH_SHORT).show();
            }
        });

        timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            if (switchNotifications.isChecked()) {
                preferencesHelper.setNotificationTime(hourOfDay, minute);
                app.cancelNotifications();
                app.scheduleNotifications(hourOfDay, minute);
                Toast.makeText(this,
                        String.format(getString(R.string.notification_time_set), hourOfDay, minute),
                        Toast.LENGTH_SHORT).show();
            } else {
                preferencesHelper.setNotificationTime(hourOfDay, minute);
            }
        });

        btnTestNotification.setOnClickListener(v -> {
            GameNotificationWorker.showTestNotification(this);
            Toast.makeText(this, R.string.test_notification_sent, Toast.LENGTH_SHORT).show();
        });
    }

    public void changeLanguage(String languageCode) {
        preferencesHelper.setLanguage(languageCode);

        Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}