package ru.track.prefork.commands;

import ru.track.prefork.commands.OptionParser;

public class CommandOption<T> {

    private String optionName;
    private boolean isRequired;
    private OptionParser<T> parser;
    private OptionGenerator<T> generator;

    public CommandOption(String optionName, boolean isRequired, OptionParser<T> parser, OptionGenerator<T> generator) {
        this.optionName = optionName;
        this.isRequired = isRequired;
        this.parser = parser;
        this.generator = generator;
    }

    public T getValue(String sValue) throws WrongValueException{
        return parser.parse(sValue);
    }

    public String getOption(Object value) {
        return generator.generate((T)value);
    }

    public boolean isRequired() {
        return isRequired;
    }

    public String getOptionName() {
        return optionName;
    }


}
