package Capstone.QR.repository;

import Capstone.QR.model.AttendanceRequest;
import Capstone.QR.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRequestRepository extends JpaRepository<AttendanceRequest, Long> {
    List<AttendanceRequest> findBySessionIdAndStatus(Long sessionId, RequestStatus status);
    // In AttendanceRequestRepository.java
    Optional<AttendanceRequest> findByStudentIdAndSessionId(Long studentId, Long sessionId);

    @Modifying
    @Query("DELETE FROM AttendanceRequest ar WHERE ar.session.id IN :sessionIds")
    void deleteBySessionIds(@Param("sessionIds") List<Long> sessionIds);
    List<AttendanceRequest> findAllByStudentId(Long id);
}
