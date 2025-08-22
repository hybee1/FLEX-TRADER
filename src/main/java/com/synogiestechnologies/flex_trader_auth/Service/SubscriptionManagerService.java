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
import com.synogiestechnologies.flex_trader_auth.UserDetails.MyUsersDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class SubscriptionManagerService {

    private final SubscriptionRepo subscriptionRepo;
    private final MyUsersRepo myUsersRepo;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;

    public SubscriptionManagerService(SubscriptionRepo subscriptionRepo,
                                      MyUsersRepo myUsersRepo,
                                      @Lazy JwtService jwtService,
                                      @Lazy SubscriptionService subscriptionService) {
        this.subscriptionRepo = subscriptionRepo;
        this.myUsersRepo = myUsersRepo;
        this.jwtService = jwtService;
        this.subscriptionService = subscriptionService;
    }



    @Transactional
    public ResponseEntity<Map<String, Object>> createSubscription(String usernameOrEmail,
                                                                  String subscriptionPlanType,
                                                                  String subscriptionDuration,
                                                                  boolean upgrade, String token) {

        SubscriptionPlanType newPlanType = SubscriptionPlanType.FREE;
        SubscriptionDuration newPlanDuration = SubscriptionDuration.ONE_WEEK;

        MyUsers user = null;

        log.info("INSIDE createSubscription 1");
        try {

            user = myUsersRepo.usernameOrEmail(usernameOrEmail)
                    .orElseThrow(() -> {
                        log.info("INSIDE createSubscription 1a");
                        log.info("User not found in createSubscription");
                        log.error("User " + usernameOrEmail + " not found in createSubscription");
                        return new NoUserFoundException("User " + "\"" + usernameOrEmail + "\" not found",
                                HttpStatus.NOT_FOUND);
                    });

            if (!user.getUsername().equals(jwtService.extractUserName(token))) {

                throw new InvalidTokenException("Token does not match user.",
                        HttpStatus.UNAUTHORIZED);
            }

            try {
                log.info("INSIDE createSubscription 2");
                log.info("About to search for plan type");
                newPlanType = SubscriptionPlanType.fromDisplayName(subscriptionPlanType);

                log.info("Found plan: " + newPlanType + " - $" + newPlanType.getPricePerMonth());
            } catch (IllegalArgumentException e) {
                log.info("INSIDE createSubscription 2a");
                log.error("Subscription Plan Type " + subscriptionPlanType +
                        " not found in createSubscription");
                throw new SubscriptionException("Subscription Plan Type " + subscriptionPlanType +
                        " not found ", HttpStatus.NOT_FOUND);

            }

            try{
                log.info("INSIDE createSubscription 2b");
                log.warn("About to check if User has already used the free plan.");
                boolean hasUsedFreePlan = subscriptionRepo.userHasUsedFreePlan(user.getUsername());

                if (hasUsedFreePlan && newPlanType.getDisplayName().equalsIgnoreCase("Free")){
                    log.info("INSIDE createSubscription 2c");
                    log.warn("User has already used the free plan.");
                    throw new SubscriptionException("User has already used the free plan.",
                            HttpStatus.FORBIDDEN);
                }
                log.info("User has not use the free plan.");
            }
            catch(Exception ex){
                log.info("INSIDE createSubscription 2d");
                log.error("Subscription DB error when checking if user has used free plan.");
                throw new SubscriptionException("Subscription DB error when checking if user has " +
                        "used free plan.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            try {
                newPlanDuration = SubscriptionDuration.fromDisplayName(subscriptionDuration);
                log.info("INSIDE createSubscription 3");
                log.info("Found plan duration: " + newPlanDuration + " - $" +
                        newPlanDuration.getDurationMillis());
            } catch (IllegalArgumentException e) {
                log.info("INSIDE createSubscription 3a");
                log.error("Subscription Plan Duration " + subscriptionDuration +
                        " not found in createSubscription");
                throw new SubscriptionException("Subscription Plan Duration " + subscriptionDuration +
                        " not found", HttpStatus.NOT_FOUND);
            }

            if (!upgrade && subscriptionRepo.existsByUserAndIsCancelledFalseAndSubExpiryDateAfter(
                    user, Instant.now())) {
                log.info("INSIDE createSubscription 4");
                log.warn("User " + user + " already has an active subscription");
                throw new SubscriptionException("User already has an active subscription",
                        HttpStatus.CONFLICT);

            }

            if (upgrade && subscriptionRepo.existsByUserAndIsCancelledFalseAndSubExpiryDateAfter(
                    user, Instant.now())) {
                log.info("INSIDE createSubscription 5");
                return subscriptionService
                        .upgradeSubLevelWhileLowerSubIsActive(usernameOrEmail, subscriptionPlanType,
                        subscriptionDuration, token);

            }

            Instant subStartDate = Instant.now();
            Instant subExpirationDate = subStartDate.plusMillis(newPlanDuration.getDurationMillis());

            Subscription sub = Subscription.builder()
                    .user(user)
                    .subscriptionPlanType(newPlanType)
                    .subscriptionDuration(newPlanDuration)
                    .subStartDate(subStartDate)
                    .subExpiryDate(subExpirationDate)
                    .build();

            log.info("INSIDE createSubscription 6");
            Subscription savedSub = subscriptionRepo.save(sub);

            if (savedSub.getId() == null) {
                log.info("INSIDE createSubscription 6a");

                log.info("Unable to save new Subscription to DB in createSubscription 6b");

                log.error("Unable to save new Subscription to DB in Subscription-Service");
                throw new SubscriptionException(
                        "something went wrong please try again later",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.info("INSIDE createSubscription 7");
            log.info("successfully created subscription plan ");

            // NOW GENERATE NEW TOKEN THAT CONTAINS THE SUBSCRIPTION
            log.info("USER BEFORE GENERATING TOKEN " + user);
            MyUsersDetails userDetails1 = new MyUsersDetails(user);
            log.info("MyUsersDetails BEFORE GENERATING TOKEN " + userDetails1.getUsername());
            log.info("INSIDE createSubscription 7a");

            ResponseEntity<Map<String, Object>> resMap = null;

            try {

                resMap = jwtService.generateLoginPlusWebsocketTokenAndSaveIt(user, userDetails1);

                log.info("INSIDE createSubscription 7b");
                log.info("generateTokenAndSaveTokenForUSer response " + resMap.getBody());

                Map<String, Object> dataBody = resMap.getBody();
                log.info("INSIDE createSubscription 7b resMap.getBody() class " +
                        resMap.getBody().getClass());
                log.info("INSIDE createSubscription 7c");
                Map<String, String> data = (Map<String, String>) dataBody.get("data"); // is immutableMap
                log.info("INSIDE createSubscription 7c dataBody.get(\"data\") class " + data.getClass());

                Map<String, String> modifiabledata = new HashMap<>(data);
                log.info("INSIDE createSubscription 7d");

                modifiabledata.put("message", "Successful");
                log.info("INSIDE createSubscription 7e");
                return ResponseEntity.ok(Map.of("data", modifiabledata));
            }
            catch(RuntimeException  ex){  // so that roll back can work
                log.info("INSIDE createSubscription 7f");
                log.info("generateTokenAndSaveTokenForUSer generated error in subscription-Service.");
                log.error("Unable to generated and saved token to DB in subscription-Service. " +
                        "\nThe error message: " + ex.getMessage() +
                        "\nThe error cause: " + ex.getCause()
                );

                throw ex;
            }

        } catch (RuntimeException ex) {
            throw ex; // Preserves rollback
        }

        catch(Exception ex){
            log.info("INSIDE createSubscription 8");
            log.info("Error with Subscription in createSubscription 8a");

            log.error("Error with Subscription in createSubscription in Subscription-Service");
            throw new SubscriptionException(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }


    }
}
