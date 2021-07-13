package gregtech.common.items.behaviors;

import java.util.List;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.raytracer.RayTracer;
import gregtech.api.capability.ConfigurationContext;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IConfigurable;
import gregtech.api.capability.impl.PlayerConfigurationContext;
import gregtech.api.cover.ICoverable;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.util.GTUtility;
import gregtech.common.items.behaviors.ModeSwitchBehavior.ILocalizationKey;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class ConfiguratorBehavior implements IItemBehaviour {

    private static final String CONFIGURATION = "metaitem.configurator.configuration";
    private static final String COPY = "metaitem.configurator.copy";
    private static final String NO_CONFIGURATION = "metaitem.configurator.no_configuration";
    private static final String NOT_CONFIGURABLE = "metaitem.configurator.not_configurable";
    private static final String PASTE = "metaitem.configurator.paste";
    private static final String WRONG_CONFIGURABLE1 = "metaitem.configurator.wrong_configurable1";
    private static final String WRONG_CONFIGURABLE2 = "metaitem.configurator.wrong_configurable2";

    public static final ModeSwitchBehavior<ConfiguratorMode> CONFIGURATOR_MODE_SWITCH_BEHAVIOR = new ModeSwitchBehavior<>(ConfiguratorMode.class);

    private static final String TAG_NAME = "GT.Configurator";
    private static final String CONFIGURATION_ID = "GT.ConfigurationID";
    private static final String CONFIGURATION_NAME = "GT.ConfigurationName";
    private final int cost;

    public ConfiguratorBehavior(final int cost) {
        this.cost = cost;
    }

    @Override
    public EnumActionResult onItemUseFirst(final EntityPlayer player, final World world, final BlockPos pos, final EnumFacing side, final float hitX, final float hitY, final float hitZ, final EnumHand hand) {
        // Everything gets done on the server side
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        final ItemStack toolStack = player.getHeldItem(hand);

        // Only acts on tile entities
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity == null) {
            player.sendMessage(new TextComponentTranslation(NOT_CONFIGURABLE));
            return EnumActionResult.PASS;
        }

        // We got told a side, but let's use the mode and ray tracing to figure out what the user really wants
        final ConfiguratorMode mode = CONFIGURATOR_MODE_SWITCH_BEHAVIOR.getModeFromItemStack(toolStack);
        EnumFacing checkSide = null; // machine mode doesn't check a side
        if (mode == ConfiguratorMode.COVER) {
            final CuboidRayTraceResult rayTraceResult = (CuboidRayTraceResult) RayTracer.retraceBlock(world, player, pos);
            checkSide = ICoverable.determineGridSideHit(rayTraceResult);
        }

        // Get the capability of the relevant machine/cover
        final IConfigurable configurable = tileEntity.getCapability(GregtechTileCapabilities.CAPABILITY_CONFIGURABLE, checkSide);
        if (configurable == null) {
            player.sendMessage(new TextComponentTranslation(NOT_CONFIGURABLE));
            return EnumActionResult.PASS;
        }

        final ConfigurationContext context = new PlayerConfigurationContext(player, toolStack);

        // Shift Right Click is save
        final boolean isShiftClick = player.isSneaking();
        if (isShiftClick) {
            final NBTTagCompound configuration = configurable.copyConfiguration(context);
            // Didn't get a configuration for some reason. Should this be a fail?
            if (configuration == null) {
                player.sendMessage(new TextComponentTranslation(NOT_CONFIGURABLE));
                return EnumActionResult.PASS;
            }
            // Remember the id and name of the machine/cover
            final String machineID = configurable.getConfigurationID().toString();
            configuration.setString(CONFIGURATION_ID, machineID);
            final String machineName = configurable.getConfigurationName();
            configuration.setString(CONFIGURATION_NAME, machineName);
            // Save the configuration in the item
            final NBTTagCompound itemTag = toolStack.getTagCompound();
            itemTag.setTag(TAG_NAME, configuration);
            player.sendMessage(new TextComponentTranslation(COPY).appendSibling(new TextComponentTranslation(machineName)));
        } else {
            // Configurator doesn't have a saved configuration
            final NBTTagCompound configuration = toolStack.getSubCompound(TAG_NAME);
            if (configuration == null) {
                player.sendMessage(new TextComponentTranslation(NO_CONFIGURATION));
                return EnumActionResult.PASS;
            }
            // Check the saved config has the same id as the target machine/cover
            final String machineID = configurable.getConfigurationID().toString();
            final String savedID = configuration.getString(CONFIGURATION_ID);
            final String machineName = configurable.getConfigurationName();
            final String savedName = configuration.getString(CONFIGURATION_NAME);
            if (!machineID.equals(savedID)) {
                player.sendMessage(new TextComponentTranslation(WRONG_CONFIGURABLE1)
                        .appendSibling(new TextComponentTranslation(savedName))
                        .appendSibling(new TextComponentTranslation(WRONG_CONFIGURABLE2))
                        .appendSibling(new TextComponentTranslation(machineName)));
                return EnumActionResult.FAIL;
            }
            // Apply the configuration, any warnings should be sent to chat
            configurable.pasteConfiguration(context, configuration);
            player.sendMessage(new TextComponentTranslation(PASTE).appendSibling(new TextComponentTranslation(savedName)));
        }
        GTUtility.doDamageItem(toolStack, this.cost, false);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(final ItemStack toolStack, final List<String> lines) {
        final NBTTagCompound configuration = toolStack.getSubCompound(TAG_NAME);
        if (configuration == null) {
            lines.add(I18n.format(NO_CONFIGURATION));
        } else {
            final String configName = configuration.getString(CONFIGURATION_NAME);
            lines.add(I18n.format(CONFIGURATION, I18n.format(configName)));
        }
    }

    private enum ConfiguratorMode implements ILocalizationKey {
        COVER("metaitem.configurator.mode.cover"),
        MACHINE("metaitem.configurator.mode.machine");

        private final String localizationKey;

        ConfiguratorMode(final String localizationKey) {
            this.localizationKey = localizationKey;
        }

        @Override
        public String getUnlocalizedName() {
            return this.localizationKey;
        }
    }
}
