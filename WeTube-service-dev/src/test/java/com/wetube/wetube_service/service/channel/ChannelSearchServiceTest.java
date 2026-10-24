package com.wetube.wetube_service.service.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wetube.wetube_service.dto.response.ChannelSearchDto;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.repository.channel.ChannelRepository;

@ExtendWith(MockitoExtension.class)
class ChannelSearchServiceTest {

    @Mock ChannelRepository channelRepository;
    @InjectMocks ChannelSearchService service;

    private static Channel channel(String name, ActiveStatus status) {
        Channel c = new Channel();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setStatus(status);
        c.setTotalSubscribers(3);
        return c;
    }

    @Test
    void returnsMatchingChannelsExceptHiddenOnes() {
        when(channelRepository.findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc("son"))
                .thenReturn(List.of(channel("Sơn TV", ActiveStatus.ACTIVE), channel("Son cũ", ActiveStatus.INACTIVE),
                        channel("Son xoá", ActiveStatus.DELETED), channel("Son mới", null)));

        List<ChannelSearchDto> result = service.search(" son ");

        assertThat(result).extracting(ChannelSearchDto::name).containsExactly("Sơn TV", "Son mới");
        assertThat(result.get(0).totalSubscribers()).isEqualTo(3);
    }

    @Test
    void blankQueryReturnsNothing() {
        assertThat(service.search("  ")).isEmpty();
        verify(channelRepository, never()).findTop20ByNameContainingIgnoreCaseOrderByTotalSubscribersDesc(anyString());
    }
}
