package com.example.gamecatalog.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gamecatalog.R;
import com.example.gamecatalog.utils.PreferencesHelper;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etUsername;
    private Button btnLogin, btnRegister;
    private TextView tvSwitchMode;
    private ProgressBar progressBar;
    private boolean isLoginMode = true;

    private PreferencesHelper preferencesHelper;
    private OkHttpClient client;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferencesHelper = new PreferencesHelper(this);
        client = new OkHttpClient();

        // Если уже залогинены, переходим в MainActivity
        if (preferencesHelper.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etUsername = findViewById(R.id.etUsername);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
        progressBar = findViewById(R.id.progressBar);

        // По умолчанию скрываем поле username (для логина)
        etUsername.setVisibility(View.GONE);
        btnRegister.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                performLogin();
            } else {
                performRegister();
            }
        });

        btnRegister.setOnClickListener(v -> performRegister());

        tvSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUIMode();
        });
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url("http://10.0.2.2:8080/?action=login")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> handleAuthResponse(responseBody, email, true));
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void performRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String username = etUsername.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);
                json.put("username", username);

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url("http://10.0.2.2:8080/?action=register")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> handleAuthResponse(responseBody, email, false));
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void handleAuthResponse(String responseBody, String email, boolean isLogin) {
        setLoading(false);
        try {
            JSONObject result = new JSONObject(responseBody);
            if (result.optBoolean("success", false)) {
                String token = result.getString("token");
                int userId = result.getInt("user_id");
                String username = result.optString("username", email.split("@")[0]);

                preferencesHelper.setLoggedIn(true);
                preferencesHelper.setAuthToken(token);
                preferencesHelper.setUserId(userId);
                preferencesHelper.setUserEmail(email);
                preferencesHelper.setUsername(username);

                Toast.makeText(this, isLogin ? "Login successful!" : "Registration successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                String error = result.optString("error", isLogin ? "Login failed" : "Registration failed");
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateUIMode() {
        if (isLoginMode) {
            // Режим логина
            etUsername.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.GONE);
            btnLogin.setText(R.string.login);
            tvSwitchMode.setText(R.string.create_account);
        } else {
            // Режим регистрации
            etUsername.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.VISIBLE);
            btnRegister.setText(R.string.register);
            tvSwitchMode.setText(R.string.back_to_login);
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnRegister.setEnabled(!isLoading);
        tvSwitchMode.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        etUsername.setEnabled(!isLoading);
    }
}