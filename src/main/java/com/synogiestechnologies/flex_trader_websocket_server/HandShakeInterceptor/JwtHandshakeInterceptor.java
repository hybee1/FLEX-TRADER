package com.synogiestechnologies.flex_trader_websocket_server.HandShakeInterceptor;

import com.synogiestechnologies.flex_trader_websocket_server.Exceptions.ExpiredTokenException;
import com.synogiestechnologies.flex_trader_websocket_server.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.nio.file.AccessDeniedException;
import java.util.Map;

@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {

        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request, @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes ) throws Exception {

        try {

            System.out.println("JwtHandshakeInterceptor -- 1");
            // Extract token from header
            HttpServletRequest httpServletRequest =
                    ((ServletServerHttpRequest) request).getServletRequest();

            String authHeader = httpServletRequest.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("JwtHandshakeInterceptor -- 2");
                System.out.println("Authorization is missing, null or does not start with 'Bearer' " +
                        "in JwtHandshakeInterceptor");
                log.warn("Authorization is missing, null or does not start with 'Bearer' " +
                        "in JwtHandshakeInterceptor");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                // either use throw or response.setStatusCode(HttpStatus.UNAUTHORIZED)
                // throw new AccessDeniedException("Permission denied");
                // return is better here
                return false;
            }
            System.out.println("JwtHandshakeInterceptor -- 3");

            String token = authHeader.substring(7);

            // Validate token
            if (token == null) {

                System.out.println("JwtHandshakeInterceptor -- 3a");
                System.out.println("token is null in JwtHandshakeInterceptor");
                log.warn("token is null JwtHandshakeInterceptor");
                // either use throw or response.setStatusCode(...)
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                throw new ExpiredTokenException("Invalid token", HttpStatus.UNAUTHORIZED);
                // return false;  // reject handshake
            }

            // Validate token
            if (token != null && !jwtService.isTokenValid(token)) {

                System.out.println("JwtHandshakeInterceptor -- 4");
                System.out.println("token not valid (invalid or expired) in " +
                        "JwtHandshakeInterceptor");
                log.warn("token not valid (invalid or expired) in JwtHandshakeInterceptor");
                // either use throw or response.setStatusCode(...)
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                throw new ExpiredTokenException("Invalid token", HttpStatus.UNAUTHORIZED);
                // return false;  // reject handshake
            }

            if (!jwtService.canConnectToWebsocket(token)) {

                System.out.println("JwtHandshakeInterceptor -- 5");
                System.out.println("User is not authorized to connect via WebSocket in " +
                        "JwtHandshakeInterceptor");
                log.warn("User is not authorized to connect via WebSocket ");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                throw new AccessDeniedException("Permission denied");
                // return false;
            }
            else {
                System.out.println("JwtHandshakeInterceptor -- 6");
                System.out.println("getting principal(User) from token in JwtHandshakeInterceptor ");
                log.warn(" getting principal(User) from token ");
                Authentication auth = jwtService.getAuthentication(token);

                // 3) Optionally store authenticated principal for later
                System.out.println("storing authenticated principal(User) in attributes " +
                        "in JwtHandshakeInterceptor");
                log.warn(" storing authenticated principal(User) in attributes in JwtHandshakeInterceptor");

                attributes.put("auth", auth);

                System.out.println("User successfully connect via WebSocket in JwtHandshakeInterceptor");
                log.warn("User successfully connect via WebSocket in JwtHandshakeInterceptor");
                return true;
            }
        }
        catch (Exception ex) {

            System.out.println("JwtHandshakeInterceptor -- 7");

            System.out.println("Handshake denied in JwtHandshakeInterceptor");
            System.out.println("Handshake denied in JwtHandshakeInterceptor: "
                    + ex.getMessage());

            log.warn("Handshake denied in JwtHandshakeInterceptor: {}", ex.getMessage());

            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            // return false;
            throw new Exception(ex.getMessage());

        }
        // allow handshake
        //return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }


}


