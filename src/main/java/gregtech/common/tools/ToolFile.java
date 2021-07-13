package gregtech.common.tools;

import gregtech.api.items.metaitem.MetaItem.MetaValueItem;
import gregtech.common.items.behaviors.ConfiguratorBehavior;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

public class ToolFile extends ToolBase {

    @Override
    public int getToolDamagePerBlockBreak(ItemStack stack) {
        return 1;
    }

    @Override
    public int getToolDamagePerContainerCraft(ItemStack stack) {
        return 4;
    }

    @Override
    public float getBaseDamage(ItemStack stack) {
        return 1.5F;
    }

    @Override
    public boolean canMineBlock(IBlockState block, ItemStack stack) {
        String tool = block.getBlock().getHarvestTool(block);
        return (tool != null && tool.equals("file")) ||
            block.getMaterial() == Material.CIRCUITS;
    }

    @SuppressWarnings("rawtypes")
    @Override
    // FIXME file is not the configurator
    public void onStatsAddedToTool(MetaValueItem metaValueItem) {
        metaValueItem.addComponents(new ConfiguratorBehavior(DamageValues.DAMAGE_FOR_CONFIGURATOR), ConfiguratorBehavior.CONFIGURATOR_MODE_SWITCH_BEHAVIOR);
    }

}
