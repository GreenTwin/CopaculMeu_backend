package ro.greentween.copaculmeu.data.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HealthLevel {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL,
    DEAD;

    @JsonCreator
    public static HealthLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        for (HealthLevel level : HealthLevel.values()) {
            if (level.name().equals(normalized)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown health level: " + value +
                ". Allowed values are: EXCELLENT, GOOD, FAIR, POOR, CRITICAL, DEAD");
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
