package com.synogiestechnologies.flex_trader_websocket_server;


import com.synogiestechnologies.flex_trader_websocket_server.DTORequest.TokenRevokedRequest;
import com.synogiestechnologies.flex_trader_websocket_server.Exceptions.ExpiredTokenException;
import com.synogiestechnologies.flex_trader_websocket_server.Exceptions.InvalidTokenException;
import com.synogiestechnologies.flex_trader_websocket_server.Exceptions.SigningKeyException;
import com.synogiestechnologies.flex_trader_websocket_server.SubscriptionPlanType.SubscriptionPlanType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import static com.synogiestechnologies.flex_trader_websocket_server.SubscriptionPlanType.SubscriptionPlanType.fromDisplayName;

@Slf4j
@Service
public class JwtService {

    @Value("${JWT_SECRET_KEY}")
    private String SECRET_KEY;
    private final CallToAuthBackend callToAuthBackend;

    public JwtService(CallToAuthBackend callToAuthBackend) {
        this.callToAuthBackend = callToAuthBackend;
    }


    private SecretKey signingKey() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(SECRET_KEY);
            if (decodedKey.length < 32) {
                throw new SigningKeyException(
                        "JWT secret key must be at least 256 bits (32 bytes) when Base64-decoded.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return Keys.hmacShaKeyFor(decodedKey);

        } catch (SigningKeyException e) {
            log.error("Invalid JWT secret key: {}", e.getMessage());
            throw e;
        }
    }

    private <T> T extractClaims(String token, Function<Claims, T> resolver){

        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        System.out.println("inside extractAllClaims IN WEBSOCKET-JWT-SERVICE");
        System.out.println("Decoding JWT token: " + token);
        log.debug("Decoding JWT token: {}", token);

        try{

            return Jwts
                    .parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // Token is expired
            System.out.println("Token is expired IN WEBSOCKET-JWT-SERVICE. " +
                    "\nThe error message: " + e.getMessage() +
                    "\nThe error cause: " + e.getCause() );

            log.error("Token is expired IN WEBSOCKET-JWT-SERVICE. " +
                    "\nThe error message: " + e.getMessage() +
                    "\nThe error cause: " + e.getCause()
            );
            throw new ExpiredTokenException("Token is expired", HttpStatus.UNAUTHORIZED);
        }
        catch (JwtException e) {
            // Token is invalid — reject it
            System.out.println("Token is invalid IN WEBSOCKET-JWT-SERVICE. " +
                    "\nThe error message: " + e.getMessage() +
                    "\nThe error cause: " + e.getCause() );

            log.error("Token is invalid IN WEBSOCKET-JWT-SERVICE. " +
                    "\nThe error message: " + e.getMessage() +
                    "\nThe error cause: " + e.getCause()
            );
            throw new InvalidTokenException("JWT token is invalid", HttpStatus.UNAUTHORIZED);
        }

    }

    public String extractUserName(String token) {
        System.out.println("inside extractUserName");
        return extractClaims(token, Claims::getSubject);
//        try {
//            return extractClaims(token, Claims::getSubject);
//        } catch (Exception e) {
//            System.out.println("Failed to extract username from token: " + e.getMessage());
//            log.warn("Failed to extract username from token: {}", e.getMessage());
//            return null; //
//        }
    }

    public Date extractExpiration(String token) {
        System.out.println("inside extractExpiration");
        return extractClaims(token, Claims::getExpiration);
//        try {
//            return extractClaims(token, Claims::getExpiration);
//        } catch (ExpiredTokenException e) {
//            System.out.println("Failed to extract expiration from token: " + e.getMessage());
//            log.warn("Failed to extract expiration from token: {}", e.getMessage());
//            return null; // or throw new InvalidTokenException(...)
//        } catch (SecurityException e) {
//            System.out.println("Failed to extract expiration from token: " + e.getMessage());
//            log.warn("Failed to extract expiration from token: {}", e.getMessage());
//            return null; // or throw new InvalidTokenException(...)
//        }
    }

