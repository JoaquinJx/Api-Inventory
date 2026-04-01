package com.joaquin.inventory.service;

import com.joaquin.inventory.entity.RefreshToken;
import com.joaquin.inventory.entity.User;
import com.joaquin.inventory.exception.InvalidCredentialsException;
import com.joaquin.inventory.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:1209600000}")
    private long refreshExpirationMs;

    @Transactional
    public String issue(User user) {
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(rawToken)
                .user(user)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public User consumeValidToken(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(InvalidCredentialsException::new);

        if (token.isRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException();
        }

        token.setRevoked(true);
        return token.getUser();
    }

    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.findAllByUserAndRevokedIsFalse(user)
                .forEach(token -> token.setRevoked(true));
    }
}