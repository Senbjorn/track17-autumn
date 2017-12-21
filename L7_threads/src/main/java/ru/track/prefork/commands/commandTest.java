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
//        try {
////            c1.addOption(o11);
////            c1.addOption(o12);
//            cm.addCommand(c1, (c, om)->{
//                System.out.println("print");
//            });
//            Map<String, Object> map= new HashMap<>();
//            System.out.println(c1.generateCommand(map));
//            cm.handleCommand(c1.generateCommand(map));
//        } catch (CommandManagerException e) {
//            e.printStackTrace();
//        }
    }
}
