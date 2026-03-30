package com.example.gamecatalog.data.api;

import com.example.gamecatalog.data.models.GameDto;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface RemoteGameApi {

    @GET("api/games")
    Call<List<GameDto>> getGames(
            @Query("search") String search,
            @Query("genre") String genre,
            @Query("sortBy") String sortBy,
            @Query("sortOrder") String sortOrder
    );

    @GET("api/games/{id}")
    Call<GameDto> getGameById(@Path("id") long id);

    @POST("api/games")
    Call<GameDto> createGame(@Body GameDto game);

    @PUT("api/games/{id}")
    Call<GameDto> updateGame(@Path("id") long id, @Body GameDto game);

    @DELETE("api/games/{id}")
    Call<Void> deleteGame(@Path("id") long id);

    @Multipart
    @POST("api/games/upload-image")
    Call<Map<String, String>> uploadImage(@Part MultipartBody.Part file);
}