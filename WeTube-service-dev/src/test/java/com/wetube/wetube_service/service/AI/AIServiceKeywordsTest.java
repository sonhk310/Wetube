package com.wetube.wetube_service.service.AI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class AIServiceKeywordsTest {

    private final GeminiClient gemini = mock(GeminiClient.class);
    private final AIService service = new AIService(gemini);

    @Test
    void cleansGeminiAnswerAndDropsWordsThatAreTheTitleOrATag() throws Exception {
        when(gemini.generateText(anyString()))
                .thenReturn("Thiên thần, Angel, \"cruel angel\", remix, Cruel Angel's Thesis, #anime");

        String keywords = service.suggestSearchKeywords("Cruel Angel's Thesis", "mô tả", "#anime #remix");

        assertThat(keywords).isEqualTo("thiên thần,angel,cruel angel");
    }

    @Test
    void geminiFailureGivesNoKeywordsInsteadOfAnError() throws Exception {
        when(gemini.generateText(anyString())).thenThrow(new RuntimeException("Gemini API error"));

        assertThat(service.suggestSearchKeywords("t", "d", "")).isEmpty();
    }

    @Test
    void emptyAnswerGivesNoKeywords() throws Exception {
        when(gemini.generateText(anyString())).thenReturn(" , \n");

        assertThat(service.suggestSearchKeywords("t", null, null)).isEmpty();
    }
}
