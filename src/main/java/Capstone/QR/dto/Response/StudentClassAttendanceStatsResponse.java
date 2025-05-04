package Capstone.QR.dto.Response;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentClassAttendanceStatsResponse {
    private Long studentId;
    private String studentName;
    private Long classId;
    private String className;

    private int totalSessions;
    private int presentCount;
    private int excusedCount;
    private int absentCount;
    private double attendancePercentage;

    private int maxAllowedAbsences;
    private int remainingAbsences;
}

