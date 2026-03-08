package co.tz.sheriaconnectapi.services;

public interface EmailService {

    void sendEmailVerification(
            String toEmail,
            String name,
            String verificationLink
    );

    void sendPasswordResetEmail(
            String toEmail,
            String name,
            String resetLink
    );
}
