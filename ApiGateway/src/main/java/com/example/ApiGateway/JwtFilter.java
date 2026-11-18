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
    private static final String AUTHENTICATED_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        AuthenticatedRequest wrappedRequest = new AuthenticatedRequest(request);
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
                        Integer id= jwtService.getUserId(token);
                        if (email == null || role == null || id == null || email.isBlank() || role.isBlank()) {
                            request.setAttribute("error", "Required JWT claims are missing");
                            SecurityContextHolder.clearContext();
                        } else {
                            wrappedRequest.setAuthenticatedEmail(email);
                            wrappedRequest.setAuthenticatedUserId(id.toString());
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
            }
        }
        filterChain.doFilter(wrappedRequest, response);
    }

    private static final class AuthenticatedRequest extends HttpServletRequestWrapper {
        private String authenticatedEmail;
        private String authenticatedUserId;

        private AuthenticatedRequest(HttpServletRequest request) {
            super(request);
        }

        private void setAuthenticatedEmail(String authenticatedEmail) {
            this.authenticatedEmail = authenticatedEmail;
        }

        private void setAuthenticatedUserId(String authenticatedUserId) {
            this.authenticatedUserId = authenticatedUserId;
        }

        @Override
        public String getHeader(String name) {
            if (AUTHENTICATED_EMAIL_HEADER.equalsIgnoreCase(name)) {
                return authenticatedEmail;
            }
            if (AUTHENTICATED_USER_ID_HEADER.equalsIgnoreCase(name)) {
                return authenticatedUserId;
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
            if (AUTHENTICATED_USER_ID_HEADER.equalsIgnoreCase(name)) {
                return authenticatedUserId == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(authenticatedUserId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> headerNames = Collections.list(super.getHeaderNames());
            headerNames.removeIf(name -> AUTHENTICATED_EMAIL_HEADER.equalsIgnoreCase(name)
                    || AUTHENTICATED_USER_ID_HEADER.equalsIgnoreCase(name));
            if (authenticatedEmail != null) {
                headerNames.add(AUTHENTICATED_EMAIL_HEADER);
            }
            if (authenticatedUserId != null) {
                headerNames.add(AUTHENTICATED_USER_ID_HEADER);
            }
            return Collections.enumeration(headerNames);
        }
    }
}
