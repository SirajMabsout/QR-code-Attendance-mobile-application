package Capstone.QR.dto.Response;

import Capstone.QR.model.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceRequestResponse {
    private Long id;
    private Long studentId;
    private String StudentName;
    private String ProfileImageURL;
    private Long classId;
    private LocalDateTime requestedAt;
    private RequestStatus status;
}
