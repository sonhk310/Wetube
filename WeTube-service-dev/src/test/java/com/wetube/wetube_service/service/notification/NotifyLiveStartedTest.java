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

/** Báo cho người đăng ký khi kênh bắt đầu phát trực tiếp: đúng người (theo chuông), đúng một lần cho mỗi buổi. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotifyLiveStartedTest {

    @Mock NotificationRepository notificationRepo;
    @Mock SubscriptionRepository subscriptionRepo;

    @InjectMocks NotificationServiceImpl service;

    private final UUID owner = UUID.randomUUID();
    private final UUID channel = UUID.randomUUID();
    private final UUID live = UUID.randomUUID();
    private final UUID fan = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    private List<Notification> savedAfterNotifying(String title) {
        when(subscriptionRepo.findNotifiableSubscriberIds(any(), any())).thenReturn(List.of(fan, owner));

        service.notifyLiveStarted(live, owner, channel, title);

        ArgumentCaptor<List<Notification>> saved = ArgumentCaptor.forClass(List.class);
        verify(notificationRepo).saveAll(saved.capture());
        return saved.getValue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void reachesAllAndPersonalizeButNotMutedAndNeverTheOwner() {
        List<Notification> saved = savedAfterNotifying("Buổi phát");

        ArgumentCaptor<Collection<SubscriptionType>> modes = ArgumentCaptor.forClass(Collection.class);
        verify(subscriptionRepo).findNotifiableSubscriberIds(eq(channel), modes.capture());
        assertThat(modes.getValue()).containsExactlyInAnyOrder(SubscriptionType.ALL, SubscriptionType.PERSONALIZE);
        assertThat(saved).extracting(Notification::getRecipientId).containsExactly(fan);
    }

    @Test
    void doesNotSendTwiceForTheSameLive() {
        when(notificationRepo.existsByTypeAndLiveId(Notification.Type.LIVE_STARTED, live)).thenReturn(true);

        service.notifyLiveStarted(live, owner, channel, "Buổi phát");

        verify(subscriptionRepo, never()).findNotifiableSubscriberIds(any(), any());
        verify(notificationRepo, never()).saveAll(any());
    }

    @Test
    void savedNotificationCarriesTypeLiveIdActorAndTitle() {
        Notification n = savedAfterNotifying("Buổi phát đầu tiên").get(0);

        assertThat(n.getType()).isEqualTo(Notification.Type.LIVE_STARTED);
        assertThat(n.getLiveId()).isEqualTo(live);
        assertThat(n.getActorId()).isEqualTo(owner);
        assertThat(n.getText()).isEqualTo("Buổi phát đầu tiên");
        assertThat(n.isRead()).isFalse();
        assertThat(n.getCreatedAt()).isNotNull();
        assertThat(n.getVideoId()).isNull();
    }

    @Test
    void longTitleIsCutTo200Characters() {
        Notification n = savedAfterNotifying("x".repeat(300)).get(0);

        assertThat(n.getText()).hasSizeLessThanOrEqualTo(200);
    }

    @Test
    void noChannelMeansNoQuery() {
        service.notifyLiveStarted(live, owner, null, "Buổi phát");

        verify(subscriptionRepo, never()).findNotifiableSubscriberIds(any(), any());
    }
}
