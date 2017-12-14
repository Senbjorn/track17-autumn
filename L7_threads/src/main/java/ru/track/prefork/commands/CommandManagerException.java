package ru.track.prefork.commands;

public class CommandManagerException extends Exception{

    public CommandManagerException(String message) {
        super(message);
    }

    public CommandManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
