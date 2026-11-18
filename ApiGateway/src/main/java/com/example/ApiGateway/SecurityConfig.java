package com.example.ApiGateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
public class SecurityConfig {
    @Autowired
    private JwtFilter jwtFilter;
    private String[] publicUrls=
            {       //after testing products define role ADMIN
                    "/products/**",
                    "/auth/**",
                    "/home/**",
                    "/actuator/**",
                    //after testing user/getAllProfile and order/getAllOrders define role ADMIN
                    "/user/getAllProfile",
                    "/order/getAllOrders",
                    "/payment/webhooks",
                    //after testing define admin role
                    "/inventory/**"
            };
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity){
        httpSecurity.csrf((csrf)->csrf.disable())
                .cors(Customizer.withDefaults())
                .formLogin(f->f.disable())
                .httpBasic(b->b.disable())
                .sessionManagement((sm)->sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req->req
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/auth/delete/{email}").hasAnyRole("USER","ADMIN")
                        .requestMatchers(publicUrls).permitAll()
                        .requestMatchers("/user/**").hasAnyRole("ADMIN","USER")
                        .requestMatchers("/order/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/payment/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/products/search").hasAnyRole("ADMIN","USER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex->ex.authenticationEntryPoint((request, response, authException) ->
                {

                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    String message = "Unauthorized: " + authException.getMessage();
                    String attrError = request.getAttribute("error")!=null?(String) request.getAttribute("error"):"Authentication fail!";
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
    //auto generawted password not show
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
