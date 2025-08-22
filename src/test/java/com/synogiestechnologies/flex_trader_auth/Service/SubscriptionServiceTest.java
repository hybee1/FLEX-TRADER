package com.synogiestechnologies.flex_trader_auth.Service;

import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionDuration;
import com.synogiestechnologies.flex_trader_auth.Exceptions.InvalidTokenException;
import com.synogiestechnologies.flex_trader_auth.Exceptions.NoUserFoundException;
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
import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionPlanType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepo subscriptionRepo;

    @Mock
    private MyUsersRepo myUsersRepo;

    @Mock
    private JwtService jwtService;

    @Mock
    private SubscriptionManagerService subscriptionManagerService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private MyUsers user;
    private Subscription activeSub;
    private Subscription cancelledSub;
    private Subscription savedSub;
    private String token = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        user = new MyUsers();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");

        activeSub = Subscription.builder()
                .id(101L)
                .user(user)
                .subscriptionPlanType(SubscriptionPlanType.FREE)
                .subscriptionDuration(SubscriptionDuration.ONE_WEEK)
                .subStartDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .subExpiryDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();

        savedSub = Subscription.builder()
                    .id(123L)
                    .user(user)
                    .subscriptionPlanType(SubscriptionPlanType.FREE)
                    .subscriptionDuration(SubscriptionDuration.ONE_WEEK)
                    .subStartDate(Instant.now())
                    .subExpiryDate(Instant.now().plusMillis(SubscriptionDuration.ONE_WEEK.getDurationMillis()))
                    .build();

        cancelledSub = Subscription.builder()
                .id(123L)
                .user(user)
                .subscriptionPlanType(SubscriptionPlanType.FREE)
                .subscriptionDuration(SubscriptionDuration.ONE_WEEK)
                .subStartDate(Instant.now())
                .subExpiryDate(Instant.now().plusMillis(SubscriptionDuration.ONE_WEEK.getDurationMillis()))
                .build();

    }

    @Test
    void testFindNonExpiredSubscriptionWithUsername_ShouldReturnSub_WhenExists() {

        when(subscriptionRepo.findNonExpiredSubscriptionWithUsername(eq("alice"), any(Instant.class)))
                .thenReturn(Optional.of(activeSub));

        Optional<Subscription> result =
                subscriptionService.findNonExpiredSubscriptionWithUsername("alice", Instant.now());

        assertTrue(result.isPresent());
        assertEquals(SubscriptionPlanType.FREE, result.get().getSubscriptionPlanType());
    }

    @Test
    void testFindNonExpiredSubscriptionWithUsername_ShouldReturnEmpty_WhenNotExists() {
        when(subscriptionRepo.findNonExpiredSubscriptionWithUsername(eq("alice"),
                any(Instant.class))).thenReturn(Optional.empty());

        Optional<Subscription> result =
                subscriptionService.findNonExpiredSubscriptionWithUsername("alice", Instant.now());

        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateSubscription_ShouldDelegateToManagerService() {
        Map<String, Object> mockResponse = Map.of("data", Map.of("message", "Successful"));
        when(subscriptionManagerService.createSubscription("alice",
                "Free", "1 Week", false, token))
                .thenReturn(ResponseEntity.ok(mockResponse));

        ResponseEntity<Map<String, Object>> response =
                subscriptionService.createSubscription("alice",
                        "Free", "1 Week", false, token);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Successful", ((Map<String, String>) response.getBody().get("data")).get("message"));
    }

    @Test
    void testUpgradeSubLevelWhileLowerSubIsActive_ShouldUpgradeSuccessfully() {
        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(subscriptionRepo.findByUserAndIsCancelledFalseAndSubExpiryDateAfter(eq(user), any()))
                .thenReturn(Optional.of(activeSub));
        when(jwtService.extractUserName(token)).thenReturn("alice");

        // simulate repo save calls
        when(subscriptionRepo.save(any(Subscription.class)))
                .thenReturn(cancelledSub) // first call
                .thenReturn(savedSub); // second call

        ResponseEntity<Map<String, Object>> response =
                subscriptionService.upgradeSubLevelWhileLowerSubIsActive(
                        "alice", "Premium",
                        "1 Month", token);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals("Successful", ((Map<String, String>) response.getBody().get("data")).get("message"));
    }

    @Test
    void testUpgradeSubLevelWhileLowerSubIsActive_ShouldThrow_WhenUserNotFound() {

//        when(myUsersRepo.usernameOrEmail("bob"))
//                .thenThrow(new SubscriptionException("User not found", HttpStatus.NOT_FOUND));

        when(myUsersRepo.usernameOrEmail("bob")).thenReturn(Optional.empty());

        assertThrows(SubscriptionException.class, () ->
                subscriptionService.upgradeSubLevelWhileLowerSubIsActive(
                        "bob", "Premium", "1 Month", token));

    }

    @Test
    void testUpgradeSubLevelWhileLowerSubIsActive_ShouldThrow_WhenNoActiveSubscription() {

        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(subscriptionRepo.findByUserAndIsCancelledFalseAndSubExpiryDateAfter(eq(user), any()))
                .thenReturn(Optional.empty());

        assertThrows(SubscriptionException.class, () ->
                subscriptionService.upgradeSubLevelWhileLowerSubIsActive(
                        "alice", "Premium", "1 Month", token));
    }

    @Test
    void testUpgradeSubLevelWhileLowerSubIsActive_ShouldThrow_WhenTokenMismatch() {
        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(subscriptionRepo.findByUserAndIsCancelledFalseAndSubExpiryDateAfter(eq(user), any()))
                .thenReturn(Optional.of(activeSub));
        when(jwtService.extractUserName(token)).thenReturn("bob");

        assertThrows(SubscriptionException.class, () ->
                subscriptionService.upgradeSubLevelWhileLowerSubIsActive(
                        "alice", "Premium", "1 Month", token));
    }

    @Test
    void testUpgradeSubLevelWhileLowerSubIsActive_ShouldThrow_WhenInvalidPlanType() {

        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(subscriptionRepo.findByUserAndIsCancelledFalseAndSubExpiryDateAfter(eq(user), any()))
                .thenReturn(Optional.of(activeSub));
        when(jwtService.extractUserName(token)).thenReturn("alice");

        assertThrows(SubscriptionException.class, () ->
                subscriptionService.upgradeSubLevelWhileLowerSubIsActive(
                        "alice", "NonExistentPlan", "One Month", token));
    }

    @Test
    void testUpgradeSubLevelWhileLowerSubIsActive_ShouldThrow_WhenInvalidDuration() {
        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));
        when(subscriptionRepo.findByUserAndIsCancelledFalseAndSubExpiryDateAfter(eq(user), any()))
                .thenReturn(Optional.of(activeSub));
        when(jwtService.extractUserName(token)).thenReturn("alice");

        assertThrows(SubscriptionException.class, () ->
                subscriptionService.upgradeSubLevelWhileLowerSubIsActive(
                        "alice", "Premium", "NonExistentDuration", token));
    }

    @Test
    void testUpgradeSubLevelWhileLowerSubIsActive_ShouldThrow_WhenInvalidUpgradePath() {
        when(myUsersRepo.usernameOrEmail("alice")).thenReturn(Optional.of(user));

        // set current plan to PREMIUM, trying to "upgrade" to BASIC
        activeSub.setSubscriptionPlanType(SubscriptionPlanType.PREMIUM);

        when(subscriptionRepo.findByUserAndIsCancelledFalseAndSubExpiryDateAfter(eq(user), any()))
                .thenReturn(Optional.of(activeSub));
        when(jwtService.extractUserName(token)).thenReturn("alice");

        assertThrows(SubscriptionException.class, () ->
                subscriptionService.upgradeSubLevelWhileLowerSubIsActive(
                        "alice", "Basic", "One Month", token));
    }




}