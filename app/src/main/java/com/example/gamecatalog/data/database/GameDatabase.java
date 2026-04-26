package com.example.gamecatalog.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.gamecatalog.data.database.entities.GameEntity;

// Увеличьте версию с 2 на 3
@Database(entities = {GameEntity.class}, version = 3, exportSchema = false)
public abstract class GameDatabase extends RoomDatabase {

    private static volatile GameDatabase INSTANCE;
    private static final String DATABASE_NAME = "game_catalog_db";

    public abstract GameDao gameDao();

    public static GameDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (GameDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    GameDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration()  // Это пересоздаст БД при изменении версии
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}