package Capstone.QR.dto.Response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class QRCodeResponse {
    private Long id;
    private Long classId;
    private String qrCodeData;
    private LocalDateTime sessionDate;
    private LocalDateTime expiresAt;
    private double latitude;
    private double longitude;
}
