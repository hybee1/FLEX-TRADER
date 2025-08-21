package com.synogiestechnologies.flex_trader_auth.Jwt;


import com.synogiestechnologies.flex_trader_auth.DTORequest.LogOutRequest;
import com.synogiestechnologies.flex_trader_auth.Exceptions.*;
import com.synogiestechnologies.flex_trader_auth.Models.JwtToken;
import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import com.synogiestechnologies.flex_trader_auth.Models.Subscription;
import com.synogiestechnologies.flex_trader_auth.Repository.JwtTokenRepo;
import com.synogiestechnologies.flex_trader_auth.Service.SubscriptionService;
import com.synogiestechnologies.flex_trader_auth.UserDetails.MyUsersDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


@Slf4j
@Service
public class JwtService {

    @Value("${JWT_SECRET_KEY}")
    private String SECRET_KEY;
    private static final long VALIDITY = TimeUnit.MINUTES.toMillis(5);
//    private static final Instant timeNow = Instant.now();

    private final SubscriptionService subscriptionService;
    private final JwtTokenRepo jwtTokenRepo;

    public JwtService(SubscriptionService subscriptionService,
                      JwtTokenRepo jwtTokenRepo) {
        this.subscriptionService = subscriptionService;
        this.jwtTokenRepo = jwtTokenRepo;
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

    public String generateLoginToken(MyUsersDetails myUsers) {
        log.info("inside generateTokenForLogin");

        Map<String, Object> allClaims = new HashMap<>();

        List<String> roles1 = myUsers.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        allClaims.put("roles", roles1);

        return Jwts.builder()
                .claims(allClaims)
                .subject(myUsers.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .expiration(
                        Date.from(Instant.now().plusMillis(VALIDITY))
                )
                .signWith(signingKey())
                .compact();
    }

    public String generateLoginPlusWebsocketToken(MyUsersDetails myUsers) {
        log.info("inside generateLoginPlusWebsocketToken");

        Optional<Subscription> subscription =
                subscriptionService.findNonExpiredSubscriptionWithUsername(
                        myUsers.getUsername(), Instant.now());

        Map<String, Object> allClaims = new HashMap<>();
        if (subscription.isEmpty()) {
            throw new SubscriptionException("No subscription found, subscribe",
                    HttpStatus.NOT_FOUND);
        }

        Subscription sub = subscription.get();

        Map<String, Object> websocketDetails = new HashMap<>();
        websocketDetails.put("subscriptionPlan", sub.getSubscriptionPlanType());
        websocketDetails.put("expiryDate", sub.getSubExpiryDate().toEpochMilli());

        List<String> roles1 = myUsers.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        allClaims.put("roles", roles1);
        allClaims.put("websocketDetails", websocketDetails);

        return Jwts.builder()
                .claims(allClaims)
                .subject(myUsers.getUsername())
                .issuedAt(
                        Date.from(sub.getSubStartDate())
                )
                .expiration(
                        Date.from(sub.getSubExpiryDate())
                )
                .signWith(signingKey())
                .compact();
    }

    private <T> T extractClaims(String token, Function<Claims, T> resolver){

        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        log.info("inside extractAllClaims");
        log.info("Decoding JWT token: " + token);

        try{
            return Jwts
                    .parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // Token is expired
            log.info("1 Token is expired in jwtService. " +
                    "\nThe error message: " + e.getMessage() +
                    "\nThe error cause: " + e.getCause() );

            log.error("Token is expired in jwtService. " +
                    "\nThe error message: " + e.getMessage() +
                    "\nThe error cause: " + e.getCause()
            );
            throw new ExpiredTokenException("JWT token is expired", HttpStatus.UNAUTHORIZED);
        }
        catch (JwtException e) {
        // Token is invalid — reject it
            log.info("2 Token is invalid in jwtService. " +
                    "\nThe error message: " + e.getMessage() +
                    "\nThe error cause: " + e.getCause() );

            log.error("Token is invalid in jwtService. " +
                    "\nThe error message: " + e.getMessage() +
                    "\nThe error cause: " + e.getCause()
            );
            throw new InvalidTokenException("JWT token is invalid", HttpStatus.UNAUTHORIZED);
    }

    }

    public String extractUserName(String token) {
        log.info("inside extractUserName");
        return extractClaims(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        log.info("inside extractExpiration");
        return extractClaims(token, Claims::getExpiration);
    }


    public boolean isTokenExpired(String token){
        log.info("inside IS-TOKEN-EXPIRED");
        try {
            Date expiration = extractExpiration(token);
            return Instant.now().isAfter(expiration.toInstant());
        } catch (ExpiredTokenException e) {
            log.info("token expired, Failed to check token expiration: " + e.getMessage());
            //log.warn("Failed to check token expiration: {}", e.getMessage());
            // If we can't parse the expiration, treat the token as expired
            return true;
        } catch (Exception e) {
            log.warn("Invalid token, Failed to check token expiration: " + e.getMessage());
            throw new InvalidTokenException("JWT is expired", HttpStatus.UNAUTHORIZED);
        }
    }

    public boolean isTokenRevoked(String token){

        log.info("inside IS-TOKEN-REVOKED");
        log.info("token: " + token);
        boolean result;
        Optional<Boolean> revocationStatus =  jwtTokenRepo.getRevocationStatus(token);

        if(revocationStatus.isEmpty()){
            log.info("No Such Token, Not Found in IS-TOKEN-REVOKED");

            throw new TokenNotFoundException("No Such Token, Not Found",
                    HttpStatus.NOT_FOUND);
        }

        log.info("token revocation Status inside IS-TOKEN-REVOKED: " + revocationStatus.get());

        result = revocationStatus.get();

        return result;

    }



    public boolean isTokenValid(String token, MyUsersDetails myUsersDetails) {

        log.info("inside IS-TOKEN-VALID");
        try {
            /*
             if the username can be extracted from the token then the token is valid and not expired,
             if not, then it will throw an exception which means that the token is either not
             valid or expired. the catch block will return FALSE as output of isTokenValid method.
            */
            String username = extractUserName(token);
            log.info("username " + username + " inside IS-TOKEN-VALID");

            boolean tokenHasExpired = false; // since the username can be extracted
            log.info("tokenHasExpired " + tokenHasExpired + " inside IS-TOKEN-VALID");

            boolean tokenWasRevoked = isTokenRevoked(token);
            log.info("tokenWasRevoked " + tokenWasRevoked + " inside IS-TOKEN-VALID");

            log.info("CheckingToken is valid or not , in IS-TOKEN-VALID");

            boolean result = (myUsersDetails.getUsername().equals(username) && !tokenHasExpired
                    && !tokenWasRevoked);

            log.info("Token " + (result ? "is valid," : "is not valid,") + " in IS-TOKEN-VALID");
            return result;

        }  catch (Exception e) {
            log.error("Exception while validating token: {}", e.getMessage());
            return false;
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public JwtToken saveNewTokenForUser(MyUsers user, String tokenValue, Instant expiryDate) {

        log.info("INSIDE saveNewTokenForUser");
        boolean hasActiveToken =
                jwtTokenRepo.existsByUserAndIsRevokedFalseAndTokenExpiryDateAfter(user, Instant.now());
        log.info("AFTER existsByUserAndIsRevokedFalseAndTokenExpiryDateAfter IN saveNewTokenForUser");

        if (hasActiveToken) {
            log.error("User already has an active token in saveNewTokenForUser. " );
            throw new UserHasActiveTokenException("User already has an active token", HttpStatus.CONFLICT);
        }

        log.info("BUILDING JwtToken");
        JwtToken token = JwtToken.builder()
                .token(tokenValue)
                .isRevoked(false)
                .tokenExpiryDate(expiryDate)
                .user(user)
                .build();

        log.info("SAVING TOKEN TO DB IN saveNewTokenForUser");
        JwtToken savedToken = jwtTokenRepo.save(token);
        log.info("ABOUT TO CHECK IF SAVED TOKEN RETURNED NULL IN saveNewTokenForUser");
        if(savedToken.getId()==null){
            log.error("SAVING TOKEN ERROR, IT RETURNED NULL IN saveNewTokenForUser. " );
            throw new SaveToDBException("Unable to saved Jwt Token to DB in Jwt-Service",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("TOKEN SAVED SUCCESSFULLY IN saveNewTokenForUser " );
        return savedToken;
    }

    @Transactional
    public boolean deleteToken( String tokenValue) {

        Optional<JwtToken> tokenExist = jwtTokenRepo.findByToken(tokenValue);
        if (tokenExist.isPresent()){
            jwtTokenRepo.deleteById(tokenExist.get().getId());
            log.info("SUCCESSFULLY DELETED TOKEN IN deleteToken");
            return true;
        }
        log.info("Token not found IN deleteToken");
        throw new TokenNotFoundException("Token not found", HttpStatus.NOT_FOUND);

    }

    @Transactional
    public boolean deleteTokenByUsername( MyUsers user) {

        log.info("INSIDE deleteTokenByUsername");
        Optional<JwtToken> activeToken = jwtTokenRepo
                .findByUserAndIsRevokedFalse(user);
        log.info("searched for token by username  IN deleteActiveTokenByUsername");
        if (activeToken.isPresent()){
            jwtTokenRepo.deleteById(activeToken.get().getId());
            log.info("SUCCESSFULLY DELETED TOKEN IN deleteActiveTokenByUsername");
        }

        return true;

    }

    public ResponseEntity<Map<String, Object>> logOut(@NotNull LogOutRequest logOutRequest) {

        log.info(("inside logOut"));

        Optional<JwtToken> token1 = jwtTokenRepo.findByToken(logOutRequest.getToken());
        if (token1.isEmpty()) {

            log.warn("No such token found in DB when trying to log out: {}", logOutRequest.getToken());
            throw new TokenNotFoundException("Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }

        JwtToken token2 = token1.get();

        if (!token2.isRevoked()) {

            token2.setRevoked(true);
            JwtToken token3 = jwtTokenRepo.save(token2);

            if (token3.getId() == null) {
                log.error("Could not save revoked token to DB");
                throw new UserCreationException("Something went wrong please try again later",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        log.warn("token revoked and saved in DB inside log out: {}", logOutRequest.getToken());
        log.info("logged out successfully inside logOut");

        Map<String, Object> resMap = Map.of(
                "message", "Token successfully revoked",
                "revoked", true
        );
        Map<String, Object> dataMap = Map.of("data", resMap);
        return new ResponseEntity<>(dataMap, HttpStatus.OK);

    }

    public ResponseEntity<Map<String, Object>> generateLoginTokenAndSaveIt(MyUsers myUser, MyUsersDetails userPrincipal) {

        log.info("INSIDE generateLoginTokenAndSaveIt");

        // Save token generateToken
        JwtToken jwtToken = null;

        while(true) {
            //first delete any previously generated user token either active or not in the db
            log.info("DELETING PREVIOUSLY GENERATED USER/LOGIN TOKEN IN DB");
            boolean deleteResult = deleteTokenByUsername(myUser);

            log.info("ABOUT TO GENERATE LOGIN TOKEN");
            String generatedToken = generateLoginToken(userPrincipal);
            log.info("LOGIN TOKEN GENERATED");
            Date tokenExpiryDate1 = extractExpiration(generatedToken);
            log.info("TOKEN EXPIRATION GOTTEN");
            Instant tokenExpiryDate = tokenExpiryDate1.toInstant();

            try {
                log.info("ABOUT TO SAVE LOGIN TOKEN");
                jwtToken = saveNewTokenForUser(myUser, generatedToken, tokenExpiryDate);
                log.info("LOGIN TOKEN SAVED");
                log.info("BREAKING OUT OF LOOP");
                break;
            } catch (UserHasActiveTokenException ex) {
                log.error("User already has an active token in MyUsers-Service. " +
                        "\nThe error message: " + ex.getMessage() +
                        "\nThe error cause: " + ex.getCause()
                );

                log.info("DELETING PREVIOUSLY GENERATED USER/LOGIN TOKEN IN DB " +
                        "when User already has an active token");
                deleteTokenByUsername(myUser);

            } catch (Exception ex) {
                log.error("Unable to saved login token to DB in jwt-Service. " +
                        "\nThe error message: " + ex.getMessage() +
                        "\nThe error cause: " + ex.getCause()
                );
                throw new SaveToDBException("Unable to saved login token to DB in jwt-Service. ",
                                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } //end of while loop

        if (jwtToken == null || jwtToken.getToken() == null) {
            // this will roll back the saved user and token
            log.error("saved login token returned null, means save to DB error, in jwt-Service");
            throw new LoginException("Something went wrong please try again later",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("Login Token successfully generated and saved for user: {}", myUser.getUsername());
        Map<String, String> tokenMap = Map.of("token", jwtToken.getToken());
        return ResponseEntity.ok(Map.of("data", tokenMap));
    }

    public ResponseEntity<Map<String, Object>> generateLoginPlusWebsocketTokenAndSaveIt(
                                                                        MyUsers myUser,
                                                                        MyUsersDetails userPrincipal) {
        // Save token generateToken
        log.info("INSIDE generateLoginPlusWebsocketTokenAndSaveIt");
        JwtToken jwtToken = null;

        while(true) {
            //first delete any previously generated user token either active or not in the db
            log.info("DELETING PREVIOUSLY GENERATED USER/WEBSOCKET TOKEN IN DB");
            boolean deleteResult = deleteTokenByUsername(myUser);

            log.info("ABOUT TO GENERATE WEBSOCKET TOKEN");
            String generatedToken = generateLoginPlusWebsocketToken(userPrincipal);
            log.info("WEBSOCKET TOKEN GENERATED");
            Date tokenExpiryDate1 = extractExpiration(generatedToken);
            log.info("WEBSOCKET TOKEN EXPIRATION GOTTEN");
            Instant tokenExpiryDate = tokenExpiryDate1.toInstant();

            try {
                log.info("ABOUT TO SAVE WEBSOCKET TOKEN");
                jwtToken = saveNewTokenForUser(myUser, generatedToken, tokenExpiryDate);
                log.info("WEBSOCKET TOKEN SAVED");
                log.info("BREAKING OUT OF LOOP");
                break;
            } catch (UserHasActiveTokenException ex) {
                log.error("User already has an active token in jwt-Service. " +
                        "\nThe error message: " + ex.getMessage() +
                        "\nThe error cause: " + ex.getCause()
                );

                log.info("DELETING PREVIOUSLY GENERATED USER/WEBSOCKET TOKEN IN DB " +
                        "when User already has an active token");
                deleteTokenByUsername(myUser);

            } catch (Exception ex) {
                log.error("Unable to saved token to DB in jwt-Service. " +
                        "\nThe error message: " + ex.getMessage() +
                        "\nThe error cause: " + ex.getCause()
                );
                throw new SaveToDBException("Unable to saved websocket token to DB in jwt-Service. ",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } //end of while loop

        if (jwtToken == null || jwtToken.getToken() == null) {
            // this will roll back the saved user and token
            log.error("saved websocket token returned null, means save to DB error, in MyUsers-Service");
            throw new LoginException("Something went wrong please try again later",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("Websocket Token successfully generated and saved for user: {}", myUser.getUsername());
        Map<String, String> tokenMap = Map.of("token", jwtToken.getToken());
        return ResponseEntity.ok(Map.of("data", tokenMap));
    }


}
