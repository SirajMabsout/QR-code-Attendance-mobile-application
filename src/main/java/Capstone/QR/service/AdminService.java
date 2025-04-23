package Capstone.QR.service;


import Capstone.QR.dto.Response.AdminClassResponse;
import Capstone.QR.dto.Response.PendingTeacherResponse;
import Capstone.QR.dto.Response.StudentInClassResponse;
import Capstone.QR.model.Klass;
import Capstone.QR.model.KlassStudent;
import Capstone.QR.model.Teacher;
import Capstone.QR.model.Student;
import Capstone.QR.repository.KlassRepository;
import Capstone.QR.repository.KlassStudentRepository;
import Capstone.QR.repository.StudentRepository;
import Capstone.QR.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final TeacherRepository teacherRepository;
    private final KlassRepository klassRepository;
    private final KlassStudentRepository klassStudentRepository;

    public void approveTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        teacher.setApproved(true);
        teacherRepository.save(teacher);
    }

    public void rejectTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        teacherRepository.delete(teacher);
    }

    public void deleteClass(Long classId) {
        klassRepository.deleteById(classId);
    }

    public List<PendingTeacherResponse> getPendingTeachers() {
        return teacherRepository.findByApprovedFalse().stream()
                .map(teacher -> new PendingTeacherResponse(
                        teacher.getId(),
                        teacher.getName(),
                        teacher.getEmail()
                ))
                .toList();
    }


    public List<StudentInClassResponse> getStudentsInClass(Long classId) {
        List<KlassStudent> klassStudents = klassStudentRepository.findByKlassIdAndApprovedTrue(classId);

        return klassStudents.stream()
                .map(ks -> new StudentInClassResponse(
                        ks.getStudent().getId(),
                        ks.getStudent().getName(),
                        ks.getStudent().getEmail(),
                        ks.getStudent().getProfileImageUrl()))
                .collect(Collectors.toList());
    }



    public List<AdminClassResponse> getAllClassesForAdmin() {
        List<Klass> classes = klassRepository.findAll();

        return classes.stream().map(klass -> {
            boolean isFinished = klass.getEndDate().isBefore(LocalDate.now());
            String teacherEmail = klass.getTeacher().getEmail(); // assuming you have this relation
            return new AdminClassResponse(
                    klass.getId(),
                    klass.getName(),
                    teacherEmail,
                    isFinished
            );
        }).toList();
    }


    // === Unregister student from class ===
    public void removeStudentFromClass(Long classId, Long studentId) {
        KlassStudent join = klassStudentRepository.findByKlassIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new RuntimeException("Student is not enrolled in this class"));

        klassStudentRepository.delete(join);
    }

}
