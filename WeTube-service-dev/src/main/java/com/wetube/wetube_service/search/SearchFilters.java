package com.wetube.wetube_service.search;

import java.util.Locale;

/**
 * Bộ lọc của trang tìm kiếm (giống YouTube). Tham số rỗng = không lọc.
 * type: video | short; duration: short (&lt; 4 phút) | medium (4–20 phút) | long (&gt; 20 phút);
 * uploaded: today | week | month | year; sort: relevance | date | views.
 */
public record SearchFilters(Boolean shortsOnly, Double minDurationSeconds, Double maxDurationSeconds,
        String uploadedSince, Sort sort) {

    public enum Sort { RELEVANCE, DATE, VIEWS }

    public static SearchFilters of(String type, String duration, String uploaded, String sort) {
        Boolean shortsOnly = switch (norm(type)) {
            case "" -> null;
            case "video" -> false;
            case "short" -> true;
            default -> throw invalid("loại", type);
        };
        Double min = null;
        Double max = null;
        switch (norm(duration)) {
            case "" -> { }
            case "short" -> max = 240.0;
            case "medium" -> { min = 240.0; max = 1200.0; }
            case "long" -> min = 1200.0;
            default -> throw invalid("thời lượng", duration);
        }
        String since = switch (norm(uploaded)) {
            case "" -> null;
            case "today" -> "now-1d";
            case "week" -> "now-7d";
            case "month" -> "now-30d";
            case "year" -> "now-365d";
            default -> throw invalid("ngày tải lên", uploaded);
        };
        Sort order = switch (norm(sort)) {
            case "", "relevance" -> Sort.RELEVANCE;
            case "date" -> Sort.DATE;
            case "views" -> Sort.VIEWS;
            default -> throw invalid("sắp xếp", sort);
        };
        return new SearchFilters(shortsOnly, min, max, since, order);
    }

    private static String norm(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static IllegalArgumentException invalid(String what, String value) {
        return new IllegalArgumentException("Bộ lọc " + what + " không hợp lệ: " + value);
    }
}
