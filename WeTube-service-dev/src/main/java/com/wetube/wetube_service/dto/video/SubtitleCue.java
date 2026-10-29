package com.wetube.wetube_service.dto.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một câu phụ đề: giờ tính bằng giây; original là câu gốc AI nghe được (để đối chiếu bản dịch), có thể null. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleCue {
    private double start;
    private double end;
    private String text;
    private String original;
}
