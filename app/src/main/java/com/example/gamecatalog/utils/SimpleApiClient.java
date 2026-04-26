package com.example.gamecatalog.utils;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class SimpleApiClient {
    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static OkHttpClient client;

    public static synchronized OkHttpClient getClient(Context context) {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }
}