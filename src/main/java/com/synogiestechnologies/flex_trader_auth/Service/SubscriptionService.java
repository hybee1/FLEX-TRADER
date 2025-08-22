package com.synogiestechnologies.flex_trader_auth.Service;

import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionDuration;
import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionPlanType;
import com.synogiestechnologies.flex_trader_auth.Exceptions.InvalidTokenException;
import com.synogiestechnologies.flex_trader_auth.Exceptions.NoUserFoundException;
import com.synogiestechnologies.flex_trader_auth.Exceptions.SubscriptionException;
import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import com.synogiestechnologies.flex_trader_auth.Models.Subscription;
import com.synogiestechnologies.flex_trader_auth.Repository.MyUsersRepo;
import com.synogiestechnologies.flex_trader_auth.Repository.SubscriptionRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class SubscriptionService {

    private final SubscriptionRepo subscriptionRepo;
    private final MyUsersRepo myUsersRepo;
    private final JwtService jwtService;
   final SubscriptionManagerService subscriptionManagerService;

    public SubscriptionService(SubscriptionRepo subscriptionRepo, MyUsersRepo myUsersRepo,
                               @Lazy JwtService jwtService,
                               SubscriptionManagerService subscriptionManagerService
    ) {

        this.subscriptionRepo = subscriptionRepo;
        this.myUsersRepo = myUsersRepo;
        this.jwtService = jwtService;
        this.subscriptionManagerService = subscriptionManagerService;
    }

    public Optional<Subscription> findNonExpiredSubscriptionWithUsername(
            String userName, Instant now) {

        return subscriptionRepo.findNonExpiredSubscriptionWithUsername(userName, now);
    }



    public ResponseEntity<Map<String, Object>> createSubscription(
                                        String usernameOrEmail, String subscriptionPlanType,
                                        String subscriptionDuration,
                                        boolean upgrade,String token) {

        return subscriptionManagerService
                .createSubscription(usernameOrEmail, subscriptionPlanType, subscriptionDuration,
                                false, token);
    }

    public ResponseEntity<Map<String, Object>> upgradeSubLevelWhileLowerSubIsActive(
                                                             String usernameOrEmail,
                                                             String subscriptionPlanType,
                                                             String subscriptionDuration, String token) {

        log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 1");

        try{

            MyUsers user = myUsersRepo.usernameOrEmail(usernameOrEmail)
                    .orElseThrow(() -> new NoUserFoundException("User not found", HttpStatus.NOT_FOUND));

            log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 2");

            Subscription currentSubPlan = subscriptionRepo
                    .findByUserAndIsCancelledFalseAndSubExpiryDateAfter(user, Instant.now())
                    .orElseThrow(() -> {
                        log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 2a");
                        log.info("You cannot upgrade: no active subscription found.");
                        return new SubscriptionException("You cannot upgrade: no active subscription found.",
                                HttpStatus.NOT_FOUND);
                    });

            if(!user.getUsername().equals(jwtService.extractUserName(token)) ){

                throw new InvalidTokenException("Token does not match user.",
                        HttpStatus.UNAUTHORIZED);
            }

            // GET THE CURRENT SUBSCRIPTION PLAN-TYPE
            SubscriptionPlanType currentSubPlanType = currentSubPlan.getSubscriptionPlanType();

           SubscriptionPlanType upgradeSubPlanType = SubscriptionPlanType.FREE;

            log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 3");

            // GET THE UPGRADE SUBSCRIPTION PLAN-TYPE
            try {
                upgradeSubPlanType = SubscriptionPlanType.fromDisplayName(subscriptionPlanType);

                log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 3a");
                log.info("Found plan: " + upgradeSubPlanType + " - $" +
                        upgradeSubPlanType.getPricePerMonth());

            } catch (IllegalArgumentException e) {

                log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 3b");

                log.error("wrong subscription plan type" + e.getMessage());
                throw new IllegalArgumentException("wrong subscription plan type");
            }

            log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 4");
            // check if plan can be upgraded to a higher (which is the plan you want to upgrade to)
            if (!currentSubPlanType.currentPlanTypeCanBeUpgradedToThis(upgradeSubPlanType)) {
                throw new SubscriptionException("You can not upgrade from your current plan to this plan",
                        HttpStatus.BAD_REQUEST);
            }

            log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 5");

            //Subscription currentSub = currentSubPlan.get();
            Instant now = Instant.now();

            // Calculate remaining duration in days
            long remainingDaysInMillis =
                    currentSubPlan.getSubExpiryDate().toEpochMilli() - now.toEpochMilli();

            // return zero or remainingDaysInMillis in case line above returns -ve
            remainingDaysInMillis = Math.max(0, remainingDaysInMillis); // prevent negative

            long remainingDays = TimeUnit.MILLISECONDS.toDays(remainingDaysInMillis);
            long bonusMillis = 0;

            // Only add half the remaining time if between 1 and 7 days
            bonusMillis = bonusDaysInMillis(remainingDays);

            log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 6");

            // Calculate new subscription expiry
            SubscriptionDuration upgradeSubPlanDuration = SubscriptionDuration.ONE_WEEK;

            try {
                log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 7");
                upgradeSubPlanDuration = SubscriptionDuration.fromDisplayName(subscriptionDuration);

                log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 7a");
                log.info("Found plan duration: " + upgradeSubPlanDuration + " - $" +
                        upgradeSubPlanDuration.getDurationMillis());

            } catch (IllegalArgumentException e) {
                log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 7b");
                log.error("wrong subscription plan duration" + e.getMessage());
                throw new IllegalArgumentException("wrong subscription plan duration");
            }

            // Calculate upgraded subscription expiry
            long newTotalDurationMillis = upgradeSubPlanDuration.getDurationMillis() + bonusMillis;
            Instant newSubStartDate = now;
            Instant newSubExpiryDate = newSubStartDate.plusMillis(newTotalDurationMillis);

            log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 8");

            // Cancel the old sub
            currentSubPlan.setCancelled(true);
            currentSubPlan.setUpgradedToHigherSUbPlan(true);
            Subscription cancelSub = subscriptionRepo.save(currentSubPlan);
            log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 8b");

            if (cancelSub.getId() == null) {

                log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 9a");
                log.error("Unable to cancel Subscription to DB in Subscription-Service");
                throw new SubscriptionException(
                        "something went wrong please try again later",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 10");

            // Create and save the new upgraded subscription
            Subscription upgradedSub = Subscription.builder()
                    .user(user)
                    .subscriptionPlanType(upgradeSubPlanType)
                    .subscriptionDuration(upgradeSubPlanDuration)
                    .subStartDate(newSubStartDate)
                    .subExpiryDate(newSubExpiryDate)
                    .build();

            Subscription savedNewSub = subscriptionRepo.save(upgradedSub);
            log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 10a");
            if (savedNewSub.getId() == null) {
                log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 10b");

                log.error("Unable to save upgraded Subscription to DB in Subscription-Service");
                throw new SubscriptionException(
                        "something went wrong please try again later",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        catch(Exception ex){
            log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 11");

            log.info("Error with Subscription upgrade in " +
                    "upgradeSubLevelWhileLowerSubIsActive ");

            log.error("Error with Subscription upgrade in upgradeSubLevelWhileLowerSubIsActive " +
                    "in Subscription-Service");
            throw new SubscriptionException(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        log.info("INSIDE upgradeSubLevelWhileLowerSubIsActive 12");
        log.info("successfully upgraded from a lower Subscription plan to a higher Subscription " +
                "plan while" + "the lower Subscription plan is Active");

        Map<String, String> resMap =
                Map.of("message", "Successful");
        Map<String, Object> data = Map.of("data", resMap);
        log.info("successfully upgraded from a lower Subscription plan to a higher Subscription \" +\n" +
                "                \"plan while\" + \"the lower Subscription plan is Active");

        return new ResponseEntity<>(data, HttpStatus.CREATED);

    }

    private static long bonusDaysInMillis(long remainingDays) {
        long bonusMillis = 0;
        if (remainingDays >= 2 && remainingDays <= 7) {
            long halfDays = remainingDays / 2; // floor division
            bonusMillis = TimeUnit.DAYS.toMillis(halfDays);
        }
        return bonusMillis;
    }
}