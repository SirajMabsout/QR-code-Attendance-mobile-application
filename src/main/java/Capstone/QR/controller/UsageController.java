package Capstone.QR.controller;

import Capstone.QR.dto.Request.DeviceUsageRequest;
import Capstone.QR.dto.Response.ApiResponse;
import Capstone.QR.service.DeviceUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {


    private final DeviceUsageService deviceUsageService;

    @PostMapping("/track")
    public ResponseEntity<ApiResponse<Boolean>> trackDeviceUsage(@RequestBody DeviceUsageRequest request) {
        boolean expired = deviceUsageService.isUsageExpired(request.getDeviceId());

        ApiResponse<Boolean> response = new ApiResponse<>(
                expired ? "Access expired for this device" : "Access is still valid",
                expired
        );

        return ResponseEntity.ok(response);
    }
}
