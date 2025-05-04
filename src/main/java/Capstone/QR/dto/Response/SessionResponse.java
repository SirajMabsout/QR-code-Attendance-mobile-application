package Capstone.QR.dto.Response;

import Capstone.QR.model.ClassSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionResponse {
    private Long id;
    private LocalDate sessionDate;
    private LocalTime sessionTime;
    private String topic;
    private boolean canceled;

    private boolean isToday;
    private boolean isUpcoming;
    private boolean isPast;

    public SessionResponse(ClassSession session) {
        this.id = session.getId();
        this.sessionDate = session.getSessionDate();
        this.sessionTime = session.getSessionTime();
        this.topic = session.getTopic();
        this.canceled = session.isCanceled();

        LocalDate today = LocalDate.now();
        this.isToday = session.getSessionDate().isEqual(today);
        this.isUpcoming = session.getSessionDate().isAfter(today);
        this.isPast = session.getSessionDate().isBefore(today);
    }

}
