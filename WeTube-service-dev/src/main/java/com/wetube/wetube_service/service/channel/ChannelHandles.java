package com.wetube.wetube_service.service.channel;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Quy tắc, cách sinh và cách tách @handle của kênh. Không phụ thuộc DB nên test thẳng được. */
public final class ChannelHandles {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 30;
    public static final int MAX_MENTIONS = 10;

    private static final Pattern VALID = Pattern.compile("^[a-z0-9](?:[a-z0-9_.-]{1,28})[a-z0-9]$");
    // @handle đứng sau khoảng trắng / đầu chuỗi / dấu câu, không phải sau chữ hay số (tránh bắt "abc@gmail.com")
    private static final Pattern MENTION = Pattern.compile("(?<![\\p{L}\\p{N}_.\\-])@([a-z0-9][a-z0-9_.-]{1,28}[a-z0-9])(?![\\p{L}\\p{N}_@])",
            Pattern.CASE_INSENSITIVE);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private ChannelHandles() {
    }

    /** Bỏ "@" đầu, bỏ khoảng trắng hai đầu, viết thường. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return s.startsWith("@") ? s.substring(1) : s;
    }

    /** Lý do không hợp lệ (tiếng Việt), hoặc null nếu hợp lệ. Nhận chuỗi đã normalize. */
    public static String invalidReason(String handle) {
        if (handle == null || handle.length() < MIN_LENGTH) {
            return "Handle phải có ít nhất " + MIN_LENGTH + " ký tự";
        }
        if (handle.length() > MAX_LENGTH) {
            return "Handle tối đa " + MAX_LENGTH + " ký tự";
        }
        if (!handle.matches("^[a-z0-9_.-]+$")) {
            return "Handle chỉ gồm chữ thường không dấu, số, _ . và -";
        }
        if (!VALID.matcher(handle).matches()) {
            return "Handle phải bắt đầu và kết thúc bằng chữ hoặc số";
        }
        return null;
    }

    /** Sinh handle từ tên kênh; {@code taken} cho biết handle đã có người dùng chưa. */
    public static String generate(String name, Predicate<String> taken) {
        String base = slug(name);
        if (base.length() < MIN_LENGTH) {
            base = "user" + randomChars(6);
        }
        String candidate = base;
        for (int i = 1; taken.test(candidate); i++) {
            String suffix = String.valueOf(i);
            candidate = base.substring(0, Math.min(base.length(), MAX_LENGTH - suffix.length())) + suffix;
            // Phần cắt có thể kết thúc bằng "." hoặc "-"; đã nối số nên vẫn hợp lệ
        }
        return candidate;
    }

    /** Tên -> chữ thường không dấu, chỉ giữ a-z 0-9 _ . -, cắt còn 30 ký tự, bỏ . và - ở hai đầu. */
    static String slug(String name) {
        if (name == null) {
            return "";
        }
        String s = name.replace('đ', 'd').replace('Đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "");
        if (s.length() > MAX_LENGTH) {
            s = s.substring(0, MAX_LENGTH);
        }
        return s.replaceAll("^[._-]+|[._-]+$", "");
    }

    /** Các @handle (chữ thường, không trùng, tối đa {@link #MAX_MENTIONS}) có trong nội dung. */
    public static Set<String> extractMentions(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null) {
            return found;
        }
        Matcher m = MENTION.matcher(text);
        while (m.find() && found.size() < MAX_MENTIONS) {
            found.add(m.group(1).toLowerCase(Locale.ROOT));
        }
        return found;
    }

    private static String randomChars(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }
}
