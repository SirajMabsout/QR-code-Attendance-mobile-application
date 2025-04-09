package Capstone.QR.repository;

import Capstone.QR.model.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    List<ClassSession> findByKlassIdOrderBySessionDateAsc(Long klassId);
    List<ClassSession> findByKlassIdAndCanceledFalse(Long klassId);
    Optional<ClassSession> findByKlassIdAndSessionDate(Long klassId, LocalDate date);
    List<ClassSession> findByKlass_Id(Long classId);
}
