package Capstone.QR.controller;

import Capstone.QR.dto.Response.AttendanceResponse;
import Capstone.QR.dto.Response.ClassResponse;
import Capstone.QR.service.StudentService;
import Capstone.QR.model.Attendance;
import Capstone.QR.model.Klass;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;

    @PostMapping("/join/{classId}")
    public ResponseEntity<?> joinClass(@PathVariable Long classId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        String message = studentService.requestJoinClass(userDetails.getUsername(), classId);
        return ResponseEntity.ok(message);
    }


    // 2. Get registered classes
    @GetMapping("/my-classes")
    public List<ClassResponse> getMyClasses(@AuthenticationPrincipal UserDetails userDetails) {
        return studentService.getMyClasses(userDetails)
                .stream()
                .map(this::mapToClassResponse)
                .collect(Collectors.toList());
    }

    // 3. Scan QR code for attendance
    @PostMapping("/scan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void scanQr(@RequestParam String qrCodeData,
                       @RequestParam double latitude,
                       @RequestParam double longitude,
                       @RequestParam String networkName,
                       @AuthenticationPrincipal UserDetails userDetails) {
        studentService.scanQr(qrCodeData, latitude, longitude, networkName, userDetails);
    }

    // 4. View attendance for a specific class
    @GetMapping("/attendance/{classId}")
    public List<AttendanceResponse> getMyAttendance(@PathVariable Long classId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return studentService.getMyAttendance(classId, userDetails)
                .stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());
    }

    // 5. View all attendance summary
    @GetMapping("/attendance-summary")
    public List<AttendanceResponse> getAttendanceSummary(@AuthenticationPrincipal UserDetails userDetails) {
        return studentService.getAttendanceSummary(userDetails)
                .stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());
    }

    // 6. Get number of absences in a class
    @GetMapping("/absences/{classId}")
    public long countAbsences(@PathVariable Long classId,
                              @AuthenticationPrincipal UserDetails userDetails) {
        return studentService.countAbsences(classId, userDetails);
    }

    // 7. Get attendance percentage in a class
    @GetMapping("/attendance-percentage/{classId}")
    public double getAttendancePercentage(@PathVariable Long classId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        return studentService.getAttendancePercentage(classId, userDetails);
    }

    // ===== DTO Mappers =====

    private ClassResponse mapToClassResponse(Klass klass) {
        ClassResponse dto = new ClassResponse();
        dto.setId(klass.getId());
        dto.setName(klass.getName());
        dto.setDescription(klass.getDescription());
        dto.setClassTime(klass.getClassTime());
        dto.setMaxAbsencesAllowed(klass.getMaxAbsencesAllowed());
        return dto;
    }

    private AttendanceResponse mapToAttendanceResponse(Attendance attendance) {
        AttendanceResponse dto = new AttendanceResponse();
        dto.setId(attendance.getId());
        dto.setClassId(attendance.getKlass().getId());
        dto.setStudentId(attendance.getStudent().getId());
        dto.setDate(attendance.getDate());
        dto.setStatus(attendance.getStatus());
        return dto;
    }
}
