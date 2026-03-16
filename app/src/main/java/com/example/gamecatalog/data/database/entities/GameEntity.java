package com.example.gamecatalog.data.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "games")
public class GameEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "genre")
    private String genre;

    @ColumnInfo(name = "release_date")
    private String releaseDate;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "image_path")
    private String imagePath; // Может быть URL или локальным путем

    @ColumnInfo(name = "api_id")
    private String apiId;

    @ColumnInfo(name = "is_synced")
    private boolean isSynced;

    // Конструктор по умолчанию (обязателен для Room)
    public GameEntity() {}

    // Конструктор для создания новой игры (помечаем @Ignore)
    @Ignore
    public GameEntity(String title, String genre, String releaseDate, String description, String imagePath) {
        this.title = title;
        this.genre = genre;
        this.releaseDate = releaseDate;
        this.description = description;
        this.imagePath = imagePath;
        this.isSynced = false;
    }

    // Конструктор с полными параметрами (если нужен)
    @Ignore
    public GameEntity(int id, String title, String genre, String releaseDate, String description,
                      String imagePath, String apiId, boolean isSynced) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.releaseDate = releaseDate;
        this.description = description;
        this.imagePath = imagePath;
        this.apiId = apiId;
        this.isSynced = isSynced;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getApiId() { return apiId; }
    public void setApiId(String apiId) { this.apiId = apiId; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { this.isSynced = synced; }
}