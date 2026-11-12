package ru.mr_hares.realmMode.objects;

public enum Mode {
    PVE("pve"),
    PVP("pvp");

    private final String value;

    Mode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Mode fromValue(String value) {
        for (Mode m : values()) {
            if (m.value.equals(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown Mode value: " + value);
    }
}