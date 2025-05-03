package Capstone.QR.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class EmailVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String Email;
    private String name;
    private String encodedPassword;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
