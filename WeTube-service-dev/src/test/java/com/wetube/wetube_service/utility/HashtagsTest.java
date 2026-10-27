package com.wetube.wetube_service.utility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class HashtagsTest {

    @Test
    void findsTagsInTitleAndDescriptionLowercaseWithoutDuplicates() {
        assertThat(Hashtags.extract("Tập 1 #AnimeVN hay", "Xem thêm #animevn và #Nhạc_Phim"))
                .containsExactly("animevn", "nhạc_phim");
    }

    @Test
    void vietnameseDiacriticsStayInsideTheTagInEitherUnicodeForm() {
        String decomposed = java.text.Normalizer.normalize("#thiênthần", java.text.Normalizer.Form.NFD);
        assertThat(Hashtags.extract(decomposed)).containsExactly("thiênthần");
    }

    @Test
    void ignoresHashAfterLettersAmpersandOrHashAndNumberOnlyTags() {
        assertThat(Hashtags.extract("Học C# và F# cơ bản", "&#39; ## #1 #2024 #")).isEmpty();
        assertThat(Hashtags.extract("#2024remix")).containsExactly("2024remix");
    }

    @Test
    void ignoresHashInsideLinks() {
        assertThat(Hashtags.extract("Xem tại https://a.b/c#phan-2 và #that")).containsExactly("that");
    }

    @Test
    void tagLongerThan64CharsIsNotATag() {
        assertThat(Hashtags.extract("#" + "a".repeat(64))).hasSize(1);
        assertThat(Hashtags.extract("#" + "a".repeat(65))).isEmpty();
    }

    @Test
    void keepsOnlyTheFirst15Tags() {
        String text = IntStream.rangeClosed(1, 20).mapToObj(i -> "#tag" + i).reduce("", (a, b) -> a + " " + b);
        assertThat(Hashtags.extract(text)).hasSize(15).startsWith("tag1", "tag2").doesNotContain("tag16");
    }

    @Test
    void mergeKeepsTheTagsInputAndAddsTheHashtags() {
        assertThat(Hashtags.mergeWithTagsInput("#nhạc rock", "Bài #hay", "mô tả #Rock")).isEqualTo("#nhạc rock hay rock");
        assertThat(Hashtags.mergeWithTagsInput(null, "Không có", null)).isEmpty();
    }
}
