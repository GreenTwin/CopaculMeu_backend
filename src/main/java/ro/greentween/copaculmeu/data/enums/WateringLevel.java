package ro.greentween.copaculmeu.data.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WateringLevel {
    CRITICAL_DROUGHT,
    LOW,
    MODERATE,
    OPTIMAL,
    OVERWATERED;

    @JsonCreator
    public static WateringLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        for (WateringLevel level : WateringLevel.values()) {
            if (level.name().equals(normalized)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown watering level: " + value +
                ". Allowed values are: CRITICAL_DROUGHT, LOW, MODERATE, OPTIMAL, OVERWATERED");
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
