package Capstone.QR.repository;

import Capstone.QR.model.KlassStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KlassStudentRepository extends JpaRepository<KlassStudent, Long> {

    Optional<KlassStudent> findByKlassIdAndStudentId(Long klassId, Long studentId);

    List<KlassStudent> findAllByKlassIdAndApprovedFalse(Long klassId);

    List<KlassStudent> findAllByKlassIdAndApprovedTrue(Long klassId);
}
