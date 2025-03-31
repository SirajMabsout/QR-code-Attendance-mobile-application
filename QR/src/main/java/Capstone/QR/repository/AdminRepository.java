package Capstone.QR.repository;

import Capstone.QR.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// === AdminRepository ===
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {}

