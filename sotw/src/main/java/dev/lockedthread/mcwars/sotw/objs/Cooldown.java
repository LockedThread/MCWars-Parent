package dev.lockedthread.mcwars.sotw.objs;

import com.gameservergroup.gsgcore.utils.Text;
import com.google.common.collect.Sets;
import dev.lockedthread.mcwars.sotw.MCWarsSOTW;
import lombok.Data;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
public class Cooldown {

    private final String message;
    private final String[] commands;
    private final boolean startsWith;
    private final CooldownTime cooldownTime;
    private final Map<Player, Long> cachedTimes = new HashMap<>();

    public Cooldown(ConfigurationSection section) {
        this.message = section.getString("in-cooldown-message");
        Set<String> commands = Sets.newHashSet(section.getStringList("commands"));
        this.startsWith = section.getBoolean("starts-with");
        if (section.getBoolean("use-bukkit-commandmap")) {
            PluginCommand pluginCommand = getFirstCommand(commands);
            if (pluginCommand == null) {
                throw new RuntimeException("Unable to find command with names: {" + commands.toString() + ")");
            }
            commands.addAll(pluginCommand.getAliases());
        }
        this.commands = commands.toArray(new String[0]);
        this.cooldownTime = new CooldownTime(section.getConfigurationSection("cooldown-time"));
        System.out.println("toString() = " + toString());
    }

    private PluginCommand getFirstCommand(Set<String> commandNames) {
        for (String commandName : commandNames) {
            PluginCommand command = MCWarsSOTW.getInstance().getCommand(commandName);
            if (command != null) {
                return command;
            }
        }
        return null;
    }

    public void sendCooldownMessage(Player player, String commandLabel, long time) {
        player.sendMessage(Text.toColor(message).replace("{command}", commandLabel).replace("{time}", String.valueOf(time)));
    }
}