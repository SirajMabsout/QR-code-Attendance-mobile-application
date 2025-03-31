package Capstone.QR.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Klass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    private Teacher teacher;

    @ManyToMany
    private List<Student> students;

    @OneToMany(mappedBy = "klass")
    private List<Attendance> attendanceRecords;

    private String description;

    private int maxAbsencesAllowed;

    // ✅ New scheduling fields:
    private LocalDate startDate;           // e.g. 2025-04-01
    private LocalDate endDate;             // e.g. 2025-07-31

    @ElementCollection
    @CollectionTable(name = "klass_days", joinColumns = @JoinColumn(name = "klass_id"))
    @Column(name = "day_of_week")
    private List<DayOfWeek> scheduledDays; // e.g. MONDAY, WEDNESDAY

    private LocalTime classTime;           // e.g. 10:00 AM
}
