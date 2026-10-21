package com.example.ApiGateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private JwtFilter jwtFilter;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity){
        httpSecurity.csrf((csrf)->csrf.disable())
                .formLogin(f->f.disable())
                .httpBasic(b->b.disable())
                .sessionManagement((sm)->sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req->req.requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/user/**").hasAnyRole("ADMIN","USER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex->ex.authenticationEntryPoint((request, response, authException) ->
                {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    String message = "Unauthorized: " + authException.getMessage();
                    String attrError = (String) request.getAttribute("error");
                    Map<String, String> body = Map.of(
                            "error",attrError,
                            "message", message,
                            "status", String.valueOf(HttpStatus.UNAUTHORIZED),
                            "path", request.getRequestURI(),
                            "timestamp", LocalDateTime.now().toString()
                    );
                    response.getWriter().write(new ObjectMapper().writeValueAsString(body));
                })
                                .accessDeniedHandler(((request, response, accessDeniedException) -> {
                                    response.setStatus(HttpStatus.FORBIDDEN.value());
                                    response.setContentType("application/json");
                                    Map<String, String> body = Map.of(
                                            "error",accessDeniedException.getMessage(),
                                            "message", "Access denied: insufficient privileges",
                                            "status", String.valueOf(HttpStatus.FORBIDDEN),
                                            "path", request.getRequestURI(),
                                            "timestamp", LocalDateTime.now().toString()
                                    );
                                    response.getWriter().write(new ObjectMapper().writeValueAsString(body));
                                }))
                ).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();

    }
}
