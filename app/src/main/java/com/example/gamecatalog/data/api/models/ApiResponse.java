package com.example.gamecatalog.data.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiResponse {


    private List<ApiGame> games;

    public List<ApiGame> getGames() {
        return games;
    }

    public void setGames(List<ApiGame> games) {
        this.games = games;
    }

    // Для совместимости с существующим кодом
    public List<ApiGame> getResults() {
        return games;
    }
}