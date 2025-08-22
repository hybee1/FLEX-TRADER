package com.synogiestechnologies.flex_trader_auth.Controller;

import com.synogiestechnologies.flex_trader_auth.DTORequest.*;
import com.synogiestechnologies.flex_trader_auth.Exceptions.InvalidTokenException;
import com.synogiestechnologies.flex_trader_auth.Service.JwtService;
import com.synogiestechnologies.flex_trader_auth.Service.MyUsersService;
import com.synogiestechnologies.flex_trader_auth.Service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/authentication")
public class MyUsersController {

    private final MyUsersService myUsersService;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;


    public MyUsersController( MyUsersService myUsersService, JwtService jwtService,
                              SubscriptionService subscriptionService) {
        this.myUsersService = myUsersService;
        this.jwtService = jwtService;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody CreateUserRequest createUserRequest ){
        log.info(("register CONTROLLER WAS CALLED"));
        return myUsersService.createNewUser(createUserRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest userLoginRequest ){
        log.info("login CONTROLLER WAS CALLED");
        return myUsersService.verifyUser(userLoginRequest);
    }

    @PostMapping("/mylogout")
    public ResponseEntity<Map<String, Object>> logOut(@Valid @RequestBody
                                                          LogOutRequest logOutRequest ){
        log.info("logout CONTROLLER WAS CALLED");
        return jwtService.logOut(logOutRequest);
    }

    @PostMapping("/revoked-token")
    public ResponseEntity<Map<String, Object>> isTokenRevoked(@Valid @RequestBody
                                                                  TokenRevokedRequest token1 ){
        String token = token1.getToken();
        return myUsersService.isTokenRevoked(token);
    }

    @PostMapping("/subscription/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@Valid @RequestBody
                                                         SubscriptionRequest subRequest,
                                                         HttpServletRequest request){

        String authHeader = request.getHeader("Authorization");
        if(authHeader==null || !authHeader.startsWith("Bearer ")){
            throw new InvalidTokenException("Authorization header missing", HttpStatus.BAD_REQUEST);
        }
        if(!authHeader.startsWith("Bearer ")){
            throw new InvalidTokenException("Token must start with Bearer", HttpStatus.BAD_REQUEST);
        }

        final String jwtToken = authHeader.substring(7);  // authHeader.substring(7)
        subRequest.setToken(jwtToken);
        log.info("TOKEN IN CREATE FRESH SUB IN MY-USERS-CONTROLLER " + subRequest.getToken());

        return subscriptionService.createSubscription(
                subRequest.getUsernameOrEmail(), subRequest.getSubscriptionPlanType(),
                subRequest.getSubscriptionDuration(), false, subRequest.getToken()
        );
    }

    @PostMapping("/subscription/upgrade")
    public ResponseEntity<Map<String, Object>> upgradeSubscription(@Valid @RequestBody
                                                                       SubscriptionRequest subRequest,
                                                                   HttpServletRequest request ){

        String authHeader = request.getHeader("Authorization");
        if(authHeader==null || !authHeader.startsWith("Bearer ")){
            throw new InvalidTokenException("Authorization header missing", HttpStatus.BAD_REQUEST);
        }
        if(!authHeader.startsWith("Bearer ")){
            throw new InvalidTokenException("Token must start with Bearer", HttpStatus.BAD_REQUEST);
        }

        final String jwtToken = authHeader.substring(7);  // authHeader.substring(7)
        subRequest.setToken(jwtToken);
        log.info("TOKEN IN UPGRADE SUB IN MY-USERS-CONTROLLER " + subRequest.getToken());

        return subscriptionService.createSubscription(
                subRequest.getUsernameOrEmail(), subRequest.getSubscriptionPlanType(),
                subRequest.getSubscriptionDuration(), true, subRequest.getToken()
        );
    }

    @GetMapping("/auth-fallback")
    public ResponseEntity<Map<String, String>> authFallback() {
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Auth service temporarily unavailable. Please try again shortly.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resp);
    }

}
