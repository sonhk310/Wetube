package com.wetube.wetube_service.service.AI;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.video.SubtitleCue;

/** Đọc mảng cue Gemini trả về thành câu phụ đề (giờ tính bằng giây, giữ câu gốc). */
class SubtitleAiCuesTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void readsAllTimeFormatsAndKeepsOriginal() throws Exception {
        List<SubtitleCue> cues = SubtitleAiService.toCues(om.readTree("""
                [{"start":"00:01:02.500","end":"01:04.000","original":"Hello","translated":"Xin chào"},
                 {"start":"65,5","end":"66","original":"","translated":"Tạm biệt"}]"""));

        assertThat(cues).hasSize(2);
        assertThat(cues.get(0)).isEqualTo(new SubtitleCue(62.5, 64.0, "Xin chào", "Hello"));
        assertThat(cues.get(1)).isEqualTo(new SubtitleCue(65.5, 66.0, "Tạm biệt", null));
    }

    @Test
    void skipsEmptyTextBrokenTimesAndNonArrays() throws Exception {
        List<SubtitleCue> cues = SubtitleAiService.toCues(om.readTree("""
                [{"start":"00:00:01.000","end":"00:00:02.000","translated":"  "},
                 {"start":"abc","end":"00:00:02.000","translated":"hỏng giờ"},
                 {"start":"00:00:05.000","end":"00:00:04.000","translated":"kết thúc trước bắt đầu"},
                 {"start":"00:00:06.000","end":"00:00:07.000","translated":"ok"}]"""));

        assertThat(cues).extracting(SubtitleCue::getText).containsExactly("ok");
        assertThat(SubtitleAiService.toCues(om.readTree("{}"))).isEmpty();
    }
}
