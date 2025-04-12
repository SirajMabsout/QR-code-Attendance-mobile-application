package Capstone.QR.repository;

import Capstone.QR.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// === AttendanceRepository ===
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findBySession_Klass_Id(Long klassId); // ✅ correct

    List<Attendance> findBySession_Klass_IdAndStudent_Id(Long KlassId, Long studentId);

    List<Attendance> findBySession_IdAndStudent_Id(Long sessionId, Long studentId);

    List<Attendance> findBySession_Id(Long sessionId);
    Optional<Attendance> findBySessionIdAndStudentId(Long sessionId, Long studentId);

}

