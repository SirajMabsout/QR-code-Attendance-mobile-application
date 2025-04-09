package Capstone.QR.dto.Response;

import Capstone.QR.model.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class StudentAttendanceRequestResponse  {
    private Long requestId;
    private Long classId;
    private String className;
    private LocalDate sessionDate;
    private LocalTime sessionTime;
    private RequestStatus status;
    private LocalDate requestedAt;
}
