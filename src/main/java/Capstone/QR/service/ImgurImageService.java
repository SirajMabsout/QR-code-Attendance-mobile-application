package Capstone.QR.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImgurImageService {

    @Value("${imgur.client.id}")
    private String clientId;

    public String uploadImage(MultipartFile image) throws IOException, InterruptedException {
        String boundary = UUID.randomUUID().toString();
        byte[] imageBytes = image.getBytes();

        String filePartHeader = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"image\"; filename=\"" + image.getOriginalFilename() + "\"\r\n" +
                "Content-Type: " + image.getContentType() + "\r\n\r\n";

        String closingBoundary = "\r\n--" + boundary + "--\r\n";

        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofByteArrays(
                List.of(
                        filePartHeader.getBytes(),
                        imageBytes,
                        closingBoundary.getBytes()
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.imgur.com/3/image"))
                .header("Authorization", "Client-ID " + clientId)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(bodyPublisher)
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = null;
        Exception lastException = null;
        int maxRetries = 2;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() < 400) {
                    break; // Success
                } else {
                    throw new IOException("Imgur upload failed with HTTP " + response.statusCode());
                }

            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    Thread.sleep(1000); // Optional backoff between attempts
                }
            }
        }

        if (response == null || response.statusCode() >= 400) {
            throw new RuntimeException("Image upload failed after retries: " +
                    (lastException != null ? lastException.getMessage() : "Unknown error"));
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        if (!root.path("success").asBoolean()) {
            throw new RuntimeException("Imgur upload returned failure: " + root.toPrettyString());
        }

        return root.path("data").path("link").asText();
    }
}
