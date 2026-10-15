package com.wetube.wetube_service.enumeration;

public enum CategoryType {
    FEATURED("Featured Videos"),
    MEMBERSHIP("Membership"),
    FOR_YOU("For you"),
    VIDEO("Videos"),
    POPULAR("Popular Videos"),
    SHORT("Short"),
    MEMBERSHIP_VIDEO("Membership videos"),
    LIVE("Live streams"),
    ;

    private final String displayName;

    CategoryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}