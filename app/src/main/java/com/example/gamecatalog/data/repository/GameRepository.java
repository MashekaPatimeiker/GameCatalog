package com.example.gamecatalog.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gamecatalog.data.api.RemoteRetrofitClient;
import com.example.gamecatalog.data.api.RetrofitClient;
import com.example.gamecatalog.data.api.models.ApiGame;
import com.example.gamecatalog.data.database.GameDatabase;
import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.data.models.GameDto;
import com.example.gamecatalog.utils.FuzzySearch;
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

    private static final double FUZZY_THRESHOLD = 0.65;

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

        RemoteRetrofitClient.getInstance().getRemoteGameApi()
                .getGames(null, null, sortBy, sortOrder)
                .enqueue(new Callback<List<GameDto>>() {
                    @Override
                    public void onResponse(Call<List<GameDto>> call, Response<List<GameDto>> response) {
                        loadingLiveData.postValue(false);

                        if (response.isSuccessful()) {
                            List<GameDto> games = response.body();
                            Log.d(TAG, "API Response: received " +
                                    (games != null ? games.size() : 0) + " games");

                            if (games != null && !games.isEmpty()) {
                                // ИСПРАВЛЕНО: используем правильный метод
                                saveGamesToDatabaseWithSort(games, sortBy, sortOrder);
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
                    public void onFailure(Call<List<GameDto>> call, Throwable t) {
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

    public void searchGamesFuzzy(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadGames();
            return;
        }

        String trimmedQuery = query.trim();
        Log.d(TAG, "Fuzzy search for: \"" + trimmedQuery + "\"");

        searchGamesFuzzyLocal(trimmedQuery);
    }

    private void saveGamesToDatabaseWithSort(List<GameDto> games, String sortBy, String sortOrder) {
        executorService.execute(() -> {
            try {
                database.gameDao().deleteAllGames();

                List<GameEntity> entities = new ArrayList<>();
                for (GameDto gameDto : games) {
                    entities.add(gameDto.toEntity());
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
                    double score = 0;

                    if (lowerTitle.equals(lowerQuery)) {
                        score = 1.0;
                    }
                    else if (lowerTitle.contains(lowerQuery)) {
                        score = 0.95;
                    }
                    else {
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

    public void searchGamesExact(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadGames();
            return;
        }

        String trimmedQuery = query.trim().toLowerCase();
        Log.d(TAG, "Exact search for: \"" + trimmedQuery + "\"");

        searchGamesExactLocal(trimmedQuery);
    }

    private void searchGamesExactLocal(String query) {
        executorService.execute(() -> {
            try {
                List<GameEntity> allGames = database.gameDao().getAllGames();
                Log.d(TAG, "Total games in DB: " + allGames.size());

                List<GameEntity> filteredResults = new ArrayList<>();
                String lowerQuery = query.toLowerCase();

                for (GameEntity game : allGames) {
                    if (game.getTitle() != null &&
                            game.getTitle().toLowerCase().contains(lowerQuery)) {
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

    public interface OnGameLoadedListener {
        void onGameLoaded(GameEntity game);
    }
    public void loadGamesFiltered(String genre, String sortBy, String sortOrder) {
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);
        Log.d(TAG, "loadGamesFiltered: isOnline = " + isOnline + ", genre = " + genre);

        isOfflineLiveData.postValue(!isOnline);

        if (isOnline) {
            Log.d(TAG, "Online mode: fetching from API with filter");
            fetchGamesFromApiWithFilter(genre, sortBy, sortOrder);
        } else {
            Log.d(TAG, "Offline mode: loading from database with filter");
            loadLocalGamesFiltered(genre, sortBy, sortOrder);
            errorLiveData.postValue("No internet connection. Showing cached data.");
        }
    }

    private void loadLocalGamesFiltered(String genre, String sortBy, String sortOrder) {
        executorService.execute(() -> {
            try {
                List<GameEntity> allGames = database.gameDao().getAllGames();

                List<GameEntity> filteredGames = new ArrayList<>();
                String lowerGenre = genre.toLowerCase();

                for (GameEntity game : allGames) {
                    if (game.getGenre() != null &&
                            game.getGenre().toLowerCase().contains(lowerGenre)) {
                        filteredGames.add(game);
                    }
                }

                Log.d(TAG, "Filtered games by genre '" + genre + "': found " + filteredGames.size() + " games");

                List<GameEntity> sortedResults = sortGames(filteredGames, sortBy, sortOrder);
                gamesLiveData.postValue(sortedResults);

                boolean isOnline = NetworkUtils.isNetworkAvailable(context);
                isOfflineLiveData.postValue(!isOnline);
            } catch (Exception e) {
                Log.e(TAG, "Error loading filtered games: " + e.getMessage());
                errorLiveData.postValue("Error loading games: " + e.getMessage());
            }
        });
    }

    private void fetchGamesFromApiWithFilter(String genre, String sortBy, String sortOrder) {
        loadingLiveData.postValue(true);

        RetrofitClient.getInstance().getApiService()
                .getGames()
                .enqueue(new Callback<List<ApiGame>>() {
                    @Override
                    public void onResponse(Call<List<ApiGame>> call, Response<List<ApiGame>> response) {
                        loadingLiveData.postValue(false);

                        if (response.isSuccessful()) {
                            List<ApiGame> apiGames = response.body();
                            if (apiGames != null && !apiGames.isEmpty()) {
                                // Фильтруем по жанру
                                List<ApiGame> filteredGames = new ArrayList<>();
                                String lowerGenre = genre.toLowerCase();

                                for (ApiGame apiGame : apiGames) {
                                    if (apiGame.getGenre() != null &&
                                            apiGame.getGenre().toLowerCase().contains(lowerGenre)) {
                                        filteredGames.add(apiGame);
                                    }
                                }

                                saveApiGamesToDatabaseWithFilter(filteredGames, sortBy, sortOrder);
                            } else {
                                loadLocalGamesFiltered(genre, sortBy, sortOrder);
                            }
                        } else {
                            loadLocalGamesFiltered(genre, sortBy, sortOrder);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ApiGame>> call, Throwable t) {
                        loadingLiveData.postValue(false);
                        loadLocalGamesFiltered(genre, sortBy, sortOrder);
                    }
                });
    }

    private void saveApiGamesToDatabaseWithFilter(List<ApiGame> apiGames, String sortBy, String sortOrder) {
        executorService.execute(() -> {
            try {
                List<GameEntity> entities = new ArrayList<>();
                for (ApiGame apiGame : apiGames) {
                    entities.add(apiGame.toEntity());
                }

                database.gameDao().insertAllGames(entities);
                Log.d(TAG, "Saved " + entities.size() + " filtered games to database");

                loadLocalGamesFiltered(apiGames.get(0).getGenre(), sortBy, sortOrder);

            } catch (Exception e) {
                Log.e(TAG, "Error saving filtered games: " + e.getMessage(), e);
                errorLiveData.postValue("Error saving games: " + e.getMessage());
            }
        });
    }

    public void updateSortSettings(String sortBy, String sortOrder) {
        preferencesHelper.setSortBy(sortBy);
        preferencesHelper.setSortOrder(sortOrder);
        loadGamesSorted(sortBy, sortOrder);
    }
}