package ru.track.prefork.commands;

public class CommandException extends CommandManagerException{

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
