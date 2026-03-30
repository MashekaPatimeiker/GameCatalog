package com.example.gamecatalog.workers;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.gamecatalog.R;
import com.example.gamecatalog.activities.MainActivity;

public class GameNotificationWorker extends Worker {

    public static final String CHANNEL_ID = "game_catalog_channel";
    public static final String CHANNEL_NAME = "Game Catalog";
    public static final String CHANNEL_DESCRIPTION = "Daily game catalog notifications";

    private static final String TAG = "GameNotificationWorker";
    private static final int NOTIFICATION_ID = 1001;

    public GameNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        Log.d(TAG, "Worker CREATED at: " + System.currentTimeMillis());
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "doWork: WORKER IS RUNNING at: " + System.currentTimeMillis());
        Log.d(TAG, "========================================");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Notification permission not granted");
                return Result.failure();
            }
        }

        showNotification();
        return Result.success();
    }

    private void showNotification() {
        Log.d(TAG, "showNotification: Creating notification");

        NotificationManager manager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager == null) {
            Log.e(TAG, "NotificationManager is null");
            return;
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        androidx.core.app.TaskStackBuilder stackBuilder = androidx.core.app.TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(intent);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(getApplicationContext().getString(R.string.notification_title))
                .setContentText(getApplicationContext().getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(stackBuilder.getPendingIntent(0,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                                android.app.PendingIntent.FLAG_IMMUTABLE))
                .build();

        manager.notify(NOTIFICATION_ID, notification);
        Log.d(TAG, "Notification SHOWN successfully at: " + System.currentTimeMillis());
    }

    public static void showTestNotification(Context context) {
        Log.d(TAG, "showTestNotification: Sending test notification");

        NotificationManager manager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager == null) {
            Log.e(TAG, "NotificationManager is null");
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        androidx.core.app.TaskStackBuilder stackBuilder = androidx.core.app.TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(intent);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.test_notification_title))
                .setContentText(context.getString(R.string.test_notification_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(stackBuilder.getPendingIntent(0,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                                android.app.PendingIntent.FLAG_IMMUTABLE))
                .build();

        manager.notify(NOTIFICATION_ID + 1, notification);
        Log.d(TAG, "Test notification shown");
    }
}