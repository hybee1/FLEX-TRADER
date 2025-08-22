package com.synogiestechnologies.flex_trader_auth.Mapper;

import com.synogiestechnologies.flex_trader_auth.DTORequest.LoginRequest;
import com.synogiestechnologies.flex_trader_auth.DTOResponse.LoginResponseDTO;
import com.synogiestechnologies.flex_trader_auth.Models.JwtToken;
import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MyUsersMapperTest {

    private MyUsersMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MyUsersMapper();
    }

    @Test
    void TestGivenValidToken_whenMapping_thenReturnLoginResponseDTO() {
        JwtToken validToken = JwtToken.builder()
                .token("valid-token")
                .isRevoked(false)
                .tokenExpiryDate(Instant.now().plusSeconds(3600))
                .build();

        MyUsers user = MyUsers.builder()
                .tokens(List.of(validToken))
                .build();

        LoginResponseDTO dto = mapper.NewUserToLoginResponseDTO(user);

        assertEquals("valid-token", dto.getToken());
    }

    @Test
    void testGivenNoTokens_whenMapping_thenThrowException() {
        MyUsers user = MyUsers.builder()
                .tokens(List.of())
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mapper.NewUserToLoginResponseDTO(user));

        assertEquals("User has No active token found", ex.getMessage());
    }

    @Test
    void testGivenOnlyRevokedTokens_whenMapping_thenThrowException() {
        JwtToken revoked = JwtToken.builder()
                .token("revoked")
                .isRevoked(true)
                .tokenExpiryDate(Instant.now().plusSeconds(3600))
                .build();

        MyUsers user = MyUsers.builder()
                .tokens(List.of(revoked))
                .build();

        assertThrows(RuntimeException.class,
                () -> mapper.NewUserToLoginResponseDTO(user));
    }

    @Test
    void testGivenOnlyExpiredTokens_whenMapping_thenThrowException() {
        JwtToken expired = JwtToken.builder()
                .token("expired")
                .isRevoked(false)
                .tokenExpiryDate(Instant.now().minusSeconds(10))
                .build();

        MyUsers user = MyUsers.builder()
                .tokens(List.of(expired))
                .build();

        assertThrows(RuntimeException.class,
                () -> mapper.NewUserToLoginResponseDTO(user));
    }

    @Test
    void testGivenMultipleTokens_whenValidExists_thenReturnFirstValidToken() {
        JwtToken expired = JwtToken.builder()
                .token("expired")
                .isRevoked(false)
                .tokenExpiryDate(Instant.now().minusSeconds(10))
                .build();

        JwtToken valid = JwtToken.builder()
                .token("valid")
                .isRevoked(false)
                .tokenExpiryDate(Instant.now().plusSeconds(3600))
                .build();

        JwtToken anotherValid = JwtToken.builder()
                .token("another")
                .isRevoked(false)
                .tokenExpiryDate(Instant.now().plusSeconds(7200))
                .build();

        MyUsers user = MyUsers.builder()
                .tokens(List.of(expired, valid, anotherValid))
                .build();

        LoginResponseDTO dto = mapper.NewUserToLoginResponseDTO(user);

        assertEquals("valid", dto.getToken());
    }

    @Test
    void testGivenTokenExpiryEqualNow_whenMapping_thenThrowException() {
        JwtToken edgeToken = JwtToken.builder()
                .token("edge")
                .isRevoked(false)
                .tokenExpiryDate(Instant.now()) // exactly now
                .build();

        MyUsers user = MyUsers.builder()
                .tokens(List.of(edgeToken))
                .build();

        assertThrows(RuntimeException.class,
                () -> mapper.NewUserToLoginResponseDTO(user));
    }

    // ******** Tests for MyUsersToLoginRequest **********

    @Test
    void testGivenValidUser_whenMapping_thenReturnLoginRequest() {
        MyUsers user = MyUsers.builder()
                .username("alice")
                .password("secret")
                .build();

        LoginRequest req = mapper.MyUsersToLoginRequest(user);

        assertEquals("alice", req.getUserNameOrEmail());
        assertEquals("secret", req.getPassword());
    }

    @Test
    void testGivenNullFields_whenMapping_thenReturnLoginRequestWithNulls() {
        MyUsers user = MyUsers.builder()
                .username(null)
                .password(null)
                .build();

        LoginRequest req = mapper.MyUsersToLoginRequest(user);

        assertNull(req.getUserNameOrEmail());
        assertNull(req.getPassword());
    }

    @Test
    void testGivenEmptyFields_whenMapping_thenReturnLoginRequestWithEmptyValues() {
        MyUsers user = MyUsers.builder()
                .username("")
                .password("")
                .build();

        LoginRequest req = mapper.MyUsersToLoginRequest(user);

        assertEquals("", req.getUserNameOrEmail());
        assertEquals("", req.getPassword());
    }

}