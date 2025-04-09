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
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        if (!root.path("success").asBoolean()) {
            throw new RuntimeException("Imgur upload failed: " + root.toPrettyString());
        }
        return root.path("data").path("link").asText();

    }
}
