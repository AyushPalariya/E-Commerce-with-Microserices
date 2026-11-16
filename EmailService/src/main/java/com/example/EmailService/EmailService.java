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
import java.net.InetAddress;
import jakarta.annotation.PostConstruct;
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
    public void sendOrderPlaced(OrderPaymentEmail orderPlace) throws MessagingException {
        MimeMessage mimeMessage=javaMailSender.createMimeMessage();
        MimeMessageHelper helper=new MimeMessageHelper(mimeMessage,true);
        helper.setTo(orderPlace.getOrder().getUserEmail());
        helper.setSubject("Order placed Successfully");
        StringBuilder orderDetails = new StringBuilder();

        orderDetails.append("Hello ").append(orderPlace.getOrder().getName())
                .append(",\n")
                .append("Thank you for placing your order!\n")
                .append("Your OrderId :").append(orderPlace.getOrderId()).append("\n")
                .append("Your PaymentId :").append(orderPlace.getPaymentId()).append("\n")
                .append("Your Order Details:\n\n");
        List<ItemDto> list=orderPlace.getOrder().getItems();
        for (int i = 0; i < list.size(); i++) {
            ItemDto item = list.get(i);
            orderDetails.append(i + 1).append(". ")
                    .append(item.getProduct_name()).append("\n")
                    .append("   Quantity: ").append(item.getQuantities()).append("\n")
                    .append("   Amount: ₹").append(item.getSubAmount()).append("\n\n");
        }
        orderDetails.append("Total Amount: ")
                .append(orderPlace.getOrder().getTotalAmount())
                .append("\n\n")
                .append("Thank you for shopping with us!");
        log.info("OrderDetails:{}",orderDetails);
        helper.setText(orderDetails.toString(),false);
        //also generate pdf and send to mail_id
        javaMailSender.send(mimeMessage);
    }
    @PostConstruct
    public void checkMailConnection() {

        try {
            InetAddress[] addresses =
                    InetAddress.getAllByName("smtp.gmail.com");

            for (InetAddress address : addresses) {
                log.info("SMTP IP: {}", address.getHostAddress());
            }

        } catch (Exception e) {
            log.error("DNS resolution failed", e);
        }
    }
    @KafkaListener(topics = "${kafka.topic.getOtp}", groupId = "email-verify-group")
    public void sendOtp(OtpVerify otpverify) throws MessagingException {
        MimeMessage message=javaMailSender.createMimeMessage();
        MimeMessageHelper helper=new MimeMessageHelper(message,true);
        helper.setTo(otpverify.getEmail());
        helper.setSubject("Otp for Verify the mail");
        helper.setText("OTP : "+otpverify.getOtp()+"\n"+"otp expire in 5 min");
        javaMailSender.send(message);
        log.info("Send Otp for verification the mail");
    }
}
