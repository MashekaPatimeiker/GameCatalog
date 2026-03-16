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

    public void loadImage(String url, ImageView imageView, int placeholderResId, OnImageLoadedListener listener) {
        imageView.setImageResource(placeholderResId);

        if (url == null || url.isEmpty()) {
            if (listener != null) {
                listener.onError("URL is empty");
            }
            return;
        }

        executorService.execute(() -> {
            try {
                Bitmap bitmap = downloadImage(url);
                if (bitmap != null) {
                    mainHandler.post(() -> {
                        imageView.setImageBitmap(bitmap);
                        if (listener != null) {
                            listener.onLoaded();
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onError("Failed to download image");
                        }
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                });
            }
        });
    }

    private Bitmap downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.connect();

        InputStream input = connection.getInputStream();
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        input.close();
        connection.disconnect();

        return bitmap;
    }

    public interface OnImageLoadedListener {
        void onLoaded();
        void onError(String error);
    }
}