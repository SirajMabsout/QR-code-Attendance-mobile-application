package Capstone.QR.dto.Response;


import lombok.Data;

@Data
public class StudentClassAttendanceSummaryResponse {
    private Long studentId;
    private String studentName;
    private int totalSessions;
    private int presentCount;
    private int excusedCount;
    private int absentCount;
    private int maxAllowedAbsences;
    private int remainingAbsences;

    // Getters and setters
}

