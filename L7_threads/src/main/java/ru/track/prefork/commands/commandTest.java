package ru.track.prefork.commands;

import java.util.HashMap;
import java.util.Map;

public class commandTest {

    public static void main(String... args) {
        System.out.println("Test!");
        CommandManager cm = new CommandManager();
        Command c1 = new Command("print");
        CommandOption<String> o11 = new CommandOption<>("message", true, (s)->s, (s)->s );
        CommandOption<Integer> o12 = new CommandOption<>("times", false,(i)->Integer.parseInt(i), (i)->i.toString() );
        try {
            c1.addOption(o11);
            c1.addOption(o12);
            cm.addCommand(c1, (c, om)->{
                String s = (String) om.get("message");
                if (om.containsKey("times")) {
                    Integer i = (Integer) om.get("times");
                    if (i < 1) {
                        throw new CommandHandlerException("Times value is too low!");
                    }
                    for (int j = 0; j < i - 1; j++) {
                        System.out.println(s);
                    }
                }
                System.out.println(s);
            });
            Map<String, Object> map= new HashMap<>();
            map.put("message", "Hello");
            map.put("times", 20000);
            System.out.println(c1.generateCommand(map));
            cm.handleCommand(c1.generateCommand(map));
        } catch (CommandManagerException e) {
            e.printStackTrace();
        }
    }
}
