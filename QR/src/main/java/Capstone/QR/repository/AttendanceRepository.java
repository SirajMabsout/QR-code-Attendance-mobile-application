package Capstone.QR.repository;

import Capstone.QR.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// === AttendanceRepository ===
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByKlassId(Long klassId); // ✅ correct

    List<Attendance> findByKlassIdAndStudentId(Long KlassId, Long studentId);
}

