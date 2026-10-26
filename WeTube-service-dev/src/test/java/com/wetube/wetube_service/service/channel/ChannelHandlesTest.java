package com.wetube.wetube_service.service.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class ChannelHandlesTest {

    @Test
    void normalizeStripsAtAndLowercases() {
        assertThat(ChannelHandles.normalize("  @HuynhKySon ")).isEqualTo("huynhkyson");
        assertThat(ChannelHandles.normalize(null)).isEmpty();
    }

    @Test
    void validHandlesPass() {
        for (String h : new String[] { "abc", "a1b", "huynh.ky-son_1", "x".repeat(30) }) {
            assertThat(ChannelHandles.invalidReason(h)).as(h).isNull();
        }
    }

    @Test
    void invalidHandlesAreRejected() {
        for (String h : new String[] { "", "ab", "x".repeat(31), "có dấu", "a b c", "Abc", ".abc", "abc-", "-abc", "ab@c" }) {
            assertThat(ChannelHandles.invalidReason(h)).as(h).isNotNull();
        }
    }

    @Test
    void generateStripsVietnameseAccents() {
        assertThat(ChannelHandles.generate("Huỳnh Kỳ Sơn", h -> false)).isEqualTo("huynhkyson");
        assertThat(ChannelHandles.generate("Đỗ Đạt", h -> false)).isEqualTo("dodat");
    }

    @Test
    void generateCutsTo30AndStaysValid() {
        String h = ChannelHandles.generate("a".repeat(50), x -> false);
        assertThat(h).hasSize(30);
        assertThat(ChannelHandles.invalidReason(h)).isNull();
    }

    @Test
    void generateAddsNumberWhenTaken() {
        Set<String> taken = Set.of("huynhkyson", "huynhkyson1");
        assertThat(ChannelHandles.generate("Huỳnh Kỳ Sơn", taken::contains)).isEqualTo("huynhkyson2");
    }

    @Test
    void generateKeepsLengthLimitWhenTakenLongName() {
        String base = "a".repeat(30);
        String h = ChannelHandles.generate(base, base::equals);
        assertThat(h).hasSize(30).endsWith("1");
        assertThat(ChannelHandles.invalidReason(h)).isNull();
    }

    @Test
    void generateFallsBackToRandomForUnusableNames() {
        for (String name : new String[] { null, "", "李小龍", "!!", "ab" }) {
            String h = ChannelHandles.generate(name, x -> false);
            assertThat(h).as(String.valueOf(name)).startsWith("user");
            assertThat(ChannelHandles.invalidReason(h)).isNull();
        }
    }

    @Test
    void extractMentionsFindsHandlesOnly() {
        assertThat(ChannelHandles.extractMentions("@huynhkyson ơi xem này, cảm ơn @lan.nguyen!"))
                .containsExactly("huynhkyson", "lan.nguyen");
        assertThat(ChannelHandles.extractMentions("mail abc@gmail.com không phải")).isEmpty();
        assertThat(ChannelHandles.extractMentions("@Abc @abc @ABC")).containsExactly("abc");
        assertThat(ChannelHandles.extractMentions("hết câu @abc.")).containsExactly("abc");
        assertThat(ChannelHandles.extractMentions("@ab quá ngắn")).isEmpty();
        assertThat(ChannelHandles.extractMentions(null)).isEmpty();
    }

    @Test
    void extractMentionsCapsAtTen() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            sb.append("@user").append(i).append(' ');
        }
        assertThat(ChannelHandles.extractMentions(sb.toString())).hasSize(10);
    }
}
