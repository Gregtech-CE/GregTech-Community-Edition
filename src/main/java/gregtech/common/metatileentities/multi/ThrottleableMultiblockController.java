package gregtech.common.metatileentities.multi;

import gregtech.api.gui.Widget;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withHoverTextTranslate;

public abstract class ThrottleableMultiblockController extends MultiblockWithDisplayBase {

    protected boolean canThrottle;
    protected int throttlePercentage = 100;

    public ThrottleableMultiblockController(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.canThrottle = false;
    }

    public ThrottleableMultiblockController(ResourceLocation metaTileEntityId, Boolean canThrottle) {
        super(metaTileEntityId);
        this.canThrottle = canThrottle;
    }

    public double getThrottleMultiplier() {
        return throttlePercentage / 100.0;
    }

    public double getThrottleEfficiency() {
        return MathHelper.clamp(1.0 + 0.3 * Math.log(getThrottleMultiplier()), 0.4, 1.0);
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed() && this.canThrottle) {
            ITextComponent throttleText = new TextComponentTranslation("gregtech.multiblock.throttle", throttlePercentage, (int) (getThrottleEfficiency() * 100));
            withHoverTextTranslate(throttleText, "gregtech.multiblock.throttle.tooltip");
            textList.add(throttleText);

            ITextComponent buttonText = new TextComponentTranslation("gregtech.multiblock.throttle_modify");
            buttonText.appendText(" ");
            buttonText.appendSibling(withButton(new TextComponentString("[-]"), "sub"));
            buttonText.appendText(" ");
            buttonText.appendSibling(withButton(new TextComponentString("[+]"), "add"));
            textList.add(buttonText);
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        super.handleDisplayClick(componentData, clickData);
        if (this.canThrottle) {
            int modifier = componentData.equals("add") ? 1 : 0;
            modifier += componentData.equals("sub") ? -1 : 0;
            int result = (clickData.isShiftClick ? 1 : 5) * modifier * (clickData.isCtrlClick ? 10 : 1);
            this.throttlePercentage = MathHelper.clamp(throttlePercentage + result, 20, 100);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("ThrottlePercentage", throttlePercentage);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey("ThrottlePercentage")) {
            this.throttlePercentage = data.getInteger("ThrottlePercentage");
        }
    }

}
