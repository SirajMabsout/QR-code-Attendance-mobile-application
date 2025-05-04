package Capstone.QR.dto.Response;

import lombok.Data;

import java.time.LocalDateTime;

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
