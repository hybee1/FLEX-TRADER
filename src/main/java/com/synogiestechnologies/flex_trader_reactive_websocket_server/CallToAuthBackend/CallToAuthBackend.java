package com.synogiestechnologies.flex_trader_reactive_websocket_server.CallToAuthBackend;

import com.synogiestechnologies.flex_trader_reactive_websocket_server.DTORequest.TokenRevokedRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Component
@FeignClient(name = "${AUTH_APP_NAME}") // (name = "FLEX-TRADER-AUTH-SERVICE")
public interface CallToAuthBackend {
// configuration = FeignConfig.class
    @PostMapping(value="/api/authentication/revoked-token", consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<Map<String, Object>> getTokenRevocationStatus(@RequestBody
                                                  TokenRevokedRequest tokenRequest) throws Exception;



}
