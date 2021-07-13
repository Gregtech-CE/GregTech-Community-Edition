package gregtech.integration.jei.utils;

import gregtech.api.unification.material.type.SolidMaterial;
import gregtech.api.items.toolitem.ToolMetaItem;
import mezz.jei.api.ISubtypeRegistry.ISubtypeInterpreter;
import net.minecraft.item.ItemStack;

public class ToolMetaItemSubtypeHandler implements ISubtypeInterpreter {

    public static final ToolMetaItemSubtypeHandler INSTANCE = new ToolMetaItemSubtypeHandler();

    @Override
    public String apply(ItemStack itemStack) {
        if (!ToolMetaItem.hasToolSubItems(itemStack))
            return MetaItemSubtypeHandler.INSTANCE.apply(itemStack);

        SolidMaterial solidMaterial = ToolMetaItem.getToolMaterial(itemStack);
        return String.format("%d;%s", itemStack.getMetadata(), solidMaterial.toString());
    }
}
