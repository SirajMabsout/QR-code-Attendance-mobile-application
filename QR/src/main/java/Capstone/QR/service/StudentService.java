package Capstone.QR.service;


import Capstone.QR.dto.Response.*;
import Capstone.QR.model.*;
import Capstone.QR.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import Capstone.QR.utils.DistanceCalc;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final KlassRepository klassRepository;
    private final AttendanceRepository attendanceRepository;
    private final QRCodeRepository qrCodeRepository;
    private final AttendanceRequestRepository attendanceRequestRepository;
    private final KlassStudentRepository klassStudentRepository;
    private final ClassSessionRepository classSessionRepository;

    public String requestJoinClass(String email, String joinCode) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Klass klass = klassRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new RuntimeException("Class not found with provided join code"));

        Optional<KlassStudent> existing = klassStudentRepository.findByKlassIdAndStudentId(klass.getId(), student.getId());

        if (existing.isPresent()) {
            if (existing.get().isApproved()) {
                throw new IllegalStateException("Already joined this class.");
            } else {
                throw new IllegalStateException("Join request already sent.");
            }
        }

        KlassStudent ks = new KlassStudent();
        ks.setKlass(klass);
        ks.setStudent(student);
        ks.setApproved(false);
        ks.setRequestedAt(LocalDateTime.now());

        klassStudentRepository.save(ks);
        return "Join request sent successfully.";
    }

    public List<Klass> getMyClasses(UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        return klassStudentRepository.findAllByStudentIdAndApprovedTrue(student.getId())
                .stream()
                .map(KlassStudent::getKlass)
                .collect(Collectors.toList());
    }


    public String scanQr(String qrCodeData, double studentLat, double studentLng, String networkName, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        QRCode qrCode = qrCodeRepository.findByQrCodeData(qrCodeData)
                .orElseThrow(() -> new RuntimeException("Invalid QR code"));

        ClassSession session = qrCode.getSession();
        Klass klass = session.getKlass();

        boolean isEnrolled = klassStudentRepository
                .findByKlassIdAndStudentId(klass.getId(), student.getId())
                .map(KlassStudent::isApproved)
                .orElse(false);

        if (!isEnrolled) {
            throw new RuntimeException("Student not registered or not approved for this class");
        }




        LocalDate today = LocalDate.now();
        boolean alreadyMarked = attendanceRepository
                .findBySession_IdAndStudent_Id(session.getId(), student.getId())
                .stream()
                .anyMatch(a -> a.getRecordedAt().toLocalDate().equals(today));

        if (alreadyMarked) {
            throw new RuntimeException("Attendance already marked for today");
        }

        double distance = DistanceCalc.calculateDistance(qrCode.getLatitude(), qrCode.getLongitude(), studentLat, studentLng);
        double allowedDistance = klass.getAcceptanceRadiusMeters();

        if (distance <= allowedDistance) {
            Attendance attendance = new Attendance();
            attendance.setSession(session);
            attendance.setStudent(student);
            attendance.setRecordedAt(LocalDateTime.now());
            attendance.setStatus(AttendanceStatus.PRESENT);
            attendanceRepository.save(attendance);
            attendanceRequestRepository.findByStudentIdAndSessionId(student.getId(), session.getId())
                    .ifPresent(request -> {
                        if (request.getStatus() == RequestStatus.PENDING) {
                            attendanceRequestRepository.delete(request);
                        }
                    });
            return "Attendance marked successfully.";
        }

        if (networkName.equalsIgnoreCase("LAU") || networkName.equalsIgnoreCase("LAU Students")) {
            AttendanceRequest request = new AttendanceRequest();
            request.setStudent(student);
            request.setSession(session);
            request.setRequestedAt(LocalDateTime.now());
            request.setStatus(RequestStatus.PENDING);
            attendanceRequestRepository.save(request);

            throw new RuntimeException("You're not near the class but on university network. Attendance request sent for approval.");
        }

        throw new RuntimeException("You're too far and not on LAU network. Attendance denied.");
    }

    public List<Attendance> getMyAttendance(Long classId, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return attendanceRepository.findBySession_Klass_IdAndStudent_Id(classId, student.getId());
    }

    public List<Attendance> getAttendanceSummary(UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return attendanceRepository.findAll()
                .stream()
                .filter(a -> a.getStudent().equals(student))
                .toList();
    }

    public long countAbsences(Long classId, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        LocalDate today = LocalDate.now();
        List<ClassSession> sessions = classSessionRepository.findByKlass_Id(classId).stream()
                .filter(s -> !s.isCanceled())
                .filter(session ->
                        session.getSessionDate().isBefore(today) ||
                                (session.getSessionDate().isEqual(today) &&
                                        session.getSessionTime().isBefore(LocalTime.now()))
                )
                .toList();

        List<Attendance> records = attendanceRepository.findBySession_Klass_IdAndStudent_Id(classId, student.getId());

        long presentOrExcused = records.stream()
                .filter(a -> (a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.EXCUSED) &&
                        sessions.stream().anyMatch(s -> s.getId().equals(a.getSession().getId())))
                .count();

        return sessions.size() - presentOrExcused;
    }

    public double getAttendancePercentage(Long classId, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        LocalDate today = LocalDate.now();
        List<ClassSession> sessions = classSessionRepository.findByKlass_Id(classId).stream()
                .filter(s -> !s.isCanceled())
                .filter(session ->
                        session.getSessionDate().isBefore(today) ||
                                (session.getSessionDate().isEqual(today) &&
                                        session.getSessionTime().isBefore(LocalTime.now()))
                )
                .toList();

        if (sessions.isEmpty()) return 0.0;

        List<Attendance> records = attendanceRepository.findBySession_Klass_IdAndStudent_Id(classId, student.getId());

        long presentCount = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT &&
                        sessions.stream().anyMatch(s -> s.getId().equals(a.getSession().getId())))
                .count();

        return (presentCount * 100.0) / sessions.size();
    }


    public List<PendingJoinRequestResponse> getPendingJoinRequests(UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        return klassStudentRepository.findAllByStudentIdAndApprovedFalse(student.getId())
                .stream()
                .map(ks -> new PendingJoinRequestResponse(
                        ks.getKlass().getId(),
                        ks.getKlass().getName(),
                        ks.getKlass().getTeacher().getName(),
                        ks.getKlass().getClassTime(),
                        ks.getKlass().getScheduledDays().stream().map(Enum::name).toList(),
                        ks.getRequestedAt().toLocalDate()
                ))
                .toList();
    }



    public ClassDetailResponse getClassDetail(Long classId, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Klass klass = klassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        boolean isApproved = klassStudentRepository
                .findByKlassIdAndStudentId(classId, student.getId())
                .map(KlassStudent::isApproved)
                .orElse(false);

        if (!isApproved) {
            throw new RuntimeException("You're not enrolled in this class.");
        }

        List<SessionResponse> sessions = klass.getSessions().stream()
                .map(SessionResponse::new)
                .toList();


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
                sessions,
                null
        );


    }
    public List<StudentAttendanceRequestResponse> getMyAttendanceRequests(UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        return attendanceRequestRepository.findAllByStudentId(student.getId())
                .stream()
                .map(request -> {
                    ClassSession session = request.getSession();
                    Klass klass = session.getKlass();

                    return new StudentAttendanceRequestResponse(
                            request.getId(),
                            klass.getId(),
                            klass.getName(),
                            session.getSessionDate(),
                            session.getSessionTime(),
                            request.getStatus(),
                            request.getRequestedAt().toLocalDate()
                    );
                })
                .toList();
    }

    public void cancelJoinRequest(Long classId, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        KlassStudent request = klassStudentRepository
                .findByKlassIdAndStudentId(classId, student.getId())
                .orElseThrow(() -> new RuntimeException("No join request found"));

        if (request.isApproved()) {
            throw new RuntimeException("You cannot cancel an approved request.");
        }

        klassStudentRepository.delete(request);
    }




}
