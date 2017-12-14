package ru.track.prefork.commands;

public class CommandHandlerException extends CommandManagerException{

    public CommandHandlerException(String message) {
        super(message);
    }

    public CommandHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
