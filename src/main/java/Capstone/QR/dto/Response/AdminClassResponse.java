package Capstone.QR.dto.Response;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminClassResponse {
    private Long id;
    private String name;
    private String teacherEmail;
    private boolean isFinished;
}
