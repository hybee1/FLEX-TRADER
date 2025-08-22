package com.synogiestechnologies.flex_trader_auth.Service;


import com.synogiestechnologies.flex_trader_auth.AllEnums.Role;
import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionPlanType;
import com.synogiestechnologies.flex_trader_auth.DTORequest.LogOutRequest;
import com.synogiestechnologies.flex_trader_auth.Exceptions.SubscriptionException;
import com.synogiestechnologies.flex_trader_auth.Exceptions.TokenNotFoundException;
import com.synogiestechnologies.flex_trader_auth.Exceptions.UserHasActiveTokenException;
import com.synogiestechnologies.flex_trader_auth.Models.JwtToken;
import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import com.synogiestechnologies.flex_trader_auth.Models.Subscription;
import com.synogiestechnologies.flex_trader_auth.Repository.JwtTokenRepo;
import com.synogiestechnologies.flex_trader_auth.UserDetails.MyUsersDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private String secretKey = "4720e2fdbc1f421d498ab2b181aafbcd31c58b8c49984a85b27d82969872ec14";
    private static final long VALIDITY = TimeUnit.SECONDS.toMillis(15);

    private MyUsers user;
    private MyUsersDetails userDetails;

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    JwtTokenRepo jwtTokenRepo;

    @Spy
    @InjectMocks
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        // inject a valid base64 secret key (32 bytes = 256 bits)
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY",
                Base64.getEncoder().encodeToString(secretKey.getBytes()));

        user = MyUsers.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .password("password")
                .role(Role.USER)
                .build();

        userDetails = new MyUsersDetails(user);
    }

    @Test
    void testGenerateLoginToken_ShouldContainUsernameAndRoles() {
        String token = jwtService.generateLoginToken(userDetails);

        assertNotNull(token);
        assertEquals("alice", jwtService.extractUserName(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void testGenerateLoginPlusWebsocketToken_ShouldThrow_WhenNoSubscription() {
        when(subscriptionService.findNonExpiredSubscriptionWithUsername(eq("alice"), any()))
                .thenReturn(Optional.empty());

        assertThrows(SubscriptionException.class,
                () -> jwtService.generateLoginPlusWebsocketToken(userDetails));
    }

    @Test
    void testGenerateLoginPlusWebsocketToken_ShouldContainSubscriptionClaims() {
        Subscription sub = Subscription.builder()
                .subscriptionPlanType(SubscriptionPlanType.PRO)
                .subStartDate(Instant.now())
                .subExpiryDate(Instant.now().plusSeconds(300))
                .build();
        when(subscriptionService.findNonExpiredSubscriptionWithUsername(eq("alice"), any()))
                .thenReturn(Optional.of(sub));

        String token = jwtService.generateLoginPlusWebsocketToken(userDetails);

        assertNotNull(token);
        assertEquals("alice", jwtService.extractUserName(token));
    }

    @Test
    void isTokenRevoked_ShouldThrow_WhenTokenNotFound() {
        String token = jwtService.generateLoginToken(userDetails);
        when(jwtTokenRepo.getRevocationStatus(token)).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundException.class, () -> jwtService.isTokenRevoked(token));
    }

    @Test
    void testIsTokenRevoked_ShouldReturnStatusFromRepo() {
        String token = jwtService.generateLoginToken(userDetails);
        when(jwtTokenRepo.getRevocationStatus(token)).thenReturn(Optional.of(true));

        assertTrue(jwtService.isTokenRevoked(token));
    }

    @Test
    void testIsTokenValid_ShouldReturnTrue_WhenTokenValidAndNotRevoked() {
        String token = jwtService.generateLoginToken(userDetails);
        when(jwtTokenRepo.getRevocationStatus(token)).thenReturn(Optional.of(false));

        boolean result = jwtService.isTokenValid(token, userDetails);

        assertTrue(result);
    }

    @Test
    void testSaveNewTokenForUser_ShouldSaveSuccessfully() {
        String token = "abc.def.ghi";
        Instant expiry = Instant.now().plusMillis(VALIDITY);

        JwtToken saved = JwtToken.builder()
                .id(1L)
                .token(token)
                .isRevoked(false)
                .tokenExpiryDate(expiry)
                .user(user)
                .build();

        when(jwtTokenRepo.existsByUserAndIsRevokedFalseAndTokenExpiryDateAfter(any(), any()))
                .thenReturn(false);
        when(jwtTokenRepo.save(any())).thenReturn(saved);

        JwtToken result = jwtService.saveNewTokenForUser(user, token, expiry);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testSaveNewTokenForUser_ShouldThrow_WhenUserHasActiveToken() {
        when(jwtTokenRepo.existsByUserAndIsRevokedFalseAndTokenExpiryDateAfter(any(), any()))
                .thenReturn(true);

        assertThrows(UserHasActiveTokenException.class,
                () -> jwtService.saveNewTokenForUser(user, "abc", Instant.now().plusSeconds(300)));
    }

    @Test
    void testDeleteToken_ShouldDelete_WhenTokenExists() {
        JwtToken token = JwtToken.builder().id(1L).token("abc").user(user).build();
        when(jwtTokenRepo.findByToken("abc")).thenReturn(Optional.of(token));

        boolean result = jwtService.deleteToken("abc");

        assertTrue(result);
        verify(jwtTokenRepo).deleteById(1L);
    }

    @Test
    void deleteToken_ShouldThrow_WhenNotFound() {
        when(jwtTokenRepo.findByToken("abc")).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundException.class, () -> jwtService.deleteToken("abc"));
    }

    @Test
    void testDeleteTokenByUsername_shouldDelete_WhenUsernameExist() {
        JwtToken token = JwtToken.builder().id(1L).token("abc").user(user).build();
        when(jwtTokenRepo.findByUserAndIsRevokedFalse(user)).thenReturn(Optional.of(token));

        boolean result = jwtService.deleteTokenByUsername(user);

        assertTrue(result);
        verify(jwtTokenRepo).deleteById(1L);
    }

    @Test
    void testDeleteTokenByUsername_ShouldThrow_WhenUsernameNotExist() {
        when(jwtTokenRepo.findByUserAndIsRevokedFalse(user)).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundException.class, () -> jwtService.deleteTokenByUsername(user));
    }

    @Test
    void testLogOut_ShouldRevokeToken_WhenTokenNotRevoked() {

        JwtToken jwtToken = JwtToken.builder()
                .token("abc")
                .user(user)
                .isRevoked(false)
                .createdDate(Instant.now())
                .tokenExpiryDate(Instant.now().plusMillis(VALIDITY))
                .id(1L)
                .build();

        when(jwtTokenRepo.findByToken("abc")).thenReturn(Optional.of(jwtToken));

        when(jwtTokenRepo.save(any())).thenReturn(jwtToken);

        ResponseEntity<Map<String, Object>> result = jwtService.logOut(new LogOutRequest("abc"));

        assertTrue(result.getStatusCode().is2xxSuccessful());

        Map<String, Object> dataBody = result.getBody();
        Map<String, Boolean> data = (Map<String, Boolean>) dataBody.get("data");

        boolean isRevoked = data.get("revoked");

        assertTrue(isRevoked);

    }

    @Test
    void testLogOut_ShouldThrow_WhenTokenNotFound() {
        when(jwtTokenRepo.findByToken("abc")).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundException.class, () -> jwtService.logOut(new LogOutRequest("abc")));
    }

    @Test
    void testGenerateLoginTokenAndSaveIt_ShouldSaveSuccessfully() {
        String fakeToken = "abc.def.ghi";
        Date expiryDate = Date.from(Instant.now().plusMillis(VALIDITY));

        JwtToken savedToken = JwtToken.builder()
                .id(1L)
                .token(fakeToken)
                .isRevoked(false)
                .tokenExpiryDate(expiryDate.toInstant())
                .user(user)
                .build();

        // mock behaviour
        Mockito.doReturn(true).when(jwtService).deleteTokenByUsername(user);
        Mockito.doReturn(fakeToken).when(jwtService).generateLoginToken(userDetails);
        Mockito.doReturn(expiryDate).when(jwtService).extractExpiration(fakeToken);
        Mockito.doReturn(savedToken).when(jwtService).saveNewTokenForUser(user, fakeToken, expiryDate.toInstant());

        // when
        ResponseEntity<Map<String, Object>> response =
                jwtService.generateLoginTokenAndSaveIt(user, userDetails);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertTrue(responseBody.containsKey("data"));

        @SuppressWarnings("unchecked")
        Map<String, String> tokenMap = (Map<String, String>) responseBody.get("data");

        assertEquals(fakeToken, tokenMap.get("token"));

        // verify interactions
        Mockito.verify(jwtService).deleteTokenByUsername(user);
        Mockito.verify(jwtService).generateLoginToken(userDetails);
        Mockito.verify(jwtService).extractExpiration(fakeToken);
        Mockito.verify(jwtService).saveNewTokenForUser(user, fakeToken, expiryDate.toInstant());
    }

    @Test
    void testGenerateLoginTokenAndSaveIt_ShouldRetryOnActiveTokenException() {
        String firstTokenString = "abc.def.ghi";
        String secondTokenString = "jkl.nop.qrs";
        Date expiryDate = Date.from(Instant.now().plusMillis(VALIDITY));

        JwtToken firstToken = JwtToken.builder()
                .id(1L)
                .token(firstTokenString)
                .isRevoked(false)
                .tokenExpiryDate(expiryDate.toInstant())
                .user(user)
                .build();

        JwtToken secondToken = JwtToken.builder()
                .id(2L)
                .token(secondTokenString)
                .isRevoked(false)
                .tokenExpiryDate(expiryDate.toInstant())
                .user(user)
                .build();

        // mock behaviour
        Mockito.doReturn(true).when(jwtService).deleteTokenByUsername(user);
        // on retry first token, second token. that is two attempts. first normal attempt, second is a retry
        Mockito
                .doReturn(firstTokenString) // first = normal attempt
                .doReturn(secondTokenString) // second is a retry
                .when(jwtService).generateLoginToken(userDetails);

        Mockito.doReturn(expiryDate).when(jwtService).extractExpiration(anyString());

        // First attempt fails
        Mockito.doThrow(new UserHasActiveTokenException("active token exists", HttpStatus.CONFLICT))
                .when(jwtService).saveNewTokenForUser(user, firstTokenString, expiryDate.toInstant());

        // second attempt succeeds
        Mockito.doReturn(secondToken).when(jwtService)
                .saveNewTokenForUser(user, secondTokenString, expiryDate.toInstant());

        // when
        ResponseEntity<Map<String, Object>> response =
                jwtService.generateLoginTokenAndSaveIt(user, userDetails);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> responseBody = response.getBody();
        assertTrue(responseBody.containsKey("data"));

        @SuppressWarnings("unchecked")
        Map<String, String> tokenMap = (Map<String, String>) responseBody.get("data");

        assertEquals(secondToken.getToken(), tokenMap.get("token"));

        // verify interactions
        Mockito.verify(jwtService, atLeastOnce()).deleteTokenByUsername(user);
        Mockito.verify(jwtService, atLeastOnce()).generateLoginToken(userDetails);
        Mockito.verify(jwtService, atLeastOnce()).extractExpiration(anyString());
        Mockito.verify(jwtService, atLeastOnce()).saveNewTokenForUser(user, secondTokenString, expiryDate.toInstant());

    }
}