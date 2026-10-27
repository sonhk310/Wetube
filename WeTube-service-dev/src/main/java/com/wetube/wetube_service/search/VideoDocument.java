package com.wetube.wetube_service.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "videos", createIndex = false)
public class VideoDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "autocomplete", searchAnalyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "autocomplete", searchAnalyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String channelId;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Keyword)
    private List<String> categories;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    @Field(type = FieldType.Text)
    private String thumbnailUrl;

    @Field(type = FieldType.Text)
    private String videoUrl;

    @Field(type = FieldType.Integer)
    private Integer totalView;

    @Field(type = FieldType.Float)
    private Float duration;

    /** Cờ Shorts, để trang tìm kiếm lọc Video / Shorts. */
    @Field(type = FieldType.Boolean)
    private Boolean isShort;

    /** Từ khóa tìm kiếm do chủ video duyệt (S2). */
    @Field(type = FieldType.Text)
    private String searchKeywords;

    @Field(type = FieldType.Text)
    private String name;

    /** Tên kênh, để gõ tên kênh ra video của kênh. */
    @Field(type = FieldType.Text)
    private String channelName;

    @Field(type = FieldType.Text)
    private String picture;
}
