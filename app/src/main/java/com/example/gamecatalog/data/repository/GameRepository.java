package com.example.gamecatalog.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gamecatalog.data.api.RemoteRetrofitClient;
import com.example.gamecatalog.data.api.models.ApiGame;
import com.example.gamecatalog.data.database.GameDatabase;
import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.data.models.GameDto;
import com.example.gamecatalog.utils.FuzzySearch;
import com.example.gamecatalog.utils.NetworkUtils;
import com.example.gamecatalog.utils.PreferencesHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameRepository {

    private static final String TAG = "GameRepository";
    private static GameRepository instance;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final GameDatabase database;
    private final ExecutorService executorService;
    private final Context context;
    private final PreferencesHelper preferencesHelper;
    private final OkHttpClient okHttpClient;

    private final MutableLiveData<List<GameEntity>> gamesLiveData;
    private final MutableLiveData<String> errorLiveData;
    private final MutableLiveData<Boolean> loadingLiveData;
    private final MutableLiveData<Boolean> isOfflineLiveData;

    private static final double FUZZY_THRESHOLD = 0.65;

    private GameRepository(Context context) {
        this.context = context.getApplicationContext();
        this.database = GameDatabase.getInstance(context);
        this.executorService = Executors.newFixedThreadPool(3);
        this.preferencesHelper = new PreferencesHelper(context);
        this.okHttpClient = new OkHttpClient();

        this.gamesLiveData = new MutableLiveData<>();
        this.errorLiveData = new MutableLiveData<>();
        this.loadingLiveData = new MutableLiveData<>(false);
        this.isOfflineLiveData = new MutableLiveData<>(false);

        // Добавляем тестовые игры если БД пуста
        executorService.execute(() -> {
            if (database.gameDao().getGamesCount() == 0) {
                insertSampleGames();
            }
        });

        loadLocalGames();
    }

    private void insertSampleGames() {
        List<GameEntity> games = new ArrayList<>();
        games.add(new GameEntity("The Legend of Zelda", "Adventure", "2023-05-12", "Epic adventure game", "https://picsum.photos/id/104/200/300"));
        games.add(new GameEntity("Super Mario Odyssey", "Platformer", "2023-11-17", "Classic platformer", "https://picsum.photos/id/106/200/300"));
        games.add(new GameEntity("Elden Ring", "RPG", "2022-02-25", "Action RPG masterpiece", "https://picsum.photos/id/107/200/300"));
        games.add(new GameEntity("God of War", "Action", "2022-11-09", "Epic Norse adventure", "https://picsum.photos/id/104/200/300"));
        games.add(new GameEntity("Cyberpunk 2077", "RPG", "2020-12-10", "Open world cyberpunk", "https://picsum.photos/id/106/200/300"));

        database.gameDao().insertAllGames(games);
        Log.d(TAG, "Inserted " + games.size() + " sample games");
    }

    public static synchronized GameRepository getInstance(Context context) {
        if (instance == null) {
            instance = new GameRepository(context);
        }
        return instance;
    }

    public LiveData<List<GameEntity>> getGames() {
        return gamesLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<Boolean> getIsOffline() {
        return isOfflineLiveData;
    }

    private void loadLocalGames() {
        String sortBy = preferencesHelper.getSortBy();
        String sortOrder = preferencesHelper.getSortOrder();
        loadLocalGamesSorted(sortBy, sortOrder);
    }

    private void loadLocalGamesSorted(String sortBy, String sortOrder) {
        executorService.execute(() -> {
            try {
                List<GameEntity> games;
                String order = sortOrder.equals("asc") ? "ASC" : "DESC";

                switch (sortBy) {
                    case "title":
                        games = order.equals("ASC") ?
                                database.gameDao().getGamesSortedByTitleAsc() :
                                database.gameDao().getGamesSortedByTitleDesc();
                        break;
                    case "release_date":
                        games = order.equals("ASC") ?
                                database.gameDao().getGamesSortedByDateAsc() :
                                database.gameDao().getGamesSortedByDateDesc();
                        break;
                    case "genre":
                        games = order.equals("ASC") ?
                                database.gameDao().getGamesSortedByGenreAsc() :
                                database.gameDao().getGamesSortedByGenreDesc();
                        break;
                    default:
                        games = database.gameDao().getGamesSortedByTitleAsc();
                }

                Log.d(TAG, "Loaded " + games.size() + " games from local DB");
                gamesLiveData.postValue(games);

                boolean isOnline = NetworkUtils.isNetworkAvailable(context);
                isOfflineLiveData.postValue(!isOnline);
            } catch (Exception e) {
                Log.e(TAG, "Error loading local games: " + e.getMessage());
                errorLiveData.postValue("Error loading games: " + e.getMessage());
            }
        });
    }

    public void loadGamesSorted(String sortBy, String sortOrder) {
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);
        Log.d(TAG, "loadGamesSorted: isOnline = " + isOnline);
        isOfflineLiveData.postValue(!isOnline);

        if (isOnline) {
            Log.d(TAG, "Online mode: fetching from API");
            fetchGamesFromApiWithSort(sortBy, sortOrder);
        } else {
            Log.d(TAG, "Offline mode: loading from database");
            loadLocalGamesSorted(sortBy, sortOrder);
            errorLiveData.postValue("No internet connection. Showing cached data.");
        }
    }

    private void fetchGamesFromApiWithSort(String sortBy, String sortOrder) {
        loadingLiveData.postValue(true);

        String token = preferencesHelper.getAuthToken();
        Log.d(TAG, "Token: " + (token != null ? "exists" : "null"));

        if (token == null) {
            loadingLiveData.postValue(false);
            loadLocalGamesSorted(sortBy, sortOrder);
            return;
        }

        try {
            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8080/?action=games")
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build();

            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e(TAG, "API call failed: " + e.getMessage());
                    loadingLiveData.postValue(false);
                    loadLocalGamesSorted(sortBy, sortOrder);
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "API Response code: " + response.code());
                    Log.d(TAG, "API Response body: " + (responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody));

                    loadingLiveData.postValue(false);

                    if (response.isSuccessful()) {
                        try {
                            // Проверяем, что ответ начинается с '['
                            if (responseBody.trim().startsWith("[")) {
                                Gson gson = new Gson();
                                Type type = new TypeToken<List<GameEntity>>(){}.getType();
                                List<GameEntity> games = gson.fromJson(responseBody, type);
                                saveGamesEntitiesToDatabase(games, sortBy, sortOrder);
                            } else {
                                Log.e(TAG, "Response is not a JSON array: " + responseBody);
                                loadLocalGamesSorted(sortBy, sortOrder);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing games: " + e.getMessage());
                            loadLocalGamesSorted(sortBy, sortOrder);
                        }
                    } else {
                        Log.e(TAG, "HTTP Error: " + response.code());
                        loadLocalGamesSorted(sortBy, sortOrder);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in fetchGamesFromApiWithSort: " + e.getMessage());
            loadingLiveData.postValue(false);
            loadLocalGamesSorted(sortBy, sortOrder);
        }
    }


    private void saveGamesEntitiesToDatabase(List<GameEntity> games, String sortBy, String sortOrder) {
        executorService.execute(() -> {
            try {
                database.gameDao().deleteAllGames();
                database.gameDao().insertAllGames(games);
                Log.d(TAG, "Saved " + games.size() + " games to database");
                loadLocalGamesSorted(sortBy, sortOrder);
            } catch (Exception e) {
                Log.e(TAG, "Error saving games: " + e.getMessage(), e);
                errorLiveData.postValue("Error saving games: " + e.getMessage());
            }
        });
    }

    private void saveGamesToDatabaseWithSort(List<GameDto> games, String sortBy, String sortOrder) {
        executorService.execute(() -> {
            try {
                List<GameEntity> entities = new ArrayList<>();
                for (GameDto gameDto : games) {
                    entities.add(gameDto.toEntity());
                }
                database.gameDao().deleteAllGames();
                database.gameDao().insertAllGames(entities);
                Log.d(TAG, "Saved " + entities.size() + " games to database");
                loadLocalGamesSorted(sortBy, sortOrder);
            } catch (Exception e) {
                Log.e(TAG, "Error saving games: " + e.getMessage(), e);
                errorLiveData.postValue("Error saving games: " + e.getMessage());
            }
        });
    }

    public void loadGames() {
        String sortBy = preferencesHelper.getSortBy();
        String sortOrder = preferencesHelper.getSortOrder();
        loadGamesSorted(sortBy, sortOrder);
    }

    private List<GameEntity> sortGames(List<GameEntity> games, String sortBy, String sortOrder) {
        if (games == null || games.isEmpty()) return games;
        List<GameEntity> sortedList = new ArrayList<>(games);
        java.util.Comparator<GameEntity> comparator;

        switch (sortBy) {
            case "title":
                comparator = java.util.Comparator.comparing(
                        GameEntity::getTitle,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "release_date":
                comparator = java.util.Comparator.comparing(
                        GameEntity::getReleaseDate,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "genre":
                comparator = java.util.Comparator.comparing(
                        GameEntity::getGenre,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            default:
                comparator = java.util.Comparator.comparing(
                        GameEntity::getTitle,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }

        if (sortOrder.equals("desc")) {
            comparator = comparator.reversed();
        }
        sortedList.sort(comparator);
        return sortedList;
    }

    public void addGame(GameEntity game) {
        executorService.execute(() -> {
            try {
                List<GameEntity> existingGames = database.gameDao().getAllGames();
                for (GameEntity existing : existingGames) {
                    if (existing.getTitle().equalsIgnoreCase(game.getTitle())) {
                        errorLiveData.postValue("Game with this title already exists");
                        return;
                    }
                }

                long id = database.gameDao().insertGame(game);
                game.setId((int) id);
                sendGameToServer(game);
                loadLocalGames();
            } catch (Exception e) {
                Log.e(TAG, "Error adding game: " + e.getMessage());
                errorLiveData.postValue("Error adding game: " + e.getMessage());
            }
        });
    }

    private void sendGameToServer(GameEntity game) {
        String token = preferencesHelper.getAuthToken();
        if (token == null) return;

        try {
            JSONObject json = new JSONObject();
            json.put("title", game.getTitle());
            json.put("genre", game.getGenre());
            json.put("release_date", game.getReleaseDate());
            json.put("description", game.getDescription());
            json.put("image_path", game.getImagePath());

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8080/?action=games")
                    .addHeader("Authorization", "Bearer " + token)
                    .post(body)
                    .build();

            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    Log.e(TAG, "Failed to send game to server: " + e.getMessage());
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sending game to server: " + e.getMessage());
        }
    }

    public void updateGame(GameEntity game) {
        executorService.execute(() -> {
            try {
                database.gameDao().updateGame(game);
                loadLocalGames();
            } catch (Exception e) {
                Log.e(TAG, "Error updating game: " + e.getMessage());
                errorLiveData.postValue("Error updating game: " + e.getMessage());
            }
        });
    }

    public void deleteGame(GameEntity game) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Deleting game: " + game.getTitle() + ", ID: " + game.getId());

                String token = preferencesHelper.getAuthToken();
                if (token != null) {
                    int serverId = game.getRemoteId() != null ? game.getRemoteId().intValue() : game.getId();
                    Log.d(TAG, "Server ID for deletion: " + serverId);
                    boolean deletedFromServer = deleteFromServerSync(serverId, token);

                    if (!deletedFromServer) {
                        Log.e(TAG, "Failed to delete from server, will retry later");
                        return;
                    }
                }

                database.gameDao().deleteGame(game);
                Log.d(TAG, "Game deleted from local DB");

                loadLocalGames();

            } catch (Exception e) {
                Log.e(TAG, "Error deleting game: " + e.getMessage(), e);
                errorLiveData.postValue("Error deleting game: " + e.getMessage());
            }
        });
    }

    private boolean deleteFromServerSync(int gameId, String token) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8080/?action=games&id=" + gameId)
                    .addHeader("Authorization", "Bearer " + token)
                    .delete()
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                Log.d(TAG, "Delete response code: " + response.code());
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Delete response body: " + responseBody);

                if (response.isSuccessful()) {
                    return true;
                } else {
                    Log.e(TAG, "Server returned error: " + response.code() + " - " + responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting from server: " + e.getMessage(), e);
            return false;
        }
    }
    private void deleteFromServer(int gameId, String token) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8080/?action=games&id=" + gameId)
                    .addHeader("Authorization", "Bearer " + token)
                    .delete()
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e(TAG, "Failed to delete from server: " + e.getMessage());
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }
    private void syncFavoriteWithServer(int gameId, boolean isFavorite, OnFavoriteToggledListener listener) {
        String token = preferencesHelper.getAuthToken();
        if (token == null) return;

        try {
            String url = "http://10.0.2.2:8080/?action=favorites";
            Request.Builder requestBuilder = new Request.Builder()
                    .addHeader("Authorization", "Bearer " + token);

            Request request;
            if (isFavorite) {
                JSONObject json = new JSONObject();
                json.put("game_id", gameId);
                RequestBody body = RequestBody.create(json.toString(), JSON);
                request = requestBuilder.url(url).post(body).build();
            } else {
                request = requestBuilder.url(url + "&game_id=" + gameId).delete().build();
            }

            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    if (listener != null) listener.onError(e.getMessage());
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    response.close();
                    if (listener != null) {
                        if (response.isSuccessful()) {
                            listener.onSuccess();
                        } else {
                            listener.onError("Server error: " + response.code());
                        }
                    }
                }
            });
        } catch (Exception e) {
            if (listener != null) listener.onError(e.getMessage());
        }
    }

    public void loadFavoritesFromServer(OnFavoritesLoadedListener listener) {
        String token = preferencesHelper.getAuthToken();
        if (token == null) return;

        try {
            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8080/?action=favorites")
                    .addHeader("Authorization", "Bearer " + token)
                    .get()
                    .build();

            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    if (listener != null) listener.onError(e.getMessage());
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        try {
                            Gson gson = new Gson();
                            Type type = new TypeToken<List<GameDto>>(){}.getType();
                            List<GameDto> favorites = gson.fromJson(responseBody, type);

                            for (GameDto fav : favorites) {
                                database.gameDao().updateFavoriteStatus(fav.getId().intValue(), true);
                            }

                            if (listener != null) listener.onSuccess();
                            loadLocalGames();
                        } catch (Exception e) {
                            if (listener != null) listener.onError(e.getMessage());
                        }
                    } else {
                        if (listener != null) listener.onError("Server error: " + response.code());
                    }
                    response.close();
                }
            });
        } catch (Exception e) {
            if (listener != null) listener.onError(e.getMessage());
        }
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnServerDeleteListener {
        void onSuccess();
        void onError(String error);
    }
    public interface OnFavoritesLoadedListener {
        void onSuccess();
        void onError(String error);
    }

    public void getGameById(int id, OnGameLoadedListener listener) {
        executorService.execute(() -> {
            try {
                GameEntity game = database.gameDao().getGameById(id);
                if (listener != null) {
                    listener.onGameLoaded(game);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting game by id: " + e.getMessage());
            }
        });
    }

    public void loadGamesFiltered(String genre, String sortBy, String sortOrder) {
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);
        isOfflineLiveData.postValue(!isOnline);

        if (isOnline) {
            fetchGamesFromApiWithFilter(genre, sortBy, sortOrder);
        } else {
            loadLocalGamesFiltered(genre, sortBy, sortOrder);
        }
    }

    private void loadLocalGamesFiltered(String genre, String sortBy, String sortOrder) {
        executorService.execute(() -> {
            try {
                List<GameEntity> allGames = database.gameDao().getAllGames();
                List<GameEntity> filteredGames = new ArrayList<>();
                String lowerGenre = genre.toLowerCase();

                for (GameEntity game : allGames) {
                    if (game.getGenre() != null && game.getGenre().toLowerCase().contains(lowerGenre)) {
                        filteredGames.add(game);
                    }
                }
                gamesLiveData.postValue(sortGames(filteredGames, sortBy, sortOrder));
            } catch (Exception e) {
                Log.e(TAG, "Error loading filtered games: " + e.getMessage());
            }
        });
    }

    private void fetchGamesFromApiWithFilter(String genre, String sortBy, String sortOrder) {
        loadingLiveData.postValue(true);
        RemoteRetrofitClient.getInstance().getRemoteGameApi()
                .getGames("games", sortBy, sortOrder)
                .enqueue(new Callback<List<GameDto>>() {
                    @Override
                    public void onResponse(Call<List<GameDto>> call, Response<List<GameDto>> response) {
                        loadingLiveData.postValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            List<GameDto> filteredGames = new ArrayList<>();
                            String lowerGenre = genre.toLowerCase();
                            for (GameDto game : response.body()) {
                                if (game.getGenre() != null && game.getGenre().toLowerCase().contains(lowerGenre)) {
                                    filteredGames.add(game);
                                }
                            }
                            saveGamesToDatabaseWithFilter(filteredGames, sortBy, sortOrder);
                        } else {
                            loadLocalGamesFiltered(genre, sortBy, sortOrder);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<GameDto>> call, Throwable t) {
                        loadingLiveData.postValue(false);
                        loadLocalGamesFiltered(genre, sortBy, sortOrder);
                    }
                });
    }

    private void saveGamesToDatabaseWithFilter(List<GameDto> games, String sortBy, String sortOrder) {
        executorService.execute(() -> {
            try {
                List<GameEntity> entities = new ArrayList<>();
                for (GameDto gameDto : games) {
                    entities.add(gameDto.toEntity());
                }
                database.gameDao().deleteAllGames();
                database.gameDao().insertAllGames(entities);
                loadLocalGamesFiltered(sortBy, sortOrder, "");
            } catch (Exception e) {
                Log.e(TAG, "Error saving filtered games: " + e.getMessage());
            }
        });
    }
    public void searchGames(String query, boolean useFuzzySearch) {
        if (query == null || query.trim().isEmpty()) {
            loadGames();
            return;
        }

        if (useFuzzySearch) {
            searchGamesFuzzy(query);
        } else {
            searchGamesExact(query);
        }
    }

    public void searchGamesFuzzy(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadGames();
            return;
        }

        String trimmedQuery = query.trim();
        Log.d(TAG, "Fuzzy search for: \"" + trimmedQuery + "\"");
        searchGamesFuzzyLocal(trimmedQuery);
    }

    public void searchGamesExact(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadGames();
            return;
        }

        String trimmedQuery = query.trim().toLowerCase();
        Log.d(TAG, "Exact search for: \"" + trimmedQuery + "\"");
        searchGamesExactLocal(trimmedQuery);
    }

    private void searchGamesFuzzyLocal(String query) {
        executorService.execute(() -> {
            try {
                List<GameEntity> allGames = database.gameDao().getAllGames();
                Log.d(TAG, "Total games in DB: " + allGames.size());

                List<FuzzySearch.SearchResult> fuzzyResults = new ArrayList<>();
                String lowerQuery = query.toLowerCase();

                for (GameEntity game : allGames) {
                    if (game.getTitle() == null) continue;

                    String lowerTitle = game.getTitle().toLowerCase();
                    double score;

                    if (lowerTitle.equals(lowerQuery)) {
                        score = 1.0;
                    } else if (lowerTitle.contains(lowerQuery)) {
                        score = 0.95;
                    } else {
                        score = FuzzySearch.similarity(lowerTitle, lowerQuery);
                    }

                    if (score >= FUZZY_THRESHOLD) {
                        fuzzyResults.add(new FuzzySearch.SearchResult(game.getTitle(), score));
                    }
                }

                fuzzyResults.sort((a, b) -> Double.compare(b.score, a.score));
                Log.d(TAG, "Fuzzy search found " + fuzzyResults.size() + " games");

                List<GameEntity> searchResults = new ArrayList<>();
                for (FuzzySearch.SearchResult result : fuzzyResults) {
                    for (GameEntity game : allGames) {
                        if (game.getTitle().equals(result.text)) {
                            searchResults.add(game);
                            break;
                        }
                    }
                }

                String sortBy = preferencesHelper.getSortBy();
                String sortOrder = preferencesHelper.getSortOrder();
                List<GameEntity> sortedResults = sortGames(searchResults, sortBy, sortOrder);
                gamesLiveData.postValue(sortedResults);

            } catch (Exception e) {
                Log.e(TAG, "Error in fuzzy search: " + e.getMessage());
                errorLiveData.postValue("Search error: " + e.getMessage());
            }
        });
    }

    private void searchGamesExactLocal(String query) {
        executorService.execute(() -> {
            try {
                List<GameEntity> allGames = database.gameDao().getAllGames();
                Log.d(TAG, "Total games in DB: " + allGames.size());

                List<GameEntity> filteredResults = new ArrayList<>();
                String lowerQuery = query.toLowerCase();

                for (GameEntity game : allGames) {
                    if (game.getTitle() != null && game.getTitle().toLowerCase().contains(lowerQuery)) {
                        filteredResults.add(game);
                    }
                }

                Log.d(TAG, "Exact search found " + filteredResults.size() + " games");

                String sortBy = preferencesHelper.getSortBy();
                String sortOrder = preferencesHelper.getSortOrder();
                List<GameEntity> sortedResults = sortGames(filteredResults, sortBy, sortOrder);
                gamesLiveData.postValue(sortedResults);

            } catch (Exception e) {
                Log.e(TAG, "Error searching local games: " + e.getMessage());
                errorLiveData.postValue("Search error: " + e.getMessage());
            }
        });
    }

    public void toggleFavorite(int gameId, boolean isFavorite, OnFavoriteToggledListener listener) {
        String token = preferencesHelper.getAuthToken();
        if (token == null) {
            if (listener != null) listener.onError("Not logged in");
            return;
        }

        executorService.execute(() -> {
            try {
                String url = "http://10.0.2.2:8080/?action=favorites";
                Request.Builder requestBuilder = new Request.Builder()
                        .url(isFavorite ? url : url + "&game_id=" + gameId)
                        .addHeader("Authorization", "Bearer " + token);

                Request request;
                if (isFavorite) {
                    JSONObject json = new JSONObject();
                    json.put("game_id", gameId);
                    RequestBody body = RequestBody.create(json.toString(), JSON);
                    request = requestBuilder.post(body).build();
                } else {
                    request = requestBuilder.delete().build();
                }

                try (okhttp3.Response response = okHttpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        database.gameDao().updateFavoriteStatus(gameId, isFavorite);
                        if (listener != null) listener.onSuccess();
                    } else {
                        if (listener != null) listener.onError("Server error: " + response.code());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error toggling favorite: " + e.getMessage());
                if (listener != null) listener.onError(e.getMessage());
            }
        });
    }

    public interface OnFavoriteToggledListener {
        void onSuccess();
        void onError(String error);
    }
    public void updateSortSettings(String sortBy, String sortOrder) {
        Log.d(TAG, "Using local database only");
        loadGamesSorted(sortBy, sortOrder);
    }

    public interface OnGameLoadedListener {
        void onGameLoaded(GameEntity game);
    }
}