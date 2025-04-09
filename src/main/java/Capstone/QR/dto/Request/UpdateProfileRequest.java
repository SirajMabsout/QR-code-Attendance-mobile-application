package Capstone.QR.dto.Request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Name too long")
    private String name;

    private MultipartFile image;
}
