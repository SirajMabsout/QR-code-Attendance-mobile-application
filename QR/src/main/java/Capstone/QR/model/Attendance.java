package Capstone.QR.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Student student;

    @ManyToOne
    @JoinColumn(name = "klass_id")
    private Klass klass; // ✅ correct name


    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;
}