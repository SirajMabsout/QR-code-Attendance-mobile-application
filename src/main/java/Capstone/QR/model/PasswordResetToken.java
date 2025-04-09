package Capstone.QR.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String code;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Date expiryDate;
    private boolean verified = false;


}
