package Capstone.QR.service;

import Capstone.QR.model.*;
import Capstone.QR.repository.AttendanceRepository;
import Capstone.QR.repository.ClassSessionRepository;
import Capstone.QR.repository.KlassStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class AttendanceScheduler {

    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final KlassStudentRepository klassStudentRepository;

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void markAbsentForExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();

        List<ClassSession> expiredSessions = classSessionRepository.findAll().stream()
                .filter(session -> !session.isCanceled())
                .filter(session -> {
                    LocalDateTime sessionStart = session.getSessionDate().atTime(session.getSessionTime());
                    LocalDateTime sessionEnd = sessionStart.plusMinutes(session.getKlass().getDurationMinutes());
                    return LocalDateTime.now().isAfter(sessionEnd);
                })
                .toList();


        for (ClassSession session : expiredSessions) {
            List<Long> presentStudentIds = attendanceRepository.findBySession_Id(session.getId()).stream()
                    .map(attendance -> attendance.getStudent().getId())
                    .toList();

            List<Student> allApprovedStudents = klassStudentRepository
                    .findAllByKlassIdAndApprovedTrue(session.getKlass().getId())
                    .stream()
                    .map(KlassStudent::getStudent)
                    .toList();

            for (Student student : allApprovedStudents) {
                if (!presentStudentIds.contains(student.getId())) {
                    boolean alreadyMarked = attendanceRepository
                            .findBySession_IdAndStudent_Id(session.getId(), student.getId())
                            .stream()
                            .anyMatch(a -> a.getStatus() == AttendanceStatus.ABSENT);

                    if (!alreadyMarked) {
                        Attendance absentRecord = new Attendance();
                        absentRecord.setSession(session);
                        absentRecord.setStudent(student);
                        absentRecord.setRecordedAt(now);
                        absentRecord.setStatus(AttendanceStatus.ABSENT);
                        attendanceRepository.save(absentRecord);
                    }
                }
            }
        }
    }
}
