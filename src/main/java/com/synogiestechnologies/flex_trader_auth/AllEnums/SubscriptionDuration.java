package com.synogiestechnologies.flex_trader_auth.AllEnums;

import lombok.RequiredArgsConstructor;


public enum SubscriptionDuration {
    ONE_WEEK("1 Week", 7L * 24 * 60 * 60 * 1000),
    TWO_WEEKS("2 Weeks", 14L * 24 * 60 * 60 * 1000),
    ONE_MONTH("1 Month", 30L * 24 * 60 * 60 * 1000),
    TWO_MONTHS("2 Months", 60L * 24 * 60 * 60 * 1000),
    THREE_MONTHS("3 Months", 90L * 24 * 60 * 60 * 1000);

    private final String displayName;
    private final long durationMillis;

    SubscriptionDuration(String displayName, long durationMillis) {
        this.displayName = displayName;
        this.durationMillis = durationMillis;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public static SubscriptionDuration fromDisplayName(String displayName) {
        for (SubscriptionDuration duration : SubscriptionDuration.values()) {
            if (duration.getDisplayName().equalsIgnoreCase(displayName)) {
                return duration;
            }
        }
        throw new IllegalArgumentException("No subscription duration found for display name: " + displayName);
    }
}

