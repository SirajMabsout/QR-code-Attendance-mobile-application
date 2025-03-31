package Capstone.QR.dto.Response;

import Capstone.QR.model.AttendanceStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AttendanceResponse {
    private Long id;
    private Long studentId;
    private Long classId;
    private LocalDateTime date;
    private AttendanceStatus status;
}
