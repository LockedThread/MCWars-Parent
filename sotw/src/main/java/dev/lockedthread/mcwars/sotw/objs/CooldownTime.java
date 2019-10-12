package dev.lockedthread.mcwars.sotw.objs;

import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

@Data
public class CooldownTime {

    private final ConfigurationSection section;
    private final boolean donatorEnabled;
    private final String donatorPermission;

    public CooldownTime(ConfigurationSection section) {
        this.section = section;
        this.donatorPermission = (this.donatorEnabled = section.getBoolean("donator.enabled")) ? section.getString("donator.permission") : null;
    }

    public long getTime(Player player) {
        System.out.println("0");
        if (donatorEnabled) {
            System.out.println("1");
            if (donatorPermission != null) {
                System.out.println("2");
                if (player.hasPermission(donatorPermission)) {
                    System.out.println("3");
                    long donator = getTimeFromSection("donator");
                    System.out.println("donator = " + donator);
                    return donator;
                }
            }
        }
        System.out.println("4");
        long regular = getTimeFromSection("regular");
        System.out.println("regular = " + regular);
        return regular;
    }

    private long getTimeFromSection(String sectionPrefix) {
        ConfigurationSection timeValues = section.getConfigurationSection(sectionPrefix + ".time");
        System.out.println("timeValues = " + timeValues);
        if (timeValues.getBoolean("ranged.enabled")) {
            long max = timeValues.getLong("ranged.max");
            System.out.println("max = " + max);
            long min = timeValues.getLong("ranged.min");
            System.out.println("min = " + min);
            long randomNumber = ThreadLocalRandom.current().nextLong(min, max);
            System.out.println("randomNumber = " + randomNumber);
            return randomNumber;
        }
        long staticLong = section.getLong("static");
        System.out.println("staticLong = " + staticLong);
        return staticLong;
    }
}
