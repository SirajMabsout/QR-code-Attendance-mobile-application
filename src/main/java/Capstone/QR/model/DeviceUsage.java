package Capstone.QR.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;

@Entity
@Data
@AllArgsConstructor
public class DeviceUsage {

    @Id
    private String deviceId;

    private Long usedMillis;




}
