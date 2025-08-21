package com.synogiestechnologies.flex_trader_auth.Models;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionDuration;
import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionPlanType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;


@ToString(exclude = {"user"})
@EqualsAndHashCode(exclude = {"user"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "subscription_plan_tbl", indexes = { @Index(name = "idx_sub_expiry",
        columnList = "subExpiryDate") })
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private SubscriptionPlanType subscriptionPlanType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private SubscriptionDuration subscriptionDuration;

    @Builder.Default
    @Column(nullable = false)
    private boolean isCancelled = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean upgradedToHigherSUbPlan = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean hasUsedFreePlan = false;

    @Column(nullable = false, updatable = false)
    private Instant subExpiryDate;

    @Column(nullable = false, updatable = false)
    private Instant subStartDate;

    @ManyToOne(fetch = FetchType.LAZY)  // FetchType.EAGER
    @JsonBackReference
    @JoinColumn(name = "user_id", nullable = false)
    private MyUsers user;

    @CreatedDate
    @Column(nullable = false, updatable = false)  // can only be inserted
    private Instant createdDate;
}
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JsonBackReference
//    private Order order;