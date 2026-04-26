package com.example.gamecatalog.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gamecatalog.data.database.entities.GameEntity;

import java.util.List;

@Dao
public interface GameDao {

    @Query("SELECT * FROM games ORDER BY title COLLATE NOCASE ASC")
    List<GameEntity> getAllGames();

    @Query("SELECT * FROM games WHERE id = :id")
    GameEntity getGameById(int id);

    @Query("SELECT * FROM games WHERE api_id = :apiId")
    GameEntity getGameByApiId(String apiId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertGame(GameEntity game);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllGames(List<GameEntity> games);
    @Query("SELECT * FROM games WHERE remote_id = :remoteId")
    GameEntity getGameByRemoteId(Long remoteId);


    @Query("UPDATE games SET is_favorite = :isFavorite WHERE id = :gameId")
    void updateFavoriteStatus(int gameId, boolean isFavorite);
    @Update
    int updateGame(GameEntity game);

    @Delete
    void deleteGame(GameEntity game);

    @Query("DELETE FROM games WHERE id = :id")
    void deleteGameById(int id);

    @Query("DELETE FROM games")
    void deleteAllGames();

    @Query("SELECT COUNT(*) FROM games")
    int getGamesCount();

    @Query("SELECT * FROM games WHERE is_synced = 0")
    List<GameEntity> getUnsyncedGames();

    @Query("SELECT * FROM games ORDER BY title COLLATE NOCASE ASC")
    List<GameEntity> getGamesSortedByTitleAsc();

    @Query("SELECT * FROM games ORDER BY title COLLATE NOCASE DESC")
    List<GameEntity> getGamesSortedByTitleDesc();

    @Query("SELECT * FROM games ORDER BY release_date ASC")
    List<GameEntity> getGamesSortedByDateAsc();

    @Query("SELECT * FROM games ORDER BY release_date DESC")
    List<GameEntity> getGamesSortedByDateDesc();

    @Query("SELECT * FROM games ORDER BY genre COLLATE NOCASE ASC")
    List<GameEntity> getGamesSortedByGenreAsc();

    @Query("SELECT * FROM games ORDER BY genre COLLATE NOCASE DESC")
    List<GameEntity> getGamesSortedByGenreDesc();
    @Query("SELECT * FROM games WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY title COLLATE NOCASE ASC")
    List<GameEntity> searchGamesByTitle(String query);

    @Query("SELECT * FROM games WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY title COLLATE NOCASE ASC")
    List<GameEntity> searchGamesByTitleAsc(String query);

    @Query("SELECT * FROM games WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY title COLLATE NOCASE DESC")
    List<GameEntity> searchGamesByTitleDesc(String query);

    @Query("SELECT * FROM games WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY release_date ASC")
    List<GameEntity> searchGamesByDateAsc(String query);

    @Query("SELECT * FROM games WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY release_date DESC")
    List<GameEntity> searchGamesByDateDesc(String query);

    @Query("SELECT * FROM games WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY genre COLLATE NOCASE ASC")
    List<GameEntity> searchGamesByGenreAsc(String query);

    @Query("SELECT * FROM games WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY genre COLLATE NOCASE DESC")
    List<GameEntity> searchGamesByGenreDesc(String query);
}