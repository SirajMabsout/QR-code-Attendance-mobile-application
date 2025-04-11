package Capstone.QR.dto.Request;


import Capstone.QR.model.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceUpdateRequest {
    private AttendanceStatus status;


}
