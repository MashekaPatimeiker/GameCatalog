package com.example.gamecatalog.data.api;

import com.example.gamecatalog.data.api.models.ApiGame;
import com.example.gamecatalog.data.api.models.ApiGameList;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    @GET("games")
    Call<List<ApiGame>> getGames();

    @GET("games")
    Call<List<ApiGame>> searchGames(@Query("title") String query);
}