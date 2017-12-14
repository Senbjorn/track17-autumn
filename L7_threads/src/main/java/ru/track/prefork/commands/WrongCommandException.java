package ru.track.prefork.commands;

public class WrongCommandException extends CommandManagerException {

    public WrongCommandException(String message) {
        super(message);
    }

    public WrongCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
