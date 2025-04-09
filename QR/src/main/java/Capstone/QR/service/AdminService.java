package Capstone.QR.service;


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

import java.util.List;
import java.util.UUID;

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

    public List<Teacher> getPendingTeachers() {
        return teacherRepository.findByApprovedFalse();
    }

    public List<Klass> getAllClasses() {
        return klassRepository.findAll();
    }

    // === Unregister student from class ===
    public void removeStudentFromClass(Long classId, Long studentId) {
        KlassStudent join = klassStudentRepository.findByKlassIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new RuntimeException("Student is not enrolled in this class"));

        klassStudentRepository.delete(join);
    }

}
