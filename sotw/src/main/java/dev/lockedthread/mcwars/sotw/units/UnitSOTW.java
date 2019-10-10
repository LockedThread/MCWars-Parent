package dev.lockedthread.mcwars.sotw.units;

import com.gameservergroup.gsgcore.units.Unit;
import com.gameservergroup.gsgcore.utils.CallBack;
import dev.lockedthread.mcwars.sotw.MCWarsSOTW;

public class UnitSOTW extends Unit {

    private boolean sotw = false;
    private static final MCWarsSOTW INSTANCE = MCWarsSOTW.getInstance();

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
    }
}
