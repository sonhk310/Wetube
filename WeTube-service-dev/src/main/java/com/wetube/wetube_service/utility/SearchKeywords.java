package com.wetube.wetube_service.utility;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Từ khóa tìm kiếm của video (S2): phân cách bằng dấu phẩy, tối đa 15 từ, mỗi từ tối đa 40 ký tự. */
public final class SearchKeywords {

    public static final int MAX_COUNT = 15;
    public static final int MAX_LENGTH = 40;

    private static final Pattern SEPARATORS = Pattern.compile("[,;\\r\\n]+");
    private static final Pattern BULLET = Pattern.compile("^\\s*(?:[-*•]|\\d+[.)])\\s+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SearchKeywords() {
    }

    /** Làm sạch chuỗi người dùng / Gemini gửi lên; null nếu không còn từ nào. */
    public static String clean(String raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw != null) {
            for (String part : SEPARATORS.split(raw)) {
                String word = WHITESPACE.matcher(BULLET.matcher(part).replaceFirst("")
                        .replace("\"", "").replace("#", "")).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
                if (word.length() > MAX_LENGTH) {
                    word = word.substring(0, MAX_LENGTH).trim();
                }
                if (!word.isEmpty() && out.size() < MAX_COUNT) {
                    out.add(word);
                }
            }
        }
        return out.isEmpty() ? null : String.join(",", out);
    }
}
