package com.wetube.wetube_service.service.premium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.premium.PremiumUserMapper;
import com.wetube.wetube_service.repository.premium.PremiumUserRepository;
import com.wetube.wetube_service.service.premium.impl.PremiumUserServiceImpl;

/** Định nghĩa Premium: gói ACTIVE còn hạn tính từ "hôm nay" theo giờ Việt Nam. */
class PremiumUserServiceTest {

    private final PremiumUserRepository repo = mock(PremiumUserRepository.class);
    private final PremiumUserServiceImpl service = new PremiumUserServiceImpl(repo, mock(PremiumUserMapper.class));
    private final UUID userId = UUID.randomUUID();

    /** 2026-10-29 18:00 UTC = 2026-10-30 01:00 giờ Việt Nam. */
    private void clockAt(String utcInstant) {
        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(Instant.parse(utcInstant), ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    @Test
    void returnsTheLatestActiveExpiryTheRepositoryFinds() {
        clockAt("2026-10-29T05:00:00Z");
        LocalDate expiry = LocalDate.of(2026, 11, 20);
        when(repo.findLatestEndDate(userId, ActiveStatus.ACTIVE, LocalDate.of(2026, 10, 29))).thenReturn(expiry);

        assertThat(service.premiumUntil(userId)).contains(expiry);
    }

    @Test
    void noActivePackMeansNotPremium() {
        clockAt("2026-10-29T05:00:00Z");

        assertThat(service.premiumUntil(userId)).isEmpty();
    }

    @Test
    void todayIsTheVietnamDateNotTheUtcDate() {
        clockAt("2026-10-29T18:00:00Z");
        LocalDate expiredInUtcTerms = LocalDate.of(2026, 10, 29);
        // gói hết hạn đúng 29/10: lúc 01:00 sáng 30/10 giờ Việt Nam thì đã hết hạn, dù UTC vẫn là 29/10
        when(repo.findLatestEndDate(userId, ActiveStatus.ACTIVE, LocalDate.of(2026, 10, 29))).thenReturn(expiredInUtcTerms);

        assertThat(service.premiumUntil(userId)).isEmpty();
        verify(repo).findLatestEndDate(userId, ActiveStatus.ACTIVE, LocalDate.of(2026, 10, 30));
    }

    @Test
    void packExpiringTodayIsStillPremium() {
        clockAt("2026-10-29T05:00:00Z");
        LocalDate today = LocalDate.of(2026, 10, 29);
        when(repo.findLatestEndDate(userId, ActiveStatus.ACTIVE, today)).thenReturn(today);

        assertThat(service.premiumUntil(userId)).contains(today);
    }

    @Test
    void anonymousIsNeverPremiumAndSkipsTheQuery() {
        assertThat(service.premiumUntil(null)).isEmpty();
        verify(repo, never()).findLatestEndDate(any(), eq(ActiveStatus.ACTIVE), any());
    }
}
