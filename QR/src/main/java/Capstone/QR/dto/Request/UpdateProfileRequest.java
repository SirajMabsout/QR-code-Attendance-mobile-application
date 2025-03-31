package Capstone.QR.dto.Request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Name too long")
    private String name;

    @Size(max = 255, message = "Image URL too long")
    private String profileImage;
}
