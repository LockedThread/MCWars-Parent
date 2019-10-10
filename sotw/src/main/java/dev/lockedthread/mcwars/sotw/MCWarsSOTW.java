package dev.lockedthread.mcwars.sotw;

import com.gameservergroup.gsgcore.plugin.Module;
import dev.lockedthread.mcwars.sotw.units.UnitSOTW;

public class MCWarsSOTW extends Module {

    private static MCWarsSOTW instance;

    @Override
    public void enable() {
        instance = this;
        saveDefaultConfig();
        registerUnits(new UnitSOTW());
    }

    @Override
    public void disable() {
        instance = null;
    }

    public static MCWarsSOTW getInstance() {
        return instance;
    }
}
