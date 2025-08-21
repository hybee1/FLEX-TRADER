package com.synogiestechnologies.flex_trader_websocket_server.Config;


import com.synogiestechnologies.flex_trader_websocket_server.HandShakeInterceptor.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@EnableMethodSecurity(prePostEnabled = true)
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public StompWebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/websocket")
                .setAllowedOrigins("*");
                // .addInterceptors(jwtHandshakeInterceptor);
//                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config
                // /app/to-server client → server prefix  /app
                .setApplicationDestinationPrefixes("/app")

                // /topic/broadcast/bot-signal = server → client
                .enableSimpleBroker("/topic");
    }
}
