package com.wetube.wetube_service.entity.channel;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "channels")
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(value = SqlTypes.VARCHAR)
    private UUID id;
    private String name;
    private String picture;
    private String backgroundImgUrl;
    private String description;

    @Column(length = 2)
    private String countryCode;
    @Enumerated(EnumType.STRING)
    private ActiveStatus status;

    private float revenue;
    private int totalSubscribers;
    private int totalVideos;
    private int totalViews;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "channel")
    private AppUser user;

    @OneToMany(mappedBy = "channel", fetch = FetchType.EAGER,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MembershipTier> membershipTiers;

    public UUID getUserId(){
        return user.getId();
    }

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderPosition ASC")
    private List<ChannelCategory> categories = new ArrayList<>();
}