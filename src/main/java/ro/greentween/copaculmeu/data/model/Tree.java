package ro.greentween.copaculmeu.data.model;

import jakarta.persistence.*;
import lombok.*;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.WateringLevel;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "trees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "original_latitude", nullable = false)
    private Double originalLatitude;

    @Column(name = "original_longitude", nullable = false)
    private Double originalLongitude;

    @Column(nullable = false)
    private String species;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_level", nullable = false, length = 50)
    private HealthLevel healthLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "watering_level", length = 50)
    private WateringLevel wateringLevel;

    @Column(name = "last_watered_at")
    private LocalDateTime lastWateredAt;

    @Column
    private Double height;

    @Column
    private Integer age;

    @Column(name = "trunk_circumference")
    private Double trunkCircumference;

    @Column(name = "location_description")
    private String locationDescription;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ro.greentween.copaculmeu.data.enums.TreeStatus status = ro.greentween.copaculmeu.data.enums.TreeStatus.APPROVED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_by_user_id")
    private User proposedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.originalLatitude == null) {
            this.originalLatitude = this.latitude;
        }
        if (this.originalLongitude == null) {
            this.originalLongitude = this.longitude;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
    }
}
