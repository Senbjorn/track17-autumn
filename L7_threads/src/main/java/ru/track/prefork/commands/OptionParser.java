package ru.track.prefork.commands;

public interface OptionParser<T> {

    T parse(String s) throws WrongValueException;

}
