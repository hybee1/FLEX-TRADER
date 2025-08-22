package com.synogiestechnologies.flex_trader_auth.Service;


import com.synogiestechnologies.flex_trader_auth.AllEnums.Role;
import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionDuration;
import com.synogiestechnologies.flex_trader_auth.AllEnums.SubscriptionPlanType;
import com.synogiestechnologies.flex_trader_auth.DTORequest.CreateUserRequest;
import com.synogiestechnologies.flex_trader_auth.DTORequest.LoginRequest;
import com.synogiestechnologies.flex_trader_auth.Exceptions.*;
import com.synogiestechnologies.flex_trader_auth.Mapper.MyUsersMapper;
import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import com.synogiestechnologies.flex_trader_auth.Models.Subscription;
import com.synogiestechnologies.flex_trader_auth.Repository.MyUsersRepo;
import com.synogiestechnologies.flex_trader_auth.UserDetails.MyUsersDetails;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class MyUsersServiceTest {

    private static final long VALIDITY = TimeUnit.SECONDS.toMillis(15);

    @Mock
    private MyUsersRepo myUsersRepo;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private MyUsersMapper myUsersMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private MyUsersService myUsersService;

    private MyUsers user;
    private MyUsersDetails userDetails;

    @BeforeEach
    void setUp() {
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
    void testVerifyUser_ShouldReturnLoginToken_WhenAuthenticatedAndNoSubscription() {
        LoginRequest request = LoginRequest.builder()
                .userNameOrEmail("alice")
                .password("password")
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(subscriptionService.findNonExpiredSubscriptionWithUsername(eq("alice"), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> expected = Map.of("data", Map.of("token", "jwt-token"));
        when(jwtService.generateLoginTokenAndSaveIt(user, userDetails))
                .thenReturn(ResponseEntity.ok(expected));

        ResponseEntity<Map<String, Object>> response = myUsersService.verifyUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-token", ((Map<String, String>) response.getBody().get("data")).get("token"));
    }

    @Test
    void testVerifyUser_ShouldReturnLoginPlusWebsocketToken_WhenAuthenticatedAndHasSubscription() {
        LoginRequest request = LoginRequest.builder()
                .userNameOrEmail("alice")
                .password("password")
                .build();

        Subscription sub = Subscription.builder()
                .user(user)
                .id(1L)
                .subStartDate(Instant.now())
                .subExpiryDate(Instant.now().plusMillis(VALIDITY))
                .subscriptionDuration(SubscriptionDuration.ONE_MONTH)
                .subscriptionPlanType(SubscriptionPlanType.PRO)
                .hasUsedFreePlan(true)
                .isCancelled(false)
                .upgradedToHigherSUbPlan(false)
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(subscriptionService.findNonExpiredSubscriptionWithUsername(eq("alice"), any()))
                .thenReturn(Optional.of(sub)); // just non-empty

        Map<String, Object> expected = Map.of("data", Map.of("token", "jwt-ws-token"));
        when(jwtService.generateLoginPlusWebsocketTokenAndSaveIt(user, userDetails))
                .thenReturn(ResponseEntity.ok(expected));

        ResponseEntity<Map<String, Object>> response = myUsersService.verifyUser(request);

        assertEquals("jwt-ws-token", ((Map<String, String>) response.getBody().get("data")).get("token"));
    }

    @Test
    void testVerifyUser_ShouldThrowInvalidCredentialsException_WhenAuthFails() {
        LoginRequest request = LoginRequest.builder()
                .userNameOrEmail("alice")
                .password("wrong")
                .build();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> myUsersService.verifyUser(request));
    }

    @Test
    void verifyUser_ShouldThrowLoginException_WhenPrincipalNotMyUsersDetails() {
        LoginRequest request = LoginRequest.builder()
                .userNameOrEmail("alice")
                .password("password")
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("not-user-details");

        when(authenticationManager.authenticate(any())).thenReturn(auth);

        assertThrows(LoginException.class, () -> myUsersService.verifyUser(request));
    }

    @Test
    void testCreateNewUser_ShouldThrowUserAlreadyExist_WhenUsernameExists() {
        CreateUserRequest dto = new CreateUserRequest("alice@example.com",
                "alice", "pass");

        Tuple tuple = mock(Tuple.class);
        when(tuple.get("usernameMatch", Long.class)).thenReturn(1L);
        when(tuple.get("emailMatch", Long.class)).thenReturn(0L);
        when(myUsersRepo.findUsernameAndEmailMatchCounts("alice", "alice@example.com"))
                .thenReturn(tuple);

        assertThrows(UserAlreadyExistException.class, () -> myUsersService.createNewUser(dto));
    }

    @Test
    void testCreateNewUser_ShouldThrowUserAlreadyExist_WhenEmailExists() {
        CreateUserRequest dto = new CreateUserRequest("alice@example.com",
                "alice", "pass");

        Tuple tuple = mock(Tuple.class);
        when(tuple.get("usernameMatch", Long.class)).thenReturn(0L);
        when(tuple.get("emailMatch", Long.class)).thenReturn(1L);
        when(myUsersRepo.findUsernameAndEmailMatchCounts("alice", "alice@example.com"))
                .thenReturn(tuple);

        assertThrows(UserAlreadyExistException.class, () -> myUsersService.createNewUser(dto));
    }

    @Test
    void testCreateNewUser_ShouldThrowUserCreationException_WhenIdIsNull() {
        CreateUserRequest dto = new CreateUserRequest("alice@example.com",
                "alice", "pass");

        Tuple tuple = mock(Tuple.class);
        when(tuple.get("usernameMatch", Long.class)).thenReturn(0L);
        when(tuple.get("emailMatch", Long.class)).thenReturn(0L);
        when(myUsersRepo.findUsernameAndEmailMatchCounts(any(), any())).thenReturn(tuple);

        MyUsers unsaved = MyUsers.builder().username("alice").email("alice@example.com").build();
        // when(myUsersMapper.myUsersDTORequestToMyUsers(dto)).thenReturn(unsaved);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(myUsersRepo.save(any())).thenReturn(unsaved); // note: id is null here, the reason for error

        assertThrows(UserCreationException.class, () -> myUsersService.createNewUser(dto));
    }

    @Test
    void testCreateNewUser_ShouldReturnOk_WhenSavedAndLoginSucceeds() {
        CreateUserRequest dto = new CreateUserRequest("alice@example.com", "alice", "pass");

        Tuple tuple = mock(Tuple.class);
        when(tuple.get("usernameMatch", Long.class)).thenReturn(0L);
        when(tuple.get("emailMatch", Long.class)).thenReturn(0L);
        when(myUsersRepo.findUsernameAndEmailMatchCounts(any(), any())).thenReturn(tuple);

        MyUsers saved = MyUsers.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        // when(myUsersMapper.myUsersDTORequestToMyUsers(dto)).thenReturn(saved);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(myUsersRepo.save(any())).thenReturn(saved);

        // mock verifyUser call
        when(jwtService.generateLoginTokenAndSaveIt(any(), any()))
                .thenReturn(ResponseEntity.ok(Map.of("data", Map.of("token", "signup-token"))));
        when(subscriptionService.findNonExpiredSubscriptionWithUsername(eq("alice"), any()))
                .thenReturn(Optional.empty());

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(new MyUsersDetails(saved));
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        ResponseEntity<Map<String, Object>> response = myUsersService.createNewUser(dto);

        assertEquals("signup-token",
                ((Map<String, String>) response.getBody().get("data")).get("token"));
    }

    @Test
    void testCreateNewUser_ShouldReturnUnauthorized_WhenLoginFailsAfterSave() {
        CreateUserRequest dto = new CreateUserRequest("alice@example.com", "alice", "pass");

        Tuple tuple = mock(Tuple.class);
        when(tuple.get("usernameMatch", Long.class)).thenReturn(0L);
        when(tuple.get("emailMatch", Long.class)).thenReturn(0L);
        when(myUsersRepo.findUsernameAndEmailMatchCounts(any(), any())).thenReturn(tuple);

        MyUsers saved = MyUsers.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        // when(myUsersMapper.myUsersDTORequestToMyUsers(dto)).thenReturn(saved);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(myUsersRepo.save(any())).thenReturn(saved);

        // simulate login failure
        when(authenticationManager.authenticate(any()))
                .thenThrow(new AuthenticationServiceException("fail"));

        ResponseEntity<Map<String, Object>> response = myUsersService.createNewUser(dto);

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("User creation rolled back because login attempt failed. " +
                        "Please try again later.", response.getBody().get("errorMessage"));
    }


    @Test
    void testIsTokenRevoked_ShouldReturnTrue() {
        String token = "abc.def";
        Mockito.doReturn(true).when(jwtService).isTokenRevoked(token);
        // when(jwtService.isTokenRevoked(token)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = myUsersService.isTokenRevoked(token);


        assertTrue(((Map<String, Boolean>) response.getBody().get("data")).get("isRevoked"));

    }

    @Test
    void testIsTokenRevoked_ShouldReturnFalse() {
        String token = "abc.def";
        // Mockito.doReturn(false).when(jwtService).isTokenRevoked(token);
        when(jwtService.isTokenRevoked(token)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = myUsersService.isTokenRevoked(token);


        assertFalse(((Map<String, Boolean>) response.getBody().get("data")).get("isRevoked"));

    }

    @Test
    void testIsTokenRevoked_ShouldThrowException_WhenTokenNotFound() {
        String token = "abc.def";
        // Mockito.doReturn(false).when(jwtService).isTokenRevoked(token);
        when(jwtService.isTokenRevoked(token)).thenThrow(new TokenNotFoundException("Bad token",
                HttpStatus.NOT_FOUND));

        assertThrows(TokenNotFoundException.class, () -> myUsersService.isTokenRevoked(token));

    }


}