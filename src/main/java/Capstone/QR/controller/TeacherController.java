package Capstone.QR.controller;

import Capstone.QR.dto.Request.CreateClassRequest;
import Capstone.QR.dto.Request.GenerateQrRequest;
import Capstone.QR.dto.Response.*;
import Capstone.QR.model.*;
import Capstone.QR.service.AttendanceExportService;
import Capstone.QR.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final TeacherService teacherService;
    private final AttendanceExportService attendanceExportService;

    // ==================== Class Management ====================

    @PostMapping("/create-class")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse createClass(@RequestBody @Valid CreateClassRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        return teacherService.createClass(request, userDetails);
    }

    @GetMapping("/my-classes")
    public List<ClassResponse> getAllClasses(@AuthenticationPrincipal UserDetails userDetails) {
        return teacherService.getAllClasses(userDetails);
    }

    @GetMapping("/class/{classId}")
    public ClassDetailResponse getClassDetails(@PathVariable Long classId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        return teacherService.getClassDetails(classId, userDetails);
    }


    @GetMapping("/session/{sessionId}")
    public SessionDetailResponse getSessionDetails(@PathVariable Long sessionId,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return teacherService.getSessionDetails(sessionId, userDetails);
    }



    // ==================== Join Requests ====================

    @PostMapping("/approve-join/{classId}/{studentId}")
    public ResponseEntity<?> approveStudent(@PathVariable Long classId,
                                            @PathVariable Long studentId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        teacherService.approveStudentJoin(classId, studentId, userDetails);
        return ResponseEntity.ok("Student approved for class.");
    }

    @DeleteMapping("/reject-join/{classId}/{studentId}")
    public ResponseEntity<?> rejectStudent(@PathVariable Long classId,
                                           @PathVariable Long studentId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        teacherService.rejectStudentJoin(classId, studentId, userDetails);
        return ResponseEntity.ok("Join request rejected.");
    }

    @GetMapping("/pending-join-requests/{classId}")
    public ResponseEntity<?> getPendingJoinRequests(@PathVariable Long classId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teacherService.getPendingJoinRequests(classId, userDetails));
    }

    // ==================== QR Code for Session ====================

    @PostMapping("/session/{sessionId}/generate-qr")
    public QRCodeResponse generateQrCode(@PathVariable Long sessionId,
                                         @RequestBody @Valid GenerateQrRequest qrRequest, @AuthenticationPrincipal UserDetails userDetails) {
        QRCode qrCode = teacherService.generateQrCodeForSession(sessionId, qrRequest.getLatitude(), qrRequest.getLongitude(), userDetails);
        return mapToQRCodeResponse(qrCode);
    }

    @GetMapping("/class/{classId}/student/{studentId}/stats")
    public StudentClassAttendanceStatsResponse getStudentStatsForClass(@PathVariable Long classId,
                                                                       @PathVariable Long studentId,
                                                                       @AuthenticationPrincipal UserDetails userDetails) {
        return teacherService.getStudentClassStats(classId, studentId, userDetails);
    }


    // ==================== Attendance Per Session ====================

    @GetMapping("/session/{sessionId}/attendance/{studentId}")
    public List<AttendanceResponse> getSessionAttendance(@PathVariable Long sessionId,
                                                         @PathVariable Long studentId,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return teacherService.getSessionAttendance(sessionId, studentId, userDetails)
                .stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/session/{sessionId}/attendance/{attendanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editAttendance(@PathVariable Long sessionId,
                               @PathVariable Long attendanceId,
                               @RequestParam AttendanceStatus newStatus,
                               @AuthenticationPrincipal UserDetails userDetails) {
        teacherService.editAttendance(sessionId, attendanceId, newStatus, userDetails);
    }

    // ==================== Attendance Requests (Session-Based) ====================

    @PostMapping("/session/{sessionId}/approve-request/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveRequest(@PathVariable Long sessionId,
                               @PathVariable Long requestId,
                               @AuthenticationPrincipal UserDetails userDetails) {
        teacherService.approveAttendanceRequest(requestId, sessionId, userDetails);
    }

    @PostMapping("/session/{sessionId}/reject-request/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectRequest(@PathVariable Long sessionId,
                              @PathVariable Long requestId,
                              @AuthenticationPrincipal UserDetails userDetails) {
        teacherService.rejectAttendanceRequest(requestId, sessionId, userDetails);
    }

    @GetMapping("/session/{sessionId}/pending-requests")
    public List<AttendanceRequestResponse> getPendingSessionRequests(@PathVariable Long sessionId,
                                                                     @AuthenticationPrincipal UserDetails userDetails) {
        return teacherService.getPendingSessionRequests(sessionId, userDetails)
                .stream()
                .map(this::mapToAttendanceRequestResponse)
                .collect(Collectors.toList());
    }

    // ==================== Export ====================

    @GetMapping("/export-attendance-excel/{classId}")
    public ResponseEntity<byte[]> exportAttendanceExcel(@PathVariable Long classId) {
        try {
            ByteArrayInputStream stream = attendanceExportService.exportAttendanceSheet(classId);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=attendance_" + classId + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(stream.readAllBytes());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ==================== DTO Mappers ====================

    private AttendanceResponse mapToAttendanceResponse(Attendance attendance) {
        AttendanceResponse dto = new AttendanceResponse();
        dto.setId(attendance.getId());
        dto.setClassId(attendance.getSession().getKlass().getId());
        dto.setStudentId(attendance.getStudent().getId());
        dto.setRecordedAt(attendance.getRecordedAt());
        dto.setStatus(attendance.getStatus());
        return dto;
    }

    private QRCodeResponse mapToQRCodeResponse(QRCode qr) {
        QRCodeResponse dto = new QRCodeResponse();
        dto.setId(qr.getId());
        dto.setClassId(qr.getSession().getKlass().getId());
        dto.setQrCodeData(qr.getQrCodeData());
        dto.setSessionDate(qr.getSessionDate());
        dto.setExpiresAt(qr.getExpiresAt());
        dto.setLatitude(qr.getLatitude());
        dto.setLongitude(qr.getLongitude());
        return dto;
    }

    private AttendanceRequestResponse mapToAttendanceRequestResponse(AttendanceRequest req) {
        AttendanceRequestResponse dto = new AttendanceRequestResponse();
        dto.setId(req.getId());
        dto.setStudentId(req.getStudent().getId());
        dto.setClassId(req.getSession().getKlass().getId());
        dto.setRequestedAt(req.getRequestedAt());
        dto.setStatus(req.getStatus());
        return dto;
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
