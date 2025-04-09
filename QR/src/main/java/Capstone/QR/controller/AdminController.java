package Capstone.QR.controller;

import Capstone.QR.model.Klass;
import Capstone.QR.model.Teacher;
import Capstone.QR.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/approve-teacher/{teacherId}")
    public ResponseEntity<?> approveTeacher(@PathVariable Long teacherId) {
        adminService.approveTeacher(teacherId);
        return ResponseEntity.ok(Map.of("message", "Teacher approved successfully"));
    }

    @DeleteMapping("/reject-teacher/{teacherId}")
    public ResponseEntity<?> rejectTeacher(@PathVariable Long teacherId) {
        adminService.rejectTeacher(teacherId);
        return ResponseEntity.ok(Map.of("message", "Teacher rejected successfully"));
    }

    @DeleteMapping("/delete-class/{classId}")
    public ResponseEntity<?> deleteClass(@PathVariable Long classId) {
        adminService.deleteClass(classId);
        return ResponseEntity.ok(Map.of("message", "Class deleted successfully"));
    }

    @GetMapping("/pending-teachers")
    public List<Teacher> getPendingTeachers() {
        return adminService.getPendingTeachers(); // Could be mapped to TeacherDTO
    }

    @GetMapping("/all-classes")
    public List<Klass> getAllClasses() {
        return adminService.getAllClasses(); // Could be mapped to ClassDTO
    }

    @DeleteMapping("/remove-student/{classId}/{studentId}")
    public ResponseEntity<?> removeStudentFromClass(@PathVariable Long classId,
                                                    @PathVariable Long studentId) {
        adminService.removeStudentFromClass(classId, studentId);
        return ResponseEntity.ok(Map.of("message", "Student removed from class"));
    }
}
