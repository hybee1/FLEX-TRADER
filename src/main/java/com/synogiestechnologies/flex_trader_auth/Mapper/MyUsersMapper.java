package com.synogiestechnologies.flex_trader_auth.Mapper;

import com.synogiestechnologies.flex_trader_auth.DTORequest.LoginRequest;
import com.synogiestechnologies.flex_trader_auth.DTOResponse.LoginResponseDTO;
import com.synogiestechnologies.flex_trader_auth.Models.JwtToken;
import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MyUsersMapper {

    public LoginResponseDTO NewUserToLoginResponseDTO(MyUsers newUser) {
        String resToken = newUser.getTokens().stream()
                .filter(token -> !token.isRevoked() &&
                        token.getTokenExpiryDate().isAfter(Instant.now()))
                .map(JwtToken::getToken)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User has No active token found"));

        return LoginResponseDTO.builder()
                .token(resToken)
                .build();
    }

    public LoginRequest MyUsersToLoginRequest(MyUsers user){
        return LoginRequest.builder()
                .userNameOrEmail(user.getUsername())
                .password(user.getPassword())
                .build();
    }

}


