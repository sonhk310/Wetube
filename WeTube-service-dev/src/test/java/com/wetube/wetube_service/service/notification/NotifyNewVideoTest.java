package com.wetube.wetube_service.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.wetube.wetube_service.entity.notification.Notification;
import com.wetube.wetube_service.enumeration.SubscriptionType;
import com.wetube.wetube_service.repository.NotificationRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.service.notification.impl.NotificationServiceImpl;

/** Chuông của kênh: ai nhận thông báo video mới tuỳ chế độ Tất cả / Cá nhân hoá / Không và loại video. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotifyNewVideoTest {

    @Mock NotificationRepository notificationRepo;
    @Mock SubscriptionRepository subscriptionRepo;

    @InjectMocks NotificationServiceImpl service;

    private final UUID owner = UUID.randomUUID();
    private final UUID channel = UUID.randomUUID();
    private final UUID video = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    private Collection<SubscriptionType> modesQueriedFor(boolean isShort) {
        UUID fan = UUID.randomUUID();
        when(subscriptionRepo.findNotifiableSubscriberIds(any(), any())).thenReturn(List.of(fan, owner));

        service.notifyNewVideo(video, owner, channel, isShort);

        ArgumentCaptor<Collection<SubscriptionType>> modes = ArgumentCaptor.forClass(Collection.class);
        verify(subscriptionRepo).findNotifiableSubscriberIds(eq(channel), modes.capture());
        return modes.getValue();
    }

    @Test
    void regularVideoReachesAllAndPersonalizedButNotMuted() {
        assertThat(modesQueriedFor(false))
                .containsExactlyInAnyOrder(SubscriptionType.ALL, SubscriptionType.PERSONALIZE)
                .doesNotContain(SubscriptionType.NONE_NOTIFICATION);
    }

    @Test
    void shortsOnlyReachSubscribersWhoChoseAll() {
        assertThat(modesQueriedFor(true)).containsExactly(SubscriptionType.ALL);
    }

    @SuppressWarnings("unchecked")
    @Test
    void ownerIsNeverNotifiedAboutOwnVideo() {
        UUID fan = UUID.randomUUID();
        when(subscriptionRepo.findNotifiableSubscriberIds(any(), any())).thenReturn(List.of(fan, owner));

        service.notifyNewVideo(video, owner, channel, false);

        ArgumentCaptor<List<Notification>> saved = ArgumentCaptor.forClass(List.class);
        verify(notificationRepo).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Notification::getRecipientId).containsExactly(fan);
    }

    @Test
    void noChannelMeansNoQuery() {
        service.notifyNewVideo(video, owner, null, false);

        verify(subscriptionRepo, never()).findNotifiableSubscriberIds(any(), any());
    }
}
