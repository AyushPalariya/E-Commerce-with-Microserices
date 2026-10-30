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

import java.util.List;

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

    @KafkaListener(topics="${kafka.topic.order}",groupId = "order-place-group")
    public void sendOrderPlaced(OrderPlace orderPlace) throws MessagingException {
        MimeMessage mimeMessage=javaMailSender.createMimeMessage();
        MimeMessageHelper helper=new MimeMessageHelper(mimeMessage,true);
        helper.setTo(orderPlace.getUserEmail());
        helper.setSubject("Order placed Successfully");
        StringBuilder orderDetails = new StringBuilder();

        orderDetails.append("Hello ").append(orderPlace.getName())
                .append(",\n\n")
                .append("Thank you for placing your order!\n\n")
                .append("Your Order Details:\n\n");
        List<ItemDto> list=orderPlace.getItems();
        orderDetails.append("   " + "Name" + "\t" + "\tQuantities" + "\t" + "amount" + "\n");
        for (int i = 0; i < list.size(); i++) {

            orderDetails .append(i + 1).append(". ").append(list.get(i).getProduct_name())
                    .append("\t").append("  ").append(list.get(i).getQuantities())
                    .append("\t").append("  ").append(list.get(i).getSubAmount()).append("\n");
        }

        orderDetails.append("Total Amount: ")
                .append(orderPlace.getTotalAmount())
                .append("\n\n")
                .append("Thank you for shopping with us!");
        System.out.println(orderDetails);
        helper.setText(orderDetails.toString());
        //also generate pdf and send to mail_id
        javaMailSender.send(mimeMessage);
    }
}
