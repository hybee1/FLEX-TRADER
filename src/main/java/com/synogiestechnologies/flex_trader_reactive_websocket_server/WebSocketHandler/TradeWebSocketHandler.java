package com.synogiestechnologies.flex_trader_reactive_websocket_server.WebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synogiestechnologies.flex_trader_reactive_websocket_server.DTOResponse.BotSignalMessage;
import com.synogiestechnologies.flex_trader_reactive_websocket_server.JwtService.JwtService;
import com.synogiestechnologies.flex_trader_reactive_websocket_server.TradeEventPublisher.TradeEventPublisher;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Component
public class TradeWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtService jwtService;
    private final TradeEventPublisher tradeEventPublisher;

    public TradeWebSocketHandler( JwtService jwtService,
                                 TradeEventPublisher tradeEventPublisher) {
        this.jwtService = jwtService;
        this.tradeEventPublisher = tradeEventPublisher;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        System.out.println("INSIDE HANDLE -- 1");
        String sessionId = session.getId();
        System.out.println("New WebSocket connection: " + sessionId);

        // Extract token from header or query
        String token = extractToken(session);

        if (token == null) {
            System.out.println("token is null ");
            return session.close(CloseStatus.BAD_DATA);
        }

        System.out.println("INSIDE HANDLE -- 2");
        return jwtService.isTokenValidReactive(token)
                .flatMap(isValid -> {
                    System.out.println("INSIDE HANDLE -- 3");
                    if (!isValid) {
                        System.out.println("INSIDE HANDLE -- 3a");
                        System.out.println("token is invalid, expired or revoked ");
                        return session.close(CloseStatus.BAD_DATA);
                    } // })  jwtService.canConnectToWebsocket(token)

                    // Token valid — now check permission to connect
                    return jwtService.canConnectToWebsocketReactive(token)
                            .onErrorResume(ex -> {
                                System.err.println("Error checking WebSocket permission: " + ex.getMessage());
                                return Mono.just(false); // deny connection if error occurs
                            })
                            .flatMap(canConnect -> {
                                System.out.println("INSIDE HANDLE -- 4");

                                if (!canConnect) {
                                    System.out.println("INSIDE HANDLE -- 4a");
                                    System.out.println("token is valid but can not connect to websocket, " +
                                            "no valid subscription, access denied ");
                                    return session.close(CloseStatus.BAD_DATA);
                                }

                                // Convert TradeMessage to WebSocket text
                                System.out.println("INSIDE HANDLE -- 5");
                                System.out.println("about to publish to publish trade event");
                                Flux<WebSocketMessage> outbound = tradeEventPublisher
                                        .getStream()
                                        .map(this::toJson)
                                        .doOnNext(json -> System.out.println("Sending message: " + json))
                                        .map(session::textMessage)
                                        .doOnError(e -> System.out.println("Outbound ERROR: " + e.getMessage()))
                                        .doOnCancel(() -> System.out.println("Outbound cancelled"))
                                        .doOnSubscribe(sub -> System.out.println("Subscribed: " +
                                                sessionId))
                                        .doFinally(signal -> {
                                                    System.out.println("Outbound cancelled");
                                                    System.out.println("Disconnected: " + sessionId);
                                                }
                                        );
                                //.doOnCancel(() -> System.out.println("Outbound cancelled"));

                                // Consume inbound to keep connection alive
                                Mono<Void> inbound = session.receive()
                                        .doOnNext(msg -> {
                                                    System.out.println("INSIDE HANDLE -- 6");
                                                    System.out.println("Received: " + msg.getPayloadAsText());
                                                }
                                        )
                                        .then()
                                        .then(session.close(CloseStatus.NORMAL)); // <-- explicit;

                                System.out.println("INSIDE HANDLE -- 7");
                                System.out.println("CONCLUDING INBOUND OUTBOUND MESSAGES");

                            return session.send(outbound)
                                  .and(inbound)
                                  .doFinally(signal -> System.out.println("WebSocket closed: " + signal));
                            });

                });
    }

    private boolean isValidToken(String token) {
        System.out.println("INSIDE isValidToken 1");
        try {
            jwtService.isTokenValidReactive(token.replace("Bearer ", ""));
            return true;
        } catch (Exception e) {
            System.out.println("INSIDE isValidToken 2 error");
            return false;
        }
    }

    private String toJson(BotSignalMessage msg) {
        System.out.println("INSIDE toJson 1");
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            System.out.println("INSIDE toJson 2 error return empty map");
            return "{}";
        }
    }


    private String extractToken(WebSocketSession session) {
        System.out.println("Extracting token from HEADER");

        String authHeader = session.getHandshakeInfo().getHeaders().getFirst("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Remove "Bearer " prefix
        }

        else {
            System.out.println("Token from HEADER is null or invalid");
            System.out.println("Extracting token from QUERY PARAM");

            token = UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                    .build()
                    .getQueryParams()
                    .getFirst("token");

            if (token == null || token.isBlank()) {
                System.out.println("Token from QUERY PARAM is null or blank");
            }
        }

        return token;
    }

}
