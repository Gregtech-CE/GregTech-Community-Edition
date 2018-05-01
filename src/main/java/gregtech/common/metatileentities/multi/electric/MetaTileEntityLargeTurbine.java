package gregtech.common.metatileentities.multi.electric;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.common.blocks.BlockTurbineCasing.TurbineCasingType;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.items.IItemHandlerModifiable;

public class MetaTileEntityLargeTurbine extends RecipeMapMultiblockController {

    public static final MultiblockAbility<IItemHandlerModifiable> ABILITY_ROTOR_HOLDER = new MultiblockAbility<>();

    public enum TurbineType {

        STEAM(RecipeMaps.STEAM_TURBINE_FUELS, MetaBlocks.TURBINE_CASING.getState(TurbineCasingType.STEEL_TURBINE_CASING), Textures.SOLID_STEEL_CASING),
        GAS(RecipeMaps.GAS_TURBINE_FUELS, MetaBlocks.TURBINE_CASING.getState(TurbineCasingType.STAINLESS_TURBINE_CASING), Textures.CLEAN_STAINLESS_STEEL_CASING),
        PLASMA(RecipeMaps.PLASMA_GENERATOR_FUELS, MetaBlocks.TURBINE_CASING.getState(TurbineCasingType.TUNGSTENSTEEL_TURBINE_CASING), Textures.ROBUST_TUNGSTENSTEEL_CASING);

        public final RecipeMap<?> recipeMap;
        public final IBlockState casingState;
        public final ICubeRenderer casingRenderer;

        TurbineType(RecipeMap<?> recipeMap, IBlockState casingState, ICubeRenderer casingRenderer) {
            this.recipeMap = recipeMap;
            this.casingState = casingState;
            this.casingRenderer = casingRenderer;
        }
    }

    private final TurbineType turbineType;

    public MetaTileEntityLargeTurbine(String metaTileEntityId, TurbineType turbineType) {
        super(metaTileEntityId, turbineType.recipeMap);
        this.turbineType = turbineType;
    }

    public boolean isActive() {
        return recipeMapWorkable.isActive();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeTurbine(metaTileEntityId, turbineType);
    }

    @Override
    protected Vec3d getCenterOffset() {
        return null;//new Vec3i(1, -1);
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
            .aisle("CCCC", "CHHC", "CCCC")
            .aisle("CHHC", "R##D", "CHHC")
            .aisle("CCCC", "CSHC", "CCCC")
            .where('S', selfPredicate())
            .where('#', blockPredicate(Blocks.AIR))
            .where('C', statePredicate(getCasingState()))
            .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(getAllowedAbilities())))
            .where('R', abilityPartPredicate(ABILITY_ROTOR_HOLDER))
            .where('D', abilityPartPredicate(MultiblockAbility.OUTPUT_ENERGY))
            .build();
    }

    public MultiblockAbility[] getAllowedAbilities() {
        return turbineType.recipeMap.getMaxFluidOutputs() > 0 ?
            new MultiblockAbility[] {MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS} :
            new MultiblockAbility[] {MultiblockAbility.IMPORT_FLUIDS};
    }

    public IBlockState getCasingState() {
        return turbineType.casingState;
    }

    @Override
    public ICubeRenderer getBaseTexture() {
        return turbineType.casingRenderer;
    }

    @Override
    protected boolean shouldUseEnergyOutputs() {
        return true;
    }
}