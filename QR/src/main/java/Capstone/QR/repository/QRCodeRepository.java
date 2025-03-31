package Capstone.QR.repository;

import Capstone.QR.model.QRCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// === QRCodeRepository ===
@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, Long> {
    Optional<QRCode> findByQrCodeData(String qrCodeData);
}
