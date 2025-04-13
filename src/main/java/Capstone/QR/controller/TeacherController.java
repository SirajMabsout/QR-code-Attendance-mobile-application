package Capstone.QR.controller;

import Capstone.QR.dto.Request.AttendanceUpdateRequest;
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

    @PostMapping("/create-class")
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(@RequestBody @Valid CreateClassRequest request,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        ClassResponse created = teacherService.createClass(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("Class created successfully", created));
    }

    @GetMapping("/my-classes")
    public ResponseEntity<ApiResponse<List<ClassResponse>>> getAllClasses(@AuthenticationPrincipal UserDetails userDetails) {
        List<ClassResponse> classes = teacherService.getAllClasses(userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Classes fetched successfully", classes));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<ClassDetailResponse>> getClassDetails(@PathVariable Long classId,
                                                                            @AuthenticationPrincipal UserDetails userDetails) {
        ClassDetailResponse details = teacherService.getClassDetails(classId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Class details fetched", details));
    }

    @GetMapping("/class/{classId}/attendance-summary")
    public ResponseEntity<List<StudentClassAttendanceSummaryResponse>> getClassAttendanceSummary(
            @PathVariable Long classId
    ) {
        List<StudentClassAttendanceSummaryResponse> summary = teacherService.getClassAttendanceSummary(classId);
        return ResponseEntity.ok(summary);
    }


    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<SessionDetailResponse>> getSessionDetails(@PathVariable Long sessionId,
                                                                                @AuthenticationPrincipal UserDetails userDetails) {
        SessionDetailResponse details = teacherService.getSessionDetails(sessionId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Session details fetched", details));
    }

    @PostMapping("/approve-join/{classId}/{studentId}")
    public ResponseEntity<ApiResponse<String>> approveStudent(@PathVariable Long classId,
                                                              @PathVariable Long studentId,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        teacherService.approveStudentJoin(classId, studentId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Student approved for class", null));
    }

    @DeleteMapping("/reject-join/{classId}/{studentId}")
    public ResponseEntity<ApiResponse<String>> rejectStudent(@PathVariable Long classId,
                                                             @PathVariable Long studentId,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        teacherService.rejectStudentJoin(classId, studentId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Join request rejected", null));
    }

    @GetMapping("/pending-join-requests/{classId}")
    public ResponseEntity<ApiResponse<Object>> getPendingJoinRequests(@PathVariable Long classId,
                                                                      @AuthenticationPrincipal UserDetails userDetails) {
        Object list = teacherService.getPendingJoinRequests(classId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Pending join requests fetched", list));
    }

    @PostMapping("/session/{sessionId}/generate-qr")
    public ResponseEntity<ApiResponse<QRCodeResponse>> generateQrCode(@PathVariable Long sessionId,
                                                                      @RequestBody @Valid GenerateQrRequest qrRequest,
                                                                      @AuthenticationPrincipal UserDetails userDetails) {
        QRCode qrCode = teacherService.generateQrCodeForSession(sessionId, qrRequest.getLatitude(), qrRequest.getLongitude(), userDetails);
        return ResponseEntity.ok(new ApiResponse<>("QR Code generated", mapToQRCodeResponse(qrCode)));
    }

    @GetMapping("/class/{classId}/student/{studentId}/stats")
    public ResponseEntity<ApiResponse<StudentClassAttendanceStatsResponse>> getStudentStatsForClass(@PathVariable Long classId,
                                                                                                    @PathVariable Long studentId,
                                                                                                    @AuthenticationPrincipal UserDetails userDetails) {
        StudentClassAttendanceStatsResponse stats = teacherService.getStudentClassStats(classId, studentId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Student stats fetched", stats));
    }

    @GetMapping("/session/{sessionId}/attendance/{studentId}")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getSessionAttendance(@PathVariable Long sessionId,
                                                                                      @PathVariable Long studentId,
                                                                                      @AuthenticationPrincipal UserDetails userDetails) {
        List<AttendanceResponse> responses = teacherService.getSessionAttendance(sessionId, studentId, userDetails)
                .stream().map(this::mapToAttendanceResponse).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>("Attendance list fetched", responses));
    }

    @PutMapping("/session/{sessionId}/attendance/{attendanceId}")
    public ResponseEntity<ApiResponse<String>> editAttendance(@PathVariable Long sessionId,
                                                              @PathVariable Long attendanceId,
                                                              @RequestBody AttendanceUpdateRequest request,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        teacherService.editAttendance(sessionId, attendanceId, request.getStatus(), userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Attendance updated", null));
    }


    @PostMapping("/session/{sessionId}/approve-request/{requestId}")
    public ResponseEntity<ApiResponse<String>> approveRequest(@PathVariable Long sessionId,
                                                              @PathVariable Long requestId,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        teacherService.approveAttendanceRequest(requestId, sessionId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Attendance request approved", null));
    }

    @PostMapping("/session/{sessionId}/reject-request/{requestId}")
    public ResponseEntity<ApiResponse<String>> rejectRequest(@PathVariable Long sessionId,
                                                             @PathVariable Long requestId,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        teacherService.rejectAttendanceRequest(requestId, sessionId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Attendance request rejected", null));
    }

    @GetMapping("/session/{sessionId}/pending-requests")
    public ResponseEntity<ApiResponse<List<AttendanceRequestResponse>>> getPendingSessionRequests(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<AttendanceRequestResponse> list = teacherService.getPendingSessionRequests(sessionId, userDetails);
        return ResponseEntity.ok(new ApiResponse<>("Pending attendance requests fetched", list));
    }


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
