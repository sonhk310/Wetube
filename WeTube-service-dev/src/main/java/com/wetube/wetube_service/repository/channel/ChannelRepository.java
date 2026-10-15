package com.wetube.wetube_service.repository.channel;

import com.wetube.wetube_service.entity.channel.Channel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ChannelRepository extends CrudRepository<Channel, UUID> {
    @Query("SELECT c FROM Channel c WHERE c.categories IS EMPTY")
    List<Channel> findChannelsWithoutCategories();
}
