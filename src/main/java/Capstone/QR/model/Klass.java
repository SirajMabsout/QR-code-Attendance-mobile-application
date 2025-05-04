package Capstone.QR.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

    @OneToMany(mappedBy = "klass", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KlassStudent> klassStudents;


    private String description;

    private int maxAbsencesAllowed;

    @Column(nullable = false)
    private int durationMinutes;

    private LocalDate startDate;
    private LocalDate endDate;
    @ElementCollection
    @CollectionTable(name = "klass_days", joinColumns = @JoinColumn(name = "klass_id"))
    @Column(name = "day_of_week")
    private List<DayOfWeek> scheduledDays;
    private LocalTime classTime;
    @Column(unique = true, nullable = false)
    private String joinCode;

    @OneToMany(mappedBy = "klass", cascade = CascadeType.ALL)
    private List<ClassSession> sessions;


    @Column(nullable = false)
    private double acceptanceRadiusMeters = 5.0;

    @ElementCollection
    @CollectionTable(name = "klass_allowed_wifi", joinColumns = @JoinColumn(name = "klass_id"))
    @Column(name = "ssid")
    private List<String> allowedWifiSSIDs;


}
