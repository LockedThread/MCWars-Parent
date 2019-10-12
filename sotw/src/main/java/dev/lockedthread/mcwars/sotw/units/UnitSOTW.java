package dev.lockedthread.mcwars.sotw.units;

import com.gameservergroup.gsgcore.commands.post.CommandPost;
import com.gameservergroup.gsgcore.events.EventPost;
import com.gameservergroup.gsgcore.units.Unit;
import com.gameservergroup.gsgcore.utils.CallBack;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import dev.lockedthread.mcwars.sotw.MCWarsSOTW;
import dev.lockedthread.mcwars.sotw.objs.Cooldown;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnitSOTW extends Unit {

    private boolean sotw = false;
    private static final MCWarsSOTW INSTANCE = MCWarsSOTW.getInstance();
    private Map<String, Cooldown> cooldownMap;

    @Override
    public void setup() {
        //noinspection rawtypes
        hookDisable(new CallBack() {
            @Override
            public void call() {
                INSTANCE.getConfig().set("sotw.enabled", sotw);
                INSTANCE.saveConfig();
            }
        });

        ConfigurationSection section = INSTANCE.getConfig().getConfigurationSection("cooldowned-commands");
        Map<String, Cooldown> map = section.getKeys(false)
                .stream()
                .collect(Collectors.toMap(key -> key, key -> new Cooldown(section.getConfigurationSection(key)), (a, b) -> b));
        this.cooldownMap = ImmutableMap.copyOf(map);

        CommandPost.create()
                .builder()
                .assertPermission("mcwars.sotw.toggle")
                .handler(c -> {
                    switch (c.getRawArgs().length) {
                        case 0:
                            sotw = !sotw;
                            c.reply(sotw ? "&aYou have enabled SOTW cooldowns" : "&cYou have disabled SOTW cooldowns");
                            break;
                        case 1:
                            boolean toggle;
                            switch (c.getRawArg(0).toLowerCase()) {
                                case "on":
                                    toggle = true;
                                    break;
                                case "off":
                                    toggle = false;
                                    break;
                                default:
                                    c.reply("&cUnable to parse arg 0 " + c.getRawArg(0) + " as boolean (on/off)");
                                    return;
                            }
                            break;
                        default:
                            c.reply("&cUnable to parse command. /sotw {on/off}");
                            break;
                    }
                }).post(INSTANCE, "sotw");

        EventPost.of(PlayerJoinEvent.class)
                .filter(event -> sotw)
                .handle(event -> setCooldowns(event.getPlayer(), new ArrayList<>(cooldownMap.keySet())))
                .post(INSTANCE);

        EventPost.of(PlayerQuitEvent.class)
                .filter(event -> sotw)
                .handle(event -> {

                }).post(INSTANCE);

        EventPost.of(PlayerCommandPreprocessEvent.class)
                .filter(event -> event.getPlayer().hasMetadata("cooldowns"))
                .handle(event -> {
                    Player player = event.getPlayer();
                    List<MetadataValue> metadataValues = player.getMetadata("cooldowns");
                    if (metadataValues.size() == 0) {
                        throw new RuntimeException("Somehow there are no instances of cooldown metadata for player: " + player.getName() + " contact LockedThread immediately.");
                    } else if (metadataValues.size() == 1) {
                        MetadataValue metadataValue = metadataValues.get(0);
                        List<String> cooldowns = (List<String>) metadataValue.value();
                        for (String cooldownString : cooldowns) {
                            Cooldown cooldown = cooldownMap.get(cooldownString);
                            Long time = cooldown.getCachedTimes().get(player);
                            if (time == null || time == 0) {
                                time = cooldown.getCooldownTime().getTime(player);
                                cooldown.getCachedTimes().put(player, cooldown.getCooldownTime().getTime(player));
                            }
                            long lastPlayed = player.getLastPlayed();
                            if (lastPlayed + time >= System.currentTimeMillis()) {
                                cooldown.sendCooldownMessage(player, event.getMessage(), (time / 1000));
                            } else {
                                cooldown.getCachedTimes().remove(player);
                                cooldowns.remove(cooldownString);
                            }
                        }
                    }
                }).post(INSTANCE);

    }

    public void setCooldowns(Player player, List<String> cooldowns) {
        player.setMetadata("cooldowns", new FixedMetadataValue(INSTANCE, cooldowns));
    }

    public void addCooldown(Player player, String cooldownName) {
        List<String> cooldowns;
        if (player.hasMetadata("cooldowns")) {
            List<MetadataValue> metadataValues = player.getMetadata("cooldowns");
            if (metadataValues.size() == 0) {
                cooldowns = Lists.newArrayList(cooldownName);
            } else if (metadataValues.size() == 1) {
                cooldowns = (List<String>) metadataValues.get(0).value();
            } else {
                throw new RuntimeException("Somehow there are more than one instance of cooldown metadata for player: " + player.getName() + " contact LockedThread immediately.");
            }
        } else {
            cooldowns = Lists.newArrayList(cooldownName);
        }
        setCooldowns(player, cooldowns);
    }
}
