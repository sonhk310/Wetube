package com.wetube.wetube_service.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchKeywordsTest {

    @Test
    void splitsLowercasesAndRemovesDuplicates() {
        assertThat(SearchKeywords.clean("Thiên Thần,  thiên thần; Angel\nangel ,Evangelion"))
                .isEqualTo("thiên thần,angel,evangelion");
    }

    @Test
    void stripsListBulletsQuotesAndHashButKeepsLeadingNumbers() {
        assertThat(SearchKeywords.clean("1. \"angel\"\n- #remix\n2024 remix")).isEqualTo("angel,remix,2024 remix");
    }

    @Test
    void keepsAtMost15KeywordsOf40CharsEach() {
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            many.append("k").append(i).append(',');
        }
        assertThat(SearchKeywords.clean(many.toString()).split(",")).hasSize(15);
        assertThat(SearchKeywords.clean("a".repeat(60))).hasSize(40);
    }

    @Test
    void sameKeywordWithAndWithoutDiacriticsIsKeptOnce() {
        assertThat(SearchKeywords.clean("nhạc phim, nhac phim, Đề án, de an, ĐỀ ÁN, anime"))
                .isEqualTo("nhạc phim,đề án,anime");
    }

    @Test
    void nothingLeftGivesNull() {
        assertThat(SearchKeywords.clean(null)).isNull();
        assertThat(SearchKeywords.clean(" , ;\n")).isNull();
    }
}
