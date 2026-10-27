package com.wetube.wetube_service.service.AI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.service.video.TopicCatalog;

class AIServiceKeywordsTest {

    private final GeminiClient gemini = mock(GeminiClient.class);
    private final AIService service = new AIService(gemini, new TopicCatalog());

    @BeforeEach
    void noPauseBetweenRetries() {
        service.retryPauseMillis = 0;
    }

    @Test
    void cleansGeminiAnswerAndDropsWordsThatAreTheTitleOrATag() throws Exception {
        when(gemini.generateText(anyString()))
                .thenReturn("Thiên thần, Angel, \"cruel angel\", remix, Cruel Angel's Thesis, #anime");

        String keywords = service.suggestSearchKeywords("Cruel Angel's Thesis", "mô tả", "#anime #remix");

        assertThat(keywords).isEqualTo("thiên thần,angel,cruel angel");
    }

    @Test
    void geminiFailingTwiceIsAnExplicit503NotAnEmptyList() throws Exception {
        when(gemini.generateText(anyString())).thenThrow(new RuntimeException("Gemini API error 503"));

        assertThatThrownBy(() -> service.suggestSearchKeywords("t", "d", ""))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getReason()).contains("quá tải");
                });
        verify(gemini, times(2)).generateText(anyString());
    }

    @Test
    void aTransientGeminiErrorIsRetriedOnce() throws Exception {
        when(gemini.generateText(anyString()))
                .thenThrow(new RuntimeException("503 high demand"))
                .thenReturn("angel, thiên thần");

        assertThat(service.suggestSearchKeywords("Cruel Angel's Thesis", "", "")).isEqualTo("angel,thiên thần");
        verify(gemini, times(2)).generateText(anyString());
    }

    @Test
    void emptyAnswerGivesNoKeywords() throws Exception {
        when(gemini.generateText(anyString())).thenReturn(" , \n");

        assertThat(service.suggestSearchKeywords("t", null, null)).isEmpty();
    }
}
