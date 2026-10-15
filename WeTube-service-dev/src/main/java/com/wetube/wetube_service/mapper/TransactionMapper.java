package com.wetube.wetube_service.mapper;

import com.wetube.wetube_service.dto.MembershipPurchaseResponse;
import com.wetube.wetube_service.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "id", target = "transactionId")

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "user.email", target = "userEmail")

    @Mapping(source = "channel.id", target = "channelId")
    @Mapping(source = "channel.name", target = "channelName")

    @Mapping(source = "membership.id", target = "membershipId")
    @Mapping(source = "membership.title", target = "membershipTitle")

    @Mapping(source = "amount", target = "membershipPrice") 
    @Mapping(source = "createdAt", target = "createdAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    MembershipPurchaseResponse toDto(Transaction transaction);
}
