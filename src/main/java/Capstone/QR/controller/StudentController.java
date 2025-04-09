package Capstone.QR.controller;

import Capstone.QR.dto.Request.ScanQrRequest;
import Capstone.QR.dto.Response.AttendanceResponse;
import Capstone.QR.dto.Response.ClassResponse;
import Capstone.QR.model.Attendance;
import Capstone.QR.model.Klass;
import Capstone.QR.service.StudentService;
import jakarta.validation.Valid;
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

    @PostMapping("/join/{classCode}")
    public ResponseEntity<?> joinClass(@PathVariable String classCode,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        String message = studentService.requestJoinClass(userDetails.getUsername(), classCode);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/my-classes")
    public List<ClassResponse> getMyClasses(@AuthenticationPrincipal UserDetails userDetails) {
        return studentService.getMyClasses(userDetails)
                .stream()
                .map(this::mapToClassResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/scan")
    public ResponseEntity<String> scanQr(@RequestBody ScanQrRequest qrScanRequest,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        String resultMessage = studentService.scanQr(
                qrScanRequest.getQrCodeData(),
                qrScanRequest.getLatitude(),
                qrScanRequest.getLongitude(),
                qrScanRequest.getNetworkName(),
                userDetails
        );
        return ResponseEntity.ok(resultMessage);
    }

    @GetMapping("/attendance/{classId}")
    public List<AttendanceResponse> getMyAttendance(@PathVariable Long classId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return studentService.getMyAttendance(classId, userDetails)
                .stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/attendance-summary")
    public List<AttendanceResponse> getAttendanceSummary(@AuthenticationPrincipal UserDetails userDetails) {
        return studentService.getAttendanceSummary(userDetails)
                .stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/absences/{classId}")
    public long countAbsences(@PathVariable Long classId,
                              @AuthenticationPrincipal UserDetails userDetails) {
        return studentService.countAbsences(classId, userDetails);
    }

    @GetMapping("/attendance-percentage/{classId}")
    public double getAttendancePercentage(@PathVariable Long classId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        return studentService.getAttendancePercentage(classId, userDetails);
    }

    // 🔄 NEW ENDPOINT: Get pending join requests
    @GetMapping("/pending-join-requests")
    public ResponseEntity<?> getPendingJoinRequests(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(studentService.getPendingJoinRequests(userDetails));
    }

    // 🔄 NEW ENDPOINT: Cancel a pending join request
    @DeleteMapping("/cancel-join-request/{classId}")
    public ResponseEntity<?> cancelJoinRequest(@PathVariable Long classId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        studentService.cancelJoinRequest(classId, userDetails);
        return ResponseEntity.ok("Join request cancelled successfully.");
    }

    // 🔄 NEW ENDPOINT: View class details (with session list)
    @GetMapping("/class/{classId}")
    public ResponseEntity<?> getClassDetails(@PathVariable Long classId,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(studentService.getClassDetail(classId, userDetails));
    }

    // 🔄 NEW ENDPOINT: View attendance requests submitted by student
    @GetMapping("/attendance-requests")
    public ResponseEntity<?> getMyAttendanceRequests(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(studentService.getMyAttendanceRequests(userDetails));
    }

    // === DTO Mappers ===

    private ClassResponse mapToClassResponse(Klass klass) {
        ClassResponse dto = new ClassResponse();
        dto.setId(klass.getId());
        dto.setName(klass.getName());
        dto.setDescription(klass.getDescription());
        dto.setClassTime(klass.getClassTime());
        dto.setMaxAbsencesAllowed(klass.getMaxAbsencesAllowed());
        dto.setJoinCode(klass.getJoinCode());
        dto.setStartDate(klass.getStartDate());
        dto.setEndDate(klass.getEndDate());
        dto.setScheduledDays(klass.getScheduledDays());
        dto.setAcceptanceRadiusMeters(klass.getAcceptanceRadiusMeters());
        return dto;
    }

    private AttendanceResponse mapToAttendanceResponse(Attendance attendance) {
        AttendanceResponse dto = new AttendanceResponse();
        dto.setId(attendance.getId());
        dto.setClassId(attendance.getSession().getKlass().getId());
        dto.setSessionId(attendance.getSession().getId());
        dto.setStudentId(attendance.getStudent().getId());
        dto.setRecordedAt(attendance.getRecordedAt());
        dto.setStatus(attendance.getStatus());
        return dto;
    }
}
