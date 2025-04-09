package Capstone.QR.controller;

import Capstone.QR.dto.Response.ApiResponse;
import Capstone.QR.model.Klass;
import Capstone.QR.model.Teacher;
import Capstone.QR.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/approve-teacher/{teacherId}")
    public ResponseEntity<ApiResponse<String>> approveTeacher(@PathVariable Long teacherId) {
        adminService.approveTeacher(teacherId);
        return ResponseEntity.ok(new ApiResponse<>("Teacher approved successfully", null));
    }

    @DeleteMapping("/reject-teacher/{teacherId}")
    public ResponseEntity<ApiResponse<String>> rejectTeacher(@PathVariable Long teacherId) {
        adminService.rejectTeacher(teacherId);
        return ResponseEntity.ok(new ApiResponse<>("Teacher rejected successfully", null));
    }

    @DeleteMapping("/delete-class/{classId}")
    public ResponseEntity<ApiResponse<String>> deleteClass(@PathVariable Long classId) {
        adminService.deleteClass(classId);
        return ResponseEntity.ok(new ApiResponse<>("Class deleted successfully", null));
    }

    @GetMapping("/pending-teachers")
    public ResponseEntity<ApiResponse<List<Teacher>>> getPendingTeachers() {
        List<Teacher> teachers = adminService.getPendingTeachers();
        return ResponseEntity.ok(new ApiResponse<>("Pending teachers fetched", teachers));
    }

    @GetMapping("/all-classes")
    public ResponseEntity<ApiResponse<List<Klass>>> getAllClasses() {
        List<Klass> classes = adminService.getAllClasses();
        return ResponseEntity.ok(new ApiResponse<>("All classes fetched", classes));
    }

    @DeleteMapping("/remove-student/{classId}/{studentId}")
    public ResponseEntity<ApiResponse<String>> removeStudentFromClass(@PathVariable Long classId,
                                                                      @PathVariable Long studentId) {
        adminService.removeStudentFromClass(classId, studentId);
        return ResponseEntity.ok(new ApiResponse<>("Student removed from class", null));
    }
}
