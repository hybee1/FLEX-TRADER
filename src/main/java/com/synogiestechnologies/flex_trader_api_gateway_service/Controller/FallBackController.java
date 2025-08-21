package com.synogiestechnologies.flex_trader_api_gateway_service.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class FallBackController {

    @RequestMapping("/auth-fallback")
    public ResponseEntity<Map<String, String>> authFallback() {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Authentication service is unavailable");
        error.put("code", "SERVICE_UNAVAILABLE");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
}