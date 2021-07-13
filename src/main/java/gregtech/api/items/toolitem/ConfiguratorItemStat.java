package gregtech.api.items.toolitem;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.tool.IConfiguratorItem;
import gregtech.api.items.metaitem.stats.IItemCapabilityProvider;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class ConfiguratorItemStat implements IItemCapabilityProvider {

    private boolean advanced = false;

    @Override
    public ICapabilityProvider createProvider(final ItemStack itemStack) {
        return new CapabilityProvider(itemStack);
    }

    public ConfiguratorItemStat advanced() {
        this.advanced = true;
        return this;
    }

    private class CapabilityProvider extends AbstractToolItemCapabilityProvider<IConfiguratorItem> implements IConfiguratorItem {

        public CapabilityProvider(final ItemStack itemStack) {
            super(itemStack);
        }

        @Override
        public boolean isAdvanced() {
            return ConfiguratorItemStat.this.advanced;
        }

        @Override
        protected Capability<IConfiguratorItem> getCapability() {
            return GregtechCapabilities.CAPABILITY_CONFIGURATOR;
        }
    }
}
