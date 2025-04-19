package Capstone.QR.dto.Request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassRequest {
    @NotBlank(message = "Class name is required")
    private String name;

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

    @Min(value = 1, message = "Acceptance radius must be at least 1 meter")
    private double acceptanceRadiusMeters;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int durationMinutes;

    private List<@NotBlank(message = "SSID cannot be blank") String> allowedWifiSSIDs;
}
