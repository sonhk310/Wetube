package com.wetube.wetube_service.repository.premium;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.wetube.wetube_service.entity.premium.SubPack;

public interface SubPackRepository extends CrudRepository<SubPack, UUID> {
    
}
