package gregtech.common.pipelike.cable.net;

import gregtech.api.pipenet.WorldPipeNet;
import gregtech.common.pipelike.cable.WireProperties;
import net.minecraft.world.World;

public class WorldENet extends WorldPipeNet<WireProperties, EnergyNet> {

    private static final String DATA_ID_BASE = "gregtech.e_net";

    public static WorldENet getWorldENet(World world) {
        final String DATA_ID = getDataID(DATA_ID_BASE, world);
        // First look for saved data
        WorldENet eNetWorldData = (WorldENet) world.loadData(WorldENet.class, DATA_ID);
        // No saved data, create it and queue it to be saved
        if (eNetWorldData == null) {
            eNetWorldData = new WorldENet(DATA_ID);
            world.setData(DATA_ID, eNetWorldData);
        }
        // See if we have old data
        if (!eNetWorldData.checkedForOldData) {
            eNetWorldData.oldData = (WorldENet) world.loadData(WorldENet.class, DATA_ID_BASE);
            eNetWorldData.checkedForOldData = true;
        }
        // Initialise
        eNetWorldData.setWorldAndInit(world);
        return eNetWorldData;
    }

    public WorldENet(String name) {
        super(name);
    }

    @Override
    protected EnergyNet createNetInstance() {
        return new EnergyNet(this);
    }

}
