package com.example.gamecatalog.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RealtimeSubscription {
    private static final String TAG = "RealtimeSubscription";
    private static RealtimeSubscription instance;
    private OnDatabaseChangeListener listener;
    private ExecutorService executor;
    private Handler mainHandler;
    private boolean isListening = false;
    private BufferedReader reader;
    private HttpURLConnection connection;

    // ИСПРАВЛЕННЫЙ URL - порт 8080
    private static final String SSE_URL = "http://10.0.2.2:8080/?action=subscribe";

    public interface OnDatabaseChangeListener {
        void onGameChanged(String operation, int gameId);
        void onConnectionError(String error);
    }

    private RealtimeSubscription() {
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized RealtimeSubscription getInstance() {
        if (instance == null) {
            instance = new RealtimeSubscription();
        }
        return instance;
    }

    public void subscribe(OnDatabaseChangeListener listener) {
        this.listener = listener;
        startListening();
    }

    private void startListening() {
        if (isListening) return;

        isListening = true;
        executor.execute(() -> {
            try {
                Log.d(TAG, "Connecting to: " + SSE_URL);
                URL url = new URL(SSE_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(0);
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.connect();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode != 200) {
                    throw new Exception("Server returned " + responseCode);
                }

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;

                while (isListening && (line = reader.readLine()) != null) {
                    Log.d(TAG, "SSE line: " + line);

                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        Log.d(TAG, "Notification: " + data);
                        processNotification(data);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "SSE error: " + e.getMessage());
                if (listener != null) {
                    mainHandler.post(() -> listener.onConnectionError(e.getMessage()));
                }
                tryReconnect();
            }
        });
    }

    private void processNotification(String data) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(data);
            String operation = json.optString("operation", "");
            int gameId = json.optInt("game_id", 0);

            Log.d(TAG, "📢 Изменение в БД: " + operation + " для игры " + gameId);

            if (listener != null) {
                mainHandler.post(() -> listener.onGameChanged(operation, gameId));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing: " + e.getMessage());
        }
    }

    private void tryReconnect() {
        mainHandler.postDelayed(() -> {
            if (isListening) {
                Log.d(TAG, "Reconnecting...");
                stopListening();
                startListening();
            }
        }, 5000);
    }

    public void stopListening() {
        isListening = false;
        try {
            if (reader != null) reader.close();
            if (connection != null) connection.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping: " + e.getMessage());
        }
    }
}