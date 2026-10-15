package com.wetube.wetube_service.entity.channel;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "tiers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_title_price",
                columnNames = {"channel_id", "title", "price"}
        )
)
public class MembershipTier {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(value = SqlTypes.VARCHAR)
    private UUID id;

    private String title;
    private float price;
    private String description;

    @Column(name = "is_default")
    private boolean defaultTier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private Channel channel;

    @OneToMany(mappedBy = "id.tier")
    private List<Subscription> subscriptions;
}
