package dev.lockedthread.mcwars.sotw.objs;

import com.gameservergroup.gsgcore.collections.ConcurrentHashSet;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CachedCooldown {

    private final long timeJoined;
    private final ConcurrentHashSet<String> cooldownStrings;
}
