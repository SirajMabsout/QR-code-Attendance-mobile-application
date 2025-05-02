package Capstone.QR.service;

import Capstone.QR.model.Attendance;
import Capstone.QR.model.AttendanceStatus;
import Capstone.QR.model.ClassSession;
import Capstone.QR.repository.AttendanceRepository;
import Capstone.QR.repository.AttendanceRequestRepository;
import Capstone.QR.repository.ClassSessionRepository;
import Capstone.QR.repository.KlassStudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceFinalizationService {

    private final ClassSessionRepository classSessionRepository;
    private final KlassStudentRepository klassStudentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceRequestRepository attendanceRequestRepository;

    @Scheduled(fixedRate = 5 * 60 * 1000) // Every 5 minutes
    @Transactional
    public void updatePendingToAbsentIfNoRequest() {
        LocalDateTime now = LocalDateTime.now();

        List<ClassSession> endedSessions = classSessionRepository.findAll().stream()
                .filter(session -> {
                    LocalDateTime sessionEnd = session.getSessionDate().atTime(session.getSessionTime())
                            .plusMinutes(session.getKlass().getDurationMinutes());
                    return sessionEnd.isBefore(now);
                })
                .toList();

        for (ClassSession session : endedSessions) {
            Long sessionId = session.getId();

            // 1. Get all PENDING attendance entries
            List<Attendance> pendingAttendances = attendanceRepository
                    .findBySession_IdAndStatus(sessionId, AttendanceStatus.PENDING);

            for (Attendance attendance : pendingAttendances) {
                Long studentId = attendance.getStudent().getId();

                boolean hasRequest = attendanceRequestRepository
                        .existsBySession_IdAndStudent_Id(sessionId, studentId);

                if (!hasRequest) {
                    attendance.setStatus(AttendanceStatus.ABSENT);
                    attendance.setRecordedAt(now);
                    attendanceRepository.save(attendance);
                }
            }
        }
    }
}
