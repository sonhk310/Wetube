package com.wetube.wetube_service.enumeration;

public enum InteractionType {
    VIEW(1),
    LIKE(3),
    COMMENT(5),
    SHARE(7),
    WATCHTIME(5);

    private final int weight;

    InteractionType(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
