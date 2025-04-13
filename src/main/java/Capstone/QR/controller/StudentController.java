package Capstone.QR.controller;

import Capstone.QR.dto.Request.ScanQrRequest;
import Capstone.QR.dto.Response.ApiResponse;
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
    public ResponseEntity<ApiResponse<String>> joinClass(@PathVariable String classCode,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        String message = studentService.requestJoinClass(userDetails.getUsername(), classCode);
        return ResponseEntity.ok(new ApiResponse<>(message, null));
    }

    @GetMapping("/my-classes")
    public ResponseEntity<ApiResponse<List<ClassResponse>>> getMyClasses(@AuthenticationPrincipal UserDetails userDetails) {
        List<ClassResponse> classes = studentService.getMyClasses(userDetails)
                .stream()
                .map(this::mapToClassResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>("Classes fetched successfully", classes));
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<String>> scanQr(@RequestBody ScanQrRequest qrScanRequest,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        String resultMessage = studentService.scanQr(
                qrScanRequest.getQrCodeData(),
                qrScanRequest.getLatitude(),
                qrScanRequest.getLongitude(),
                qrScanRequest.getNetworkName(),
                userDetails
        );
        return ResponseEntity.ok(new ApiResponse<>(resultMessage, null));
    }

    @GetMapping("/attendance/{classId}")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getMyAttendance(@PathVariable Long classId,
                                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        List<AttendanceResponse> list = studentService.getMyAttendance(classId, userDetails)
                .stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>("Attendance fetched", list));
    }

    @GetMapping("/attendance-summary")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceSummary(@AuthenticationPrincipal UserDetails userDetails) {
        List<AttendanceResponse> list = studentService.getAttendanceSummary(userDetails)
                .stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>("Attendance summary fetched", list));
    }

    @GetMapping("/absences/{classId}")
    public ResponseEntity<ApiResponse<Long>> countAbsences(@PathVariable Long classId,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        long count = studentService.countAbsences(classId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Absences counted", count));
    }

    @GetMapping("/attendance-percentage/{classId}")
    public ResponseEntity<ApiResponse<Double>> getAttendancePercentage(@PathVariable Long classId,
                                                                       @AuthenticationPrincipal UserDetails userDetails) {
        double percentage = studentService.getAttendancePercentage(classId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Attendance percentage calculated", percentage));
    }

    @GetMapping("/pending-join-requests")
    public ResponseEntity<ApiResponse<Object>> getPendingJoinRequests(@AuthenticationPrincipal UserDetails userDetails) {
        Object data = studentService.getPendingJoinRequests(userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Pending join requests fetched", data));
    }

    @DeleteMapping("/cancel-join-request/{classId}")
    public ResponseEntity<ApiResponse<String>> cancelJoinRequest(@PathVariable Long classId,
                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        studentService.cancelJoinRequest(classId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Join request cancelled successfully", null));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<Object>> getClassDetails(@PathVariable Long classId,
                                                               @AuthenticationPrincipal UserDetails userDetails) {
        Object details = studentService.getClassDetail(classId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Class details fetched", details));
    }

    @GetMapping("/attendance-requests")
    public ResponseEntity<ApiResponse<Object>> getMyAttendanceRequests(@AuthenticationPrincipal UserDetails userDetails) {
        Object data = studentService.getMyAttendanceRequests(userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Attendance requests fetched", data));
    }

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
        dto.setDurationMinutes(klass.getDurationMinutes());
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
