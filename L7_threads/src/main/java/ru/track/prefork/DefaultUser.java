package ru.track.prefork;

public class DefaultUser {

    private long id;
    private long lastLoginAt;
    private long registeredAt;
    private String name = "AnonymousUser";

    public DefaultUser(long id, long lastLoginAt, long registeredAt, String name) {
        this.id = id;
        this.lastLoginAt = lastLoginAt;
        this.registeredAt = registeredAt;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public long getLastLoginAt() {
        return lastLoginAt;
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    public String getName() {
        return name;
    }

}
