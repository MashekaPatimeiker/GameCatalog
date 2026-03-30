package com.example.gamecatalog.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
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
import com.example.gamecatalog.utils.PreferencesHelper;
import com.example.gamecatalog.viewmodel.MainViewModel;

public class MainActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private GameAdapter adapter;
    private Button btnAdd, btnSettings;
    private ProgressBar progressBar;
    private TextView tvNoInternet, tvEmpty;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Toolbar toolbar;
    private SearchView searchView;
    private Spinner spinnerGenre;
    private Switch switchFuzzySearch;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;
    private MainViewModel viewModel;
    private PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestNotificationPermission();
        preferencesHelper = new PreferencesHelper(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        setupButtons();
        setupSwipeRefresh();
        setupSearchAndFilter();

        viewModel.loadGames();
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted");
            } else {
                Log.d("MainActivity", "Notification permission denied");
            }
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
        toolbar = findViewById(R.id.toolbar);
        searchView = findViewById(R.id.searchView);
        spinnerGenre = findViewById(R.id.spinnerGenre);
        switchFuzzySearch = findViewById(R.id.switchFuzzySearch);  // <--- ДОБАВЬТЕ ЭТУ СТРОКУ
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
                Log.d("MainActivity", "Search submitted: " + query);
                performSearch(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d("MainActivity", "Search text changed: " + newText);
                performSearch(newText);
                return true;
            }
        });

        if (switchFuzzySearch != null) {
            switchFuzzySearch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                viewModel.setUseFuzzySearch(isChecked);
                String currentQuery = searchView.getQuery().toString();
                if (!currentQuery.isEmpty()) {
                    viewModel.searchGames(currentQuery);
                }
            });
        }

        String[] genres = getResources().getStringArray(R.array.genres_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenre.setAdapter(adapter);

        spinnerGenre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGenre = parent.getItemAtPosition(position).toString();
                if (selectedGenre.equals("All")) {
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
    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_sort, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        RadioGroup radioGroupSortBy = dialogView.findViewById(R.id.radioGroupSortBy);
        RadioGroup radioGroupSortOrder = dialogView.findViewById(R.id.radioGroupSortOrder);

        String currentSortBy = preferencesHelper.getSortBy();
        String currentSortOrder = preferencesHelper.getSortOrder();

        switch (currentSortBy) {
            case "title":
                radioGroupSortBy.check(R.id.radioSortByTitle);
                break;
            case "release_date":
                radioGroupSortBy.check(R.id.radioSortByDate);
                break;
            case "genre":
                radioGroupSortBy.check(R.id.radioSortByGenre);
                break;
            default:
                radioGroupSortBy.check(R.id.radioSortByTitle);
                break;
        }

        if (currentSortOrder.equals("asc")) {
            radioGroupSortOrder.check(R.id.radioSortAscending);
        } else {
            radioGroupSortOrder.check(R.id.radioSortDescending);
        }

        dialogView.findViewById(R.id.btnApplySort).setOnClickListener(v -> {
            String sortBy;
            int selectedSortById = radioGroupSortBy.getCheckedRadioButtonId();

            if (selectedSortById == R.id.radioSortByTitle) {
                sortBy = "title";
            } else if (selectedSortById == R.id.radioSortByDate) {
                sortBy = "release_date";
            } else if (selectedSortById == R.id.radioSortByGenre) {
                sortBy = "genre";
            } else {
                sortBy = "title";
            }

            String sortOrder = radioGroupSortOrder.getCheckedRadioButtonId() == R.id.radioSortDescending ?
                    "desc" : "asc";

            preferencesHelper.setSortBy(sortBy);
            preferencesHelper.setSortOrder(sortOrder);

            viewModel.updateSortSettings(sortBy, sortOrder);

            String sortText;
            switch (sortBy) {
                case "title":
                    sortText = getString(R.string.title);
                    break;
                case "release_date":
                    sortText = getString(R.string.release_date);
                    break;
                case "genre":
                    sortText = getString(R.string.genre);
                    break;
                default:
                    sortText = getString(R.string.title);
                    break;
            }

            String orderText = sortOrder.equals("asc") ? getString(R.string.ascending) : getString(R.string.descending);

            Toast.makeText(MainActivity.this,
                    getString(R.string.sorted_by) + ": " + sortText + " (" + orderText + ")",
                    Toast.LENGTH_SHORT).show();

            dialog.dismiss();
        });

        dialog.show();
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
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_game_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> viewModel.deleteGame(game))
                .setNegativeButton(R.string.no, null)
                .show();
    }
}