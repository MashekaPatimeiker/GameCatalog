package com.example.gamecatalog.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gamecatalog.R;
import com.example.gamecatalog.adapters.GameAdapter;
import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.viewmodel.MainViewModel;

import java.util.List;

public class MainActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private GameAdapter adapter;
    private Button btnAdd, btnSettings;
    private ProgressBar progressBar;
    private TextView tvNoInternet, tvEmpty;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Toolbar toolbar;

    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        setupButtons();
        setupSwipeRefresh();

        // Загружаем данные
        viewModel.loadGames();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        btnAdd = findViewById(R.id.btnAdd);
        btnSettings = findViewById(R.id.btnSettings);
        progressBar = findViewById(R.id.progressBar);
        tvNoInternet = findViewById(R.id.tvNoInternet);
        tvEmpty = findViewById(R.id.tvEmpty);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GameAdapter(new GameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(GameEntity game) {
                Intent intent = new Intent(MainActivity.this, GameDetailActivity.class);
                intent.putExtra("game_id", game.getId());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(GameEntity game) {
                showDeleteDialog(game);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getGames().observe(this, games -> {
            if (games != null) {
                adapter.updateGames(games);
                showEmptyState(games.isEmpty());
            } else {
                adapter.updateGames(null);
                showEmptyState(true);
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        viewModel.getIsOffline().observe(this, isOffline -> {
            tvNoInternet.setVisibility(isOffline ? View.VISIBLE : View.GONE);
        });
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

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refreshData());
    }

    private void showEmptyState(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showDeleteDialog(GameEntity game) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.delete_game_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    viewModel.deleteGame(game);
                    Toast.makeText(this, R.string.game_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint(getString(R.string.search_hint));

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        performSearch(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        performSearch(newText);
                        return true;
                    }
                });
            }
        }

        return true;
    }

    private void performSearch(String query) {
        if (query == null) return;

        if (query.trim().isEmpty()) {
            viewModel.loadGames();
        } else {
            viewModel.searchGames(query);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sync) {
            viewModel.refreshData();
            Toast.makeText(this, R.string.syncing, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}