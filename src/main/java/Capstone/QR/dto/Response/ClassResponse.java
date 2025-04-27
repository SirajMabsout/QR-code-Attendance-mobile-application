package Capstone.QR.dto.Response;

import Capstone.QR.model.ClassSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ClassResponse {
    private Long id;
    private String name;
    private String description;
    private LocalTime classTime;
    private int maxAbsencesAllowed;
    private String joinCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private int durationMinutes;
    private List<DayOfWeek> scheduledDays;
    private double acceptanceRadiusMeters;





}
