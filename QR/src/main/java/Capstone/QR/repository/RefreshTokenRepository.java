
package Capstone.QR.repository;

import Capstone.QR.model.RefreshToken;
import Capstone.QR.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken t WHERE t.expiryDate < CURRENT_TIMESTAMP")
    void deleteAllExpiredSinceNow();

}
