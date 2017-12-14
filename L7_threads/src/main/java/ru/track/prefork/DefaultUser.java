package ru.track.prefork;

import java.net.Socket;

public class DefaultUser {

    private long id;
    private long lastLoginAt;
    private long registeredAt;
    private Socket socket;
    private String name = "AnonymousUser";

    public DefaultUser(long id, long lastLoginAt, long registeredAt, Socket socket, String name) {
        this.id = id;
        this.lastLoginAt = lastLoginAt;
        this.registeredAt = registeredAt;
        this.socket = socket;
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

    public Socket getSocket() {
        return socket;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
