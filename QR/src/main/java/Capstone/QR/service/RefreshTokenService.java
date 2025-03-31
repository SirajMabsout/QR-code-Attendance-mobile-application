package Capstone.QR.service;

import Capstone.QR.model.RefreshToken;
import Capstone.QR.model.User;
import Capstone.QR.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // Create a refresh token for user
    public String createRefreshToken(User user) {
        // Remove old one if it exists
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        // Valid for 30 days
        long refreshTokenDurationMs = 1000L * 60 * 60 * 24 * 30;
        refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + refreshTokenDurationMs));

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    // Validate refresh token: check expiration
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().before(new Date())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }

    // Revoke refresh token (on logout)
    public void revokeRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Scheduled(fixedRate = 1000 * 60 * 60*24) // every day
    public void cleanUpExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredSinceNow();
        System.out.println("Expired refresh tokens cleaned up at " + new Date());
    }

}
