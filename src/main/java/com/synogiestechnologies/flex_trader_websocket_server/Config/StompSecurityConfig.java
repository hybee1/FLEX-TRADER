package com.synogiestechnologies.flex_trader_websocket_server.Config;

import com.synogiestechnologies.flex_trader_websocket_server.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.nio.file.AccessDeniedException;
import java.security.Principal;

@Slf4j
@Configuration
public class StompSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    public StompSecurityConfig(JwtService jwtService) {

        this.jwtService = jwtService;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {

                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                accessor.setLeaveMutable(true);
                StompHeaderAccessor errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);

                System.out.println("configureClientInboundChannel preSend -- 1");

                try {

                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                        System.out.println("configureClientInboundChannel StompCommand.CONNECT -- 1");


                        String bearer = accessor.getFirstNativeHeader("Authorization");
                        String token = (bearer != null && bearer.startsWith("Bearer "))
                                ? bearer.substring(7)
                                : null;

                        if (token == null) {
                            System.out.println("configureClientInboundChannel StompCommand.CONNECT-- 1a");
                            System.out.println("token is null, permission denied for " +
                                    "stomp over websocket ");
                            log.warn("token is null, permission denied for stomp over websocket");
                            // throw new AccessDeniedException("Permission denied");
                            throw new MessagingException("Token is bad or expired or not subscribed");

//                            errorAccessor.setHeader("message", "Token is missing");
//                            errorAccessor.setMessage("Unauthorized, token is missing");
//                            errorAccessor.setSessionId(accessor.getSessionId());
//                            return MessageBuilder.createMessage(
//                                    "Token is missing,".getBytes(), errorAccessor.getMessageHeaders());

                        }

                        // Validate token
                        if (token != null && !jwtService.isTokenValid(token)) {

                            System.out.println("configureClientInboundChannel StompCommand.CONNECT -- 2");
                            System.out.println("token not valid (invalid or expired) in " +
                                    "configureClientInboundChannel StompCommand.CONNECT");
                            log.warn("token not valid (invalid or expired) in " +
                                    "configureClientInboundChannel StompCommand.CONNECT");
                            throw new MessagingException("Token is bad or expired or not subscribed");
//                            errorAccessor.setHeader("message", "token is bad or expired or " +
//                                    "stomp over websocket");
//                            errorAccessor.setMessage("Unauthorized or token is invalid or expired");
//                            return MessageBuilder.createMessage(
//                                    "Token is bad or expired ".getBytes(),
//                                    errorAccessor.getMessageHeaders());
                        }

                        if (token != null && !jwtService.canConnectToWebsocket(token)) {
                            System.out.println("configureClientInboundChannel StompCommand.CONNECT-- 3");
                            System.out.println("token is bad or expired or or not subscribed, " +
                                    "permission denied for stomp over websocket ");
                            log.warn("token is bad or expired or not subscribed, " +
                                    "permission denied for stomp over websocket");
                            // throw new AccessDeniedException("Permission denied");
                            throw new MessagingException("Token is bad or expired or not subscribed");

//                            errorAccessor.setHeader("message", "token is bad or expired or " +
//                                    "not subscribed, permission denied for " +
//                                    "stomp over websocket");
//                            errorAccessor.setMessage("Unauthorized or token invalid or not subscribed");
//                            return MessageBuilder.createMessage(
//                                    "Token is bad or expired or not subscribed".getBytes(),
//                                    errorAccessor.getMessageHeaders());
                        }

                        System.out.println("configureClientInboundChannel StompCommand.CONNECT-- 4");
                        System.out.println("Successfully connected to stomp over websocket");

                        System.out.println("configureClientInboundChannel StompCommand.CONNECT -- 5");
                        System.out.println("getting principal(User) from token ");
                        log.warn(" getting principal(User) from token in configureClientInboundChannel " +
                                "StompCommand.CONNECT ");
                        Authentication auth = jwtService.getAuthentication(token);
                        accessor.setUser(auth);

                    }

                    if (StompCommand.SEND.equals(accessor.getCommand())
                            && "/app/to-server".equals(accessor.getDestination())) {
                        System.out.println("configureClientInboundChannel preSend -- 6");
                        Principal user = accessor.getUser();
                        if (user == null || !"alice".equals(user.getName())) {
                            System.out.println("configureClientInboundChannel preSend -- 6a");
                            throw new AccessDeniedException("Not allowed to send");
                        }
                        System.out.println("configureClientInboundChannel preSend -- 6b");
                        log.info("An Authorized client " + user.getName() + " sent message to " +
                                "the server for broadcast");
                        System.out.println("An Authorized client sent message to " +
                                "the server for broadcast");
                    }
                }
                catch (Exception ex) {
                    System.out.println("configureClientInboundChannel preSend -- 7");
                    System.out.println("reason for exception: " + ex.getMessage());
                    log.warn("Handshake denied: {}", ex.getMessage());
                    throw new MessagingException("Access Denied", ex);

                }

                return message;
            }
        });
    }
}

