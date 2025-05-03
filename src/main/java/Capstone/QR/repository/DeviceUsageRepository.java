package Capstone.QR.repository;

import Capstone.QR.model.DeviceUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceUsageRepository extends JpaRepository<DeviceUsage, String> {
}
