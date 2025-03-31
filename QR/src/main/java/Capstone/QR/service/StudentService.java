package Capstone.QR.service;


import Capstone.QR.model.*;
import Capstone.QR.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    public String requestJoinClass(String email, Long classId) {

        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Klass klass = klassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Optional<KlassStudent> existing = klassStudentRepository.findByKlassIdAndStudentId(classId, student.getId());

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
        return student.getRegisteredClasses();
    }

// === Updated scanQr method in StudentService.java ===

    public void scanQr(String qrCodeData, double studentLat, double studentLng, String networkName, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        QRCode qrCode = qrCodeRepository.findByQrCodeData(qrCodeData)
                .orElseThrow(() -> new RuntimeException("Invalid QR code"));

        if (qrCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("QR code expired");
        }

        Klass klass = qrCode.getKlass();

        if (!klass.getStudents().contains(student)) {
            throw new RuntimeException("Student not registered in this class");
        }

        boolean alreadyMarked = attendanceRepository
                .findByKlassIdAndStudentId(klass.getId(), student.getId())
                .stream()
                .anyMatch(a -> a.getDate().toLocalDate().equals(qrCode.getSessionDate().toLocalDate()));

        if (alreadyMarked) {
            throw new RuntimeException("Attendance already marked for today");
        }

        double teacherLat = qrCode.getLatitude();
        double teacherLng = qrCode.getLongitude();
        double distance = DistanceCalc.calculateDistance(teacherLat, teacherLng, studentLat, studentLng);

        if (distance <= 5.0) {
            Attendance attendance = new Attendance();
            attendance.setKlass(klass);
            attendance.setStudent(student);
            attendance.setDate(LocalDateTime.now());
            attendance.setStatus(AttendanceStatus.PRESENT);
            attendanceRepository.save(attendance);
            return;
        }

        // Check if student is on university Wi-Fi
        if (networkName.equalsIgnoreCase("LAU") || networkName.equalsIgnoreCase("LAU Students")) {
            AttendanceRequest request = new AttendanceRequest();
            request.setStudent(student);
            request.setKlass(klass);
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
        return attendanceRepository.findByKlassIdAndStudentId(classId, student.getId());
    }

    public List<Attendance> getAttendanceSummary(UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return attendanceRepository.findAll()
                .stream()
                .filter(a -> a.getStudent().equals(student))
                .toList();
    }

    // === View number of absences per class ===
    public long countAbsences(Long classId, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return attendanceRepository.findByKlassIdAndStudentId(classId, student.getId())
                .stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .count();
    }

    // === Get total attendance percentage ===
    public double getAttendancePercentage(Long classId, UserDetails userDetails) {
        Student student = studentRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        List<Attendance> records = attendanceRepository.findByKlassIdAndStudentId(classId, student.getId());
        if (records.isEmpty()) return 0.0;
        long presentCount = records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        return (presentCount * 100.0) / records.size();
    }
}


