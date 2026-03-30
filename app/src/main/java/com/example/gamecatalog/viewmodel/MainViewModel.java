package com.example.gamecatalog.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.data.repository.GameRepository;
import com.example.gamecatalog.utils.NetworkUtils;
import com.example.gamecatalog.utils.PreferencesHelper;

import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private GameRepository repository;
    private PreferencesHelper preferencesHelper;
    private MutableLiveData<Boolean> isConnected = new MutableLiveData<>();
    private MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private MutableLiveData<String> selectedGenre = new MutableLiveData<>("");
    private MutableLiveData<Boolean> useFuzzySearch = new MutableLiveData<>(true);

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = GameRepository.getInstance(application);
        preferencesHelper = new PreferencesHelper(application);
        checkNetworkStatus();
    }

    public LiveData<List<GameEntity>> getGames() {
        return repository.getGames();
    }

    public LiveData<String> getError() {
        return repository.getError();
    }

    public LiveData<Boolean> getLoading() {
        return repository.getLoading();
    }

    public LiveData<Boolean> getIsOffline() {
        return repository.getIsOffline();
    }

    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public LiveData<Boolean> getUseFuzzySearch() {
        return useFuzzySearch;
    }

    public void checkNetworkStatus() {
        boolean connected = NetworkUtils.isNetworkAvailable(getApplication());
        isConnected.setValue(connected);
    }

    public void loadGames() {
        checkNetworkStatus();
        String sortBy = preferencesHelper.getSortBy();
        String sortOrder = preferencesHelper.getSortOrder();
        repository.loadGamesSorted(sortBy, sortOrder);
    }

    public void loadGamesWithFilters() {
        checkNetworkStatus();
        String sortBy = preferencesHelper.getSortBy();
        String sortOrder = preferencesHelper.getSortOrder();
        String genre = selectedGenre.getValue();

        if (genre != null && !genre.isEmpty()) {
            repository.loadGamesFiltered(genre, sortBy, sortOrder);
        } else {
            repository.loadGamesSorted(sortBy, sortOrder);
        }
    }

    public void searchGames(String query) {
        searchQuery.setValue(query);

        if (query == null || query.trim().isEmpty()) {
            loadGamesWithFilters();
            return;
        }

        if (useFuzzySearch.getValue() != null && useFuzzySearch.getValue()) {
            repository.searchGamesFuzzy(query);
        } else {
            repository.searchGamesExact(query);
        }
    }

    public void setUseFuzzySearch(boolean use) {
        useFuzzySearch.setValue(use);
        String query = searchQuery.getValue();
        if (query != null && !query.isEmpty()) {
            searchGames(query);
        }
    }

    public void setSelectedGenre(String genre) {
        selectedGenre.setValue(genre);
    }

    public void addGame(GameEntity game) {
        repository.addGame(game);
    }

    public void updateGame(GameEntity game) {
        repository.updateGame(game);
    }

    public void deleteGame(GameEntity game) {
        repository.deleteGame(game);
    }

    public void refreshData() {
        checkNetworkStatus();
        loadGamesWithFilters();
    }

    public void updateSortSettings(String sortBy, String sortOrder) {
        preferencesHelper.setSortBy(sortBy);
        preferencesHelper.setSortOrder(sortOrder);

        String query = searchQuery.getValue();
        if (query != null && !query.isEmpty()) {
            searchGames(query);
        } else {
            loadGamesWithFilters();
        }
    }
}