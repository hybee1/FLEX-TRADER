package com.synogiestechnologies.flex_trader_auth.Service;

import com.synogiestechnologies.flex_trader_auth.AllEnums.Role;
import com.synogiestechnologies.flex_trader_auth.DTORequest.CreateUserRequest;
import com.synogiestechnologies.flex_trader_auth.Exceptions.*;
import com.synogiestechnologies.flex_trader_auth.Jwt.JwtService;
import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import com.synogiestechnologies.flex_trader_auth.Models.Subscription;
import com.synogiestechnologies.flex_trader_auth.Repository.MyUsersRepo;
import com.synogiestechnologies.flex_trader_auth.UserDetails.MyUsersDetails;
import com.synogiestechnologies.flex_trader_auth.DTORequest.LoginRequest;
import jakarta.persistence.Tuple;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import com.synogiestechnologies.flex_trader_auth.Mapper.MyUsersMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class MyUsersService {

    private final MyUsersRepo myUsersRepo;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MyUsersMapper myUsersMapper;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;


    public MyUsersService(
                AuthenticationManager authenticationManager, JwtService jwtService,
                MyUsersRepo myUsersRepo, MyUsersMapper myUsersMapper,
                PasswordEncoder passwordEncoder,
                @Lazy SubscriptionService subscriptionService) {
        this.authenticationManager = authenticationManager;
        this.myUsersRepo = myUsersRepo;
        this.jwtService = jwtService;
        this.myUsersMapper = myUsersMapper;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionService = subscriptionService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<String, Object>> verifyUser(@NotNull LoginRequest userLoginRequest) {

        try {

            log.info("TRYING TO AUTHENTICATE");
            log.info("Raw password: " + userLoginRequest.getPassword());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userLoginRequest.getUserNameOrEmail(),
                            userLoginRequest.getPassword()
                    )
            );

            log.info("ABOUT TO TEST: IS AUTHENTICATED ?: " );
            if (!authentication.isAuthenticated()) {

                log.warn("Authentication failed for user: {}", userLoginRequest.getUserNameOrEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication failed"));
            }

            if (!(authentication.getPrincipal() instanceof MyUsersDetails)) {

                log.info("Authentication failed for user: " +
                        userLoginRequest.getUserNameOrEmail());
                log.error("Unexpected authentication principal type");
                throw new LoginException("Internal authentication error",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if (authentication.isAuthenticated()) {

                log.info("IS AUTHENTICATED ?: YES");

                MyUsersDetails userPrincipal = (MyUsersDetails) authentication.getPrincipal();

                // convert MyUsersDetails to MyUsers
                MyUsers myUser = userPrincipal.getMyUser();

                // IF USER HAS ACTIVE SUBSCRIPTION
                Optional<Subscription> subscription =
                        subscriptionService.findNonExpiredSubscriptionWithUsername(
                                myUser.getUsername(), Instant.now() );

                if (subscription.isPresent()) {
                    log.info("USER HAS ACTIVE SUBSCRIPTION, SO SEND WEBSOCKET TOKEN");
                    return jwtService.generateLoginPlusWebsocketTokenAndSaveIt(myUser, userPrincipal);
                }

                log.info("USER HAS NO ACTIVE SUBSCRIPTION, SO SEND LOGIN TOKEN");
                return jwtService.generateLoginTokenAndSaveIt(myUser, userPrincipal);
            }

        } catch (AuthenticationException ex) {

            log.warn("2. Invalid credentials. " +
                    "\nThe error message: " + ex.getMessage() +
                    "\nThe error cause: " + ex.getCause());
            throw new InvalidCredentialsException("2. Invalid credentials", HttpStatus.BAD_REQUEST);
        }

        log.warn("Authentication failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("errorMessage", "Authentication failed"));
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<String, Object>> createNewUser(
            @NotEmpty CreateUserRequest newUserRequest) {

        // user already exist
        Tuple result = myUsersRepo.findUsernameAndEmailMatchCounts(
                newUserRequest.getUsername());

        Long usernameCount = result.get("usernameMatch", Long.class);
        Long emailCount = result.get("emailMatch", Long.class);

        if (usernameCount.intValue() > 0 ){

            log.error("A new user is trying to create an exist entity: \"{}\" already exist in DB", newUserRequest.getUsername());
            throw new UserAlreadyExistException("This username: " + "\"" +
                    newUserRequest.getUsername() + "\" " + "already exist", HttpStatus.CONFLICT);
        }

        if (emailCount.intValue()  > 0 ){

            log.error("A new user is trying to create an exist entity: \"{}\" already exist in DB", newUserRequest.getEmail());
            throw new UserAlreadyExistException("This email: " + "\"" + newUserRequest.getEmail() +
                    "\"" + " already exist", HttpStatus.CONFLICT);
        }

        // Build the user
        MyUsers user = MyUsers.builder()
                .username(newUserRequest.getUsername())
                .email(newUserRequest.getEmail())
                .password(passwordEncoder.encode(newUserRequest.getPassword()))
                .role(Role.USER)
                .build();

        // Save user
        MyUsers savedUser = myUsersRepo.save(user);

        if (savedUser.getId() == null) {
            // this will roll back the saved user
            log.error("Could not save User to DB");
            throw new UserCreationException("Something went wrong please try again later",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        LoginRequest loginRequest = LoginRequest.builder()
                .userNameOrEmail(newUserRequest.getUsername())
                .password(newUserRequest.getPassword())
                .build();

        // Authenticate user to generate token
        ResponseEntity<Map<String, Object>> loginData = verifyUser(loginRequest);

        if (loginData.getStatusCode() == HttpStatus.OK) {

            Map<String, Object> dataBody = loginData.getBody();
            Map<String, String> data = (Map<String, String>) dataBody.get("data");

            String token = data.get("token");

            Map<String, String> resultMap = Map.of("token", token);
            return ResponseEntity.ok(Map.of("data", resultMap));
        }

        // Handle failure to generate token
        log.error("Unable tto verify created user with it details, " +
                "though user is not saved to DB");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("errorMessage", "Something went wrong please try again later"));
    }

    public ResponseEntity<Map<String, Object>> isTokenRevoked( @NotEmpty String token) {

        log.info("inside IS-TOKEN-REVOKED in myusers-service");
        try {
            boolean isRevoked = jwtService.isTokenRevoked(token);

            Map<String, Boolean> resMap = Map.of("isRevoked", isRevoked);
            Map<String, Object> dataMap = Map.of("data", resMap);
            return new ResponseEntity<>(dataMap, HttpStatus.OK);
        }
        catch(TokenNotFoundException ex){
            log.warn("No such token found when checking revocation status {}", token);
            throw new TokenNotFoundException("Bad token", HttpStatus.NOT_FOUND);
        }

    }


}
