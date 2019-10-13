package dev.lockedthread.mcwars.sotw.units;

import com.gameservergroup.gsgcore.collections.ConcurrentHashSet;
import com.gameservergroup.gsgcore.commands.post.CommandPost;
import com.gameservergroup.gsgcore.events.EventPost;
import com.gameservergroup.gsgcore.units.Unit;
import com.gameservergroup.gsgcore.utils.CallBack;
import com.google.common.collect.ImmutableMap;
import dev.lockedthread.mcwars.sotw.MCWarsSOTW;
import dev.lockedthread.mcwars.sotw.objs.CachedCooldown;
import dev.lockedthread.mcwars.sotw.objs.Cooldown;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class UnitSOTW extends Unit {

    private boolean sotw = false;
    private static final MCWarsSOTW INSTANCE = MCWarsSOTW.getInstance();
    private ImmutableMap<String, Cooldown> cooldownMap;
    private Map<UUID, CachedCooldown> cachedCooldowns;

    @Override
    public void setup() {
        this.cachedCooldowns = new HashMap<>();
        this.sotw = INSTANCE.getConfig().getBoolean("sotw.enabled");
        //noinspection rawtypes
        hookDisable(new CallBack() {
            @Override
            public void call() {
                INSTANCE.getConfig().set("sotw.enabled", sotw);
                INSTANCE.saveConfig();
            }
        });

        ConfigurationSection section = INSTANCE.getConfig().getConfigurationSection("sotw.cooldowned-commands");
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
                            sotw = toggle;
                            c.reply(sotw ? "&aYou have enabled SOTW cooldowns" : "&cYou have disabled SOTW cooldowns");
                            break;
                        default:
                            c.reply("&cUnable to parse command. /sotw {on/off}");
                            break;
                    }
                }).post(INSTANCE, "sotw");

        EventPost.of(PlayerJoinEvent.class)
                .filter(event -> sotw)
                .handle(event -> setCooldowns(event.getPlayer(), new CachedCooldown(System.currentTimeMillis(), new ConcurrentHashSet<>(cooldownMap.keySet()))))
                .post(INSTANCE);

        EventPost.of(PlayerQuitEvent.class)
                .filter(event -> sotw)
                .handle(event -> {
                    Player player = event.getPlayer();
                    CachedCooldown cooldownList = cachedCooldowns.get(player.getUniqueId());
                    if (cooldownList == null) {
                        throw new RuntimeException("Somehow there are no instances of cooldown metadata for player: " + player.getName() + " contact LockedThread immediately.");
                    } else {
                        cooldownList.getCooldownStrings().forEach(cooldown -> cooldownMap.remove(cooldown).getCachedTimes().remove(player));
                    }
                }).post(INSTANCE);

        EventPost.of(PlayerCommandPreprocessEvent.class, EventPriority.LOWEST)
                .filter(event -> sotw)
                .handle(event -> {
                    Player player = event.getPlayer();
                    CachedCooldown cachedCooldown = cachedCooldowns.get(player.getUniqueId());
                    if (cachedCooldown == null) {
                        throw new RuntimeException("Somehow there are no instances of cooldown metadata for player: " + player.getName() + " contact LockedThread immediately.");
                    } else {
                        for (String cooldownString : cachedCooldown.getCooldownStrings()) {
                            Cooldown cooldown = cooldownMap.get(cooldownString);
                            boolean pass;
                            if (cooldown.isStartsWith()) {
                                pass = event.getMessage().toLowerCase().startsWith("/" + cooldownString);
                            } else {
                                pass = event.getMessage().equalsIgnoreCase("/" + cooldownString);
                            }
                            System.out.println("pass = " + pass);
                            System.out.println("cooldown = " + cooldown);
                            if (pass) {
                                Long time = cooldown.getCachedTimes().get(player);
                                System.out.println("time = " + time);
                                if (time == null) {
                                    System.out.println("time == null");
                                    time = cooldown.getCooldownTime().getTime(player);
                                    cooldown.getCachedTimes().put(player, cooldown.getCooldownTime().getTime(player));
                                }
                                System.out.println("time == null, now time = " + time);
                                long timeJoined = cachedCooldown.getTimeJoined();
                                System.out.println("timeJoined = " + timeJoined);
                                if (timeJoined + time >= System.currentTimeMillis()) {
                                    long theCalculatedTime = Math.abs((System.currentTimeMillis() - timeJoined - time) / 1000);
                                    //long seconds = time / 1000;
                                    cooldown.sendCooldownMessage(player, event.getMessage(), theCalculatedTime);
                                    System.out.println("send cooldown message");
                                    event.setCancelled(true);
                                } else {
                                    System.out.println("removed cached and removed cooldown from player meta");
                                    cooldown.getCachedTimes().remove(player);
                                    cachedCooldown.getCooldownStrings().remove(cooldownString);
                                }
                            }
                        }
                    }
                }).post(INSTANCE);
    }

    public void setCooldowns(Player player, CachedCooldown cachedCooldown) {
        this.cachedCooldowns.put(player.getUniqueId(), cachedCooldown);
    }
}
