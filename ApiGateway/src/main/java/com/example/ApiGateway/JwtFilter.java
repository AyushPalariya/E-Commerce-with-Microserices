package com.example.ApiGateway;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.naming.AuthenticationException;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path=request.getRequestURI();
        if(path.startsWith("/auth")||path.equals("/user/profile")){
            filterChain.doFilter(request,response);
            System.out.println("auth api passess.."+path);
            return;
        }
        String authorization = request.getHeader("Authorization");
        if(authorization!=null&&authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                if (!jwtService.isAccessToken(token)) {
                    request.setAttribute("error", "Token is not an access token");
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                String email = jwtService.getUserEmail(token);
                String role = jwtService.getRole(token);
                if (email == null || role == null || email.isBlank() || role.isBlank()) {
                    request.setAttribute("error", "Required JWT claims are missing");
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(role)));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } catch (SignatureException ex) {
                SecurityContextHolder.clearContext();
                request.setAttribute("error", "Invalid Token signature");
            } catch (ExpiredJwtException ex) {
                SecurityContextHolder.clearContext();
                request.setAttribute("error", "Access token Expired");
            } catch (JwtException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute("error", "Invalid or mailFormed Jwt");
            }
        }
        filterChain.doFilter(request,response);
    }
}
