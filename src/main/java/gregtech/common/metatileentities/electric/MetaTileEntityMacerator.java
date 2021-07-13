package gregtech.common.metatileentities.electric;

import gregtech.api.GTValues;
import gregtech.api.capability.impl.RecipeLogicEnergy;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.util.GTUtility;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;

public class MetaTileEntityMacerator extends SimpleMachineMetaTileEntity {

    private int outputAmount;

    public MetaTileEntityMacerator(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap, int outputAmount, OrientedOverlayRenderer renderer, int tier) {
        super(metaTileEntityId, recipeMap, renderer, tier);
        this.outputAmount = outputAmount;
        initializeInventory();
    }

    @Override
    protected RecipeLogicEnergy createWorkable(RecipeMap<?> recipeMap) {
        final RecipeLogicEnergy result = new RecipeLogicEnergy(this, recipeMap, () -> energyContainer) {
            @Override
            protected int getMachineTierForRecipe(Recipe recipe) {
                int tier = GTUtility.getTierByVoltage(getMaxVoltage());
                if (tier > GTValues.MV) {
                    return tier - GTValues.MV;
                }
                return 0;
            }
        };
        result.enableOverclockVoltage();
        return result;
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return new NotifiableItemStackHandler(outputAmount, this, true);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityMacerator(metaTileEntityId, workable.recipeMap, outputAmount, renderer, getTier());
    }
}
