package Capstone.QR.controller;


import Capstone.QR.dto.Request.ChatQueryRequest;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Value("${chatbot.api.url}")
    private String chatbotUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/ask")
    public ResponseEntity<?> askChatbot(@RequestBody ChatQueryRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ChatQueryRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    chatbotUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Chatbot failed: " + e.getMessage()));
        }
    }
}
