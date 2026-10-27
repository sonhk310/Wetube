package com.wetube.wetube_service.service.video;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Danh sách chủ đề cố định 2 tầng (C8), đọc từ resources/topics.json lúc khởi động. Id nhóm dạng "music",
 * id chủ đề con dạng "music.lo-fi". Mỗi video có đúng một id (nhóm hoặc chủ đề con); chủ đề con kéo theo nhóm.
 */
@Component
public class TopicCatalog {

    public record Topic(String id, String name) {
    }

    /** chip: tên ngắn hiện trên chip trang chủ. */
    public record Group(String id, String name, String chip, List<Topic> topics) {
    }

    private record TopicFile(List<Group> groups) {
    }

    private final List<Group> groups;
    private final Map<String, Group> groupById = new HashMap<>();
    private final Map<String, Topic> topicById = new HashMap<>();

    public TopicCatalog() {
        this(load());
    }

    TopicCatalog(List<Group> groups) {
        this.groups = List.copyOf(groups);
        for (Group g : groups) {
            if (g.id().contains(".") || groupById.put(g.id(), g) != null) {
                throw new IllegalStateException("topics.json: id nhóm sai hoặc trùng: " + g.id());
            }
            for (Topic t : g.topics()) {
                if (!t.id().startsWith(g.id() + ".") || topicById.put(t.id(), t) != null) {
                    throw new IllegalStateException("topics.json: id chủ đề sai hoặc trùng: " + t.id());
                }
            }
        }
    }

    private static List<Group> load() {
        try (InputStream in = new ClassPathResource("topics.json").getInputStream()) {
            return new ObjectMapper().readValue(in, TopicFile.class).groups();
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được topics.json", e);
        }
    }

    public List<Group> groups() {
        return groups;
    }

    public boolean exists(String id) {
        return id != null && (groupById.containsKey(id) || topicById.containsKey(id));
    }

    public Group group(String groupId) {
        return groupById.get(groupId);
    }

    /** Id đã chọn kèm id nhóm của nó (trường categories của Elasticsearch); id không có trong danh sách: rỗng. */
    public List<String> withGroup(String id) {
        if (!exists(id)) {
            return List.of();
        }
        return topicById.containsKey(id) ? List.of(id, groupIdOf(id)) : List.of(id);
    }

    /** Nhãn chip: tên ngắn của nhóm, hoặc tên chủ đề con. */
    public String label(String id) {
        Group g = groupById.get(id);
        if (g != null) {
            return g.chip();
        }
        Topic t = topicById.get(id);
        return t != null ? t.name() : id;
    }

    static String groupIdOf(String id) {
        int dot = id.indexOf('.');
        return dot < 0 ? id : id.substring(0, dot);
    }
}
