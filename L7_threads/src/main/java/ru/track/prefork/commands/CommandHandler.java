package ru.track.prefork.commands;

import java.util.Map;

public interface CommandHandler {

    void handle(Command command, Map<String, ?> options) throws CommandHandlerException;

}
