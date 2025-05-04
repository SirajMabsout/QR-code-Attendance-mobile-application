package Capstone.QR.repository;

import Capstone.QR.model.ClassSession;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    List<ClassSession> findByKlassIdOrderBySessionDateAsc(Long klassId);

    List<ClassSession> findByKlassIdAndCanceledFalse(Long klassId);

    Optional<ClassSession> findByKlassIdAndSessionDate(Long klassId, LocalDate date);

    List<ClassSession> findByKlass_Id(Long classId);

    @Query("SELECT s FROM ClassSession s JOIN s.klass.klassStudents ks " +
            "WHERE ks.student.id = :studentId AND ks.approved = true AND s.sessionDate = :date")
    List<ClassSession> findSessionsByStudentIdAndDate(@Param("studentId") Long studentId, @Param("date") LocalDate date);


    @Query("SELECT s FROM ClassSession s WHERE s.klass.teacher.id = :teacherId AND s.sessionDate = :date")
    List<ClassSession> findAllByTeacherIdAndDate(@Param("teacherId") Long teacherId, @Param("date") LocalDate date);


    @Transactional
    @Modifying
    @Query("DELETE FROM ClassSession cs WHERE cs.klass.id = :klassId")
    void deleteByKlassId(@Param("klassId") Long klassId);


    @Query("SELECT cs.id FROM ClassSession cs WHERE cs.klass.id = :klassId")
    List<Long> findSessionIdsByClassId(@Param("klassId") Long klassId);
}
