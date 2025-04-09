package Capstone.QR.dto.Response;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
public class PendingJoinRequestResponse {
    private Long classId;
    private String className;
    private String teacherName;
    private LocalTime classTime;
    private List<String> scheduledDays;
    private LocalDate requestedAt;
}
