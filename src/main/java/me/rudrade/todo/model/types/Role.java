package me.rudrade.todo.model.types;

public enum Role {
    ROLE_USER("USER"),
    ROLE_ADMIN("ADMIN");

    private final String suffix;
    Role(String suffix) {
        this.suffix = suffix;
    }
    public String getSuffix() {
        return suffix;
    }
}
