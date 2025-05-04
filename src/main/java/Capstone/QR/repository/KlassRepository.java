package Capstone.QR.repository;

import Capstone.QR.model.Klass;
import Capstone.QR.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KlassRepository extends JpaRepository<Klass, Long> {
    List<Klass> findByTeacher(Teacher teacher);

    Optional<Klass> findByJoinCode(String joinCode);


    boolean existsByTeacherAndName(Teacher teacher, String name);
}

