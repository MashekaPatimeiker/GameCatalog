package com.example.gamecatalog.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.gamecatalog.R;
import com.example.gamecatalog.data.database.entities.GameEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SocialShareHelper {

    public static void shareGame(Context context, GameEntity game, String imagePath) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String shareText = String.format(
                "🎮 Check out this game: %s\n\n" +
                        "Genre: %s\n" +
                        "Release Date: %s\n\n" +
                        "%s\n\n" +
                        "Shared via Game Catalog App",
                game.getTitle(),
                game.getGenre(),
                game.getReleaseDate(),
                game.getDescription() != null ? game.getDescription().substring(0, Math.min(100, game.getDescription().length())) : ""
        );

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, game.getTitle());

        context.startActivity(Intent.createChooser(shareIntent,
                context.getString(R.string.share_via)));
    }

    public static void shareGameWithImage(Context context, GameEntity game, String imagePath) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        if (imagePath != null && new File(imagePath).exists()) {
            shareIntent.setType("image/*");
            try {
                Uri imageUri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".fileprovider",
                        new File(imagePath));
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                shareIntent.setType("text/plain");
            }
        } else {
            shareIntent.setType("text/plain");
        }

        String shareText = String.format(
                "🎮 %s\nGenre: %s\n\nCheck it out on Game Catalog!",
                game.getTitle(),
                game.getGenre()
        );

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        context.startActivity(Intent.createChooser(shareIntent,
                context.getString(R.string.share_via)));
    }
}