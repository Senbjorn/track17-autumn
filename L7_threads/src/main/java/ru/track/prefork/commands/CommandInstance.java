package ru.track.prefork.commands;

import java.util.Map;

public class CommandInstance {

    private Command command;
    private Map<String, ?> optMap;

    public CommandInstance(Command command, Map<String, ?> optMap) {
        this.command = command;
        this.optMap = optMap;
    }

    public Map<String, ?> getOptMap() {
        return optMap;
    }

    public Command getCommand() {
        return command;
    }
}
