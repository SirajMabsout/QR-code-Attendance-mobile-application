package Capstone.QR.repository;

import Capstone.QR.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByToken(String token);
    Optional<EmailVerificationToken> findByEmail(String email);

}
