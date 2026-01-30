package com.wellsfargo.signaturestudio.enums;

/**
 * Enum representing different schedule frequencies for automated report generation.
 */
public enum ScheduleFrequency {
    NONE("none", "No Schedule", null),
    DAILY("daily", "Daily", "0 0 2 * * ?"),      // 2 AM daily
    WEEKLY("weekly", "Weekly", "0 0 2 ? * MON"),  // 2 AM Monday
    MONTHLY("monthly", "Monthly", "0 0 2 1 * ?"); // 2 AM 1st of month

    private final String code;
    private final String displayName;
    private final String cronExpression;

    ScheduleFrequency(String code, String displayName, String cronExpression) {
        this.code = code;
        this.displayName = displayName;
        this.cronExpression = cronExpression;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public static ScheduleFrequency fromCode(String code) {
        for (ScheduleFrequency frequency : values()) {
            if (frequency.code.equals(code)) {
                return frequency;
            }
        }
        throw new IllegalArgumentException("Unknown schedule frequency code: " + code);
    }
}
