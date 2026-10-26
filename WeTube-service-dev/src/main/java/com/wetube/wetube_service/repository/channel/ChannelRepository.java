package com.wetube.wetube_service.repository.channel;

import com.wetube.wetube_service.entity.channel.Channel;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRepository extends CrudRepository<Channel, UUID> {
    @Query("SELECT c FROM Channel c WHERE c.categories IS EMPTY")
    List<Channel> findChannelsWithoutCategories();

    List<Channel> findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc(String name);

    Optional<Channel> findByHandle(String handle);

    boolean existsByHandle(String handle);

    @Query("select c.handle from Channel c where c.handle is not null")
    List<String> findAllHandles();

    /** (id, name) của kênh chưa có handle, kênh cũ nhất trước. */
    @Query("select c.id, c.name from Channel c where c.handle is null order by c.createdAt asc")
    List<Object[]> findIdAndNameWithoutHandle();

    @Transactional
    @Modifying
    @Query("update Channel c set c.handle = :handle where c.id = :id and c.handle is null")
    int setInitialHandle(@Param("id") UUID id, @Param("handle") String handle);

    /** Chạy ngay (không đợi flush cuối giao dịch) để lỗi trùng handle bắt được tại chỗ. */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Channel c set c.handle = :handle, c.handleChangedAt = :at where c.id = :id")
    int updateHandle(@Param("id") UUID id, @Param("handle") String handle, @Param("at") LocalDateTime at);
}
