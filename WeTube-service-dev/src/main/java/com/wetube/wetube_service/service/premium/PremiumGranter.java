package com.wetube.wetube_service.service.premium;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.wetube.wetube_service.entity.premium.PaymentMomo;
import com.wetube.wetube_service.entity.premium.PremiumUser;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.PaymentStatus;
import com.wetube.wetube_service.repository.premium.PremiumUserRepository;

import lombok.RequiredArgsConstructor;

/** Cấp gói Premium cho đơn đã thanh toán thành công, dùng chung cho mọi cổng thanh toán (MoMo, PayPal). */
@Component
@RequiredArgsConstructor
public class PremiumGranter {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PremiumUserRepository premiumUserRepo;
    private final PremiumUserService premiumUserService;

    /**
     * Tạo dòng premium_user cho chủ đơn và đánh dấu đơn thành công. Gia hạn cộng dồn: gói mới bắt đầu sau ngày hết
     * hạn xa nhất còn hiệu lực, không mất ngày còn lại. Người gọi lưu lại đơn (payment).
     */
    public void grant(PaymentMomo payment) {
        LocalDate start = premiumUserService.premiumUntil(payment.getUser().getId())
                .map(until -> until.plusDays(1))
                .orElseGet(() -> LocalDate.now(VIETNAM));

        PremiumUser premiumUser = PremiumUser.builder()
                .user(payment.getUser())
                .subpack(payment.getSubpack())
                .startDate(start)
                .endDate(start.plusDays(payment.getSubpack().getDurationDays()))
                .status(ActiveStatus.ACTIVE)
                .build();
        premiumUserRepo.saveAndFlush(premiumUser);

        payment.setPremiumUser(premiumUser);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setEventType("PAYMENT_SUCCESS");
    }
}
