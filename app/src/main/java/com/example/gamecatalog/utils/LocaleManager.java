package com.example.gamecatalog.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import java.util.Locale;

public class LocaleManager {

    private static final String TAG = "LocaleManager";

    public static Context setLocale(Context context, String languageCode) {
        return updateResources(context, languageCode);
    }

    private static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }

        Log.d(TAG, "updateResources: Language updated to " + languageCode);
        return context;
    }

    public static String getSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    public static boolean isSameLanguage(String lang1, String lang2) {
        if (lang1 == null || lang2 == null) return false;
        return lang1.equals(lang2);
    }
}