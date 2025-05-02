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

    // ========== CLASS MANAGEMENT ==========

    public ClassResponse createClass(CreateClassRequest request, UserDetails userDetails) {
        Teacher teacher = teacherRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        // ❌ Check for duplicate class name
        if (klassRepository.existsByTeacherAndName(teacher, request.getName())) {
            throw new RuntimeException("You already have a class with this name.");
        }

        // ❌ Check for class schedule conflict using date + day + time + duration
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

            // ✅ Correct conflict check: overlaps or touches
            boolean timeConflict = newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);

            if (dayConflict && timeConflict) {
                throw new RuntimeException("Class schedule conflicts with an existing class: " + existing.getName());
            }
        }

        // ✅ Proceed to create the class
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
        klass.setDurationMinutes(request.getDurationMinutes()); // ✅ Set class duration
        klass.setAllowedWifiSSIDs(request.getAllowedWifiSSIDs());

        // ✅ Generate and set sessions
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

        // ✅ Check if there is a session today
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
                   // ✅ Added here
        );
    }


    public StudentClassAttendanceStatsResponse getStudentClassStats(Long classId, Long studentId, UserDetails userDetails) {
        Klass klass = validateTeacherOwnsClass(classId, userDetails);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Valid sessions = not canceled and not in the future
        List<ClassSession> validSessions = classSessionRepository.findByKlass_Id(classId).stream()
                .filter(s -> !s.isCanceled() &&
                        !s.getSessionDate().atTime(s.getSessionTime()).isAfter(LocalDateTime.now()))
                .toList();

        int total = validSessions.size();

        // Fetch all attendance records for this student in this class
        List<Attendance> attendances = attendanceRepository.findBySession_Klass_IdAndStudent_Id(classId, studentId);

        // Count by actual status instead of computing indirectly
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

    // ========== QR CODE ==========

    public QRCode generateQrCodeForSession(Long sessionId, double latitude, double longitude, UserDetails userDetails) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        validateTeacherOwnsClass(session.getKlass().getId(), userDetails);

        String qrText = UUID.randomUUID().toString();
        String qrBase64 = generateQrCodeImage(qrText);

        // ✅ Check if QR already exists for the session
        QRCode qrCode = qrCodeRepository.findBySessionId(sessionId)
                .orElse(new QRCode());

        qrCode.setSession(session); // safe even if already set
        qrCode.setSessionDate(session.getSessionDate().atTime(session.getSessionTime()));
        qrCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        qrCode.setQrCodeData(qrBase64);
        qrCode.setLatitude(latitude);
        qrCode.setLongitude(longitude);

        return qrCodeRepository.save(qrCode);
    }


    // ========== SESSION DETAILS ==========

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
                    String imageUrl = student.getProfileImageUrl(); // Get student's profile image URL

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

                                // ✅ NEW: compute maxAbsence directly inline
                                attendanceRepository.countBySession_Klass_IdAndStudent_IdAndStatus(
                                        session.getKlass().getId(),
                                        student.getId(),
                                        AttendanceStatus.ABSENT
                                ) >= session.getKlass().getMaxAbsencesAllowed()
                        );

                    } else if (now.isBefore(sessionEnd)) {
                        // Student did not scan yet but session is still ongoing → PENDING
                        Attendance newPending = new Attendance();
                        newPending.setStudent(student);
                        newPending.setSession(session);
                        newPending.setRecordedAt(LocalDateTime.now());
                        newPending.setStatus(AttendanceStatus.PENDING);

                        Attendance savedPending = attendanceRepository.save(newPending);

                        return new AttendanceResponse(
                                attendance.getId(),
                                session.getKlass().getId(),
                                imageUrl,
                                student.getId(),
                                sessionId,
                                student.getName(),
                                attendance.getRecordedAt(),
                                attendance.getStatus(),

                                // ✅ NEW: compute maxAbsence directly inline
                                attendanceRepository.countBySession_Klass_IdAndStudent_IdAndStatus(
                                        session.getKlass().getId(),
                                        student.getId(),
                                        AttendanceStatus.ABSENT
                                ) >= session.getKlass().getMaxAbsencesAllowed()
                        );

                    } else {
                        // Student did not scan and session ended → ABSENT
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

                                // ✅ NEW: compute maxAbsence directly inline
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

    // ========== ATTENDANCE ==========

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

    // ========== ATTENDANCE REQUESTS ==========

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

        // ✅ Check if attendance already exists
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

        // ✅ Update attendance if exists
        Optional<Attendance> existingAttendance = attendanceRepository
                .findBySession_IdAndStudent_Id(session.getId(), request.getStudent().getId());

        if (existingAttendance.isPresent()) {
            Attendance attendance = existingAttendance.get();

            // You can either:
            // - delete it if you want no trace
            // - OR update it to ABSENT if session already passed
            attendance.setStatus(AttendanceStatus.ABSENT);
            attendance.setRecordedAt(LocalDateTime.now()); // update recordedAt for audit
            attendanceRepository.save(attendance);
        }

        // ✅ Update the request status
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
                            student.getEmail(),                      // ✅ add this
                            student.getProfileImageUrl(),
                            session.getKlass().getId(),
                            request.getRequestedAt(),
                            request.getStatus()
                    );
                }).collect(Collectors.toList());
    }


    // ========== STUDENT JOIN REQUESTS ==========

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
                            s.getProfileImageUrl() // Assuming your Student entity has this method
                    );
                })
                .collect(Collectors.toList());
    }


    // ========== HELPERS ==========

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

        // ✅ Handle cancelation
        if (req.getCanceled()) {
            session.setCanceled(true);

            List<Attendance> attendances = attendanceRepository.findBySession_Id(session.getId());
            for (Attendance a : attendances) {
                a.setStatus(AttendanceStatus.EXCUSED);
            }
            attendanceRepository.saveAll(attendances);
        }

        // ✅ Update topic
        if (req.getTopic() != null) {
            session.setTopic(req.getTopic());
        }

        // ✅ Update date and time (with conflict checks)
        if (req.getSessionDate() != null && req.getSessionTime() != null) {
            LocalDateTime newStart = req.getSessionDate().atTime(req.getSessionTime());
            LocalDateTime newEnd = newStart.plusMinutes(klass.getDurationMinutes());

            // ✅ Check teacher conflicts
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

            // ✅ Check student conflicts
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
            List<Attendance> attendanceRecords = attendanceRepository.findBySession_Klass_IdAndStudent_Id(classId,student.getId());

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
                // ✅ Include in response
        );
    }

}
