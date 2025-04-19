package Capstone.QR.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String message;
    private String role;
    private String UserName;
    private Long UserId;
}
