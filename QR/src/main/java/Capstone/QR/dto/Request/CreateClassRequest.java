package Capstone.QR.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassRequest {

    @NotBlank(message = "Class name is required")
    private String name;

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    @NotEmpty(message = "At least one student must be assigned")
    private List<Long> studentIds;

    private String description;

    @Min(value = 0, message = "Max absences allowed cannot be negative")
    private int maxAbsencesAllowed;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotEmpty(message = "Scheduled days cannot be empty")
    private List<DayOfWeek> scheduledDays;

    @NotNull(message = "Class time is required")
    private LocalTime classTime;
}
