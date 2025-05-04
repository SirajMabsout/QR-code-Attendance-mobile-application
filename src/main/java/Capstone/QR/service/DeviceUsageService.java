package Capstone.QR.service;

import Capstone.QR.model.DeviceUsage;
import Capstone.QR.repository.DeviceUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceUsageService {


    private static final long LIMIT = 15 * 60 * 1000;
    private final DeviceUsageRepository repository;

    public boolean isUsageExpired(String deviceId, long usedMillisThisSession) {
        DeviceUsage usage = repository.findById(deviceId)
                .orElse(new DeviceUsage(deviceId, 0L));

        usage.setUsedMillis(usage.getUsedMillis() + usedMillisThisSession);
        repository.save(usage);

        return usage.getUsedMillis() >= LIMIT;
    }
}


