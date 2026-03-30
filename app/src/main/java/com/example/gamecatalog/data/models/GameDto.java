package com.example.gamecatalog.data.models;

import com.example.gamecatalog.data.database.entities.GameEntity;

public class GameDto {
    private Long id;
    private String title;
    private String genre;
    private String releaseDate;
    private String description;
    private String imageUrl;

    public GameDto() {}

    public static GameDto fromEntity(GameEntity entity) {
        GameDto dto = new GameDto();
        dto.setId(entity.getRemoteId());
        dto.setTitle(entity.getTitle());
        dto.setGenre(entity.getGenre());
        dto.setReleaseDate(entity.getReleaseDate());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImagePath());
        return dto;
    }

    public GameEntity toEntity() {
        GameEntity entity = new GameEntity();
        entity.setTitle(this.title);
        entity.setGenre(this.genre);
        entity.setReleaseDate(this.releaseDate);
        entity.setDescription(this.description);
        entity.setImagePath(this.imageUrl);
        entity.setRemoteId(this.id);
        entity.setSynced(true);
        return entity;
    }

    // геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
