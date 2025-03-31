package Capstone.QR.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student extends User {
    @ManyToMany(mappedBy = "students")
    private List<Klass> registeredClasses;

    @OneToMany(mappedBy = "student")
    private List<Attendance> attendanceRecords;
}