package Capstone.QR.controller;

import Capstone.QR.model.Klass;
import Capstone.QR.model.Teacher;
import Capstone.QR.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/approve-teacher/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveTeacher(@PathVariable Long teacherId) {
        adminService.approveTeacher(teacherId);
    }

    @DeleteMapping("/reject-teacher/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectTeacher(@PathVariable Long teacherId) {
        adminService.rejectTeacher(teacherId);
    }

    @DeleteMapping("/delete-class/{classId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClass(@PathVariable Long classId) {
        adminService.deleteClass(classId);
    }

    @GetMapping("/pending-teachers")
    public List<Teacher> getPendingTeachers() {
        return adminService.getPendingTeachers(); // Could map to TeacherDTO if needed
    }

    @GetMapping("/all-classes")
    public List<Klass> getAllClasses() {
        return adminService.getAllClasses(); // Could map to ClassDTO if needed
    }

    @DeleteMapping("/remove-student/{classId}/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudentFromClass(@PathVariable Long classId, @PathVariable Long studentId) {
        adminService.removeStudentFromClass(classId, studentId);
    }
}
