package com.wetube.wetube_service.service.premium;


import java.util.List;
import com.wetube.wetube_service.dto.PremiumUserDto;

public interface PremiumUserService {
    List<PremiumUserDto> getAllPremiumUsers();
}
