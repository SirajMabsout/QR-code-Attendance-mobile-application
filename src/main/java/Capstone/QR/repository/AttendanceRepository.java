package Capstone.QR.repository;

import Capstone.QR.model.Attendance;
import Capstone.QR.model.AttendanceStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// === AttendanceRepository ===
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findBySession_Klass_Id(Long klassId); // ✅ correct

    List<Attendance> findBySession_Klass_IdAndStudent_Id(Long KlassId, Long studentId);

    List<Attendance> findAllBySession_IdAndStudent_Id(Long sessionId, Long studentId);

    List<Attendance> findBySession_Id(Long sessionId);

    Optional<Attendance> findBySession_IdAndStudent_Id(Long sessionId, Long studentId);

    long countByKlass_IdAndStudent_IdAndStatus(Long klassId, Long studentId, AttendanceStatus status);

    @Transactional
    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.session.id IN :sessionIds")
    void deleteBySessionIds(@Param("sessionIds") List<Long> sessionIds);


}

