package ru.track.prefork.commands;

import org.jetbrains.annotations.NotNull;

public interface OptionGenerator<T> {

    String generate(@NotNull T value);

}
