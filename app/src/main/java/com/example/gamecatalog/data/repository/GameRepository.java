package com.example.gamecatalog.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gamecatalog.data.api.RetrofitClient;
import com.example.gamecatalog.data.api.models.ApiGame;
import com.example.gamecatalog.data.database.GameDatabase;
import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.utils.NetworkUtils;
import com.example.gamecatalog.utils.PreferencesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameRepository {

    private static final String TAG = "GameRepository";
    private static GameRepository instance;

    private GameDatabase database;
    private ExecutorService executorService;
    private Context context;
    private PreferencesHelper preferencesHelper;

    private MutableLiveData<List<GameEntity>> gamesLiveData;
    private MutableLiveData<String> errorLiveData;
    private MutableLiveData<Boolean> loadingLiveData;
    private MutableLiveData<Boolean> isOfflineLiveData;

    private GameRepository(Context context) {
        this.context = context.getApplicationContext();
        this.database = GameDatabase.getInstance(context);
        this.executorService = Executors.newFixedThreadPool(3);
        this.preferencesHelper = new PreferencesHelper(context);

        this.gamesLiveData = new MutableLiveData<>();
        this.errorLiveData = new MutableLiveData<>();
        this.loadingLiveData = new MutableLiveData<>(false);
        this.isOfflineLiveData = new MutableLiveData<>(false);

        loadLocalGames();
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

                if (sortBy.equals("title")) {
                    if (sortOrder.equals("asc")) {
                        games = database.gameDao().getGamesSortedByTitleAsc();
                    } else {
                        games = database.gameDao().getGamesSortedByTitleDesc();
                    }
                } else if (sortBy.equals("release_date")) {
                    if (sortOrder.equals("asc")) {
                        games = database.gameDao().getGamesSortedByDateAsc();
                    } else {
                        games = database.gameDao().getGamesSortedByDateDesc();
                    }
                } else if (sortBy.equals("genre")) {
                    if (sortOrder.equals("asc")) {
                        games = database.gameDao().getGamesSortedByGenreAsc();
                    } else {
                        games = database.gameDao().getGamesSortedByGenreDesc();
                    }
                } else {
                    games = database.gameDao().getGamesSortedByTitleAsc();
                }

                Log.d(TAG, "Loaded " + games.size() + " games from local DB with sort: " + sortBy + " " + sortOrder);
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
        Log.d(TAG, "loadGamesSorted: isOnline = " + isOnline + ", sortBy = " + sortBy + ", order = " + sortOrder);

        isOfflineLiveData.postValue(!isOnline);

        if (isOnline) {
            Log.d(TAG, "Online mode: fetching from API with sort");
            fetchGamesFromApiWithSort(sortBy, sortOrder);
        } else {
            Log.d(TAG, "Offline mode: loading from database with sort");
            loadLocalGamesSorted(sortBy, sortOrder);
            errorLiveData.postValue("No internet connection. Showing cached data.");
        }
    }

    private void fetchGamesFromApiWithSort(String sortBy, String sortOrder) {
        loadingLiveData.postValue(true);

        RetrofitClient.getInstance().getApiService()
                .getGames()
                .enqueue(new Callback<List<ApiGame>>() {
                    @Override
                    public void onResponse(Call<List<ApiGame>> call, Response<List<ApiGame>> response) {
                        loadingLiveData.postValue(false);

                        if (response.isSuccessful()) {
                            List<ApiGame> apiGames = response.body();
                            Log.d(TAG, "API Response: received " +
                                    (apiGames != null ? apiGames.size() : 0) + " games");

                            if (apiGames != null && !apiGames.isEmpty()) {
                                saveApiGamesToDatabaseWithSort(apiGames, sortBy, sortOrder);
                            } else {
                                Log.w(TAG, "API returned empty list");
                                errorLiveData.postValue("No games received from API");
                                loadLocalGamesSorted(sortBy, sortOrder);
                            }
                        } else {
                            String errorMsg = "API Error: " + response.code();
                            Log.e(TAG, errorMsg);
                            errorLiveData.postValue(errorMsg);
                            loadLocalGamesSorted(sortBy, sortOrder);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ApiGame>> call, Throwable t) {
                        loadingLiveData.postValue(false);
                        String errorMsg = "Network Error: " + t.getMessage();
                        Log.e(TAG, errorMsg, t);
                        errorLiveData.postValue(errorMsg);
                        loadLocalGamesSorted(sortBy, sortOrder);
                    }
                });
    }

    private void saveApiGamesToDatabaseWithSort(List<ApiGame> apiGames, String sortBy, String sortOrder) {
        executorService.execute(() -> {
            try {
                database.gameDao().deleteAllGames();

                List<GameEntity> entities = new ArrayList<>();
                for (ApiGame apiGame : apiGames) {
                    entities.add(apiGame.toEntity());
                }

                database.gameDao().insertAllGames(entities);
                Log.d(TAG, "Saved " + entities.size() + " new games to database");

                loadLocalGamesSorted(sortBy, sortOrder);

            } catch (Exception e) {
                Log.e(TAG, "Error saving to database: " + e.getMessage(), e);
                errorLiveData.postValue("Error saving games: " + e.getMessage());
            }
        });
    }

    public void loadGames() {
        String sortBy = preferencesHelper.getSortBy();
        String sortOrder = preferencesHelper.getSortOrder();
        loadGamesSorted(sortBy, sortOrder);
    }
    public void searchGames(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadGames();
            return;
        }

        String trimmedQuery = query.trim();
        Log.d(TAG, "Searching for: \"" + trimmedQuery + "\"");

        String sortBy = preferencesHelper.getSortBy();
        String sortOrder = preferencesHelper.getSortOrder();

        boolean isOnline = NetworkUtils.isNetworkAvailable(context);

        if (isOnline) {
            searchGamesOnline(trimmedQuery, sortBy, sortOrder);
        } else {
            searchGamesLocal(trimmedQuery, sortBy, sortOrder);
        }
    }

    private void searchGamesOnline(String query, String sortBy, String sortOrder) {
        loadingLiveData.postValue(true);

        RetrofitClient.getInstance().getApiService()
                .searchGames(query)
                .enqueue(new Callback<List<ApiGame>>() {
                    @Override
                    public void onResponse(Call<List<ApiGame>> call, Response<List<ApiGame>> response) {
                        loadingLiveData.postValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<ApiGame> apiGames = response.body();
                            Log.d(TAG, "API search found " + apiGames.size() + " games");

                            List<GameEntity> searchResults = new ArrayList<>();
                            for (ApiGame apiGame : apiGames) {
                                searchResults.add(apiGame.toEntity());
                            }

                            List<GameEntity> sortedResults = sortGames(searchResults, sortBy, sortOrder);
                            gamesLiveData.postValue(sortedResults);

                            saveSearchResultsToDatabase(apiGames);

                        } else {
                            Log.w(TAG, "API search failed, falling back to local search");
                            searchGamesLocal(query, sortBy, sortOrder);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ApiGame>> call, Throwable t) {
                        loadingLiveData.postValue(false);
                        Log.e(TAG, "API search failed: " + t.getMessage());
                        searchGamesLocal(query, sortBy, sortOrder);
                    }
                });
    }

    /**
     * Локальный поиск в базе данных
     */
    private void searchGamesLocal(String query, String sortBy, String sortOrder) {
        executorService.execute(() -> {
            try {
                List<GameEntity> results;

                // Выбираем метод поиска в зависимости от сортировки
                if (sortBy.equals("title")) {
                    if (sortOrder.equals("asc")) {
                        results = database.gameDao().searchGamesByTitleAsc(query);
                    } else {
                        results = database.gameDao().searchGamesByTitleDesc(query);
                    }
                } else if (sortBy.equals("release_date")) {
                    if (sortOrder.equals("asc")) {
                        results = database.gameDao().searchGamesByDateAsc(query);
                    } else {
                        results = database.gameDao().searchGamesByDateDesc(query);
                    }
                } else if (sortBy.equals("genre")) {
                    if (sortOrder.equals("asc")) {
                        results = database.gameDao().searchGamesByGenreAsc(query);
                    } else {
                        results = database.gameDao().searchGamesByGenreDesc(query);
                    }
                } else {
                    results = database.gameDao().searchGamesByTitleAsc(query);
                }

                Log.d(TAG, "Local search found " + results.size() + " games");
                gamesLiveData.postValue(results);

            } catch (Exception e) {
                Log.e(TAG, "Error searching local games: " + e.getMessage());
            }
        });
    }

    private void saveSearchResultsToDatabase(List<ApiGame> apiGames) {
        executorService.execute(() -> {
            try {
                List<GameEntity> entities = new ArrayList<>();
                for (ApiGame apiGame : apiGames) {
                    entities.add(apiGame.toEntity());
                }
                database.gameDao().insertAllGames(entities);
                Log.d(TAG, "Saved " + entities.size() + " search results to database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving search results: " + e.getMessage());
            }
        });
    }

    private List<GameEntity> sortGames(List<GameEntity> games, String sortBy, String sortOrder) {
        if (games == null || games.isEmpty()) return games;

        List<GameEntity> sortedList = new ArrayList<>(games);

        java.util.Comparator<GameEntity> comparator = null;

        switch (sortBy) {
            case "title":
                comparator = java.util.Comparator.comparing(
                        GameEntity::getTitle,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                );
                break;
            case "release_date":
                comparator = java.util.Comparator.comparing(
                        GameEntity::getReleaseDate,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                );
                break;
            case "genre":
                comparator = java.util.Comparator.comparing(
                        GameEntity::getGenre,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                );
                break;
            default:
                comparator = java.util.Comparator.comparing(
                        GameEntity::getTitle,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                );
        }

        if (sortOrder.equals("desc")) {
            comparator = comparator.reversed();
        }

        sortedList.sort(comparator);
        return sortedList;
    }

    public void updateSortSettings(String sortBy, String sortOrder) {
        preferencesHelper.setSortBy(sortBy);
        preferencesHelper.setSortOrder(sortOrder);
        loadGamesSorted(sortBy, sortOrder);
    }

    public void addGame(GameEntity game) {
        executorService.execute(() -> {
            try {
                long id = database.gameDao().insertGame(game);
                if (id > 0) {
                    game.setId((int) id);
                    Log.d(TAG, "Game added with ID: " + id);
                    loadLocalGames();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding game: " + e.getMessage());
                errorLiveData.postValue("Error adding game: " + e.getMessage());
            }
        });
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
                database.gameDao().deleteGame(game);
                loadLocalGames();
            } catch (Exception e) {
                Log.e(TAG, "Error deleting game: " + e.getMessage());
                errorLiveData.postValue("Error deleting game: " + e.getMessage());
            }
        });
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

    public void clearOldData() {
        executorService.execute(() -> {
            try {
                int count = database.gameDao().getGamesCount();
                database.gameDao().deleteAllGames();
                Log.d(TAG, "Cleared " + count + " old games from database");

                List<GameEntity> emptyList = new ArrayList<>();
                gamesLiveData.postValue(emptyList);
            } catch (Exception e) {
                Log.e(TAG, "Error clearing data: " + e.getMessage());
            }
        });
    }

    public interface OnGameLoadedListener {
        void onGameLoaded(GameEntity game);
    }
}