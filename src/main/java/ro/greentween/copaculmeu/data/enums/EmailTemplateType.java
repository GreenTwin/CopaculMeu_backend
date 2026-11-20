package ro.greentween.copaculmeu.data.enums;

public enum EmailTemplateType {
    REGISTRATION_CONFIRMATION,
    FORGOT_PASSWORD;

    public static EmailTemplateType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_");
        if (normalized.equals("CONFIRMATION_CODE") || normalized.equals("REGISTRATION")) {
            return REGISTRATION_CONFIRMATION;
        }
        if (normalized.equals("PASSWORD_RESET") || normalized.equals("RESET_PASSWORD")) {
            return FORGOT_PASSWORD;
        }
        return EmailTemplateType.valueOf(normalized);
    }
}
