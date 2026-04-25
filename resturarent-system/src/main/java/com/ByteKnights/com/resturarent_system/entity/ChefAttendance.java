package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chef_attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Connects to the Staff member
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    // THIS IS WHAT YOU WANTED: Tracks the exact day!
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    // When they arrived
    @Column(name = "clock_in_time")
    private LocalDateTime clockInTime;

    // When they left (will be null until they leave)
    @Column(name = "clock_out_time")
    private LocalDateTime clockOutTime;

    // ON_DUTY or OFF_DUTY
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false)
    @Builder.Default
    private ChefAttendanceStatus attendanceStatus = ChefAttendanceStatus.OFF_DUTY;

    // AVAILABLE or COOKING
    @Enumerated(EnumType.STRING)
    @Column(name = "work_status", nullable = false)
    @Builder.Default
    private ChefWorkStatus workStatus = ChefWorkStatus.AVAILABLE;

    @PrePersist
    protected void onCreate() {
        if (this.attendanceDate == null) {
            this.attendanceDate = LocalDate.now(); // Automatically sets to "today"
        }
    }
}
