package Capstone.QR.service;

import Capstone.QR.model.RefreshToken;
import Capstone.QR.model.User;
import Capstone.QR.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
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
    @Transactional
    public String createRefreshToken(User user) {
        try {
            System.out.println("Removing old token...");
            refreshTokenRepository.deleteByUser(user);

            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setUser(user);
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30));

            refreshTokenRepository.save(refreshToken);
            System.out.println("Refresh token created successfully.");
            return refreshToken.getToken();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create refresh token: " + e.getMessage());
        }
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
