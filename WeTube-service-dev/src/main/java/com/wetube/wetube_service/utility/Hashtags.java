package com.wetube.wetube_service.utility;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nhận diện #thẻ trong tiêu đề / mô tả. Cùng quy tắc với client (linkified-text.tsx): # rồi 1-64 chữ / số / _,
 * không đứng ngay sau chữ / số / & / # ("C#", "&#39;"), không chỉ toàn số ("#1"), không nằm trong link.
 */
public final class Hashtags {

    public static final int MAX_FROM_TEXT = 15;

    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG = Pattern.compile(
            "(?<![\\p{L}\\p{N}\\p{M}_&#])#([\\p{L}\\p{N}\\p{M}_]{1,64})(?![\\p{L}\\p{N}\\p{M}_])");
    private static final Pattern ONLY_DIGITS = Pattern.compile("\\p{N}+");

    private Hashtags() {
    }

    /** #thẻ trong các đoạn văn, viết thường, không trùng, tối đa {@link #MAX_FROM_TEXT} thẻ đầu tiên. */
    public static List<String> extract(String... texts) {
        Set<String> found = new LinkedHashSet<>();
        for (String text : texts) {
            if (text == null || text.isEmpty()) {
                continue;
            }
            String withoutLinks = URL.matcher(Normalizer.normalize(text, Normalizer.Form.NFC)).replaceAll(" ");
            Matcher m = TAG.matcher(withoutLinks);
            while (m.find() && found.size() < MAX_FROM_TEXT) {
                String name = m.group(1);
                if (!ONLY_DIGITS.matcher(name).matches()) {
                    found.add(name.toLowerCase(Locale.ROOT));
                }
            }
        }
        return new ArrayList<>(found);
    }

    /** Ô "Thẻ" người dùng nhập cộng với #thẻ trong tiêu đề / mô tả, dạng chuỗi cho addTags (chưa bỏ trùng). */
    public static String mergeWithTagsInput(String tagsInput, String title, String description) {
        StringBuilder sb = new StringBuilder(tagsInput == null ? "" : tagsInput);
        for (String tag : extract(title, description)) {
            sb.append(' ').append(tag);
        }
        return sb.toString().trim();
    }
}
