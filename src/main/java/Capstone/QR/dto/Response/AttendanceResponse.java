package Capstone.QR.dto.Response;

import Capstone.QR.model.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceResponse {
    private Long id;
    private Long classId;
    private String profileImageUrl;
    private Long studentId;
    private long sessionId;
    private String studentname;
    private LocalDateTime recordedAt;
    private AttendanceStatus status;
    private boolean MaxAbsence;
}
