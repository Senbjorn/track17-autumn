package ru.track.prefork.commands;

public class CommandRuntimeException extends RuntimeException {
    public CommandRuntimeException(String message) {
        super(message);
    }

    public CommandRuntimeException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
