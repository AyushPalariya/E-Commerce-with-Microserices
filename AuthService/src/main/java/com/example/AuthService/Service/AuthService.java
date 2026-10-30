package com.example.AuthService.Service;

import com.example.AuthService.DTO.*;
import com.example.AuthService.Entities.AuthUserEntity;
import com.example.AuthService.Entities.Role;
import com.example.AuthService.GlobalException.AuthenticationExceptionClass;
import com.example.AuthService.GlobalException.BadRequestExceptionClass;
import com.example.AuthService.GlobalException.ConflictException;
import com.example.AuthService.GlobalException.NotFoundClass;
import com.example.AuthService.JwtUtils.JwtService;
import com.example.AuthService.Repository.AuthRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthService {
    private final CacheService cacheService;
    private final AuthRepo authRepo;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RestClient restclient;
    @Autowired
    private KafkaTemplate<String, Object> template;
    @Value("${kafka.topic.name}")
    private String topicName;
    @Value("${kafka.topic.reset}")
    private String resetTopic;

    @Transactional
    public UserRegistration register(UserRegistration userRegistration) {
        if (userRegistration.getEmail() == null || userRegistration.getEmail().isBlank())
            throw new BadRequestExceptionClass("Please enter mail id!");
        if (userRegistration.getName() == null || userRegistration.getName().isBlank())
            throw new BadRequestExceptionClass("Please enter name!");
        if (userRegistration.getPhone_no() == null || userRegistration.getPhone_no().isBlank())
            throw new BadRequestExceptionClass("Please enter phone_no!");
        if (authRepo.existsByEmail(userRegistration.getEmail())) throw new ConflictException("Email already exist.");
        AuthUserEntity u = new AuthUserEntity();
        u.setEmail(userRegistration.getEmail());
        u.setName(userRegistration.getName());
        u.setPhone_no(userRegistration.getPhone_no());
        u.setEnable(true);
        u.setPassword(passwordEncoder.encode(userRegistration.getPassword()));
        u.setRole(Role.ROLE_USER);
        u = authRepo.save(u);
        //restClient
        String s = sendData(u);
        if (s == null) throw new BadRequestExceptionClass("Profile is not created for user!");
        //
        try {
            var result = template.send(
                    topicName,
                    String.valueOf(u.getId()),
                    new RegistrationEmailEvent(
                            u.getId(),
                            u.getName(),
                            u.getEmail(),
                            u.getPhone_no(),
                            u.getRole().toString()
                    )
            ).get();
            log.info("Registration email event published for userId={} at offset={}",
                    u.getId(), result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing registration email event", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to publish registration email event", e.getCause());
        }
        return new UserRegistration(u.getId(), u.getName(), u.getPassword(), u.getEmail(), u.getPhone_no(), u.getEnable(), u.getRole(), u.getCreatedAt(), u.getUpdatedAt());
    }

    private String sendData(AuthUserEntity user) {
        UserDataSend send = new UserDataSend(user.getId(), user.getName(), user.getEmail(), user.getPhone_no());
        System.out.println(send);
        String json = new ObjectMapper().writeValueAsString(send);
        System.out.println("ObjectMapper use\n" + json);
        return restclient.post()
                .uri("http://localhost:8083/user/profile")
                .body(send)
                .retrieve()
                .body(String.class);
    }

    public String refreshTokenJti() {
        return UUID.randomUUID().toString();
    }

    public LoginResponse login(UserLoginRequest userLogin, HttpServletResponse response) {
        if (userLogin.getEmail() == null || userLogin.getEmail().isBlank())
            throw new BadRequestExceptionClass("Please enter mail id!");
        if (userLogin.getPassword() == null || userLogin.getPassword().isBlank())
            throw new BadRequestExceptionClass("Please enter password!");
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userLogin.getEmail(), userLogin.getPassword()));
        System.out.println("authentication successful");
        UserDetails details = (UserDetails) auth.getPrincipal();
        String email=details.getUsername();
        AuthUserEntity data = cacheService.getData(email);
        String accessToken = jwtService.generateToken(details,data.getId());
        String jti = refreshTokenJti();
        String refreshToken = jwtService.refreshToken(details, jti,data.getId());

        cacheService.putJit(email, jti);
        cacheService.putUser(details);

        cookieService.attachToken(refreshToken, jwtService.getRftAccessibleTime(), response);
        String role = jwtService.getRole(details);
        return new LoginResponse(data.getId(),data.getName() ,accessToken, refreshToken, details.getUsername(), Role.valueOf(role));
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        String token = readToken(refreshToken, request);
        if (token == null) throw new AuthenticationExceptionClass("Refresh token missing!");
        if (!jwtService.isRefreshToken(token)) {
            throw new AuthenticationExceptionClass("Invalid refresh token type!");
        }
        String email = jwtService.getUserEmail(token);
        if (email == null || email.isBlank()) throw new AuthenticationExceptionClass("Email missing!");
        System.out.println("token email: " + email);

        UserDetails user = cacheService.getUser(email);
        if (user == null) throw new AuthenticationExceptionClass("user not present in cache!");
        System.out.println("usercahce: " + user.getUsername());

        if (!email.equals(user.getUsername())) throw new AuthenticationExceptionClass("Email mismatched!");
        String cacheJti = cacheService.getJti(email);
        if (cacheJti == null) throw new AuthenticationExceptionClass("cache Jti is missing");
        System.out.println("cachejti: " + cacheJti);
        if (!cacheJti.equals(jwtService.getJti(token))) throw new AuthenticationExceptionClass("Jti mismatched!");
        AuthUserEntity data = cacheService.getData(email);
        String accessToken = jwtService.generateToken(user, data.getId());
        String jti = refreshTokenJti();
        String refreshToken1 = jwtService.refreshToken(user, jti, data.getId());
        cacheService.putJit(email, jti);
        cacheService.putUser(user);
        cookieService.attachToken(refreshToken1, jwtService.getRftAccessibleTime(), response);
        String role = jwtService.getRole(user);
        return new LoginResponse(data.getId(),data.getName(),accessToken, refreshToken1, user.getUsername(), Role.valueOf(role));
    }

    private String readToken(String refreshToken, HttpServletRequest request) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            return refreshToken;
        }
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies()).filter(c -> c.getName().equals(cookieService.getRefreshTokenName()))
                    .map(Cookie::getValue).filter(v -> !v.isBlank()).findFirst().orElse(null);

        }
        return null;
    }

    @Transactional
    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String s = readToken(null, req);
        if (s == null) throw new AuthenticationExceptionClass("refresh token missing from logout!");
        String email = jwtService.getUserEmail(s);
        cacheService.removeJti(email);
        cacheService.removeUser(email);
        cookieService.clear(res);
        cookieService.clearHeader(res);
        SecurityContextHolder.clearContext();
        System.out.println("logout success for user :" + email);
    }

    @Transactional
    public String delete(String email, HttpServletResponse res) {
        authRepo.deleteByEmail(email);
        cacheService.removeJti(email);
        cacheService.removeUser(email);
        cookieService.clear(res);
        cookieService.clearHeader(res);
        return "User deleted form authService with email_id:" + email;
    }
    @Transactional
    public Map<String, String> forgot(Map<String, String> mp) {
        //"email":"..."
        if (mp == null) throw new BadRequestExceptionClass("mp not be null!");
        String email = mp.get("email");
        if (email == null || email.isBlank()) throw new BadRequestExceptionClass("Email is required!");
        AuthUserEntity authUserEntity = authRepo.findByEmail(email).orElseThrow(() -> new NotFoundClass("email not found!"));
        ForgotPassword forget = new ForgotPassword();
        forget.setEmail(email);
        forget.setName(authUserEntity.getName());
        String otp=cacheService.generateOtp(email);
        forget.setOtp(otp);

        try {
            template.send(resetTopic, email, forget);
            System.out.println("send email to emailService for reset password");
        } catch (Exception e) {
            System.out.println("email service not work for forgotPassword!");
            System.out.println(e.getMessage());
        }

        return Map.of("email", email,"datetime",String.valueOf(LocalDateTime.now().plusMinutes(5)));
    }
    @Transactional
    public Map<String, String> resetPassword(PasswordResetRequest request) {
        String email = request.getEmail();
        if (email == null || email.isBlank()) throw new BadRequestExceptionClass("Email invalid!");
        if (request.getNewPassword() == null || request.getNewPassword().isBlank())
            throw new BadRequestExceptionClass("Password required!");
        if (request.getConfirmPassword() == null || request.getConfirmPassword().isBlank())
            throw new BadRequestExceptionClass("Confirm password required!");
        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new RuntimeException("Password not matches with Confirm password!");
        cacheService.removeJti(email);
        cacheService.removeUser(email);
        int v=authRepo.savePasswordByEmail(email,passwordEncoder.encode(request.getConfirmPassword()),LocalDateTime.now());
        if(v==0) throw new NotFoundClass("Password not update! "+email);
        return Map.of("SUCCESS","Password changed");
    }

    public Map<String, String> validateOtp(OtpToken otpToken) {
        LocalDateTime afterTime = LocalDateTime.parse(otpToken.getDatetime());
        if(LocalDateTime.now().isAfter(afterTime)) {
            cacheService.removeOtp(otpToken.getEmail());
            throw new AuthenticationExceptionClass("Otp expired!");
        }
        String otp = cacheService.getOtp(otpToken.getEmail());
        if(otp==null||!otp.equals(otpToken.getOtp())) {
            throw new AuthenticationExceptionClass("Otp incorrect!");
        }
        cacheService.removeOtp(otpToken.getEmail());
        return Map.of("SUCCESS","OTP Correct");
    }


}
