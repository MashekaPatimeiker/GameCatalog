package com.example.gamecatalog.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gamecatalog.R;
import com.example.gamecatalog.adapters.GameAdapter;
import com.example.gamecatalog.database.DatabaseHelper;
import com.example.gamecatalog.models.Game;
import java.util.List;

public class MainActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private GameAdapter adapter;
    private DatabaseHelper dbHelper;
    private Button btnAdd, btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupRecyclerView();
        setupButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGames();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        btnAdd = findViewById(R.id.btnAdd);
        btnSettings = findViewById(R.id.btnSettings);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupButtons() {
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameDetailActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void loadGames() {
        List<Game> games = dbHelper.getAllGames();
        adapter = new GameAdapter(games, new GameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Game game) {
                Intent intent = new Intent(MainActivity.this, GameDetailActivity.class);
                intent.putExtra("game_id", game.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Game game) {
                dbHelper.deleteGame(game.getId());
                loadGames();
            }
        });
        recyclerView.setAdapter(adapter);
    }
}