package com.example.gamecatalog.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.example.gamecatalog.R;

import java.io.File;

public class ImageManager {

    public static void loadImageOptimized(String imagePath, ImageView imageView) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_game_placeholder);
            return;
        }

        File imgFile = new File(imagePath);
        if (imgFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_game_placeholder);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_game_placeholder);
        }
    }
}