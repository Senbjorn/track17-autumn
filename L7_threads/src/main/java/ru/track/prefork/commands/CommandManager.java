package ru.track.prefork.commands;


import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandManager {

    private Map<String, Command> commands;
    private Pattern commandPattern;

    public CommandManager() {
        commands = new HashMap<>();
        commandPattern = Pattern.compile("^(?<name>\\p{Alpha}+)(?<optionLine>.*)$");
    }

    public CommandInstance paresCommand(String  command) throws CommandManagerException {
        Matcher matcher = commandPattern.matcher(command);
        if (matcher.matches()) {
            String name = matcher.group("name");
            String optionLine = matcher.group("optionLine");
            if (!commands.containsKey(name)) {
                throw new WrongCommandException("Unknown command");
            }
            Map<String, ?> optMap = commands.get(name).getOptionsList(optionLine);
            return new CommandInstance(commands.get(name), optMap);
        } else {
            throw new WrongCommandException("Wrong command syntax");
        }
    }

    public void handleCommand(CommandInstance commandInstance, CommandHandler commandHandler) throws CommandManagerException {
        commandHandler.handle(commandInstance.getCommand(), commandInstance.getOptMap());
    }

    public void addCommand(Command c) throws CommandRuntimeException {
        if (commands.containsKey(c.getCommandName())) {
            throw new CommandRuntimeException("Command already exists");
        }
        commands.put(c.getCommandName(), c);
    }

    public Command getCommand(String name) {
        if (!commands.containsKey(name)) {
            throw new CommandRuntimeException("Unknown command");
        }
        return commands.get(name);
    }

}
