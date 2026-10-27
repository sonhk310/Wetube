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
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.service.video.TopicCatalog;

class AIServiceTopicTest {

    private final GeminiClient gemini = mock(GeminiClient.class);
    private final AIService service = new AIService(gemini, new TopicCatalog());

    @BeforeEach
    void noPauseBetweenRetries() {
        service.retryPauseMillis = 0;
    }

    @Test
    void picksAGroupThenATopicInsideIt() throws Exception {
        when(gemini.generateText(anyString())).thenReturn("music", "`music.lo-fi`.");

        assertThat(service.suggestTopic("Lofi chill", "nhạc học bài", "#lofi", "")).isEqualTo("music.lo-fi");

        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(gemini, times(2)).generateText(prompts.capture());
        // Bước 2 chỉ đưa chủ đề của nhóm đã chọn
        assertThat(prompts.getAllValues().get(1)).contains("music.lo-fi").doesNotContain("sports.bong-da");
    }

    @Test
    void anUnknownTopicFallsBackToTheWholeGroup() throws Exception {
        when(gemini.generateText(anyString())).thenReturn("sports", "sports.khong-co");

        assertThat(service.suggestTopic("t", "d", "", "")).isEqualTo("sports");
    }

    @Test
    void aTopicFromAnotherGroupFallsBackToTheChosenGroup() throws Exception {
        when(gemini.generateText(anyString())).thenReturn("sports", "music.lo-fi");

        assertThat(service.suggestTopic("t", "d", "", "")).isEqualTo("sports");
    }

    @Test
    void anUnknownGroupGivesNoSuggestion() throws Exception {
        when(gemini.generateText(anyString())).thenReturn("khong-co");

        assertThat(service.suggestTopic("t", "d", "", "")).isEmpty();
        verify(gemini, times(1)).generateText(anyString());
    }

    @Test
    void geminiFailingTwiceIs503() throws Exception {
        when(gemini.generateText(anyString())).thenThrow(new RuntimeException("Gemini API error 503"));

        assertThatThrownBy(() -> service.suggestTopic("t", "d", "", ""))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(gemini, times(2)).generateText(anyString());
    }
}
