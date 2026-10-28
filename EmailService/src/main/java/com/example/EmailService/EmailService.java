package com.example.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {
    private final JavaMailSender javaMailSender;

    @KafkaListener(topics = "${kafka.topic.name}", groupId = "profile-group")
    public void sendRegisterMail(RegistrationEmailEvent profileRequest) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("Registration");
        message.setTo(profileRequest.getEmail());
        message.setText(
                "Hello " + profileRequest.getName() + ",\n\n"
                        + "Your registration was successful.\n\n"
                        + "Email: " + profileRequest.getEmail() + "\n"
                        + "Phone: " + profileRequest.getPhoneNo() + "\n"
                        + "Role: " + profileRequest.getRole() + "\n\n"
                        + "Thank you for registering."
        );
        javaMailSender.send(message);
        log.info("Registration email sent for userId={}", profileRequest.getId());
    }

    @KafkaListener(
            topics = "${kafka.topic.reset}",
            groupId = "forgot-password-group"
    )
    public void sendPasswordResetMail(ForgotPassword resetRequest) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper=new MimeMessageHelper(mimeMessage,true);
        helper.setTo(resetRequest.getEmail());
        helper.setSubject("Password reset request");
        helper.setText(
                "Hello " + resetRequest.getName() + ",\n\n"
                        + "Use the OTP below to reset your password. OTP expires in 5 minutes "
                        + "and can only be used once:\n\n"
                        +"OTP : "
                        + resetRequest.getOtp() + "\n\n"
                        + "If you did not request a password reset, you can ignore this email."
        );
        javaMailSender.send(mimeMessage);
    }
}
