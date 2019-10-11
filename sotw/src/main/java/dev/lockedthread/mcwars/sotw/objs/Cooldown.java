package dev.lockedthread.mcwars.sotw.objs;

import java.util.List;
import java.util.Set;

import dev.lockedthread.mcwars.sotw.MCWarsSOTW;
import dev.lockedthread.mcwars.sotw.iface.CooldownTime;

@Data
public class Cooldown {

    private final String name;
    private final String[] commands;
    private final CooldownTime cooldownTime;
    private final boolean startsWith;

    public Cooldown(ConfigurationSection section) {
        String name = section.getName();
        List<String> commands = section.getStringList("commands");
        boolean startsWith = section.getBoolean("starts-with");
        if (section.getBoolean("use-bukkit-commandmap")) {
            Command command = MCWarsSOTW.getInstance().getCommand(commands[0]);
            if (command == null) {
                throw new RuntimeException("Unable to find command with " + commands[0]);
            }
            commands.addAll(command.getAliases());
        }
    }


}