package com.example.gamecatalog.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.example.gamecatalog.R;

public class SplashActivity extends BaseActivity {
    private static final int SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}