package com.example.gamecatalog.data.api;

import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.data.models.ApiResponse;
import com.example.gamecatalog.data.models.LoginRequest;
import com.example.gamecatalog.data.models.LoginResponse;
import com.example.gamecatalog.data.models.RegisterRequest;
import com.example.gamecatalog.data.models.RegisterResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("/?action=register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @POST("/?action=login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("/?action=games")
    Call<ApiResponse<List<GameEntity>>> getGames(
            @Query("search") String search,
            @Query("genre") String genre,
            @Query("sortBy") String sortBy,
            @Query("sortOrder") String sortOrder
    );

    @POST("/?action=games")
    Call<ApiResponse<GameEntity>> createGame(@Body GameEntity game);

    @PUT("/?action=games&id={id}")
    Call<ApiResponse<Void>> updateGame(@Path("id") int id, @Body GameEntity game);

    @DELETE("/?action=games&id={id}")
    Call<ApiResponse<Void>> deleteGame(@Path("id") int id);

    @GET("/?action=favorites")
    Call<ApiResponse<List<GameEntity>>> getFavorites();

    @POST("/?action=favorites")
    Call<ApiResponse<Void>> addToFavorites(@Body GameEntity game);

    @DELETE("/?action=favorites&game_id={id}")
    Call<ApiResponse<Void>> removeFromFavorites(@Path("id") int gameId);
}