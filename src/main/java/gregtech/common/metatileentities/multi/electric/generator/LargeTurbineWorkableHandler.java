package gregtech.common.metatileentities.multi.electric.generator;

import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.recipes.machines.FuelRecipeMap;
import gregtech.api.recipes.recipes.FuelRecipe;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.material.type.FluidMaterial;
import gregtech.common.ConfigHolder;
import gregtech.common.MetaFluids;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine.TurbineType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Supplier;

public class LargeTurbineWorkableHandler extends FuelRecipeLogic {

    private static final int CYCLE_LENGTH = 230;
    private static final int BASE_ROTOR_DAMAGE = 11;
    private static final int BASE_EU_OUTPUT = 2048;

    private MetaTileEntityLargeTurbine largeTurbine;
    private int rotorCycleLength = CYCLE_LENGTH;
    private double cachedThrottle = 0;

    public LargeTurbineWorkableHandler(MetaTileEntityLargeTurbine metaTileEntity, FuelRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> fluidTank) {
        super(metaTileEntity, recipeMap, energyContainer, fluidTank, 0L);
        this.largeTurbine = metaTileEntity;
    }

    @Override
    public void update() {
        super.update();
        MetaTileEntityRotorHolder rotorHolder = largeTurbine.getAbilities(MetaTileEntityLargeTurbine.ABILITY_ROTOR_HOLDER).get(0);
        if (!rotorHolder.isHasRotor()) {
            setActive(false);
        }
    }

    public FluidStack getFuelStack() {
        if (previousRecipe == null)
            return null;
        FluidStack fuelStack = previousRecipe.getRecipeFluid();
        return fluidTank.get().drain(new FluidStack(fuelStack.getFluid(), Integer.MAX_VALUE), false);
    }

    @Override
    public boolean checkRecipe(FuelRecipe recipe) {
        MetaTileEntityRotorHolder rotorHolder = largeTurbine.getAbilities(MetaTileEntityLargeTurbine.ABILITY_ROTOR_HOLDER).get(0);
        if (++rotorCycleLength >= CYCLE_LENGTH) {
            int damageToBeApplied = (int) Math.round(BASE_ROTOR_DAMAGE * rotorHolder.getRelativeRotorSpeed()) + 1;
            if (rotorHolder.applyDamageToRotor(damageToBeApplied, false)) {
                this.rotorCycleLength = 0;
                return true;
            } else return false;
        }
        return true;
    }

    @Override
    public long getMaxVoltage() {
        return BASE_EU_OUTPUT + getBonusForTurbineType(largeTurbine);
    }

    @Override
    protected boolean isReadyForRecipes() {
        MetaTileEntityRotorHolder rotorHolder = largeTurbine.getAbilities(MetaTileEntityLargeTurbine.ABILITY_ROTOR_HOLDER).get(0);
        return rotorHolder.isHasRotor();
    }

    @Override
    protected long startRecipe(FuelRecipe currentRecipe, int fuelAmountUsed, int recipeDuration) {
        addOutputFluids(currentRecipe, fuelAmountUsed);
        this.cachedThrottle = this.largeTurbine.getThrottleMultiplier();
        return super.startRecipe(currentRecipe, fuelAmountUsed, recipeDuration);
    }

    private void addOutputFluids(FuelRecipe currentRecipe, int fuelAmountUsed) {
        if (largeTurbine.turbineType == TurbineType.STEAM) {
            int waterFluidAmount = fuelAmountUsed / 15;
            if (waterFluidAmount > 0) {
                FluidStack waterStack = Materials.Water.getFluid(waterFluidAmount);
                largeTurbine.exportFluidHandler.fill(waterStack, true);
            }
        } else if (largeTurbine.turbineType == TurbineType.PLASMA) {
            FluidMaterial material = MetaFluids.getMaterialFromFluid(currentRecipe.getRecipeFluid().getFluid());
            if (material != null) {
                largeTurbine.exportFluidHandler.fill(material.getFluid(fuelAmountUsed), true);
            }
        }
    }

    private int getBonusForTurbineType(MetaTileEntityLargeTurbine turbine) {
        switch (turbine.turbineType) {
            case GAS:
                return ConfigHolder.gasTurbineBonusOutput;
            case PLASMA:
                return ConfigHolder.plasmaTurbineBonusOutput;
            case STEAM:
                return ConfigHolder.steamTurbineBonusOutput;
            default:
                return 1;
        }
    }

    @Override
    public double getEnergyEfficiency() { //energy is added each tick while the rotor speed is >0 RPM
        MetaTileEntityRotorHolder rotorHolder = largeTurbine.getAbilities(MetaTileEntityLargeTurbine.ABILITY_ROTOR_HOLDER).get(0);
        double relativeRotorSpeed = rotorHolder.getRelativeRotorSpeed();
        return rotorHolder.getRotorEfficiency() *
                (relativeRotorSpeed * relativeRotorSpeed) *
                this.largeTurbine.getThrottleEfficiency() * this.largeTurbine.getThrottleMultiplier();
    }

    @Override
    public boolean canProduceEnergy() {
        MetaTileEntityRotorHolder rotorHolder = largeTurbine.getAbilities(MetaTileEntityLargeTurbine.ABILITY_ROTOR_HOLDER).get(0);
        return rotorHolder.getCurrentRotorSpeed() > 0 && rotorHolder.hasRotorInInventory();
    }

    @Override
    public double getFuelConsumptionMultiplier() {
        return this.largeTurbine.getThrottleMultiplier();
    }

    @Override
    public double getRecipeDurationMultiplier() {
        return this.largeTurbine.getThrottleMultiplier() / this.cachedThrottle;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("CycleLength", rotorCycleLength);
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        this.rotorCycleLength = compound.getInteger("CycleLength");
    }

    @Override
    protected boolean shouldVoidExcessiveEnergy() {
        return true;
    }
}
