package com.example.gamecatalog.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FuzzySearch {
    public static int levenshteinDistance(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";

        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }
    public static double similarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1;

        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLen;
    }
    public static List<SearchResult> fuzzySearch(String query, List<String> items, double threshold) {
        List<SearchResult> results = new ArrayList<>();

        if (query == null || query.isEmpty()) {
            for (String item : items) {
                results.add(new SearchResult(item, 1.0));
            }
            return results;
        }

        String lowerQuery = query.toLowerCase();

        for (String item : items) {
            if (item == null) continue;

            String lowerItem = item.toLowerCase();

            if (lowerItem.equals(lowerQuery)) {
                results.add(new SearchResult(item, 1.0));
                continue;
            }

            if (lowerItem.contains(lowerQuery)) {
                results.add(new SearchResult(item, 0.95));
                continue;
            }

            double similarity = similarity(lowerItem, lowerQuery);
            if (similarity >= threshold) {
                results.add(new SearchResult(item, similarity));
            }
        }

        Collections.sort(results, (a, b) -> Double.compare(b.score, a.score));

        return results;
    }
    public static class SearchResult {
        public String text;
        public double score;

        public SearchResult(String text, double score) {
            this.text = text;
            this.score = score;
        }
    }
}