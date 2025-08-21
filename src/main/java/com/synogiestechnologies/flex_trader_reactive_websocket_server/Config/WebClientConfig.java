package com.synogiestechnologies.flex_trader_reactive_websocket_server.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced //
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient authWebClient(WebClient.Builder builder,
                                   @Value("${AUTH_APP_NAME}") String authAppName) {
        // ib This will use the Eureka service ID instead of host:port
        return builder
                .baseUrl("http://" + authAppName) // ib No port needed here it will be resolved by LoadBalancer
                .build();
    }
}

