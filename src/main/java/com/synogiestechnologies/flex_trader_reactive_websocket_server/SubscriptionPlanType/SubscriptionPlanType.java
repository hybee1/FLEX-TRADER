package com.synogiestechnologies.flex_trader_reactive_websocket_server.SubscriptionPlanType;

public enum SubscriptionPlanType {
    FREE("Free", 0.0, "Basic access with limited features"),
    STARTER("Starter", 9.99, "For casual users with more signals and alerts"),
    PRO("Pro", 29.99, "Unlimited signals and premium tools"),
    PREMIUM("Premium", 79.99, "Full access, live push alerts, and priority support"),
    ENTERPRISE("Enterprise", -1, "Custom plan for businesses and advanced users");

    private final String displayName;
    private final double pricePerMonth;
    private final String description;

    SubscriptionPlanType(String displayName, double pricePerMonth, String description) {
        this.displayName = displayName;
        this.pricePerMonth = pricePerMonth;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPricePerMonth() {
        return pricePerMonth;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPaidPlan() {
        return this != FREE;
    }

    public boolean currentPlanTypeCanBeUpgradedToThis(SubscriptionPlanType newPlanType) {

        return this.pricePerMonth < newPlanType.pricePerMonth;
    }

    public boolean isCustomPricing() {
        return this == ENTERPRISE;
    }

    public static SubscriptionPlanType fromDisplayName(String displayName) {
        for (SubscriptionPlanType type : SubscriptionPlanType.values()) {
            if (type.getDisplayName().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No subscription plan found for display name: " + displayName);
    }
}
