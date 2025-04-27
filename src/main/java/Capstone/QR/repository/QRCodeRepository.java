package Capstone.QR.repository;

import Capstone.QR.model.QRCode;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// === QRCodeRepository ===
@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, Long> {
    Optional<QRCode> findByQrCodeData(String qrCodeData);
    Optional<QRCode> findBySessionId(Long sessionId);



    @Transactional
    @Modifying
    @Query("DELETE FROM QRCode q WHERE q.session.id IN :sessionIds")
    void deleteBySessionIds(@Param("sessionIds") List<Long> sessionIds);
}
