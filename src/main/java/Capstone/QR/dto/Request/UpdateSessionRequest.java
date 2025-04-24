package Capstone.QR.dto.Request;


import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class UpdateSessionRequest {
    private Long sessionId;
    private Boolean canceled;           // true or false
    private LocalDate sessionDate;
    private LocalTime sessionTime;
    private String topic;
}
