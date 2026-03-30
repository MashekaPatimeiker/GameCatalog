package com.example.gamecatalog.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.gamecatalog.R;
import com.example.gamecatalog.data.repository.GameRepository;
import com.example.gamecatalog.utils.PreferencesHelper;

public class SortSettingsActivity extends BaseActivity {

    private RadioGroup radioGroupSortBy;
    private RadioGroup radioGroupSortOrder;
    private Button btnApply;
    private PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sort_settings);

        preferencesHelper = new PreferencesHelper(this);
        initViews();
        loadCurrentSettings();
        setupListeners();
    }

    private void initViews() {
        radioGroupSortBy = findViewById(R.id.radioGroupSortBy);
        radioGroupSortOrder = findViewById(R.id.radioGroupSortOrder);
        btnApply = findViewById(R.id.btnApplySort);
    }

    private void loadCurrentSettings() {
        String currentSortBy = preferencesHelper.getSortBy();
        String currentSortOrder = preferencesHelper.getSortOrder();

        switch (currentSortBy) {
            case "title":
                ((RadioButton) findViewById(R.id.radioSortByTitle)).setChecked(true);
                break;
            case "release_date":
                ((RadioButton) findViewById(R.id.radioSortByDate)).setChecked(true);
                break;
            case "genre":
                ((RadioButton) findViewById(R.id.radioSortByGenre)).setChecked(true);
                break;
            default:
                ((RadioButton) findViewById(R.id.radioSortByTitle)).setChecked(true);
        }

        // Устанавливаем порядок сортировки
        if (currentSortOrder.equals("asc")) {
            ((RadioButton) findViewById(R.id.radioSortAscending)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.radioSortDescending)).setChecked(true);
        }
    }

    private void setupListeners() {
        btnApply.setOnClickListener(v -> {
            String sortBy = getSelectedSortBy();
            String sortOrder = getSelectedSortOrder();

            if (sortBy != null && sortOrder != null) {
                preferencesHelper.setSortBy(sortBy);
                preferencesHelper.setSortOrder(sortOrder);

                GameRepository repository = GameRepository.getInstance(getApplicationContext());
                repository.updateSortSettings(sortBy, sortOrder);

                Toast.makeText(this, R.string.sort_settings_applied, Toast.LENGTH_SHORT).show();

                finish();
            } else {
                Toast.makeText(this, R.string.select_sort_options, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSelectedSortBy() {
        int selectedId = radioGroupSortBy.getCheckedRadioButtonId();
        if (selectedId == R.id.radioSortByTitle) {
            return "title";
        } else if (selectedId == R.id.radioSortByDate) {
            return "release_date";
        } else if (selectedId == R.id.radioSortByGenre) {
            return "genre";
        }
        return null;
    }

    private String getSelectedSortOrder() {
        int selectedId = radioGroupSortOrder.getCheckedRadioButtonId();
        if (selectedId == R.id.radioSortAscending) {
            return "asc";
        } else if (selectedId == R.id.radioSortDescending) {
            return "desc";
        }
        return null;
    }
}