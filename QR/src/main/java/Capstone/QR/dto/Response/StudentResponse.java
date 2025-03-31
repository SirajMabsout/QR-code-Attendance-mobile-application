package Capstone.QR.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentResponse {
    private Long id;
    private String name;
    private String email;
}

