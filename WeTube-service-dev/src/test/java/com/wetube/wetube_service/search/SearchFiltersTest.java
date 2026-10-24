package com.wetube.wetube_service.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SearchFiltersTest {

    @Test
    void emptyParamsMeanNoFilterAndRelevance() {
        SearchFilters f = SearchFilters.of(null, "", null, null);

        assertThat(f.shortsOnly()).isNull();
        assertThat(f.minDurationSeconds()).isNull();
        assertThat(f.maxDurationSeconds()).isNull();
        assertThat(f.uploadedSince()).isNull();
        assertThat(f.sort()).isEqualTo(SearchFilters.Sort.RELEVANCE);
    }

    @Test
    void typeVideoOrShort() {
        assertThat(SearchFilters.of("video", null, null, null).shortsOnly()).isFalse();
        assertThat(SearchFilters.of("short", null, null, null).shortsOnly()).isTrue();
    }

    @Test
    void durationBuckets() {
        SearchFilters s = SearchFilters.of(null, "short", null, null);
        assertThat(s.minDurationSeconds()).isNull();
        assertThat(s.maxDurationSeconds()).isEqualTo(240.0);

        SearchFilters m = SearchFilters.of(null, "medium", null, null);
        assertThat(m.minDurationSeconds()).isEqualTo(240.0);
        assertThat(m.maxDurationSeconds()).isEqualTo(1200.0);

        SearchFilters l = SearchFilters.of(null, "long", null, null);
        assertThat(l.minDurationSeconds()).isEqualTo(1200.0);
        assertThat(l.maxDurationSeconds()).isNull();
    }

    @Test
    void uploadedUsesElasticDateMath() {
        assertThat(SearchFilters.of(null, null, "today", null).uploadedSince()).isEqualTo("now-1d");
        assertThat(SearchFilters.of(null, null, "week", null).uploadedSince()).isEqualTo("now-7d");
        assertThat(SearchFilters.of(null, null, "month", null).uploadedSince()).isEqualTo("now-30d");
        assertThat(SearchFilters.of(null, null, "year", null).uploadedSince()).isEqualTo("now-365d");
    }

    @Test
    void sortOptions() {
        assertThat(SearchFilters.of(null, null, null, "date").sort()).isEqualTo(SearchFilters.Sort.DATE);
        assertThat(SearchFilters.of(null, null, null, "views").sort()).isEqualTo(SearchFilters.Sort.VIEWS);
    }

    @Test
    void unknownValuesAreRejected() {
        assertThatThrownBy(() -> SearchFilters.of("movie", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchFilters.of(null, "huge", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchFilters.of(null, null, "decade", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SearchFilters.of(null, null, null, "rating"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
