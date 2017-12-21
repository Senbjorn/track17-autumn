package ru.track.prefork.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

    private String commandName;
    private Pattern optionPattern;
    private Map<String, CommandOption<?>> options;

    public Command(String commandName) {
        this.commandName = commandName;
        options = new HashMap<>();
        optionPattern = Pattern.compile("^(?<name>\\p{Alpha}*?)=(?<value>.+)$");
    }

    public Map<String, ?> getOptionsList(String optionLine) throws WrongOptionLineException {
        String[] opt = Pattern.compile("\\p{Blank}+--").split(optionLine);
        Map<String, Object> optMap = new HashMap<>();
        for (String option: opt) {
            if (option.equals("")) continue;
            Matcher matcher = optionPattern.matcher(option);
            if (matcher.matches()) {
                String name = matcher.group("name");
                String value = matcher.group("value");
                if (optMap.containsKey(name)) {
                    throw new WrongOptionLineException("Duplicate option: " + name);
                }
                if (!options.containsKey(name)) {
                    throw new WrongOptionLineException("Unavailable option: " + name);
                }
                try {
                    optMap.put(name, options.get(name).getValue(value));
                } catch (WrongValueException e) {
                    throw new WrongOptionLineException("Inner option exception.", e);
                }
            } else {
                throw new WrongOptionLineException(option);
            }
        }
        for (Map.Entry e: options.entrySet()) {
            if (!optMap.containsKey(e.getKey()) &&
                    ((CommandOption<?>) e.getValue()).isRequired()) {
                throw new WrongOptionLineException("Required option is missing: " + e.getKey());
            }
        }
        return optMap;
    }

    public String generateCommand(Map<String, ?> opt) throws CommandRuntimeException{
        StringBuilder sb = new StringBuilder();
        sb.append(commandName);
        for (Map.Entry e: options.entrySet()) {
            if (!opt.containsKey(e.getKey()) &&
                    ((CommandOption<?>) e.getValue()).isRequired()) {
                throw new CommandRuntimeException("Required option is missing: " + e.getKey());
            }
            if (opt.containsKey(e.getKey())) {
                sb.append(" --").append(e.getKey()).
                        append("=").
                        append(((CommandOption<?>)e.getValue()).getOption(opt.get(e.getKey())));
            }
        }
        return sb.toString();
    }

    public void addOption(CommandOption<?> option) throws CommandRuntimeException {
        if (options.containsKey(option.getOptionName())) {
            throw new CommandRuntimeException("Option already exists!");
        }
        options.put(option.getOptionName(), option);
    }

    public String getCommandName() {
        return commandName;
    }

}
