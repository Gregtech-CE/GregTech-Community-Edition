package gregtech.common.metatileentities.multi.electric.generator;

import gregtech.api.GTValues;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.machines.FuelRecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.common.blocks.BlockTurbineCasing.TurbineCasingType;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

public class MetaTileEntityLargeTurbine extends RotorHolderMultiblockController {

    private static final int MIN_DURABILITY_TO_WARN = 10;

    public enum TurbineType {

        STEAM(RecipeMaps.STEAM_TURBINE_FUELS, MetaBlocks.TURBINE_CASING.getState(TurbineCasingType.STEEL_TURBINE_CASING), Textures.SOLID_STEEL_CASING, true),
        GAS(RecipeMaps.GAS_TURBINE_FUELS, MetaBlocks.TURBINE_CASING.getState(TurbineCasingType.STAINLESS_TURBINE_CASING), Textures.CLEAN_STAINLESS_STEEL_CASING, false),
        PLASMA(RecipeMaps.PLASMA_GENERATOR_FUELS, MetaBlocks.TURBINE_CASING.getState(TurbineCasingType.TUNGSTENSTEEL_TURBINE_CASING), Textures.ROBUST_TUNGSTENSTEEL_CASING, true);

        public final FuelRecipeMap recipeMap;
        public final IBlockState casingState;
        public final ICubeRenderer casingRenderer;
        public final boolean hasOutputHatch;

        TurbineType(FuelRecipeMap recipeMap, IBlockState casingState, ICubeRenderer casingRenderer, boolean hasOutputHatch) {
            this.recipeMap = recipeMap;
            this.casingState = casingState;
            this.casingRenderer = casingRenderer;
            this.hasOutputHatch = hasOutputHatch;
        }
    }

    public final TurbineType turbineType;
    public IFluidHandler exportFluidHandler;

    public MetaTileEntityLargeTurbine(ResourceLocation metaTileEntityId, TurbineType turbineType) {
        super(metaTileEntityId, turbineType.recipeMap, GTValues.V[4], true);
        this.turbineType = turbineType;
        reinitializeStructurePattern();
    }

    @Override
    protected FuelRecipeLogic createWorkable(long maxVoltage) {
        return new LargeTurbineWorkableHandler(this, recipeMap, () -> energyContainer, () -> importFluidHandler);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeTurbine(metaTileEntityId, turbineType);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.exportFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.EXPORT_FLUIDS));
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.exportFluidHandler = null;
    }

    @Override
    public int getRotorSpeedIncrement() {
        return 1;
    }

    @Override
    public int getRotorSpeedDecrement() {
        return -3;
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (isStructureFormed()) {
            MetaTileEntityRotorHolder rotorHolder = getRotorHolder();
            FluidStack fuelStack = ((LargeTurbineWorkableHandler) workableHandler).getFuelStack();
            int fuelAmount = fuelStack == null ? 0 : fuelStack.amount;

            ITextComponent fuelName = new TextComponentTranslation(fuelAmount == 0 ? "gregtech.fluid.empty" : fuelStack.getUnlocalizedName());
            textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.fuel_amount", fuelAmount, fuelName));
            int consumption = 0;
            if (workableHandler.getRecipeDuration() > 0) {
                consumption = workableHandler.getFuelConsumption() / workableHandler.getRecipeDuration();
            }
            textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.consumption_rate", consumption));

            if (rotorHolder.getRotorEfficiency() > 0.0) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.rotor_speed", rotorHolder.getCurrentRotorSpeed(), rotorHolder.getMaxRotorSpeed()));
                textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.rotor_efficiency", (int) (rotorHolder.getRotorEfficiency() * 100)));
                int rotorDurability = (int) (rotorHolder.getRotorDurability() * 100);
                if (rotorDurability > MIN_DURABILITY_TO_WARN) {
                    textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.rotor_durability", rotorDurability));
                } else {
                    textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.low_rotor_durability",
                            MIN_DURABILITY_TO_WARN, rotorDurability).setStyle(new Style().setColor(TextFormatting.RED)));
                }
            }
        }
        super.addDisplayText(textList);
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return turbineType == null ? null :
                FactoryBlockPattern.start()
                        .aisle("CCCC", "CHHC", "CCCC")
                        .aisle("CHHC", "R##D", "CHHC")
                        .aisle("CCCC", "CSHC", "CCCC")
                        .where('S', selfPredicate())
                        .where('#', isAirPredicate())
                        .where('C', statePredicate(getCasingState()))
                        .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(getAllowedAbilities())))
                        .where('R', abilityPartPredicate(ABILITY_ROTOR_HOLDER))
                        .where('D', abilityPartPredicate(MultiblockAbility.OUTPUT_ENERGY))
                        .build();
    }

    public MultiblockAbility[] getAllowedAbilities() {
        return turbineType.hasOutputHatch ?
                new MultiblockAbility[]{MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS} :
                new MultiblockAbility[]{MultiblockAbility.IMPORT_FLUIDS};
    }

    public IBlockState getCasingState() {
        return turbineType.casingState;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return turbineType.casingRenderer;
    }

    /**
     * Deprecated method please use {@code {@see isRotorFaceFree}} instead
     */
    @Deprecated
    public boolean isTurbineFaceFree() {
        return isRotorFaceFree();
    }

}
