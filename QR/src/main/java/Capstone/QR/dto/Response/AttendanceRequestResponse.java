package Capstone.QR.dto.Response;

import Capstone.QR.model.RequestStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AttendanceRequestResponse {
    private Long id;
    private Long studentId;
    private Long classId;
    private LocalDateTime requestedAt;
    private RequestStatus status;
}
