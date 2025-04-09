package Capstone.QR.dto.Request;


import lombok.Data;

@Data
public class GenerateQrRequest {
    private double latitude;
    private double longitude;
}
