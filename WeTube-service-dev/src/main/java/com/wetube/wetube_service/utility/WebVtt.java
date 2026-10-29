package com.wetube.wetube_service.utility;

import java.util.List;

import com.wetube.wetube_service.dto.video.SubtitleCue;

/** Dựng file WebVTT từ danh sách câu phụ đề. */
public final class WebVtt {

    private WebVtt() {
    }

    public static String build(List<SubtitleCue> cues) {
        StringBuilder sb = new StringBuilder("WEBVTT\n\n");
        for (SubtitleCue cue : cues) {
            // Dòng trống hoặc "-->" trong chữ sẽ làm vỡ khối cue
            String text = cue.getText().trim().replace("-->", "->").replaceAll("\\n\\s*\\n", "\n");
            sb.append(time(cue.getStart())).append(" --> ").append(time(cue.getEnd())).append('\n')
                    .append(text).append("\n\n");
        }
        return sb.toString();
    }

    /** Giây → "HH:MM:SS.mmm". */
    public static String time(double seconds) {
        long millis = Math.round(seconds * 1000);
        return String.format("%02d:%02d:%02d.%03d",
                millis / 3_600_000, (millis / 60_000) % 60, (millis / 1000) % 60, millis % 1000);
    }
}
