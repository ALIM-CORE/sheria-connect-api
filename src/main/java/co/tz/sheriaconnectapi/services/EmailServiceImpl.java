package co.tz.sheriaconnectapi.services;

import co.tz.sheriaconnectapi.utils.EmailTemplateBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // 🔹 Central mail sending logic
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Sheria Connect <noreply@sheriaconnect.co.tz>");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
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
}
