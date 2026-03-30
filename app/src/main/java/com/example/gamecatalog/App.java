package com.example.gamecatalog;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.gamecatalog.utils.LocaleManager;
import com.example.gamecatalog.utils.PreferencesHelper;
import com.example.gamecatalog.workers.GameNotificationWorker;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class App extends Application {

    private static final String TAG = "App";
    private static App instance;
    private PreferencesHelper preferencesHelper;

    private static final String NOTIFICATION_WORK_NAME = "game_notification_work";

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        preferencesHelper = new PreferencesHelper(this);

        Log.d(TAG, "onCreate: Application started");

        createNotificationChannel();
        scheduleNotificationsIfEnabled();
    }

    @Override
    protected void attachBaseContext(Context base) {
        PreferencesHelper prefs = new PreferencesHelper(base);
        String language = prefs.getLanguage();
        super.attachBaseContext(LocaleManager.setLocale(base, language));
    }

    public static App getInstance() {
        return instance;
    }

    public PreferencesHelper getPreferencesHelper() {
        return preferencesHelper;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    GameNotificationWorker.CHANNEL_ID,
                    GameNotificationWorker.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(GameNotificationWorker.CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created with importance HIGH");
            }
        }
    }

    public void scheduleNotificationsIfEnabled() {
        boolean notificationsEnabled = preferencesHelper.isNotificationsEnabled();
        Log.d(TAG, "scheduleNotificationsIfEnabled: enabled = " + notificationsEnabled);

        if (notificationsEnabled) {
            int hour = preferencesHelper.getNotificationHour();
            int minute = preferencesHelper.getNotificationMinute();
            scheduleNotifications(hour, minute);
        } else {
            cancelNotifications();
        }
    }

    public void scheduleNotifications(int hour, int minute) {
        long initialDelay = calculateInitialDelay(hour, minute);

        Log.d(TAG, "========================================");
        Log.d(TAG, "SCHEDULING NOTIFICATION");
        Log.d(TAG, "Target time: " + hour + ":" + minute);
        Log.d(TAG, "Initial delay: " + initialDelay + " ms");
        Log.d(TAG, "Delay in minutes: " + (initialDelay / 1000 / 60) + " min");
        Log.d(TAG, "========================================");

        cancelNotifications();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(GameNotificationWorker.class)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                NOTIFICATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );

        Log.d(TAG, "WorkManager enqueued with ID: " + workRequest.getId());
    }

    public void cancelNotifications() {
        WorkManager.getInstance(this).cancelUniqueWork(NOTIFICATION_WORK_NAME);
        Log.d(TAG, "All notifications cancelled");
    }

    private long calculateInitialDelay(int targetHour, int targetMinute) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();

        target.set(Calendar.HOUR_OF_DAY, targetHour);
        target.set(Calendar.MINUTE, targetMinute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        Log.d(TAG, "Current time: " + now.getTime());
        Log.d(TAG, "Target time: " + target.getTime());

        return target.getTimeInMillis() - now.getTimeInMillis();
    }

    public String getNextNotificationTime() {
        int hour = preferencesHelper.getNotificationHour();
        int minute = preferencesHelper.getNotificationMinute();

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        return String.format(Locale.getDefault(), "%02d:%02d",
                target.get(Calendar.HOUR_OF_DAY),
                target.get(Calendar.MINUTE));
    }

    public void scheduleTestNotificationIn10Seconds() {
        Log.d(TAG, "Scheduling test notification in 10 seconds");
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(GameNotificationWorker.class)
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(this).enqueue(workRequest);
    }
}