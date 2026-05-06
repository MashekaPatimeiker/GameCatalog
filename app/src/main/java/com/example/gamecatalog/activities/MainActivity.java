package com.example.gamecatalog.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gamecatalog.R;
import com.example.gamecatalog.adapters.GameAdapter;
import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.data.repository.GameRepository;
import com.example.gamecatalog.utils.GeolocationManager;
import com.example.gamecatalog.utils.PreferencesHelper;
import com.example.gamecatalog.utils.RealtimeSubscription;
import com.example.gamecatalog.utils.SocialShareHelper;
import com.example.gamecatalog.viewmodel.MainViewModel;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private GameAdapter adapter;
    private Button btnAdd, btnSettings;
    private ProgressBar progressBar;
    private TextView tvNoInternet, tvEmpty, tvLocation;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;
    private Spinner spinnerGenre;
    private SwitchCompat switchFuzzySearch;
    private RealtimeSubscription realtimeSubscription;
    private MainViewModel viewModel;
    private PreferencesHelper preferencesHelper;
    private GeolocationManager geolocationManager;

    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private final Set<Integer> favoriteIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferencesHelper = new PreferencesHelper(this);
        geolocationManager = new GeolocationManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        setupButtons();
        setupSwipeRefresh();
        setupSearchAndFilter();
        setupLocation();
        setupRealtimeSubscription();
        viewModel.loadGames();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    private void setupLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            startLocationUpdates();
        }

        geolocationManager.getCurrentLocation().observe(this, location -> {
            if (location != null) {
                updateLocationUI(location);
            }
        });
    }
    private void setupRealtimeSubscription() {
        realtimeSubscription = RealtimeSubscription.getInstance();
        realtimeSubscription.subscribe(new RealtimeSubscription.OnDatabaseChangeListener() {
            @Override
            public void onGameChanged(String operation, int gameId) {
                Log.d("MainActivity", "🔄 БД изменилась! Операция: " + operation + ", игра: " + gameId);

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "База данных изменена! Обновление...",
                            Toast.LENGTH_SHORT).show();
                });

                viewModel.refreshData();
            }

            @Override
            public void onConnectionError(String error) {
                Log.e("MainActivity", "Ошибка подключения: " + error);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "Ошибка realtime подключения",
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (realtimeSubscription != null) {
            realtimeSubscription.stopListening();
        }
    }
    private void startLocationUpdates() {
        geolocationManager.requestLocationUpdates();
    }

    private void updateLocationUI(Location location) {
        if (tvLocation != null) {
            String locationText = String.format(Locale.getDefault(), "📍 %.2f, %.2f",
                    location.getLatitude(), location.getLongitude());
            tvLocation.setText(locationText);
            tvLocation.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        btnAdd = findViewById(R.id.btnAdd);
        btnSettings = findViewById(R.id.btnSettings);
        progressBar = findViewById(R.id.progressBar);
        tvNoInternet = findViewById(R.id.tvNoInternet);
        tvEmpty = findViewById(R.id.tvEmpty);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        searchView = findViewById(R.id.searchView);
        spinnerGenre = findViewById(R.id.spinnerGenre);
        switchFuzzySearch = findViewById(R.id.switchFuzzySearch);
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

            @Override
            public void onFavoriteClick(GameEntity game, boolean isFavorite) {
                if (isFavorite) {
                    favoriteIds.add(game.getId());
                    Toast.makeText(MainActivity.this,
                            game.getTitle() + " " + getString(R.string.added_to_favorites),
                            Toast.LENGTH_SHORT).show();
                } else {
                    favoriteIds.remove(game.getId());
                    Toast.makeText(MainActivity.this,
                            game.getTitle() + " " + getString(R.string.removed_from_favorites),
                            Toast.LENGTH_SHORT).show();
                }

                // Синхронизация с сервером
                viewModel.toggleFavorite(game.getId(), isFavorite, new GameRepository.OnFavoriteToggledListener() {
                    @Override
                    public void onSuccess() {
                        // Успешно
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Sync error: " + error, Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            }

            @Override
            public void onShareClick(GameEntity game) {
                SocialShareHelper.shareGameWithImage(MainActivity.this, game, game.getImagePath());
            }
        }, this);
        recyclerView.setAdapter(adapter);
    }
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getGames().observe(this, games -> {
            if (games != null) {
                favoriteIds.clear();
                for (GameEntity game : games) {
                    if (game.isFavorite()) {
                        favoriteIds.add(game.getId());
                    }
                }
                adapter.setFavoriteIds(favoriteIds);
                adapter.updateGames(games);
                showEmptyState(games.isEmpty());
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            swipeRefreshLayout.setRefreshing(false);
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

        Button btnSort = findViewById(R.id.btnSort);
        btnSort.setOnClickListener(v -> showSortDialog());

        Button btnSync = findViewById(R.id.btnSync);
        btnSync.setOnClickListener(v -> {
            viewModel.refreshData();
            Toast.makeText(this, R.string.syncing, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refreshData());
    }

    private void setupSearchAndFilter() {
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint(getString(R.string.search_hint));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }
        });

        if (switchFuzzySearch != null) {
            switchFuzzySearch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                viewModel.setUseFuzzySearch(isChecked);
                String currentQuery = searchView.getQuery() != null ? searchView.getQuery().toString() : "";
                if (!currentQuery.isEmpty()) {
                    viewModel.searchGames(currentQuery);
                }
            });
        }

        String[] genres = getResources().getStringArray(R.array.genres_array);
        ArrayAdapter<String> genreAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genres);
        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenre.setAdapter(genreAdapter);

        spinnerGenre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGenre = parent.getItemAtPosition(position).toString();
                if ("All".equals(selectedGenre)) {
                    viewModel.setSelectedGenre("");
                } else {
                    viewModel.setSelectedGenre(selectedGenre);
                }
                viewModel.loadGamesWithFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                viewModel.setSelectedGenre("");
                viewModel.loadGamesWithFilters();
            }
        });
    }

    private void performSearch(String query) {
        if (query == null) return;
        if (query.trim().isEmpty()) {
            viewModel.loadGamesWithFilters();
        } else {
            viewModel.searchGames(query);
        }
    }

    private void showEmptyState(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showDeleteDialog(GameEntity game) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.delete_game_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> viewModel.deleteGame(game))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showSortDialog() {
        String[] sortOptions = {getString(R.string.title), getString(R.string.release_date), getString(R.string.genre)};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.sort_by)
                .setItems(sortOptions, (dialog, which) -> {
                    String sortBy;
                    switch (which) {
                        case 0:
                            sortBy = "title";
                            break;
                        case 1:
                            sortBy = "release_date";
                            break;
                        default:
                            sortBy = "genre";
                            break;
                    }
                    String currentSortOrder = preferencesHelper.getSortOrder();
                    preferencesHelper.setSortBy(sortBy);
                    viewModel.updateSortSettings(sortBy, currentSortOrder);
                    Toast.makeText(this, getString(R.string.sorted_by) + ": " + sortOptions[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_logout) {
            preferencesHelper.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        if (itemId == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.menu_about) {
            Toast.makeText(this, R.string.about_message, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else if (tvLocation != null) {
                tvLocation.setText(R.string.location_permission_denied);
            }
        }
    }

}