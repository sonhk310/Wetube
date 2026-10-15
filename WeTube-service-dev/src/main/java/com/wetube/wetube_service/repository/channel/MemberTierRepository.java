package com.wetube.wetube_service.repository.channel;

import com.wetube.wetube_service.entity.channel.MembershipTier;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface MemberTierRepository extends CrudRepository<MembershipTier, UUID> {
}
