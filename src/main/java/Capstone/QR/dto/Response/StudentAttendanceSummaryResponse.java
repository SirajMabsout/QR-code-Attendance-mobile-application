package Capstone.QR.dto.Response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceSummaryResponse {

    private List<SessionStatus> sessionStatuses;
    private int MaxAllowedAbsences;
    private int totalSessions;
    private int totalPresent;
    private int totalExcused;
    private int totalAbsent;
    private int remainingAllowedAbsences;
    private double attendancePercentage;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionStatus {
        private LocalDate date;
        private String status;
    }
}
