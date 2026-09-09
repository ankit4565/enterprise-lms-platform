package com.enterprise.authservice.service;

import com.enterprise.authservice.entity.Otp;
import com.enterprise.authservice.entity.OtpPurpose;
import com.enterprise.authservice.entity.User;
import com.enterprise.authservice.repository.OtpRepository;
import com.enterprise.authservice.security.JwtService;
import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.expiration-minutes:15}")
    private int otpExpirationMinutes;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public String generateAndSaveOtp(User user, String email, OtpPurpose purpose) {
        String plainOtp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String codeHash = JwtService.hashToken(plainOtp);

        Otp otp = Otp.builder()
                .userId(user != null ? user.getId() : null)
                .email(email.toLowerCase().trim())
                .purpose(purpose)
                .codeHash(codeHash)
                .attempts(0)
                .maxAttempts(maxAttempts)
                .expiresAt(Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES))
                .build();

        otpRepository.save(otp);
        return plainOtp;
    }

    @Transactional
    public Otp verifyOtp(String email, OtpPurpose purpose, String plainOtp) {
        String normalizedEmail = email.toLowerCase().trim();
        Otp otp = otpRepository.findLatestActiveOtp(normalizedEmail, purpose)
                .orElseThrow(() -> new ApiException(ErrorCode.OTP_INVALID, "No valid OTP request found for this email"));

        if (otp.isExpired()) {
            throw new ApiException(ErrorCode.OTP_EXPIRED, "OTP has expired. Please request a new one");
        }

        if (otp.getAttempts() >= otp.getMaxAttempts()) {
            throw new ApiException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED, "Maximum OTP verification attempts exceeded");
        }

        otp.setAttempts(otp.getAttempts() + 1);

        String inputHash = JwtService.hashToken(plainOtp);
        if (!otp.getCodeHash().equals(inputHash)) {
            otpRepository.save(otp);
            throw new ApiException(ErrorCode.OTP_INVALID, "Invalid OTP code");
        }

        otp.setConsumedAt(Instant.now());
        return otpRepository.save(otp);
    }
}
