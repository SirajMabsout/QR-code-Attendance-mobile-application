package Capstone.QR.service;

import Capstone.QR.model.DeviceUsage;
import Capstone.QR.repository.DeviceUsageRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceUsageService {

    private static final long USAGE_LIMIT_MILLIS = 15 * 60 * 1000; // 15 minutes

    private final DeviceUsageRepository deviceUsageRepository;



    public boolean isUsageExpired(String deviceId) {
        DeviceUsage usage = deviceUsageRepository.findById(deviceId).orElse(null);
        if (usage == null) {
            usage = new DeviceUsage();
            usage.setDeviceId(deviceId);
            usage.setFirstUsedAt(System.currentTimeMillis());
            usage.setExpired(false);
            deviceUsageRepository.save(usage);
            return false;
        }

        if (usage.isExpired()) return true;

        long elapsed = System.currentTimeMillis() - usage.getFirstUsedAt();
        if (elapsed >= USAGE_LIMIT_MILLIS) {
            usage.setExpired(true);
            deviceUsageRepository.save(usage);
            return true;
        }

        return false;
    }
}
