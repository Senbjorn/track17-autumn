package ru.track.prefork.commands;

public class WrongValueException extends OptionException{

    WrongValueException(String message) {
        super(message);
    }

    WrongValueException(String message, Throwable cause) {
        super(message, cause);
    }

}
