package com.example.gamecatalog.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.example.gamecatalog.R;
import com.example.gamecatalog.utils.PreferencesHelper;

public class SplashActivity extends BaseActivity {
    private static final int SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        PreferencesHelper preferencesHelper = new PreferencesHelper(this);

        new Handler().postDelayed(() -> {
            // Проверяем, залогинен ли пользователь
            if (preferencesHelper.isLoggedIn()) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, SPLASH_DURATION);
    }
}