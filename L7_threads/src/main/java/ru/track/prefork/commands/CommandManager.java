package ru.track.prefork.commands;


import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandManager {

    private Map<String, Command> commands;
    private Map<String, CommandHandler> handlers;
    private Pattern commandPattern;

    public CommandManager() {
        commands = new HashMap<>();
        handlers = new HashMap<>();
        commandPattern = Pattern.compile("^(?<name>\\p{Alpha}+)(?<optionLine>.*)$");
    }

    public void handleCommand(String  command) throws CommandManagerException {
        Matcher matcher = commandPattern.matcher(command);
        if (matcher.matches()) {
            String name = matcher.group("name");
            String optionLine = matcher.group("optionLine");
            if (!commands.containsKey(name)) {
                throw new WrongCommandException("Unavailable command");
            }
            Map<String, ?> optMap = commands.get(name).getOptionsList(optionLine);
            handlers.get(name).handle(commands.get(name), optMap);
        } else {
            throw new WrongCommandException("Wrong command syntax");
        }
    }

    public void addCommand(Command c, CommandHandler handler) {
        commands.put(c.getCommandName(), c);
        handlers.put(c.getCommandName(), handler);
    }

}
