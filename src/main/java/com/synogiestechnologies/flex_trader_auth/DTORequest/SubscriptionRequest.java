package com.synogiestechnologies.flex_trader_auth.DTORequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionRequest {
    private String usernameOrEmail;
    private String subscriptionPlanType;
    private String subscriptionDuration;
    private String token;

}
