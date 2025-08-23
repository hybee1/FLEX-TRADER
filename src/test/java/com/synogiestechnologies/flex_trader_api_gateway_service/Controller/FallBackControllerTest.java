package com.synogiestechnologies.flex_trader_api_gateway_service.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FallBackController.class)
@ActiveProfiles("test")
class FallBackControllerTest {

    @Autowired private WebTestClient webTestClient;

    @Test
    void testAuthFallback_ReturnsServiceUnavailable() throws Exception {
        webTestClient.get()
                .uri("/auth-fallback")
                .exchange()
                .expectStatus().isEqualTo(503) // 503 Service Unavailable
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Authentication service is unavailable")
                .jsonPath("$.code").isEqualTo("SERVICE_UNAVAILABLE");
    }
}
