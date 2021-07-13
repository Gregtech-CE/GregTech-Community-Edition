package gregtech.api.capability.impl;

import gregtech.api.capability.ConfigurationContext;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.tool.IConfiguratorItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;

public class PlayerConfigurationContext implements ConfigurationContext {

    private final EntityPlayer player;
    private final ItemStack configurator;
    private final boolean advanced;

    public PlayerConfigurationContext(final EntityPlayer player, final ItemStack configurator) {
        this.player = player;
        this.configurator = configurator;
        boolean isAdvanced = false;
        // Review: Should this be an error (the passed item is not a configurator)?
        if (configurator != null) {
            final IConfiguratorItem item = configurator.getCapability(GregtechCapabilities.CAPABILITY_CONFIGURATOR, null);
            if (item != null) {
                isAdvanced = item.isAdvanced();
            }
        }
        this.advanced = isAdvanced;
    }

    @Override
    public boolean isAdvanced() {
        return this.advanced;
    }

    // Review: available via casting - not part of the generic api
    public ItemStack getConfigurator() {
        return this.configurator;
    }

    @Override
    public void sendMessage(final ITextComponent component) {
        this.player.sendMessage(component);
    }

    @Override
    public <T> T getCapability(final Capability<T> capability, final EnumFacing facing) {
        return isAdvanced() ? this.player.getCapability(capability, facing) : null;
    }
}