    public boolean isTokenExpired(String token){
        System.out.println("inside IS-TOKEN-EXPIRED");
        try {
            Date expiration = extractExpiration(token);
            return Instant.now().isAfter(expiration.toInstant());
        } catch (ExpiredTokenException e) {
            System.out.println("token expired, Failed to check token expiration: " + e.getMessage());
            // log.warn("Failed to check token expiration: {}", e.getMessage());
            // If we can't parse the expiration, treat the token as expired
            return true;
        } catch (InvalidTokenException e) {
            System.out.println("Invalid token, Failed to check token expiration: " + e.getMessage());
            log.warn("Failed to check token expiration: {}", e.getMessage());
            // If we can't parse the expiration, treat the token as expired
//            return true;
            throw new InvalidTokenException("JWT is expired", HttpStatus.UNAUTHORIZED);
        }
    }


/// USE FEIGN CLIENT TO MAKE HTTP CALL TO GET REVOCATION STATUS FROM AUTH BACKEND///
    public boolean isTokenRevoked(String token) throws Exception {
        System.out.println("INSIDE isTokenRevoked " );
        boolean isRevoked = false;
        TokenRevokedRequest tokenRequest = new TokenRevokedRequest(token);
        System.out.println("INSIDE isTokenRevoked 1" );
        ResponseEntity<Map<String, Object>> result = callToAuthBackend.getTokenRevocationStatus(tokenRequest);

        System.out.println("INSIDE isTokenRevoked 1a" );
        System.out.println("result: " + result );
        System.out.println("INSIDE isTokenRevoked 1b" );

        if(result.getStatusCode() == HttpStatus.OK){

            System.out.println("INSIDE isTokenRevoked 2" );

            Map<String, Object> dataBody = result.getBody();
            System.out.println("INSIDE isTokenRevoked 3" );
            Map<String, Boolean> data = (Map<String, Boolean>) dataBody.get("data");
            System.out.println("INSIDE isTokenRevoked 4" );

            isRevoked = data.get("isRevoked");
            System.out.println("isRevoked: " + isRevoked );

        }
        System.out.println("INSIDE isTokenRevoked 5" );
        return isRevoked;
    }

    public boolean isTokenValid(String token) throws Exception {

        System.out.println("INSIDE isTokenValid " );

        try {

            boolean tokenHasExpired = isTokenExpired(token);
            System.out.println("tokenHasExpired: " + tokenHasExpired);

            boolean tokenWasRevoked = isTokenRevoked(token);
            System.out.println("tokenWasRevoked: " + tokenWasRevoked);

            boolean result = !tokenHasExpired && !tokenWasRevoked;
            System.out.println("!tokenHasExpired && !tokenWasRevoked: " + result);

            return result;

        }  catch (Exception e) {
            System.out.println("Exception occurred when checking if token is valid: " + e.getMessage());
            log.error("Exception occurred when checking if token is valid: {}", e.getMessage());
            return false;
        }

    }

    public boolean canConnectToWebsocket(String token) throws Exception {

        System.out.println("INSIDE canConnectToWebsocket 1");
        try{

            Claims claims = extractAllClaims(token);
            System.out.println("INSIDE canConnectToWebsocket 2");

            List<String> roles = claims.get("roles", List.class);
            System.out.println("roles: " + roles);
            System.out.println("INSIDE canConnectToWebsocket 3");

            Map<String, Object> websocketDetails = claims.get("websocketDetails", Map.class);

            if (websocketDetails == null) {
                System.out.println("websocketDetails is null — ");
                log.warn("websocketDetails is null — denying connection");
                return false;
            }

            System.out.println("websocketDetails: " + websocketDetails);
            System.out.println("INSIDE canConnectToWebsocket 4");
            String subPlan = (String) websocketDetails.get("subscriptionPlan");
            System.out.println("Websocket Subscription : " + subPlan);
            System.out.println("INSIDE canConnectToWebsocket 5");

            SubscriptionPlanType subType = fromDisplayName(subPlan);
            boolean suTypeBool = subType.getDisplayName() != null;
            System.out.println("INSIDE canConnectToWebsocket 5a");

            long expiryDate = ((Number) websocketDetails.get("expiryDate")).longValue();
            System.out.println("INSIDE canConnectToWebsocket 6");

            boolean result = (roles.contains("ROLE_USER") &&
                    expiryDate >= Instant.now().toEpochMilli() && suTypeBool);
            System.out.println("( roles.contains(\"ROLE_USER\") && expiryDate >= " +
                    "Instant.now().toEpochMilli() = " + "&& suTypeBool ) = " +  result);

            return result;
        }
        catch (Exception e) {
            System.out.println("Exception occurred when checking if canConnectToWebsocket: "
                    + e.getMessage());
            log.error("Exception occurred when checking if canConnectToWebsocket: {}",
                    e.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = extractAllClaims(token);
        String username = claims.getSubject();

        // roles from claim if present
        List<GrantedAuthority> authorities = new ArrayList<>();
        Object rolesClaim = claims.get("roles");

        if (rolesClaim instanceof List) {
            for (Object role : (List<?>) rolesClaim) {
                authorities.add(new SimpleGrantedAuthority(role.toString()));
            }
            return new UsernamePasswordAuthenticationToken(username, token, authorities);
        }

        authorities = Collections.singletonList(new SimpleGrantedAuthority((String) rolesClaim));

        return new UsernamePasswordAuthenticationToken(username, token, authorities);
    }
}
