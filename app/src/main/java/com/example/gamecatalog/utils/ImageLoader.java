package com.example.gamecatalog.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
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
        this.executorService = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ImageLoader getInstance() {
        if (instance == null) {
            instance = new ImageLoader();
        }
        return instance;
    }

    public void loadImage(String url, ImageView imageView, int placeholderResId) {
        imageView.setImageResource(placeholderResId);

        if (url == null || url.isEmpty()) {
            return;
        }

        executorService.execute(() -> {
            try {
                Bitmap bitmap = downloadImage(url);
                if (bitmap != null) {
                    mainHandler.post(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private Bitmap downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.connect();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return null;
        }

        InputStream input = connection.getInputStream();
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        input.close();
        connection.disconnect();

        return bitmap;
    }
}