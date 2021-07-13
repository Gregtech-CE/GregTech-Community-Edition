package gregtech.api.capability;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * Context used during configuration events
 */
public interface ConfigurationContext extends ICapabilityProvider {

    default boolean isAdvanced() {
        return false;
    }

    // For feedback
    void sendMessage(ITextComponent component);

    @Override
    default boolean hasCapability(final Capability<?> capability, final EnumFacing facing) {
        return getCapability(capability, facing) != null;
    }
}