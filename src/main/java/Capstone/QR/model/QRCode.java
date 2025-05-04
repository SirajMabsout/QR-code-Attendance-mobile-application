package Capstone.QR.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private ClassSession session;
    private LocalDateTime sessionDate;

    @Lob
    private String qrCodeData;
    private LocalDateTime expiresAt;

    private double latitude;
    private double longitude;
}
