package ru.track.prefork.commands;

public class WrongOptionLineException extends CommandException{

    WrongOptionLineException(String message) {
        super(message);
    }

    WrongOptionLineException(String message, Throwable cause) {
        super(message, cause);
    }

}
