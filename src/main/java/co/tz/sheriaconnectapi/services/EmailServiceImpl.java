package co.tz.sheriaconnectapi.services;

import co.tz.sheriaconnectapi.exceptions.EmailDeliveryException;
import co.tz.sheriaconnectapi.utils.EmailTemplateBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.Instant;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromName;
    private final String fromAddress;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${spring.mail.from.name:Sheria Connect}") String fromName,
            @Value("${spring.mail.from.address:noreply@sheriaconnect.co.tz}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromName = fromName;
        this.fromAddress = fromAddress;
    }

    // Central mail sending logic.
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            throw new EmailDeliveryException(e);
        }
    }

    @Override
    public void sendEmailVerification(
            String toEmail,
            String name,
            String verificationLink
    ) {

        String html = EmailTemplateBuilder.buildActionEmail(
                name,
                "Verify your email address",
                "Please confirm your email address to activate your account.",
                "Verify Email",
                verificationLink,
                "This link will expire in 24 hours."
        );

        sendHtmlEmail(toEmail, "Verify your email address", html);
    }

    @Override
    public void sendPasswordResetEmail(
            String toEmail,
            String name,
            String resetLink
    ) {

        String html = EmailTemplateBuilder.buildActionEmail(
                name,
                "Reset Your Password",
                "Click the button below to reset your password.",
                "Reset Password",
                resetLink,
                "This link will expire in 15 minutes."
        );

        sendHtmlEmail(toEmail, "Reset your password", html);
    }

    @Override
    public void sendStaffInvitation(
            String toEmail,
            String name,
            String invitationLink,
            Instant expiresAt
    ) {
        String html = EmailTemplateBuilder.buildActionEmail(
                name,
                "You have been invited to Sheria Connect",
                "A platform administrator invited you to join the Sheria Connect owner portal.",
                "Activate Staff Account",
                invitationLink,
                "This invitation expires in 48 hours."
        );
        sendHtmlEmail(toEmail, "Your Sheria Connect staff invitation", html);
    }
}
