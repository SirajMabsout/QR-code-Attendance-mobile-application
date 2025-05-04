package Capstone.QR.service;


import Capstone.QR.dto.Response.*;
import Capstone.QR.model.*;
import Capstone.QR.repository.*;
import Capstone.QR.utils.DistanceCalc;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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


    @Transactional
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

        Optional<Attendance> optionalAttendance = attendanceRepository
                .findAllBySession_IdAndStudent_Id(session.getId(), student.getId())
                .stream()
                .filter(a -> a.getStatus() != AttendanceStatus.PENDING).findFirst();

        if (optionalAttendance.isPresent()) {
            throw new RuntimeException("Attendance already marked for this session");
        }

        double distance = DistanceCalc.calculateDistance(qrCode.getLatitude(), qrCode.getLongitude(), studentLat, studentLng);
        double allowedDistance = klass.getAcceptanceRadiusMeters();

        List<String> allowedSSIDs = klass.getAllowedWifiSSIDs();
        boolean onAllowedNetwork = allowedSSIDs.contains(networkName);

        if (distance <= allowedDistance) {
            Optional<Attendance> attendanceOpt = attendanceRepository
                    .findAllBySession_IdAndStudent_Id(session.getId(), student.getId())
                    .stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.PENDING || a.getStatus() == AttendanceStatus.ABSENT || a.getStatus() == AttendanceStatus.EXCUSED)
                    .findFirst();

            if (attendanceOpt.isPresent()) {
                Attendance attendance = attendanceOpt.get();
                attendance.setStatus(AttendanceStatus.PRESENT);
                attendance.setRecordedAt(LocalDateTime.now());
                attendanceRepository.save(attendance);
            } else {
                Attendance newAttendance = new Attendance();
                newAttendance.setSession(session);
                newAttendance.setStudent(student);
                newAttendance.setRecordedAt(LocalDateTime.now());
                newAttendance.setStatus(AttendanceStatus.PRESENT);
                attendanceRepository.save(newAttendance);
            }

            attendanceRequestRepository.findByStudentIdAndSessionId(student.getId(), session.getId())
                    .ifPresent(request -> {
                        if (request.getStatus() == RequestStatus.PENDING) {
                            attendanceRequestRepository.delete(request);
                        }
                    });

            return "Attendance marked successfully.";
        }

        if (onAllowedNetwork) {
            AttendanceRequest existingRequest = attendanceRequestRepository
                    .findByStudentIdAndSessionId(student.getId(), session.getId())
                    .orElse(null);

            if (existingRequest != null && existingRequest.getStatus() == RequestStatus.PENDING) {
                return "There is already pending attendance request ! Check your instructor.";
            } else if (existingRequest != null) {
                existingRequest.setRequestedAt(LocalDateTime.now());
                existingRequest.setStatus(RequestStatus.PENDING);
                attendanceRequestRepository.save(existingRequest);

            } else {
                AttendanceRequest newRequest = new AttendanceRequest();
                newRequest.setStudent(student);
                newRequest.setSession(session);
                newRequest.setRequestedAt(LocalDateTime.now());
                newRequest.setStatus(RequestStatus.PENDING);
                attendanceRequestRepository.save(newRequest);
            }

            Optional<Attendance> attendanceOpt = attendanceRepository
                    .findAllBySession_IdAndStudent_Id(session.getId(), student.getId())
                    .stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                    .findFirst();

            if (attendanceOpt.isPresent()) {
                Attendance attendance = attendanceOpt.get();
                attendance.setStatus(AttendanceStatus.PENDING);
                attendance.setRecordedAt(LocalDateTime.now());
                attendanceRepository.save(attendance);
            }

            return "You're not near the class but connected to an approved Wi-Fi network. An Attendance Request has been sent to the instructor.";

        }

        throw new RuntimeException(
                "You're too far and connected to an unapproved network: " + networkName +
                        ". Approved networks for this class are: " + String.join(", ", allowedSSIDs)
        );
    }


    public StudentAttendanceSummaryResponse getMyAttendanceSummary(Long classId, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<ClassSession> pastSessions = classSessionRepository.findByKlass_Id(classId).stream()
                .filter(s -> !s.isCanceled() && !s.getSessionDate().isAfter(LocalDate.now()))
                .collect(Collectors.toList());

        List<Attendance> attendanceList = attendanceRepository.findBySession_Klass_IdAndStudent_Id(classId, student.getId());

        Map<Long, AttendanceStatus> attendanceMap = attendanceList.stream()
                .collect(Collectors.toMap(
                        a -> a.getSession().getId(),
                        Attendance::getStatus
                ));

        List<StudentAttendanceSummaryResponse.SessionStatus> sessionStatuses = new ArrayList<>();
        int present = 0, excused = 0, absent = 0;

        for (ClassSession session : pastSessions) {
            AttendanceStatus status = attendanceMap.get(session.getId());

            String displayStatus;
            if (status == AttendanceStatus.PRESENT) {
                present++;
                displayStatus = "PRESENT";
            } else if (status == AttendanceStatus.EXCUSED) {
                excused++;
                displayStatus = "EXCUSED";
            } else if (status == AttendanceStatus.ABSENT) {
                absent++;
                displayStatus = "ABSENT";
            } else {
                displayStatus = "NOT_RECORDED";
            }

            sessionStatuses.add(new StudentAttendanceSummaryResponse.SessionStatus(
                    session.getSessionDate(),
                    displayStatus
            ));
        }

        Klass klass = klassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        int total = pastSessions.size();
        int allowedAbsences = klass.getMaxAbsencesAllowed();
        int remaining = Math.max(allowedAbsences - absent, 0);
        double percentage = total == 0 ? 0 : (present * 100.0 / total);

        return new StudentAttendanceSummaryResponse(
                sessionStatuses,
                allowedAbsences,
                total,
                present,
                excused,
                absent,
                remaining,
                percentage
        );
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
                false,
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
