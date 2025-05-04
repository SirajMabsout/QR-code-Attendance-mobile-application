package Capstone.QR.dto.Request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatQueryRequest {
    private String question;
    private String role;
    private String email;
}
