package Capstone.QR.dto.Response;


import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private String message;
    private T data;
}
