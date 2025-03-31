package Capstone.QR.controller;

import Capstone.QR.dto.CreateClassRequest;
import Capstone.QR.dto.Response.AttendanceRequestResponse;
import Capstone.QR.dto.Response.AttendanceResponse;
import Capstone.QR.dto.Response.ClassResponse;
import Capstone.QR.dto.Response.QRCodeResponse;
import Capstone.QR.model.*;
import Capstone.QR.service.AttendanceExportService;
import Capstone.QR.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final TeacherService teacherService;
    private final AttendanceExportService attendanceExportService;

    // 1. Create class
    @PostMapping("/create-class")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse createClass(@RequestBody CreateClassRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        return teacherService.createClass(request, userDetails);
    }

    // 2. List all classes for this teacher
    @GetMapping("/my-classes")
    public List<ClassResponse> getAllClasses(@AuthenticationPrincipal UserDetails userDetails) {
        return teacherService.getAllClasses(userDetails);
    }

    // 3. Get details of a specific class (optional: add DTO)
    @GetMapping("/class/{classId}")
    public ClassResponse getClassDetails(@PathVariable Long classId,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        return teacherService.getClassDetails(classId,userDetails);    }


    @PostMapping("/approve-join/{classId}/{studentId}")
    public ResponseEntity<?> approveStudent(@PathVariable Long classId,
                                            @PathVariable Long studentId,
                                            @AuthenticationPrincipal UserDetails userDetails) throws AccessDeniedException {
        teacherService.approveStudentJoin(userDetails.getUsername(), classId, studentId);
        return ResponseEntity.ok("Student approved for class.");
    }

    @DeleteMapping("/reject-join/{classId}/{studentId}")
    public ResponseEntity<?> rejectStudent(@PathVariable Long classId,
                                           @PathVariable Long studentId,
                                           @AuthenticationPrincipal
                                               UserDetails userDetails) throws AccessDeniedException {
        teacherService.rejectStudentJoin(userDetails.getUsername(), classId, studentId);
        return ResponseEntity.ok("Join request rejected.");
    }

    @GetMapping("/pending-join-requests/{classId}")
    public ResponseEntity<?> getPendingRequests(@PathVariable Long classId,
                                                @AuthenticationPrincipal UserDetails userDetails) throws AccessDeniedException {
        return ResponseEntity.ok(teacherService.getPendingJoinRequests(userDetails.getUsername(), classId));
    }

    // 6. Generate QR code
    @PostMapping("/generate-qr")
    public QRCodeResponse generateQrCode(@RequestParam Long classId,
                                         @RequestParam String sessionDateTime,
                                         @RequestParam double latitude,
                                         @RequestParam double longitude) {
        QRCode qrCode = teacherService.generateQrCode(
                classId,
                LocalDateTime.parse(sessionDateTime),
                latitude,
                longitude
        );
        return mapToQRCodeResponse(qrCode);
    }

    // 7. View student attendance
    @GetMapping("/attendance/{classId}/{studentId}")
    public List<AttendanceResponse> getStudentAttendance(@PathVariable Long classId,
                                                         @PathVariable Long studentId) {
        return teacherService.getStudentAttendance(classId, studentId)
                .stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());
    }

    // 8. Edit attendance
    @PutMapping("/attendance/{attendanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editAttendance(@PathVariable Long attendanceId,
                               @RequestParam AttendanceStatus newStatus) {
        teacherService.editAttendance(attendanceId, newStatus);
    }


    // 10. Approve attendance request
    @PostMapping("/approve-request/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveRequest(@PathVariable Long requestId) {
        teacherService.approveAttendanceRequest(requestId);
    }

    // 11. Reject attendance request
    @PostMapping("/reject-request/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectRequest(@PathVariable Long requestId) {
        teacherService.rejectAttendanceRequest(requestId);
    }

    // 12. List pending attendance requests for a class
    @GetMapping("/pending-requests/{classId}")
    public List<AttendanceRequestResponse> getPendingRequests(@PathVariable Long classId) {
        return teacherService.getPendingRequests(classId)
                .stream()
                .map(this::mapToAttendanceRequestResponse)
                .collect(Collectors.toList());
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }


    // ===== DTO Mappers =====

    private AttendanceResponse mapToAttendanceResponse(Attendance attendance) {
        AttendanceResponse dto = new AttendanceResponse();
        dto.setId(attendance.getId());
        dto.setClassId(attendance.getKlass().getId());
        dto.setStudentId(attendance.getStudent().getId());
        dto.setDate(attendance.getDate());
        dto.setStatus(attendance.getStatus());
        return dto;
    }

    private QRCodeResponse mapToQRCodeResponse(QRCode qr) {
        QRCodeResponse dto = new QRCodeResponse();
        dto.setId(qr.getId());
        dto.setClassId(qr.getKlass().getId());
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
        dto.setClassId(req.getKlass().getId());
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

