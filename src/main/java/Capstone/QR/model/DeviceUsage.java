package Capstone.QR.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class DeviceUsage {

    @Id
    private String deviceId;

    private Long firstUsedAt;

    private boolean expired = false;


}
