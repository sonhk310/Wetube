package com.wetube.wetube_service.service.impl;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.Optional;

@Service
@AllArgsConstructor
public class GeoIPService {
    private final DatabaseReader dbReader;

    public Optional<String> getCountryCode(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            CountryResponse response = dbReader.country(inetAddress);
            return Optional.ofNullable(response.getCountry().getIsoCode());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
