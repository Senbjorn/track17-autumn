package ru.track.prefork.commands;

public class WrongValueException extends OptionException{

    public WrongValueException(String message) {
        super(message);
    }

    public WrongValueException(String message, Throwable cause) {
        super(message, cause);
    }

}
