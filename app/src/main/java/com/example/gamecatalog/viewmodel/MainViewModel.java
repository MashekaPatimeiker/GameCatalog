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

    public void searchGames(String query) {
        repository.searchGames(query);
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
        loadGames();
    }

    public void updateSortSettings(String sortBy, String sortOrder) {
        preferencesHelper.setSortBy(sortBy);
        preferencesHelper.setSortOrder(sortOrder);
        repository.updateSortSettings(sortBy, sortOrder);
    }

    public void loadGamesWithSort() {
        loadGames();
    }

    public void searchGamesWithSort(String query) {
        searchGames(query);
    }

    public void clearOldData() {
        repository.clearOldData();
    }
}