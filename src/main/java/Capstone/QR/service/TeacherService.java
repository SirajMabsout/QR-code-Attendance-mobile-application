package Capstone.QR.service;

import Capstone.QR.dto.Request.CreateClassRequest;
import Capstone.QR.dto.Request.UpdateSessionRequest;
import Capstone.QR.dto.Response.*;
import Capstone.QR.model.*;
import Capstone.QR.repository.*;
import Capstone.QR.utils.CodeGeneratorUtil;
import Capstone.QR.utils.GenerateSessions;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static Capstone.QR.utils.GenerateQR.generateQrCodeImage;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final KlassRepository klassRepository;
    private final AttendanceRepository attendanceRepository;
    private final QRCodeRepository qrCodeRepository;
    private final AttendanceRequestRepository attendanceRequestRepository;
    private final KlassStudentRepository klassStudentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentRepository studentRepository;


    public ClassResponse createClass(CreateClassRequest request, UserDetails userDetails) {
        Teacher teacher = teacherRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (klassRepository.existsByTeacherAndName(teacher, request.getName())) {
            throw new RuntimeException("You already have a class with this name.");
        }

        List<Klass> existingClasses = klassRepository.findByTeacher(teacher);
        for (Klass existing : existingClasses) {
            boolean dateOverlap = !request.getEndDate().isBefore(existing.getStartDate()) &&
                    !request.getStartDate().isAfter(existing.getEndDate());
            if (!dateOverlap) continue;

            boolean dayConflict = existing.getScheduledDays().stream()
                    .anyMatch(request.getScheduledDays()::contains);

            LocalTime newStart = request.getClassTime();
            LocalTime newEnd = newStart.plusMinutes(request.getDurationMinutes());
            LocalTime existingStart = existing.getClassTime();
            LocalTime existingEnd = existingStart.plusMinutes(existing.getDurationMinutes());

            boolean timeConflict = newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);

            if (dayConflict && timeConflict) {
                throw new RuntimeException("Class schedule conflicts with an existing class: " + existing.getName());
            }
        }

        Klass klass = new Klass();
        klass.setName(request.getName());
        klass.setDescription(request.getDescription());
        klass.setTeacher(teacher);
        klass.setMaxAbsencesAllowed(request.getMaxAbsencesAllowed());
        klass.setStartDate(request.getStartDate());
        klass.setEndDate(request.getEndDate());
        klass.setScheduledDays(request.getScheduledDays());
        klass.setClassTime(request.getClassTime());
        klass.setJoinCode(CodeGeneratorUtil.generateJoinCode());
        klass.setAcceptanceRadiusMeters(request.getAcceptanceRadiusMeters());
        klass.setDurationMinutes(request.getDurationMinutes());
        klass.setAllowedWifiSSIDs(request.getAllowedWifiSSIDs());

        List<ClassSession> sessions = GenerateSessions.generateSessionsForClass(klass);
        klass.setSessions(sessions);

        klassRepository.save(klass);
        classSessionRepository.saveAll(sessions);

        return mapToClassResponse(klass);
    }

    public List<ClassResponse> getAllClasses(UserDetails userDetails) {
        Teacher teacher = teacherRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        return klassRepository.findByTeacher(teacher)
                .stream()
                .map(this::mapToClassResponse)
                .collect(Collectors.toList());
    }

    public ClassDetailResponse getClassDetails(Long classId, UserDetails userDetails) {
        Klass klass = validateTeacherOwnsClass(classId, userDetails);

        List<StudentResponse> enrolledStudents = klassStudentRepository
                .findAllByKlassIdAndApprovedTrue(classId)
                .stream()
                .map(ks -> new StudentResponse(
                        ks.getStudent().getId(),
                        ks.getStudent().getName(),
                        ks.getStudent().getEmail(),
                        ks.getStudent().getProfileImageUrl())
                )
                .toList();

        LocalDate today = LocalDate.now();

        List<SessionResponse> sessionResponses = klass.getSessions().stream()
                .map(session -> {
                    boolean isToday = session.getSessionDate().equals(today);
                    boolean isUpcoming = session.getSessionDate().isAfter(today);
                    boolean isPast = session.getSessionDate().isBefore(today);

                    return new SessionResponse(
                            session.getId(),
                            session.getSessionDate(),
                            session.getSessionTime(),
                            session.getTopic(),
                            session.isCanceled(),
                            isToday,
                            isUpcoming,
                            isPast
                    );
                })
                .toList();

        boolean todaySession = sessionResponses.stream()
                .anyMatch(SessionResponse::isToday);

        return new ClassDetailResponse(
                klass.getId(),
                klass.getName(),
                klass.getDescription(),
                klass.getClassTime(),
                klass.getMaxAbsencesAllowed(),
                klass.getJoinCode(),
                klass.getStartDate(),
                klass.getEndDate(),
                klass.getDurationMinutes(),
                klass.getScheduledDays(),
                todaySession,
                sessionResponses,
                enrolledStudents
        );
    }


    public StudentClassAttendanceStatsResponse getStudentClassStats(Long classId, Long studentId, UserDetails userDetails) {
        Klass klass = validateTeacherOwnsClass(classId, userDetails);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<ClassSession> validSessions = classSessionRepository.findByKlass_Id(classId).stream()
                .filter(s -> !s.isCanceled() &&
                        !s.getSessionDate().atTime(s.getSessionTime()).isAfter(LocalDateTime.now()))
                .toList();

        int total = validSessions.size();

        List<Attendance> attendances = attendanceRepository.findBySession_Klass_IdAndStudent_Id(classId, studentId);

        int present = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        int excused = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.EXCUSED).count();
        int absent = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();

        double percentage = total == 0 ? 0.0 : (present * 100.0) / total;

        return new StudentClassAttendanceStatsResponse(
                student.getId(),
                student.getName(),
                klass.getId(),
                klass.getName(),
                total,
                present,
                excused,
                absent,
                percentage,
                klass.getMaxAbsencesAllowed(),
                klass.getMaxAbsencesAllowed() - absent
        );
    }


    public QRCode generateQrCodeForSession(Long sessionId, double latitude, double longitude, UserDetails userDetails) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        validateTeacherOwnsClass(session.getKlass().getId(), userDetails);

        String qrText = UUID.randomUUID().toString();
        String qrBase64 = generateQrCodeImage(qrText);

        QRCode qrCode = qrCodeRepository.findBySessionId(sessionId)
                .orElse(new QRCode());

        qrCode.setSession(session);
        qrCode.setSessionDate(session.getSessionDate().atTime(session.getSessionTime()));
        qrCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        qrCode.setQrCodeData(qrBase64);
        qrCode.setLatitude(latitude);
        qrCode.setLongitude(longitude);

        return qrCodeRepository.save(qrCode);
    }


    public SessionDetailResponse getSessionDetails(Long sessionId, UserDetails userDetails) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        validateTeacherOwnsClass(session.getKlass().getId(), userDetails);

        if (session.isCanceled()) {
            return new SessionDetailResponse(
                    session.getId(),
                    session.getSessionDate(),
                    session.getSessionTime(),
                    true,
                    session.getTopic(),
                    List.of()
            );
        }

        List<Student> enrolledStudents = klassStudentRepository.findAllByKlassIdAndApprovedTrue(session.getKlass().getId())
                .stream()
                .map(KlassStudent::getStudent)
                .toList();

        List<Attendance> recordedAttendances = attendanceRepository.findBySession_Id(sessionId);

        Map<Long, Attendance> attendanceMap = recordedAttendances.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        LocalDateTime sessionStart = session.getSessionDate().atTime(session.getSessionTime());
        LocalDateTime sessionEnd = sessionStart.plusMinutes(session.getKlass().getDurationMinutes());
        LocalDateTime now = LocalDateTime.now();

        List<AttendanceResponse> attendanceResponses = enrolledStudents.stream()
                .map(student -> {
                    Attendance attendance = attendanceMap.get(student.getId());
                    String imageUrl = student.getProfileImageUrl();
                    if (attendance != null) {
                        return new AttendanceResponse(
                                attendance.getId(),
                                session.getKlass().getId(),
                                imageUrl,
                                student.getId(),
                                sessionId,
                                student.getName(),
                                attendance.getRecordedAt(),
                                attendance.getStatus(),

                                attendanceRepository.countBySession_Klass_IdAndStudent_IdAndStatus(
                                        session.getKlass().getId(),
                                        student.getId(),
                                        AttendanceStatus.ABSENT
                                ) >= session.getKlass().getMaxAbsencesAllowed()
                        );

                    } else if (now.isBefore(sessionEnd)) {
                        Attendance newPending = new Attendance();
                        newPending.setStudent(student);
                        newPending.setSession(session);
                        newPending.setRecordedAt(LocalDateTime.now());
                        newPending.setStatus(AttendanceStatus.PENDING);

                        Attendance savedPending = attendanceRepository.save(newPending);

                        return new AttendanceResponse(
                                savedPending.getId(),
                                session.getKlass().getId(),
                                imageUrl,
                                student.getId(),
                                sessionId,
                                student.getName(),
                                savedPending.getRecordedAt(),
                                savedPending.getStatus(),

                                attendanceRepository.countBySession_Klass_IdAndStudent_IdAndStatus(
                                        session.getKlass().getId(),
                                        student.getId(),
                                        AttendanceStatus.ABSENT
                                ) >= session.getKlass().getMaxAbsencesAllowed()
                        );

                    } else {
                        Attendance newAbsent = new Attendance();
                        newAbsent.setStudent(student);
                        newAbsent.setSession(session);
                        newAbsent.setRecordedAt(LocalDateTime.now());
                        newAbsent.setStatus(AttendanceStatus.ABSENT);

                        Attendance savedAbsent = attendanceRepository.save(newAbsent);

                        return new AttendanceResponse(
                                savedAbsent.getId(),
                                session.getKlass().getId(),
                                imageUrl,
                                student.getId(),
                                sessionId,
                                student.getName(),
                                attendance.getRecordedAt(),
                                attendance.getStatus(),

                                attendanceRepository.countBySession_Klass_IdAndStudent_IdAndStatus(
                                        session.getKlass().getId(),
                                        student.getId(),
                                        AttendanceStatus.ABSENT
                                ) >= session.getKlass().getMaxAbsencesAllowed()
                        );

                    }

                })
                .toList();

        return new SessionDetailResponse(
                session.getId(),
                session.getSessionDate(),
                session.getSessionTime(),
                false,
                session.getTopic(),
                attendanceResponses
        );
    }


    public List<Attendance> getSessionAttendance(Long sessionId, Long studentId, UserDetails userDetails) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        validateTeacherOwnsClass(session.getKlass().getId(), userDetails);

        return attendanceRepository.findAllBySession_IdAndStudent_Id(sessionId, studentId);
    }

    public void editAttendance(Long sessionId, Long attendanceId, AttendanceStatus newStatus, UserDetails userDetails) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));

        if (!attendance.getSession().getId().equals(sessionId)) {
            throw new RuntimeException("Attendance does not belong to this session");
        }

        validateTeacherOwnsClass(attendance.getSession().getKlass().getId(), userDetails);

        attendance.setStatus(newStatus);
        attendanceRepository.save(attendance);
    }


    public void approveAttendanceRequest(Long requestId, Long sessionId, UserDetails userDetails) {
        AttendanceRequest request = attendanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        validateTeacherOwnsClass(session.getKlass().getId(), userDetails);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request already handled.");
        }

        request.setStatus(RequestStatus.APPROVED);
        attendanceRequestRepository.save(request);

        Optional<Attendance> existingAttendance = attendanceRepository
                .findBySession_IdAndStudent_Id(session.getId(), request.getStudent().getId());

        if (existingAttendance.isPresent()) {
            Attendance attendance = existingAttendance.get();
            attendance.setRecordedAt(LocalDateTime.now());
            attendance.setStatus(AttendanceStatus.PRESENT);
            attendanceRepository.save(attendance);
        } else {
            Attendance newAttendance = new Attendance();
            newAttendance.setSession(session);
            newAttendance.setStudent(request.getStudent());
            newAttendance.setRecordedAt(LocalDateTime.now());
            newAttendance.setStatus(AttendanceStatus.PRESENT);
            attendanceRepository.save(newAttendance);
        }
    }

    public void rejectAttendanceRequest(Long requestId, Long sessionId, UserDetails userDetails) {
        AttendanceRequest request = attendanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        validateTeacherOwnsClass(session.getKlass().getId(), userDetails);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request already handled.");
        }

        Optional<Attendance> existingAttendance = attendanceRepository
                .findBySession_IdAndStudent_Id(session.getId(), request.getStudent().getId());

        if (existingAttendance.isPresent()) {
            Attendance attendance = existingAttendance.get();

            attendance.setStatus(AttendanceStatus.ABSENT);
            attendance.setRecordedAt(LocalDateTime.now());
            attendanceRepository.save(attendance);
        }

        request.setStatus(RequestStatus.REJECTED);
        attendanceRequestRepository.save(request);
    }


    public List<AttendanceRequestResponse> getPendingSessionRequests(Long sessionId, UserDetails userDetails) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        validateTeacherOwnsClass(session.getKlass().getId(), userDetails);

        List<AttendanceRequest> requests = attendanceRequestRepository.findBySessionIdAndStatus(sessionId, RequestStatus.PENDING);

        return requests.stream()
                .map(request -> {
                    Student student = request.getStudent();
                    return new AttendanceRequestResponse(
                            request.getId(),
                            student.getId(),
                            student.getName(),
                            student.getEmail(), student.getProfileImageUrl(),
                            session.getKlass().getId(),
                            request.getRequestedAt(),
                            request.getStatus()
                    );
                }).collect(Collectors.toList());
    }


    public void approveStudentJoin(Long classId, Long studentId, UserDetails userDetails) {
        validateTeacherOwnsClass(classId, userDetails);

        KlassStudent joinRequest = klassStudentRepository.findByKlassIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new RuntimeException("Join request not found"));

        joinRequest.setApproved(true);
        klassStudentRepository.save(joinRequest);
    }

    public void rejectStudentJoin(Long classId, Long studentId, UserDetails userDetails) {
        validateTeacherOwnsClass(classId, userDetails);

        KlassStudent joinRequest = klassStudentRepository.findByKlassIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new RuntimeException("Join request not found"));

        klassStudentRepository.delete(joinRequest);
    }

    public List<StudentResponse> getPendingJoinRequests(Long classId, UserDetails userDetails) {
        validateTeacherOwnsClass(classId, userDetails);

        return klassStudentRepository.findAllByKlassIdAndApprovedFalse(classId).stream()
                .map(ks -> {
                    Student s = ks.getStudent();
                    return new StudentResponse(
                            s.getId(),
                            s.getName(),
                            s.getEmail(),
                            s.getProfileImageUrl());
                })
                .collect(Collectors.toList());
    }


    private Klass validateTeacherOwnsClass(Long classId, UserDetails userDetails) {
        Klass klass = klassRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        if (!klass.getTeacher().getEmail().equals(userDetails.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return klass;
    }

    @Transactional
    public void updateClassSession(UpdateSessionRequest req, UserDetails userDetails) {
        ClassSession session = classSessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Klass klass = session.getKlass();
        validateTeacherOwnsClass(klass.getId(), userDetails);

        if (req.getCanceled()) {
            session.setCanceled(true);

            List<Attendance> attendances = attendanceRepository.findBySession_Id(session.getId());
            for (Attendance a : attendances) {
                a.setStatus(AttendanceStatus.EXCUSED);
            }
            attendanceRepository.saveAll(attendances);
        }

        if (req.getTopic() != null) {
            session.setTopic(req.getTopic());
        }

        if (req.getSessionDate() != null && req.getSessionTime() != null) {
            LocalDateTime newStart = req.getSessionDate().atTime(req.getSessionTime());
            LocalDateTime newEnd = newStart.plusMinutes(klass.getDurationMinutes());

            Long teacherId = klass.getTeacher().getId();
            List<ClassSession> teacherSessions = classSessionRepository.findAllByTeacherIdAndDate(
                    teacherId, req.getSessionDate());

            for (ClassSession s : teacherSessions) {
                if (s.getId().equals(session.getId())) continue;

                LocalDateTime existingStart = s.getSessionDate().atTime(s.getSessionTime());
                LocalDateTime existingEnd = existingStart.plusMinutes(s.getKlass().getDurationMinutes());

                if (!(newEnd.isBefore(existingStart) || newStart.isAfter(existingEnd))) {
                    throw new RuntimeException("Conflict with another session you teach on the same day.");
                }
            }

            List<Student> enrolledStudents = klassStudentRepository.findApprovedStudentsByClassId(klass.getId());
            for (Student student : enrolledStudents) {
                List<ClassSession> studentSessions = classSessionRepository.findSessionsByStudentIdAndDate(student.getId(), req.getSessionDate());
                for (ClassSession s : studentSessions) {
                    if (s.getId().equals(session.getId())) continue;

                    LocalDateTime existingStart = s.getSessionDate().atTime(s.getSessionTime());
                    LocalDateTime existingEnd = existingStart.plusMinutes(s.getKlass().getDurationMinutes());

                    if (!(newEnd.isBefore(existingStart) || newStart.isAfter(existingEnd))) {
                        String conflictMessage = String.format(
                                "Conflict for student %s with class '%s' at %s",
                                student.getName(),
                                s.getKlass().getName(),
                                s.getSessionTime()
                        );
                        throw new RuntimeException(conflictMessage);
                    }
                }
            }

            session.setSessionDate(req.getSessionDate());
            session.setSessionTime(req.getSessionTime());
        }

        classSessionRepository.save(session);
    }


    public List<StudentClassAttendanceSummaryResponse> getClassAttendanceSummary(Long classId) {
        Klass klass = klassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        List<ClassSession> sessions = classSessionRepository.findByKlass_Id(classId);
        int totalSessions = (int) sessions.stream()
                .filter(session -> !session.getSessionDate().isAfter(LocalDate.now()))
                .count();

        List<Student> students = klassStudentRepository.findApprovedStudentsByClassId(classId);

        List<StudentClassAttendanceSummaryResponse> summaries = new ArrayList<>();

        for (Student student : students) {
            List<Attendance> attendanceRecords = attendanceRepository.findBySession_Klass_IdAndStudent_Id(classId, student.getId());

            int present = (int) attendanceRecords.stream().filter((a -> a.getStatus() == AttendanceStatus.PRESENT)).count();
            int excused = (int) attendanceRecords.stream().filter((a -> a.getStatus() == AttendanceStatus.EXCUSED)).count();
            int absent = (int) attendanceRecords.stream().filter((a -> a.getStatus() == AttendanceStatus.ABSENT)).count();


            int remaining = klass.getMaxAbsencesAllowed() - absent;

            StudentClassAttendanceSummaryResponse dto = new StudentClassAttendanceSummaryResponse();
            dto.setStudentId(student.getId());
            dto.setStudentName(student.getName());
            dto.setStudentEmail(student.getEmail());
            dto.setTotalSessions(totalSessions);
            dto.setPresentCount(present);
            dto.setExcusedCount(excused);
            dto.setAbsentCount(absent);
            dto.setMaxAllowedAbsences(klass.getMaxAbsencesAllowed());
            dto.setRemainingAbsences(Math.max(0, remaining));
            dto.setProfileImageUrl(student.getProfileImageUrl());

            summaries.add(dto);
        }

        return summaries;
    }


    private ClassResponse mapToClassResponse(Klass klass) {


        return new ClassResponse(
                klass.getId(),
                klass.getName(),
                klass.getDescription(),
                klass.getClassTime(),
                klass.getMaxAbsencesAllowed(),
                klass.getJoinCode(),
                klass.getStartDate(),
                klass.getEndDate(),
                klass.getDurationMinutes(),
                klass.getScheduledDays(),
                klass.getAcceptanceRadiusMeters()
        );
    }

}
