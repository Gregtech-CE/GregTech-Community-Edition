package gregtech.api.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**
 * For configurable things
 */
public interface IConfigurable {

    ResourceLocation getConfigurationID();

    String getConfigurationName();

    NBTTagCompound copyConfiguration(ConfigurationContext context);

    void pasteConfiguration(ConfigurationContext context, NBTTagCompound configuration);
}