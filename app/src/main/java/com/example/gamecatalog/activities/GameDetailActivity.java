package com.example.gamecatalog.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gamecatalog.R;
import com.example.gamecatalog.database.DatabaseHelper;
import com.example.gamecatalog.models.Game;
import com.example.gamecatalog.utils.ImagePickerHelper;

public class GameDetailActivity extends BaseActivity {
    private EditText etTitle, etGenre, etDate, etDescription;
    private Button btnSave, btnDelete, btnSelectImage;
    private ImageView ivGameImage;
    private DatabaseHelper dbHelper;
    private int gameId = -1;
    private String currentImagePath = null;
    private ImagePickerHelper imagePickerHelper;

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        dbHelper = new DatabaseHelper(this);
        initViews();

        imagePickerHelper = new ImagePickerHelper(this, imagePath -> {
            currentImagePath = imagePath;
            ImagePickerHelper.loadImageIntoView(imagePath, ivGameImage);
        });

        if (getIntent().hasExtra("game_id")) {
            gameId = getIntent().getIntExtra("game_id", -1);
            loadGameData();
            btnDelete.setVisibility(View.VISIBLE);
        }

        setupButtons();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etGenre = findViewById(R.id.etGenre);
        etDate = findViewById(R.id.etDate);
        etDescription = findViewById(R.id.etDescription);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivGameImage = findViewById(R.id.ivGameImage);
    }

    private void loadGameData() {
        Game game = dbHelper.getGame(gameId);
        if (game != null) {
            etTitle.setText(game.getTitle());
            etGenre.setText(game.getGenre());
            etDate.setText(game.getReleaseDate());
            etDescription.setText(game.getDescription());
            currentImagePath = game.getImagePath();
            ImagePickerHelper.loadImageIntoView(currentImagePath, ivGameImage);
        }
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> saveGame());
        btnDelete.setOnClickListener(v -> deleteGame());
        btnSelectImage.setOnClickListener(v -> showImagePickerDialog());
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_image_picker, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btnCamera).setOnClickListener(v -> {
            dialog.dismiss();
            checkCameraPermissionAndOpen();
        });

        dialogView.findViewById(R.id.btnGallery).setOnClickListener(v -> {
            dialog.dismiss();
            imagePickerHelper.openGallery();
        });

        dialog.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            imagePickerHelper.openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                imagePickerHelper.openCamera();
            } else {
                Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imagePickerHelper.handleActivityResult(requestCode, resultCode, data);
    }

    private void saveGame() {
        String title = etTitle.getText().toString().trim();
        String genre = etGenre.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty() || genre.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_required_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (gameId == -1) {
            // Создание новой игры
            Game game = new Game(title, genre, date, description, currentImagePath);
            dbHelper.addGame(game);
            Toast.makeText(this, getString(R.string.game_added), Toast.LENGTH_SHORT).show();
        } else {
            Game game = new Game(gameId, title, genre, date, description, currentImagePath);
            dbHelper.updateGame(game);
            Toast.makeText(this, getString(R.string.game_updated), Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private void deleteGame() {
        if (gameId != -1) {
            dbHelper.deleteGame(gameId);
            Toast.makeText(this, getString(R.string.game_deleted), Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}