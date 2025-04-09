package Capstone.QR.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QRCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "session_id")
    private ClassSession session; // ✅ Link directly to session instead of Klass

    private LocalDateTime sessionDate;

    @Lob
    private String qrCodeData; // Base64 encoded QR code image

    private LocalDateTime expiresAt;

    private double latitude;
    private double longitude;
}
