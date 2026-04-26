package com.example.gamecatalog.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

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

        imageView.setImageResource(placeholderResId);

        executorService.execute(() -> {
            try {
                Bitmap bitmap = downloadImage(url);
                if (bitmap != null) {
                    mainHandler.post(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image: " + e.getMessage());
            }
        });
    }

    private Bitmap downloadImage(String imageUrl) {
        HttpURLConnection connection = null;
        InputStream input = null;

        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + connection.getResponseCode());
                return null;
            }

            input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);

        } catch (IOException e) {
            Log.e(TAG, "Download failed: " + e.getMessage());
            return null;
        } finally {
            try {
                if (input != null) input.close();
                if (connection != null) connection.disconnect();
            } catch (IOException e) { }
        }
    }
}