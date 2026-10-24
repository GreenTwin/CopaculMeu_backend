package ro.greentween.copaculmeu.data.model;

import jakarta.persistence.*;
import lombok.*;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.WateringLevel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "tree_checkins")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeCheckin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tree_id", nullable = false)
    private Tree tree;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "deviation_meters", nullable = false)
    private Double deviationMeters;

    @Column(name = "is_valid", nullable = false)
    private Boolean valid;

    @Enumerated(EnumType.STRING)
    @Column(name = "watering_level", nullable = false, length = 50)
    private WateringLevel wateringLevel;

    @Column(nullable = false)
    private Boolean watered;

    @Column(name = "proposed_species")
    private String proposedSpecies;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_level", length = 50)
    private HealthLevel healthLevel;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("UTC"));

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
        }
    }
}
