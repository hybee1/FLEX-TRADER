package com.synogiestechnologies.flex_trader_auth.Config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.synogiestechnologies.flex_trader_auth.Exceptions.ExpiredTokenException;
import com.synogiestechnologies.flex_trader_auth.Jwt.JwtService;
import com.synogiestechnologies.flex_trader_auth.Service.MyUsersDetailsService;
import com.synogiestechnologies.flex_trader_auth.UserDetails.MyUsersDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MyUsersDetailsService myUsersDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   MyUsersDetailsService myUsersDetailsService) {
        this.jwtService = jwtService;
        this.myUsersDetailsService = myUsersDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {


        try {
            log.info("doFilterInternal -- 1");
            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer")) {
                log.info("doFilterInternal -- 2");
                filterChain.doFilter(request, response);
                log.info("doFilterInternal -- 2a");
                return;
            }

            log.info("doFilterInternal -- 3");
            final String jwtToken = authHeader.substring(7);  // authHeader.substring(7)
            log.info("jwt token: " + jwtToken);

            final String tokenUsername = jwtService.extractUserName(jwtToken);
            log.info("tokeUsername: " + tokenUsername);
            log.info("doFilterInternal -- 4");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            log.info("doFilterInternal -- 5");
            if (tokenUsername != null && authentication == null) {
                log.info("doFilterInternal -- 6");
                MyUsersDetails usersDetails =
                        (MyUsersDetails) myUsersDetailsService.loadUserByUsername(tokenUsername);
                log.info("usersDetails: " + usersDetails.getUsername());

                if (jwtService.isTokenValid(jwtToken, usersDetails)) {
                    log.info("doFilterInternal -- 7");

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    usersDetails.getUsername(),
                                    null,
                                    usersDetails.getAuthorities()
                            );

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource()
                            .buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
                else{
                  //  (jwtService.isTokenValid(jwtToken, usersDetails))  was false
                    log.info("doFilterInternal -- 7a was false");
                    log.info("Token is expired, but allowing /mylogout request only.");

                    String path = request.getRequestURI();
                    log.info("request path "+ path);
                    boolean isPermittedPath = path.equals("/api/authentication/mylogout");

                    if (isPermittedPath) {
                        log.info("doFilterInternal -- 7b was false");
                        return; // this will Prevent further filters if unauthorized
                    }
                    else{
                        log.info("doFilterInternal -- 7c was false");
                        throw new ExpiredTokenException("Invalid or expired token",
                                HttpStatus.BAD_REQUEST);
                    }
                }
            }
            else{
                // (tokeUsername != null && authentication == null) was false
                log.info("doFilterInternal -- 6a was false");

            }

        } catch (Exception ex) {

            log.info("INSIDE TOKEN EXCEPTION OR AuthorizationDeniedException");

            log.warn("TOKEN EXCEPTION OR AuthorizationDeniedException: {}", ex.getMessage());

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());

            new ObjectMapper().writeValue(response.getOutputStream(), error);

        }

        filterChain.doFilter(request, response);

    }

}
