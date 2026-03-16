package com.example.gamecatalog.data.api.models;

import com.google.gson.annotations.SerializedName;
import com.example.gamecatalog.data.database.entities.GameEntity;

public class ApiGame {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("genre")
    private String genre;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("short_description")
    private String shortDescription;

    @SerializedName("thumbnail")
    private String thumbnail;

    @SerializedName("platform")
    private String platform;

    @SerializedName("publisher")
    private String publisher;

    @SerializedName("developer")
    private String developer;

    // Геттеры
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public String getReleaseDate() { return releaseDate; }
    public String getShortDescription() { return shortDescription; }
    public String getThumbnail() { return thumbnail; }
    public String getPlatform() { return platform; }
    public String getPublisher() { return publisher; }
    public String getDeveloper() { return developer; }

    public GameEntity toEntity() {
        GameEntity entity = new GameEntity();
        entity.setTitle(this.title != null ? this.title : "Unknown Title");
        entity.setGenre(this.genre != null ? this.genre : "Unknown");
        entity.setReleaseDate(this.releaseDate != null ? this.releaseDate : "Unknown Date");

        String description = "";
        if (shortDescription != null) description += shortDescription + "\n";
        if (platform != null) description += "Platform: " + platform + "\n";
        if (publisher != null) description += "Publisher: " + publisher + "\n";
        if (developer != null) description += "Developer: " + developer;

        entity.setDescription(description);
        entity.setImagePath(this.thumbnail);
        entity.setApiId(String.valueOf(this.id));
        entity.setSynced(true);
        return entity;
    }
}