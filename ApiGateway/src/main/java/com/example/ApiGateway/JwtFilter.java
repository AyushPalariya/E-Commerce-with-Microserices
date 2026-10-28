package com.example.ApiGateway;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private static final String AUTHENTICATED_EMAIL_HEADER = "X-Authenticated-Email";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        AuthenticatedEmailRequest wrappedRequest = new AuthenticatedEmailRequest(request);
        if (!request.getRequestURI().startsWith("/auth")) {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                try {
                    if (!jwtService.isAccessToken(token)) {
                        request.setAttribute("error", "Token is not an access token");
                        SecurityContextHolder.clearContext();
                    } else {
                        String email = jwtService.getUserEmail(token);
                        String role = jwtService.getRole(token);
                        if (email == null || role == null || email.isBlank() || role.isBlank()) {
                            request.setAttribute("error", "Required JWT claims are missing");
                            SecurityContextHolder.clearContext();
                        } else {
                            wrappedRequest.setAuthenticatedEmail(email);
                            request.setAttribute("Email", email);
                            UsernamePasswordAuthenticationToken authenticationToken =
                                    new UsernamePasswordAuthenticationToken(
                                            email, null, List.of(new SimpleGrantedAuthority(role)));
                            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        }
                    }
                } catch (SignatureException ex) {
                    SecurityContextHolder.clearContext();
                    request.setAttribute("error", "Invalid Token signature");
                } catch (ExpiredJwtException ex) {
                    SecurityContextHolder.clearContext();
                    request.setAttribute("error", "Access token Expired");
                } catch (JwtException ex) {
                    SecurityContextHolder.clearContext();
                    request.setAttribute("error", "Invalid or malformed JWT");
                }
                catch (Exception ex){
                    SecurityContextHolder.clearContext();
                    request.setAttribute("error","Token missing");
                }
            }
        }
        filterChain.doFilter(wrappedRequest, response);
    }

    private static final class AuthenticatedEmailRequest extends HttpServletRequestWrapper {
        private String authenticatedEmail;

        private AuthenticatedEmailRequest(HttpServletRequest request) {
            super(request);
        }

        private void setAuthenticatedEmail(String authenticatedEmail) {
            this.authenticatedEmail = authenticatedEmail;
        }

        @Override
        public String getHeader(String name) {
            if (AUTHENTICATED_EMAIL_HEADER.equalsIgnoreCase(name)) {
                return authenticatedEmail;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (AUTHENTICATED_EMAIL_HEADER.equalsIgnoreCase(name)) {
                return authenticatedEmail == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(authenticatedEmail));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> headerNames = Collections.list(super.getHeaderNames());
            headerNames.removeIf(AUTHENTICATED_EMAIL_HEADER::equalsIgnoreCase);
            if (authenticatedEmail != null) {
                headerNames.add(AUTHENTICATED_EMAIL_HEADER);
            }
            return Collections.enumeration(headerNames);
        }
    }
}
