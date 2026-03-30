package com.example.gamecatalog.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.data.repository.GameRepository;

public class GameDetailViewModel extends AndroidViewModel {

    private GameRepository repository;
    private MutableLiveData<GameEntity> currentGame = new MutableLiveData<>();
    private MutableLiveData<Boolean> isEditMode = new MutableLiveData<>(false);

    public GameDetailViewModel(@NonNull Application application) {
        super(application);
        repository = GameRepository.getInstance(application);
    }

    public void loadGameById(int gameId) {
        if (gameId != -1) {
            isEditMode.setValue(true);
            repository.getGameById(gameId, game -> {
                currentGame.postValue(game);
            });
        } else {
            isEditMode.setValue(false);
            currentGame.setValue(null);
        }
    }

    public MutableLiveData<GameEntity> getCurrentGame() {
        return currentGame;
    }

    public MutableLiveData<Boolean> getIsEditMode() {
        return isEditMode;
    }

    public void saveGame(String title, String genre, String releaseDate,
                         String description, String imagePath) {
        if (isEditMode.getValue() != null && isEditMode.getValue()) {
            GameEntity current = currentGame.getValue();
            if (current != null) {
                current.setTitle(title);
                current.setGenre(genre);
                current.setReleaseDate(releaseDate);
                current.setDescription(description);
                current.setImagePath(imagePath);
                repository.updateGame(current);
            }
        } else {
            GameEntity newGame = new GameEntity(title, genre, releaseDate, description, imagePath);
            repository.addGame(newGame);
        }
    }
    public void deleteGame() {
        if (isEditMode.getValue() != null && isEditMode.getValue()) {
            GameEntity current = currentGame.getValue();
            if (current != null) {
                repository.deleteGame(current);
            }
        }
    }
}