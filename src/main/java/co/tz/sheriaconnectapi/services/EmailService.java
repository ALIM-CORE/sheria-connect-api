package co.tz.sheriaconnectapi.services;

import java.time.Instant;

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

    void sendStaffInvitation(
            String toEmail,
            String name,
            String invitationLink,
            Instant expiresAt
    );
}
