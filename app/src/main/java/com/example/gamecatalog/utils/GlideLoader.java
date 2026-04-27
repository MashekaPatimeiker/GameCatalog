package com.example.gamecatalog.utils;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.gamecatalog.R;

public class GlideLoader {
    private static final String TAG = "GlideLoader";

    public static void loadImage(Context context, String imageUrl, ImageView imageView) {
        Log.d(TAG, "=== LOADING IMAGE ===");
        Log.d(TAG, "URL: " + imageUrl);
        Log.d(TAG, "Context: " + (context != null ? "ok" : "null"));
        Log.d(TAG, "ImageView: " + (imageView != null ? "ok" : "null"));

        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.e(TAG, "URL is null or empty!");
            imageView.setImageResource(R.drawable.ic_game_placeholder);
            return;
        }

        // Проверяем, что URL начинается с http
        if (!imageUrl.startsWith("http")) {
            Log.e(TAG, "URL doesn't start with http: " + imageUrl);
            imageView.setImageResource(R.drawable.ic_game_placeholder);
            return;
        }

        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.ic_game_placeholder)
                .error(R.drawable.ic_game_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true);

        Log.d(TAG, "Starting Glide load...");

        try {
            Glide.with(context)
                    .load(imageUrl)
                    .apply(options)
                    .into(imageView);
            Log.d(TAG, "Glide load called successfully");
        } catch (Exception e) {
            Log.e(TAG, "Glide exception: " + e.getMessage(), e);
            imageView.setImageResource(R.drawable.ic_game_placeholder);
        }
    }
}