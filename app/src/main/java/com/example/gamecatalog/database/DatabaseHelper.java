package com.example.gamecatalog.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.gamecatalog.models.Game;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "GameCatalog.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_GAMES = "games";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_GENRE = "genre";
    private static final String COLUMN_RELEASE_DATE = "release_date";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_IMAGE_PATH = "image_path";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_GAMES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_GENRE + " TEXT,"
                + COLUMN_RELEASE_DATE + " TEXT,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_IMAGE_PATH + " TEXT" + ")";
        db.execSQL(createTable);

        addSampleData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GAMES);
        onCreate(db);
    }

    private void addSampleData(SQLiteDatabase db) {
        Object[][] sampleGames = {
                {"The Witcher 3", "RPG", "2015", "Epic fantasy RPG", null},
                {"Minecraft", "Sandbox", "2011", "Build and explore", null}
        };

        for (Object[] game : sampleGames) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, (String) game[0]);
            values.put(COLUMN_GENRE, (String) game[1]);
            values.put(COLUMN_RELEASE_DATE, (String) game[2]);
            values.put(COLUMN_DESCRIPTION, (String) game[3]);
            values.put(COLUMN_IMAGE_PATH, (String) game[4]);
            db.insert(TABLE_GAMES, null, values);
        }
    }

    public long addGame(Game game) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, game.getTitle());
        values.put(COLUMN_GENRE, game.getGenre());
        values.put(COLUMN_RELEASE_DATE, game.getReleaseDate());
        values.put(COLUMN_DESCRIPTION, game.getDescription());
        values.put(COLUMN_IMAGE_PATH, game.getImagePath());

        long id = db.insert(TABLE_GAMES, null, values);
        db.close();
        return id;
    }

    public List<Game> getAllGames() {
        List<Game> gameList = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_GAMES + " ORDER BY " + COLUMN_TITLE;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Game game = new Game(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENRE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RELEASE_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH))
                );
                gameList.add(game);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return gameList;
    }

    public int updateGame(Game game) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, game.getTitle());
        values.put(COLUMN_GENRE, game.getGenre());
        values.put(COLUMN_RELEASE_DATE, game.getReleaseDate());
        values.put(COLUMN_DESCRIPTION, game.getDescription());
        values.put(COLUMN_IMAGE_PATH, game.getImagePath());

        return db.update(TABLE_GAMES, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(game.getId())});
    }

    public void deleteGame(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GAMES, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    public Game getGame(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_GAMES, null, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);

        Game game = null;
        if (cursor != null && cursor.moveToFirst()) {
            game = new Game(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENRE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RELEASE_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH))
            );
            cursor.close();
        }
        db.close();
        return game;
    }
}