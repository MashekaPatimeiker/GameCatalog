package com.example.gamecatalog.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
    private static final String TAG = "ImageLoader";
    private static ImageLoader instance;
    private ExecutorService executorService;
    private Handler mainHandler;

    private ImageLoader() {
        this.executorService = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ImageLoader getInstance() {
        if (instance == null) {
            instance = new ImageLoader();
        }
        return instance;
    }

    public void loadImage(String url, ImageView imageView, int placeholderResId) {
        if (url == null || url.isEmpty()) {
            imageView.setImageResource(placeholderResId);
            return;
        }

        // Устанавливаем заглушку сразу
        imageView.setImageResource(placeholderResId);

        executorService.execute(() -> {
            try {
                // Проверяем, что URL начинается с http
                String imageUrl = url;
                if (!url.startsWith("http")) {
                    // Если это локальный путь, пробуем загрузить из файла
                    loadImageFromFile(imageUrl, imageView, placeholderResId);
                    return;
                }

                Bitmap bitmap = downloadImage(imageUrl);
                if (bitmap != null) {
                    mainHandler.post(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image: " + e.getMessage());
            }
        });
    }

    private void loadImageFromFile(String path, ImageView imageView, int placeholderResId) {
        try {
            java.io.File file = new java.io.File(path);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                if (bitmap != null) {
                    mainHandler.post(() -> imageView.setImageBitmap(bitmap));
                    return;
                }
            }
            mainHandler.post(() -> imageView.setImageResource(placeholderResId));
        } catch (Exception e) {
            Log.e(TAG, "Error loading from file: " + e.getMessage());
            mainHandler.post(() -> imageView.setImageResource(placeholderResId));
        }
    }

    private Bitmap downloadImage(String imageUrl) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + connection.getResponseCode());
                return null;
            }

            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "Download error: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}