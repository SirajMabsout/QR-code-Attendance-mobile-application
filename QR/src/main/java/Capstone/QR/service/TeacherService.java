package Capstone.QR.service;

import Capstone.QR.dto.CreateClassRequest;
import Capstone.QR.dto.Response.ClassResponse;
import Capstone.QR.dto.Response.StudentResponse;
import Capstone.QR.model.*;
import Capstone.QR.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import static Capstone.QR.utils.generateQR.generateQrCodeImage;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final KlassRepository klassRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final QRCodeRepository qrCodeRepository;
    private final AttendanceRequestRepository attendanceRequestRepository;
    private final KlassStudentRepository klassStudentRepository;

    // 1. Create a class
    public ClassResponse createClass(CreateClassRequest request, UserDetails teacherUser) {
        Teacher teacher = teacherRepository.findByEmail(teacherUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        Klass klass = new Klass();
        klass.setName(request.getName());
        klass.setDescription(request.getDescription());
        klass.setClassTime(request.getClassTime());
        klass.setMaxAbsencesAllowed(request.getMaxAbsencesAllowed());
        klass.setTeacher(teacher);

        klass = klassRepository.save(klass);

        return mapToResponse(klass);
    }

    // 2. List all classes by teacher
    public List<ClassResponse> getAllClasses(UserDetails teacherUser) {
        Teacher teacher = teacherRepository.findByEmail(teacherUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        return klassRepository.findByTeacher(teacher)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 3. View class details
    public ClassResponse getClassDetails(Long classId, UserDetails teacherUser) {
        Klass klass = klassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (!klass.getTeacher().getEmail().equals(teacherUser.getUsername())) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(klass);
    }





    // 6. Generate QR code for session
    public QRCode generateQrCode(Long classId, LocalDateTime sessionDateTime, double latitude, double longitude) {
        Klass klass = klassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        String qrText = UUID.randomUUID().toString(); // or encode session info here
        String qrBase64 = generateQrCodeImage(qrText);

        QRCode qrCode = new QRCode();
        qrCode.setKlass(klass);
        qrCode.setSessionDate(sessionDateTime);
        qrCode.setExpiresAt(sessionDateTime.plusMinutes(10));
        qrCode.setQrCodeData(qrBase64); // 🔁 Now stores actual QR image
        qrCode.setLatitude(latitude);
        qrCode.setLongitude(longitude);

        return qrCodeRepository.save(qrCode);
    }


    // 7. View student attendance
    public List<Attendance> getStudentAttendance(Long classId, Long studentId) {
        return attendanceRepository.findByKlassIdAndStudentId(classId, studentId);
    }

    // 8. Edit student attendance
    public void editAttendance(Long attendanceId, AttendanceStatus newStatus) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));

        attendance.setStatus(newStatus);
        attendanceRepository.save(attendance);
    }




    public void approveAttendanceRequest(Long requestId) {
        AttendanceRequest request = attendanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request is already handled.");
        }

        request.setStatus(RequestStatus.APPROVED);
        attendanceRequestRepository.save(request);

        // Mark attendance
        Attendance attendance = new Attendance();
        attendance.setKlass(request.getKlass());
        attendance.setStudent(request.getStudent());
        attendance.setDate(LocalDateTime.now());
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);
    }

    public void rejectAttendanceRequest(Long requestId) {
        AttendanceRequest request = attendanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request is already handled.");
        }

        request.setStatus(RequestStatus.REJECTED);
        attendanceRequestRepository.save(request);
    }

    public List<AttendanceRequest> getPendingRequests(Long classId) {
        return attendanceRequestRepository.findByKlassIdAndStatus(classId, RequestStatus.PENDING);
    }


    public void approveStudentJoin(String email, Long classId, Long studentId) throws AccessDeniedException {
            Klass klass = validateOwnership(email, classId);

            KlassStudent joinRequest = klassStudentRepository.findByKlassIdAndStudentId(classId, studentId)
                    .orElseThrow(() -> new RuntimeException("Join request not found"));

            joinRequest.setApproved(true);
            klassStudentRepository.save(joinRequest);
        }

        public void rejectStudentJoin(String email, Long classId, Long studentId) throws AccessDeniedException {
            Klass klass = validateOwnership(email, classId);

            KlassStudent joinRequest = klassStudentRepository.findByKlassIdAndStudentId(classId, studentId)
                    .orElseThrow(() -> new RuntimeException("Join request not found"));

            klassStudentRepository.delete(joinRequest);
        }

        public List<StudentResponse> getPendingJoinRequests(String email, Long classId) throws AccessDeniedException {
            Klass klass = validateOwnership(email, classId);

            return klassStudentRepository.findAllByKlassIdAndApprovedFalse(classId).stream()
                    .map(ks -> {
                        Student s = ks.getStudent();
                        return new StudentResponse(s.getId(), s.getName(), s.getEmail());
                    })
                    .toList();
        }

        private Klass validateOwnership(String email, Long classId) throws AccessDeniedException {
            Klass klass = klassRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found"));

            if (!klass.getTeacher().getEmail().equals(email)) {
                throw new AccessDeniedException("You do not own this class.");
            }

            return klass;
        }


    private ClassResponse mapToResponse(Klass klass) {
        ClassResponse response = new ClassResponse();
        response.setId(klass.getId());
        response.setName(klass.getName());
        response.setDescription(klass.getDescription());
        response.setClassTime(klass.getClassTime());
        response.setMaxAbsencesAllowed(klass.getMaxAbsencesAllowed());
        return response;
    }
}





