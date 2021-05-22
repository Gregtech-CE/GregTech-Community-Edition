package gregtech.api.capability.impl;

import gregtech.api.GTValues;
import gregtech.api.capability.*;
import gregtech.api.metatileentity.MTETrait;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.machines.FuelRecipeMap;
import gregtech.api.recipes.recipes.FuelRecipe;
import gregtech.api.util.GTUtility;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class FuelRecipeLogic extends MTETrait implements IControllable, IFuelable {

    public final FuelRecipeMap recipeMap;
    protected FuelRecipe previousRecipe;

    protected final Supplier<IEnergyContainer> energyContainer;
    protected final Supplier<IMultipleTankHandler> fluidTank;
    public final long maxVoltage;

    private float recipeDurationLeft;
    private int fuelPerOperation;
    private int operationDuration;
    private Fluid currentFluid;

    private boolean isActive;
    private boolean workingEnabled = true;
    private boolean wasActiveAndNeedsUpdate = false;

    public FuelRecipeLogic(MetaTileEntity metaTileEntity, FuelRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> fluidTank, long maxVoltage) {
        super(metaTileEntity);
        this.recipeMap = recipeMap;
        this.energyContainer = energyContainer;
        this.fluidTank = fluidTank;
        this.maxVoltage = maxVoltage;
    }

    public long getRecipeOutputVoltage() {
        return (long) (getMaxVoltage() * getEnergyEfficiency());
    }

    @Override
    public String getName() {
        return "FuelRecipeMapWorkable";
    }

    @Override
    public int getNetworkID() {
        return TraitNetworkIds.TRAIT_ID_WORKABLE;
    }

    @Override
    public Collection<IFuelInfo> getFuels() {
        if (!isReadyForRecipes())
            return Collections.emptySet();
        final IMultipleTankHandler fluidTanks = this.fluidTank.get();
        if (fluidTanks == null)
            return Collections.emptySet();

        final LinkedHashMap<String, IFuelInfo> fuels = new LinkedHashMap<>();
        // Fuel capacity is all tanks
        int fuelCapacity = 0;
        for (IFluidTank fluidTank : fluidTanks) {
            fuelCapacity += fluidTank.getCapacity();
        }

        for (IFluidTank fluidTank : fluidTanks) {
            final FluidStack tankContents = fluidTank.drain(Integer.MAX_VALUE, false);
            if (tankContents == null || tankContents.amount <= 0)
                continue;
            int fuelRemaining = tankContents.amount;
            FuelRecipe recipe = findRecipe(tankContents);
            if (recipe == null)
                continue;
            int amountPerRecipe = calculateFuelAmount(recipe);
            int duration = calculateRecipeDuration(recipe);
            int fuelBurnTime = duration * fuelRemaining / amountPerRecipe;

            FluidFuelInfo fuelInfo = (FluidFuelInfo) fuels.get(tankContents.getUnlocalizedName());
            if (fuelInfo == null) {
                fuelInfo = new FluidFuelInfo(tankContents, fuelRemaining, fuelCapacity, amountPerRecipe, fuelBurnTime);
                fuels.put(tankContents.getUnlocalizedName(), fuelInfo);
            } else {
                fuelInfo.addFuelRemaining(fuelRemaining);
                fuelInfo.addFuelBurnTime(fuelBurnTime);
            }
        }
        return fuels.values();
    }

    @Override
    public <T> T getCapability(Capability<T> capability) {
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE) {
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        }
        if (capability == GregtechCapabilities.CAPABILITY_FUELABLE) {
            return GregtechCapabilities.CAPABILITY_FUELABLE.cast(this);
        }
        return null;
    }

    @Override
    public void update() {
        if (getMetaTileEntity().getWorld().isRemote) return;
        if (workingEnabled) {
            if (recipeDurationLeft >= getRecipeDurationMultiplier()) {
                if (energyContainer.get().getEnergyCanBeInserted() >=
                        getRecipeOutputVoltage() || shouldVoidExcessiveEnergy()) {
                    if (canProduceEnergy()) {
                        energyContainer.get().addEnergy(getRecipeOutputVoltage());
                    }
                    if (canConsumeFuel()) {
                        if (recipeDurationLeft % operationDuration < getRecipeDurationMultiplier()) { // last operation has ended time to re-consume fuel
                            IFluidTank tank = getCurrentTank(this.fuelPerOperation);
                            if (tank == null) {
                                this.recipeDurationLeft = 0;
                            } else {
                                tank.drain(this.fuelPerOperation, true);
                            }
                        }

                        this.recipeDurationLeft -= getRecipeDurationMultiplier();

                        if (this.recipeDurationLeft < getRecipeDurationMultiplier()) {
                            this.wasActiveAndNeedsUpdate = true;
                        }
                    }
                }
            }
            if (recipeDurationLeft < getRecipeDurationMultiplier() && isReadyForRecipes()) {
                tryAcquireNewRecipe();
            }
        }
        if (wasActiveAndNeedsUpdate) {
            setActive(false);
            this.recipeDurationLeft = 0;
            this.wasActiveAndNeedsUpdate = false;
        }
    }

    protected boolean isReadyForRecipes() {
        return true;
    }

    protected boolean shouldVoidExcessiveEnergy() {
        return false;
    }

    @Nullable
    private IFluidTank getCurrentTank(int minMb) {
        IMultipleTankHandler fluidTanks = this.fluidTank.get();
        for (IFluidTank fluidTank : fluidTanks) {
            FluidStack fluid = fluidTank.getFluid();
            if (fluid != null && fluid.getFluid().equals(this.currentFluid) && fluid.amount >= minMb) {
                return fluidTank;
            }
        }
        return null;
    }

    private void tryAcquireNewRecipe() {
        IMultipleTankHandler fluidTanks = this.fluidTank.get();
        for (IFluidTank fluidTank : fluidTanks) {
            FluidStack tankContents = fluidTank.getFluid();
            if (tankContents != null && tankContents.amount > 0) {
                if (tryAcquireNewRecipe(tankContents) > 0) {
                    this.currentFluid = tankContents.getFluid();
                    break; //recipe is found and ready to use
                }
            }
        }
    }

    public boolean isActive() {
        return isActive;
    }

    private int tryAcquireNewRecipe(FluidStack fluidStack) {
        FuelRecipe currentRecipe;
        if (previousRecipe != null && previousRecipe.matches(getMaxVoltage(), fluidStack)) {
            //if previous recipe still matches inputs, try to use it
            currentRecipe = previousRecipe;
        } else {
            //else, try searching new recipe for given inputs
            currentRecipe = recipeMap.findRecipe(getMaxVoltage(), fluidStack);
            //if we found recipe that can be buffered, buffer it
            if (currentRecipe != null) {
                this.previousRecipe = currentRecipe;
            }
        }
        if (currentRecipe != null && checkRecipe(currentRecipe)) {
            int fuelAmountToUse = calculateFuelAmount(currentRecipe);
            if (fluidStack.amount >= fuelAmountToUse) {
                int recipeDuration = calculateRecipeDuration(currentRecipe);
                this.recipeDurationLeft = recipeDuration;
                startRecipe(currentRecipe, fuelAmountToUse, recipeDuration);
                if (wasActiveAndNeedsUpdate) {
                    this.wasActiveAndNeedsUpdate = false;
                } else {
                    setActive(true);
                }

                int g = gcd(fuelAmountToUse, recipeDuration);

                this.operationDuration = recipeDuration / g;
                this.fuelPerOperation = fuelAmountToUse / g;

                return fuelAmountToUse;
            }
        }
        return 0;
    }

    // may need to move this somewhere else but where ?
    public int gcd(int a, int b) {
        if (b == 0) return a;
        return gcd(b, a % b);
    }

    // Similar to tryAcquire but with no side effects
    private FuelRecipe findRecipe(FluidStack fluidStack) {
        FuelRecipe currentRecipe;
        if (previousRecipe != null && previousRecipe.matches(getMaxVoltage(), fluidStack)) {
            currentRecipe = previousRecipe;
        } else {
            currentRecipe = recipeMap.findRecipe(getMaxVoltage(), fluidStack);
        }
        if (currentRecipe != null && checkRecipe(currentRecipe))
            return currentRecipe;
        return null;
    }

    protected boolean checkRecipe(FuelRecipe recipe) {
        return true;
    }

    public long getMaxVoltage() {
        return maxVoltage;
    }

    protected int calculateFuelAmount(FuelRecipe currentRecipe) {
        return (int) (currentRecipe.getRecipeFluid().amount *
                getVoltageMultiplier(getMaxVoltage(), currentRecipe.getMinVoltage()) *
                getFuelConsumptionMultiplier());
    }

    public int getFuelConsumption() {
        return this.previousRecipe != null ? calculateFuelAmount(this.previousRecipe) : 0;
    }

    protected int calculateRecipeDuration(FuelRecipe currentRecipe) {
        return (int) (currentRecipe.getDuration() * getRecipeDurationMultiplier());
    }

    public int getRecipeDuration() {
        return this.previousRecipe != null ? calculateRecipeDuration(this.previousRecipe) : 0;
    }

    /**
     * @return if the recipe duration should get decreased during the current tick does not effect energy production.
     */

    public boolean canConsumeFuel() {
        return true;
    }

    /**
     * @return if energy should be produced during the current tick does not effect fuel consumption.
     */
    public boolean canProduceEnergy() {
        return true;
    }

    /**
     * @return the multiplier applied to fuel consumption
     */
    public double getFuelConsumptionMultiplier() {
        return 1.0;
    }

    /**
     * @return the multiplier applied to recipe duration
     */
    public double getRecipeDurationMultiplier() {
        return 1.0f;
    }

    /**
     * @return the multiplier applied to voltage
     */
    public double getEnergyEfficiency() {
        return 1.0f;
    }

    /**
     * Performs preparations for starting given recipe output voltage is now given by getOutputVoltage
     *
     * @return currently unused
     */
    protected long startRecipe(FuelRecipe currentRecipe, int fuelAmountUsed, int recipeDuration) {
        return 1L;
    }

    public static int getVoltageMultiplier(long maxVoltage, long minVoltage) {
        return (int) (maxVoltage / minVoltage);
    }

    public static long getTieredVoltage(long voltage) {
        return GTValues.V[GTUtility.getTierByVoltage(voltage)];
    }

    protected void setActive(boolean active) {
        // If we are changing states (active -> inactive), clear remaining recipe duration
        if (this.isActive && !active) {
            recipeDurationLeft = 0;
        }
        this.isActive = active;
        if (!metaTileEntity.getWorld().isRemote) {
            metaTileEntity.markDirty();
            writeCustomData(1, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        this.workingEnabled = workingEnabled;
        metaTileEntity.markDirty();
    }

    @Override
    public boolean isWorkingEnabled() {
        return workingEnabled;
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
            getMetaTileEntity().getHolder().scheduleChunkForRenderUpdate();
        }
    }

    @Override
    public void writeInitialData(PacketBuffer buf) {
        buf.writeBoolean(this.isActive);
    }

    @Override
    public void receiveInitialData(PacketBuffer buf) {
        this.isActive = buf.readBoolean();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setBoolean("WorkEnabled", this.workingEnabled);
        // HELP
        compound.setInteger("RecipeDurationLeft", (int) this.recipeDurationLeft);
        compound.setInteger("FuelPerOperation", this.fuelPerOperation);
        compound.setInteger("OperationDuration", this.operationDuration);
        // how to save the currentFluid ?
        //compound.setTag("",this.currentFluid);

        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        if (!compound.hasKey("WorkEnabled", NBT.TAG_BYTE)) {
            //change working mode only if there is a tag compound with it's value
            this.workingEnabled = compound.getBoolean("WorkEnabled");
        }
        this.recipeDurationLeft = compound.getInteger("RecipeDurationLeft");
        this.isActive = recipeDurationLeft >= getFuelConsumptionMultiplier();

        if (compound.hasKey("FuelPerOperation")) {
            this.fuelPerOperation = compound.getInteger("FuelPerOperation");
        } else this.fuelPerOperation = 0;

        if (compound.hasKey("OperationDuration")) {
            this.operationDuration = compound.getInteger("OperationDuration");
        } else this.operationDuration = (int) this.recipeDurationLeft;
    }

}
