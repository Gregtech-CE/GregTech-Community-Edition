package gregtech.api.util;

import net.minecraft.world.World;

public interface FirstTickTask {

    void handleFirstTick();

    World getWorld();
}