package ru.track.prefork.commands;

public class OptionException extends CommandManagerException {

    public OptionException(String message) {
        super(message);
    }

    public OptionException(String message, Throwable cause) {
        super(message, cause);
    }

}
