package com.wetube.wetube_service.service.premium;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wetube.wetube_service.dto.PremiumUserDto;

public interface PremiumUserService {
    List<PremiumUserDto> getAllPremiumUsers();

    /**
     * Ngày hết hạn xa nhất của các gói Premium còn hiệu lực (ACTIVE, hết hạn từ hôm nay theo giờ Việt Nam
     * trở đi); rỗng nếu người dùng không có Premium. Mọi chỗ cần biết Premium đều hỏi hàm này.
     */
    Optional<LocalDate> premiumUntil(UUID userId);
}
