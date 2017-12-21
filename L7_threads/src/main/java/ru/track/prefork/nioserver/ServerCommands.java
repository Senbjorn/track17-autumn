package ru.track.prefork.nioserver;

import ru.track.prefork.Message;
import ru.track.prefork.commands.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerCommands {



    public static Command commandSend() {
        Command send = new Command("send");
        CommandOption<String> username = new CommandOption<>(
                "username",
                true,
                (str)-> str,
                (val)-> val
        );
        CommandOption<String> text = new CommandOption<>(
                "text",
                true,
                (str)-> str,
                (val)-> val
        );
        send.addOption(username);
        send.addOption(text);
        return send;
    }

    public static Command commandBroadcast() {
        Command broadcast = new Command("broadcast");
        CommandOption<String> text = new CommandOption<>(
                "text",
                true,
                (str)-> str,
                (val)-> val
        );
        broadcast.addOption(text);
        return broadcast;
    }

    public static Command commandRegister() {
        Command register = new Command("register");
        CommandOption<String> login = new CommandOption<>(
                "login",
                true,
                (str)-> str,
                (val)-> val
        );
        CommandOption<String> password = new CommandOption<>(
                "password",
                true,
                (str)-> str,
                (val)-> val
        );
        CommandOption<String> confirmPassword = new CommandOption<>(
                "confirmpassword",
                true,
                (str)-> str,
                (val)-> val
        );
        register.addOption(login);
        register.addOption(password);
        register.addOption(confirmPassword);
        return register;
    }

    public static Command commandLogin() {
        Command login = new Command("login");
        CommandOption<String> loginOpt = new CommandOption<>(
                "login",
                true,
                (str)-> str,
                (val)-> val
        );
        CommandOption<String> password = new CommandOption<>(
                "password",
                true,
                (str)-> str,
                (val)-> val
        );
        login.addOption(loginOpt);
        login.addOption(password);
        return login;
    }

    public static Command commandInput() {
        return null;
    }

    public static Command commandRequest() {
        return null;
    }

    public static Command commandResponse() {
        Command response = new Command("response");
        CommandOption<String> command = new CommandOption<>(
                "command",
                true,
                (str)-> str,
                (val)-> val
        );
        CommandOption<String> message = new CommandOption<>(
                "message",
                true,
                (str)-> str,
                (val)-> val
        );
        CommandOption<Object[]> auth = new CommandOption<>(
                "authorised",
                false,
                (String str)-> {
                    Pattern p = Pattern.compile("^\\{(?<id>\\p{Digit}+), \"(?<username>\\p{Alpha}+)\"\\}$");
                    Matcher m = p.matcher(str);
                    try {
                        if (m.matches()) {
                            Long id = Long.parseLong(m.group("id"));
                            String name = m.group("username");
                            return new Object[]{id, name};
                        }
                    } catch (NumberFormatException e) {}
                    throw new WrongValueException("worng authorization info");
                },
                (Object[] val)-> {
                    if (val.length != 2 || !(val[0] instanceof Long) || !(val[1] instanceof String)) {
                        return null;
                    }
                    return String.format("{%d, \"%s\"}", val[0], val[1]);
                }
        );
        CommandOption<String> errors = new CommandOption<>(
                "errors",
                false,
                (str)-> str,
                (val)-> val
        );
        response.addOption(command);
        response.addOption(auth);
        response.addOption(errors);
        response.addOption(message);
        return response;
    }

    public static Command commandAuthorization() {
        Command authorization = new Command("authorization");
        CommandOption<String> comment = new CommandOption<>(
                "comment",
                false,
                (str)-> str,
                (val)-> val
        );
        authorization.addOption(comment);
        return authorization;
    }

    public static Command commandMessage() {
        Command message = new Command("message");
        CommandOption<String> text = new CommandOption<>(
                "text",
                true,
                (str)-> str,
                (val)-> val
        );
        message.addOption(text);
        return message;
    }

    public static String stringWithMeta(Message msg, long id) {
        return String.format(
                "%s --ts=%d --author=%s --id=%d",
                msg.getText(),
                msg.getTime(),
                msg.getAuthor(),
                id
        );
    }

    public static String responseOK(CommandInstance ci, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("command", "command");
        map.put("message", message);
        return ServerCommands.commandResponse().generateCommand(map);
    }

    public static class UnavailableCommandHandler implements CommandHandler {

        @Override
        public void handle(Command command, Map<String, ?> options) throws CommandHandlerException {
            throw new CommandHandlerException("unavailable command");
        }
    }

}
