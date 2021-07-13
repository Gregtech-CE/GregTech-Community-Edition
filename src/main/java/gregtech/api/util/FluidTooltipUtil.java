package gregtech.api.util;

import com.google.common.collect.Lists;
import com.mojang.realmsclient.gui.ChatFormatting;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.stack.MaterialStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FluidTooltipUtil {

    /**
     * Registry Mapping of <Fluid, Tooltip>
     */
    private static final Map<Fluid, List<String>> tooltips = new HashMap<>();

    /**
     * Used to register a tooltip to a Fluid. A Fluid can only have one tooltip, on one line.
     *
     * Ignores a tooltip applied for Water, so that it will be handled correctly for the chemical formula.
     *
     * @param fluid   The fluid to register a tooltip for.
     * @param tooltips The tooltip.
     * @return        False if either parameter is null or if tooltip is empty, true otherwise.
     */
    public static boolean registerTooltip(Fluid fluid, List<String> tooltips) {
        if (fluid != null && tooltips != null && !tooltips.isEmpty()) {
            for (String tooltip : tooltips) {
                registerTooltip(fluid, tooltip);
            }
        }
        return false;
    }

    public static boolean registerTooltip(Fluid fluid, String tooltip) {
        if (fluid != null && tooltip != null && !tooltip.trim().isEmpty()) {
            if(tooltips.containsKey(fluid)) {
                tooltips.get(fluid).add(tooltip);
            } else {
                tooltips.put(fluid, Lists.newArrayList(tooltip));
            }
            return true;
        }
        return false;
    }



    /**
     * Used to get a Fluid's tooltip.
     *
     * @param fluid The Fluid to get the tooltip of.
     * @return      The tooltip.
     */
    public static List<String> getFluidTooltips(Fluid fluid) {
        if (fluid == null)
            return null;

        return tooltips.get(fluid);
    }

    @Deprecated
    public static String getFluidTooltip(Fluid fluid) {
        if (fluid == null)
            return null;

        return tooltips.get(fluid).get(1);
    }

    /**
     * Used to get a Fluid's tooltip.
     *
     * @param stack A FluidStack, containing the Fluid to get the tooltip of.
     * @return      The tooltip.
     */
    public static List<String> getFluidTooltips(FluidStack stack) {
        if (stack == null)
            return null;

        return getFluidTooltips(stack.getFluid());
    }

    @Deprecated
    public static String getFluidTooltip(FluidStack stack) {
        if (stack == null)
            return null;

        return getFluidTooltip(stack.getFluid());
    }

    /**
     * Used to get a Fluid's tooltip.
     *
     * @param fluidName A String representing a Fluid to get the tooltip of.
     * @return          The tooltip.
     */
    public static List<String> getFluidTooltips(String fluidName) {
        if (fluidName == null || fluidName.isEmpty())
            return null;

        return getFluidTooltips(FluidRegistry.getFluid(fluidName));
    }

    @Deprecated
    public static String getFluidTooltip(String fluidName) {
        if (fluidName == null || fluidName.isEmpty())
            return null;

        return getFluidTooltip(FluidRegistry.getFluid(fluidName));
    }

    /**
     * A simple helper method to get the tooltip for Water, since it is an edge case of fluids.
     *
     * @return "H₂O"
     */
    public static String getWaterTooltip() {
        // Done like this to not return parenthesis around the tooltip
        return (ChatFormatting.GRAY + (new MaterialStack(Materials.Hydrogen, 2)).toString() + "O");
    }
}
