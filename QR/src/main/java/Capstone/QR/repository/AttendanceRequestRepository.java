package Capstone.QR.repository;

import Capstone.QR.model.AttendanceRequest;
import Capstone.QR.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceRequestRepository extends JpaRepository<AttendanceRequest, Long> {
    List<AttendanceRequest> findByKlassIdAndStatus(Long classId, RequestStatus status);
}
