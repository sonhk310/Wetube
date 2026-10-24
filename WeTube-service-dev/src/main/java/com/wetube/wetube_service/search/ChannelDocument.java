package com.wetube.wetube_service.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Kênh trong chỉ mục "channels" (mapping ở resources/elastic/channels-mapping.json). Chỉ dùng để tìm; dữ liệu hiển thị lấy từ MySQL. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "channels", createIndex = false)
public class ChannelDocument {
    @Id
    private String id;
    private String name;
    private Integer totalSubscribers;
}
