package com.example.flight.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.config.JwtProperties;
import com.example.flight.entity.RefreshToken;
import com.example.flight.entity.User;
import com.example.flight.exception.InvalidRefreshTokenException;
import com.example.flight.exception.RefreshTokenExpiredException;
import com.example.flight.exception.RefreshTokenReuseException;
import com.example.flight.repository.RefreshTokenRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Getter
    public static class RotationResult {
        private final String rawRefreshToken;
        private final RefreshToken refreshTokenEntity;
        private final User user;

        public RotationResult(String rawRefreshToken, RefreshToken refreshTokenEntity, User user) {
            this.rawRefreshToken = rawRefreshToken;
            this.refreshTokenEntity = refreshTokenEntity;
            this.user = user;
        }
    }

    @Transactional
    public RotationResult createRefreshToken(User user, String familyId, String deviceInfo, String ipAddress) {
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenHash = jwtService.hashToken(rawToken);

        if (familyId == null || familyId.isBlank()) {
            familyId = UUID.randomUUID().toString();
        }

        long expirationMs = jwtProperties.getRefresh().getExpiration();
        LocalDateTime expiryDate = LocalDateTime.now().plus(Duration.ofMillis(expirationMs));

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .familyId(familyId)
                .user(user)
                .expiryDate(expiryDate)
                .revoked(false)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        return new RotationResult(rawToken, saved, user);
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RotationResult result = createRefreshToken(user, null, null, null);
        return result.getRefreshTokenEntity();
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public RotationResult rotateRefreshToken(String rawRefreshToken, String deviceInfo, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing or empty.");
        }

        String incomingHash = jwtService.hashToken(rawRefreshToken);

        // Pessimistic Write Lock query to prevent concurrent refresh race conditions
        RefreshToken currentToken = refreshTokenRepository.findByTokenHashWithLock(incomingHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token."));

        // REUSE DETECTED: If token was already revoked
        if (currentToken.isRevoked()) {
            refreshTokenRepository.revokeAllByFamilyId(currentToken.getFamilyId());
            throw new RefreshTokenReuseException("Refresh token reuse detected. Revoking all sessions in family.");
        }

        // EXPIRATION CHECK
        if (currentToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            currentToken.setRevoked(true);
            refreshTokenRepository.save(currentToken);
            throw new RefreshTokenExpiredException("Refresh token has expired. Please log in again.");
        }

        // Revoke current token
        currentToken.setRevoked(true);
        refreshTokenRepository.save(currentToken);

        // Create new token in the same family
        return createRefreshToken(currentToken.getUser(), currentToken.getFamilyId(), deviceInfo, ipAddress);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String incomingHash = jwtService.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(incomingHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional
    public void logoutAllUserTokens(Long userId) {
        if (userId != null) {
            refreshTokenRepository.revokeAllByUserId(userId);
        }
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public RefreshToken verifyExpiration(String token) {
        String hash = jwtService.hashToken(token);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            refreshTokenRepository.revokeAllByFamilyId(refreshToken.getFamilyId());
            throw new RefreshTokenReuseException("Refresh token reuse detected. Revoking all sessions.");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public void deleteToken(String token) {
        if (token != null && !token.isBlank()) {
            String hash = jwtService.hashToken(token);
            refreshTokenRepository.deleteByTokenHash(hash);
        }
    }
}