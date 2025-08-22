package com.synogiestechnologies.flex_trader_auth.Service;

import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionDuration;
import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionPlanType;
import com.synogiestechnologies.flex_trader_auth.Exceptions.InvalidTokenException;
import com.synogiestechnologies.flex_trader_auth.Exceptions.SubscriptionException;
import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import com.synogiestechnologies.flex_trader_auth.Models.Subscription;
import com.synogiestechnologies.flex_trader_auth.Repository.MyUsersRepo;
import com.synogiestechnologies.flex_trader_auth.Repository.SubscriptionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class SubscriptionManagerServiceTest {

    @Mock
    private SubscriptionRepo subscriptionRepo;

    @Mock
    private MyUsersRepo myUsersRepo;

    @Mock
    private JwtService jwtService;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionManagerService subscriptionManagerService;

    private MyUsers user;
    private String token = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        user = new MyUsers();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
    }

    // helper to create subscription entity
    private Subscription mockSubscription() {
        return Subscription.builder()
                .id(123L)
                .user(user)
                .subscriptionPlanType(SubscriptionPlanType.FREE)
                .subscriptionDuration(SubscriptionDuration.ONE_WEEK)
                .subStartDate(Instant.now())
                .subExpiryDate(Instant.now().plusMillis(SubscriptionDuration.ONE_WEEK.getDurationMillis()))
                .build();
    }

    private Subscription mockUpgradedSubscription() {
        return Subscription.builder()
                .id(123L)
                .user(user)
                .subscriptionPlanType(SubscriptionPlanType.PRO)
                .subscriptionDuration(SubscriptionDuration.ONE_MONTH)
                .upgradedToHigherSUbPlan(true)
                .isCancelled(false)
                .hasUsedFreePlan(true)
                .subStartDate(Instant.now())
                .subExpiryDate(Instant.now().plusMillis(SubscriptionDuration.ONE_MONTH.getDurationMillis()))
                .build();
    }

    @Test
    void testCreateSubscription_ShouldReturnOkResponse_WhenValidInput() {
        // Arrange
        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(jwtService.extractUserName(token)).thenReturn("alice");
        when(subscriptionRepo.userHasUsedFreePlan("alice")).thenReturn(false);
        when(subscriptionRepo.existsByUserAndIsCancelledFalseAndSubExpiryDateAfter(eq(user), any()))
                .thenReturn(false);
        when(subscriptionRepo.save(any())).thenReturn(mockSubscription());

        Map<String, Object> mockResponse = Map.of(
                "data", Map.of("token", "new.jwt.token")
        );
        when(jwtService.generateLoginPlusWebsocketTokenAndSaveIt(eq(user), any()))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        ResponseEntity<Map<String, Object>> response = subscriptionManagerService
                .createSubscription("alice", "Free",
                        "1 Week", false, token);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(((Map<String, String>) body.get("data")).containsKey("token"));
        assertEquals("Successful", ((Map<String, String>) body.get("data")).get("message"));
    }

    @Test
    void testCreateSubscription_ShouldThrowException_WhenUsernameAndTokenExtractUsernameDoesNotMatch() {
        // Arrange
        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(jwtService.extractUserName(token)).thenReturn("eve");

        // Assert
        assertThrows(InvalidTokenException.class, () -> subscriptionManagerService
                .createSubscription("alice", "Free",
                        "1 Week", false, token));
    }

    @Test
    void testCreateSubscription_ShouldThrow_WhenFreePlanAlreadyUsed() {
        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(jwtService.extractUserName(token)).thenReturn("alice");
        when(subscriptionRepo.userHasUsedFreePlan("alice")).thenReturn(true);

        assertThrows(SubscriptionException.class, () ->
                subscriptionManagerService.createSubscription("alice", "Free", "One Week", false, token)
        );
    }

    @Test
    void testCreateSubscription_ShouldThrowException_WhenSubscriptionPlanTypeNotFound() {
        // Arrange
        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(jwtService.extractUserName(token)).thenReturn("alice");

        // Assert
        assertThrows(SubscriptionException.class, () -> subscriptionManagerService
                .createSubscription("alice", "No such plan",
                        "1 Week", false, token));
    }

    @Test
    void testCreateSubscription_ShouldThrowException_WhenSubscriptionDurationNotFound() {
        // Arrange
        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(jwtService.extractUserName(token)).thenReturn("alice");

        // Assert
        assertThrows(SubscriptionException.class, () -> subscriptionManagerService
                .createSubscription("alice", "Free",
                        "No such plan duration", false, token));
    }

    @Test
    void testCreateSubscription_ShouldCallUpgrade_WhenUpgradeTrueAndActiveSubExists() {
        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(jwtService.extractUserName(token)).thenReturn("alice");
        when(subscriptionRepo.userHasUsedFreePlan("alice")).thenReturn(false);
        when(subscriptionRepo.existsByUserAndIsCancelledFalseAndSubExpiryDateAfter(eq(user), any()))
                .thenReturn(true);

        ResponseEntity<Map<String, Object>> expected = ResponseEntity.ok(Map.of("data",
                Map.of("token", "upgraded.jwt")));
        when(subscriptionService.upgradeSubLevelWhileLowerSubIsActive("alice",
                "Premium", "1 Month", token))
                .thenReturn(expected);

        ResponseEntity<Map<String, Object>> result =
                subscriptionManagerService.createSubscription("alice",
                        "Premium", "1 Month", true, token);

        assertEquals("upgraded.jwt",
                ((Map<String, String>) result.getBody().get("data")).get("token"));
    }


}