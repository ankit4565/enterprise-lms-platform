package com.enterprise.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendVerificationEmail(String toEmail, String otpCode) {
        log.info("Sending email verification OTP [{}] to {}", otpCode, toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@enterprise-lms.com");
            message.setTo(toEmail);
            message.setSubject("Verify Your Enterprise LMS Account");
            message.setText("Welcome to Enterprise LMS Platform!\n\n"
                    + "Your account verification OTP code is: " + otpCode + "\n\n"
                    + "This code will expire in 15 minutes. If you did not register, please ignore this email.");

            mailSender.send(message);
            log.info("Verification email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            // In dev / local environments when mail server is offline, the code is logged above
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String otpCode) {
        log.info("Sending password reset OTP [{}] to {}", otpCode, toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@enterprise-lms.com");
            message.setTo(toEmail);
            message.setSubject("Reset Your Enterprise LMS Password");
            message.setText("You requested to reset your Enterprise LMS password.\n\n"
                    + "Your password reset OTP code is: " + otpCode + "\n\n"
                    + "This code will expire in 15 minutes. If you did not request this, please secure your account immediately.");

            mailSender.send(message);
            log.info("Password reset email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
