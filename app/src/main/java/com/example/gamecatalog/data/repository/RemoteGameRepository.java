package com.example.gamecatalog.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.gamecatalog.data.api.RemoteGameApi;
import com.example.gamecatalog.data.api.RemoteRetrofitClient;
import com.example.gamecatalog.data.database.GameDatabase;
import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.data.models.GameDto;
import com.example.gamecatalog.utils.NetworkUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RemoteGameRepository {
    private static final String TAG = "RemoteRepository";
    private RemoteGameApi api;
    private GameDatabase database;
    private ExecutorService executor;
    private Context context;

    public RemoteGameRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RemoteRetrofitClient.getInstance().getRemoteGameApi();
        this.database = GameDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }


    public void fetchGames(String search, String genre, String sortBy, String sortOrder, Callback<List<GameDto>> callback) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "No network, skip fetch");
            callback.onFailure(null, new Exception("No internet"));
            return;
        }
        api.getGames("games", sortBy, sortOrder).enqueue(callback);
    }

    public void syncAllGames(String search, String genre, String sortBy, String sortOrder) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "No network, skip sync");
            return;
        }
        api.getGames("games", sortBy, sortOrder).enqueue(new Callback<List<GameDto>>() {
            @Override
            public void onResponse(Call<List<GameDto>> call, Response<List<GameDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<GameDto> games = response.body();
                    executor.execute(() -> {
                        database.gameDao().deleteAllGames();
                        for (GameDto dto : games) {
                            database.gameDao().insertGame(dto.toEntity());
                        }
                        Log.d(TAG, "Synced " + games.size() + " games");
                    });
                } else {
                    Log.e(TAG, "Sync failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<GameDto>> call, Throwable t) {
                Log.e(TAG, "Sync error", t);
            }
        });
    }

    public void sendGame(GameEntity game, Callback<GameDto> callback) {
        GameDto dto = GameDto.fromEntity(game);
        Call<GameDto> call;
        if (game.getRemoteId() != null && game.getRemoteId() != 0) {
            call = api.updateGame(game.getRemoteId(), dto);
        } else {
            call = api.createGame(dto);
        }
        call.enqueue(callback);
    }

    public void deleteGameOnServer(long remoteId, Callback<Void> callback) {
        api.deleteGame(remoteId).enqueue(callback);
    }

    public void uploadImage(byte[] imageData, Callback<Map<String, String>> callback) {
        RequestBody reqBody = RequestBody.create(MediaType.parse("image/*"), imageData);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", "image.jpg", reqBody);
        api.uploadImage(part).enqueue(callback);
    }
}