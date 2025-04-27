package Capstone.QR.repository;

import Capstone.QR.model.KlassStudent;
import Capstone.QR.model.Student;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KlassStudentRepository extends JpaRepository<KlassStudent, Long> {

    Optional<KlassStudent> findByKlassIdAndStudentId(Long klassId, Long studentId);

    List<KlassStudent> findAllByKlassIdAndApprovedFalse(Long klassId);

    List<KlassStudent> findAllByKlassIdAndApprovedTrue(Long klassId);

    List<KlassStudent> findAllByStudentIdAndApprovedTrue(Long studentId);

    List<KlassStudent> findAllByStudentIdAndApprovedFalse(Long studentId);

    List<KlassStudent> findByKlassIdAndApprovedTrue(Long klassId);

    @Query("""
                SELECT ks.student FROM KlassStudent ks
                WHERE ks.klass.id = :classId
                  AND ks.approved = true
            """)
    List<Student> findApprovedStudentsByClassId(@Param("classId") Long classId);


        @Transactional
        @Modifying
        @Query("DELETE FROM KlassStudent ks WHERE ks.klass.id = :klassId")
        void deleteByKlassId(@Param("klassId") Long klassId);




}