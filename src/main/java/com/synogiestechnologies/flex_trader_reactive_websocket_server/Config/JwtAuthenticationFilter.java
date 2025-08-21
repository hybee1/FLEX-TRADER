package com.synogiestechnologies.flex_trader_reactive_websocket_server.Config;

import com.synogiestechnologies.flex_trader_reactive_websocket_server.JwtService.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;


@Slf4j
@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService ) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        System.out.println("filter -- 1");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            System.out.println("filter -- 2");
            String token = authHeader.substring(7);
            System.out.println("filter -- 3");

            return jwtService.isTokenValidReactive(token)

                    .flatMap(isValid -> {
                        if (!isValid) {
                            System.out.println("filter -- 4");
                            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT"));
                        }

                        // Extract user details from JWT
                        System.out.println("filter -- 5");
                        Authentication auth = jwtService.getAuthentication(token);
                        // Store authentication in reactive security context
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                    });

        }

        System.out.println("filter -- 6");
        return chain.filter(exchange);
    }
}
