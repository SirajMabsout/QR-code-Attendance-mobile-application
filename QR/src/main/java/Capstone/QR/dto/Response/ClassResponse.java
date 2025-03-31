package Capstone.QR.dto.Response;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class ClassResponse {
    private Long id;
    private String name;
    private String description;
    private LocalTime ClassTime;
    private int maxAbsencesAllowed;
}
