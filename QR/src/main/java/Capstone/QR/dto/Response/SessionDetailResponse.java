package Capstone.QR.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionDetailResponse {
    private Long id;
    private LocalDate sessionDate;
    private LocalTime sessionTime;
    private boolean canceled;
    private String topic;
    private List<AttendanceResponse> attendance;
}
