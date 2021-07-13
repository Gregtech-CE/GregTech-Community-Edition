package gregtech.common.covers.filter;

import gregtech.api.capability.ConfigurationContext;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.util.GTUtility;
import gregtech.api.util.IDirtyNotifiable;
import gregtech.api.util.ItemStackKey;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class ItemFilterContainer implements INBTSerializable<NBTTagCompound> {

    private static final String WRONG_FILTER = "metaitem.configurator.wrong_filter";

    private final ItemStackHandler filterInventory;
    private final ItemFilterWrapper filterWrapper;
    private int maxStackSizeLimit = 1;
    private int transferStackSize;

    public ItemFilterContainer(IDirtyNotifiable dirtyNotifiable) {
        this.filterWrapper = new ItemFilterWrapper(dirtyNotifiable);
        this.filterWrapper.setOnFilterInstanceChange(this::onFilterInstanceChange);
        this.filterInventory = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return FilterTypeRegistry.getItemFilterForStack(stack) != null;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onLoad() {
                onFilterSlotChange(false);
            }

            @Override
            protected void onContentsChanged(int slot) {
                onFilterSlotChange(true);
            }
        };
    }

    public ItemStackHandler getFilterInventory() {
        return filterInventory;
    }

    private void onFilterInstanceChange() {
        this.filterWrapper.setMaxStackSize(getTransferStackSize());
    }

    public int getMaxStackSize() {
        return maxStackSizeLimit;
    }

    public int getTransferStackSize() {
        if (!showGlobalTransferLimitSlider()) {
            return getMaxStackSize();
        }
        return transferStackSize;
    }

    public void setTransferStackSize(int transferStackSize) {
        this.transferStackSize = MathHelper.clamp(transferStackSize, 1, getMaxStackSize());
        this.filterWrapper.setMaxStackSize(getTransferStackSize());
    }

    public void adjustTransferStackSize(int amount) {
        setTransferStackSize(transferStackSize + amount);
    }

    public void initUI(int y, Consumer<Widget> widgetGroup) {
        widgetGroup.accept(new LabelWidget(10, y, "cover.conveyor.item_filter.title"));
        widgetGroup.accept(new SlotWidget(filterInventory, 0, 10, y + 15)
            .setBackgroundTexture(GuiTextures.SLOT, GuiTextures.FILTER_SLOT_OVERLAY));

        ServerWidgetGroup stackSizeGroup = new ServerWidgetGroup(this::showGlobalTransferLimitSlider);
        stackSizeGroup.addWidget(new ClickButtonWidget(91, 70, 20, 20, "-1", data -> adjustTransferStackSize(data.isShiftClick ? -10 : -1)));
        stackSizeGroup.addWidget(new ClickButtonWidget(146, 70, 20, 20, "+1", data -> adjustTransferStackSize(data.isShiftClick ? +10 : +1)));
        stackSizeGroup.addWidget(new ImageWidget(111, 70, 35, 20, GuiTextures.DISPLAY));
        stackSizeGroup.addWidget(new SimpleTextWidget(128, 80, "", 0xFFFFFF, () -> Integer.toString(transferStackSize)));
        widgetGroup.accept(stackSizeGroup);

        this.filterWrapper.initUI(y + 38, widgetGroup);
    }

    protected void onFilterSlotChange(boolean notify) {
        ItemStack filterStack = filterInventory.getStackInSlot(0);
        ItemFilter newItemFilter = FilterTypeRegistry.getItemFilterForStack(filterStack);
        ItemFilter currentItemFilter = filterWrapper.getItemFilter();
        if(newItemFilter == null) {
            if(currentItemFilter != null) {
                filterWrapper.setItemFilter(null);
                filterWrapper.setBlacklistFilter(false);
                if (notify) filterWrapper.onFilterInstanceChange();
            }
        } else if (currentItemFilter == null ||
            newItemFilter.getClass() != currentItemFilter.getClass()) {
            filterWrapper.setItemFilter(newItemFilter);
            if (notify) filterWrapper.onFilterInstanceChange();
        }
    }

    public void setMaxStackSize(int maxStackSizeLimit) {
        this.maxStackSizeLimit = maxStackSizeLimit;
        setTransferStackSize(transferStackSize);
    }

    public boolean showGlobalTransferLimitSlider() {
        return getMaxStackSize() > 1 && filterWrapper.showGlobalTransferLimitSlider();
    }

    public int getSlotTransferLimit(Object slotIndex, Set<ItemStackKey> matchedStacks) {
        return filterWrapper.getSlotTransferLimit(slotIndex, matchedStacks, getTransferStackSize());
    }

    public Object matchItemStack(ItemStack itemStack) {
        return filterWrapper.matchItemStack(itemStack);
    }

    public boolean testItemStack(ItemStack itemStack) {
        return matchItemStack(itemStack) != null;
    }

    /**
     * Remove the filter into an external inventory
     *
     * @param externalInventory the external inventory
     * @param simulate true to simulate the clear
     * @return true when there is no filter or the filter can be placed in the inventory
     */
    public boolean clearFilter(final IItemHandler externalInventory, final boolean simulate) {
        final ItemStack currentFilter = this.filterInventory.getStackInSlot(0);
        if (currentFilter.isEmpty()) {
            return true;
        }
        if (!ItemHandlerHelper.insertItemStacked(externalInventory, currentFilter, simulate).isEmpty()) {
            return false;
        }
        if (!simulate) {
            this.filterInventory.extractItem(0, Integer.MAX_VALUE, false);
        }
        return true;
    }

    /**
     * Swaps the filter with one from an external inventory
     *
     * @param requiredFilter the filter to be used
     * @param externalInventory the external inventory
     * @return true when the filter can be obtained from the external inventory
     */
    public boolean swapFilter(final ItemStack requiredFilter, final IItemHandler externalInventory) {
        if (!this.filterInventory.isItemValid(0, requiredFilter)) {
            return false;
        }
        if (!extractItem(requiredFilter, externalInventory, true)) {
            return false;
        }
        // Review: This will stop the swap if there is no space, even though there will be space freed when the new filter is removed
        //         but not doing this check will mean we void items for read only inventories
        if (!clearFilter(externalInventory, true)) {
            return false;
        }
        extractItem(requiredFilter, externalInventory, false);
        clearFilter(externalInventory, false);
        this.filterInventory.insertItem(0, requiredFilter, false);
        return true;
    }

    private static boolean extractItem(final ItemStack itemStack, final IItemHandler itemHandler, final boolean simulate) {
        final int count = itemStack.getCount();
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            final ItemStack stackInSlot = itemHandler.extractItem(i, count, true);
            if (equalsIgnoreNBT(itemStack, stackInSlot)) {
                if (!simulate) {
                    itemHandler.extractItem(i, count, false);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean equalsIgnoreNBT(final ItemStack one, final ItemStack two) {
        return one.getItem().equals(two.getItem()) && one.getCount() == two.getCount() &&
                GTUtility.getActualItemDamageFromStack(one) == GTUtility.getActualItemDamageFromStack(two);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setTag("FilterInventory", filterInventory.serializeNBT());
        tagCompound.setBoolean("IsBlacklist", filterWrapper.isBlacklistFilter());
        tagCompound.setInteger("MaxStackSize", maxStackSizeLimit);
        tagCompound.setInteger("TransferStackSize", transferStackSize);
        if(filterWrapper.getItemFilter() != null) {
            NBTTagCompound filterInventory = new NBTTagCompound();
            filterWrapper.getItemFilter().writeToNBT(filterInventory);
            tagCompound.setTag("Filter", filterInventory);
        }
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tagCompound) {
        this.filterInventory.deserializeNBT(tagCompound.getCompoundTag("FilterInventory"));
        this.filterWrapper.setBlacklistFilter(tagCompound.getBoolean("IsBlacklist"));
        if(tagCompound.hasKey("MaxStackSize")) {
            setMaxStackSize(tagCompound.getInteger("MaxStackSize"));
        }
        if (tagCompound.hasKey("TransferStackSize")) {
            setTransferStackSize(tagCompound.getInteger("TransferStackSize"));
        }
        if(filterWrapper.getItemFilter() != null) {
            //LEGACY SAVE FORMAT SUPPORT
            if(tagCompound.hasKey("ItemFilter") ||
                tagCompound.hasKey("OreDictionaryFilter")) {
                this.filterWrapper.getItemFilter().readFromNBT(tagCompound);
            } else {
                NBTTagCompound filterInventory = tagCompound.getCompoundTag("Filter");
                this.filterWrapper.getItemFilter().readFromNBT(filterInventory);
            }
        }
    }

    public NBTTagCompound copyConfiguration(final ConfigurationContext context) {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setTag("FilterInventory", this.filterInventory.serializeNBT());
        tagCompound.setBoolean("IsBlacklist", this.filterWrapper.isBlacklistFilter());
        tagCompound.setInteger("MaxStackSize", this.maxStackSizeLimit);
        tagCompound.setInteger("TransferStackSize", this.transferStackSize);
        final ItemFilter filter = this.filterWrapper.getItemFilter();
        if (filter != null) {
            final NBTTagCompound filterInventory = new NBTTagCompound();
            filter.writeToNBT(filterInventory);
            tagCompound.setTag("Filter", filterInventory);
            tagCompound.setString("FilterClassName", filter.getClass().getName());
        }
        return tagCompound;
    }

    public void pasteConfiguration(final ConfigurationContext context, final NBTTagCompound tagCompound) {
        ItemFilter filter = this.filterWrapper.getItemFilter();
        final String configClassName = tagCompound.getString("FilterClassName");
        final String filterClassName = filter != null ? filter.getClass().getName() : null;
        if (!Objects.equals(configClassName, filterClassName) && !swapFilter(context, tagCompound)) {
            context.sendMessage(new TextComponentTranslation(WRONG_FILTER));
            return;
        }

        this.filterInventory.deserializeNBT(tagCompound.getCompoundTag("FilterInventory"));
        this.filterWrapper.setBlacklistFilter(tagCompound.getBoolean("IsBlacklist"));
        if (tagCompound.hasKey("MaxStackSize")) {
            setMaxStackSize(tagCompound.getInteger("MaxStackSize"));
        }
        if (tagCompound.hasKey("TransferStackSize")) {
            setTransferStackSize(tagCompound.getInteger("TransferStackSize"));
        }
        filter = this.filterWrapper.getItemFilter();
        if (filter != null) {
            //LEGACY SAVE FORMAT SUPPORT
            if (tagCompound.hasKey("ItemFilter") ||
                tagCompound.hasKey("OreDictionaryFilter")) {
                filter.readFromNBT(tagCompound);
            } else {
                final NBTTagCompound filterInventory = tagCompound.getCompoundTag("Filter");
                filter.readFromNBT(filterInventory);
            }
        }
    }

    private boolean swapFilter(final ConfigurationContext context, final NBTTagCompound tagCompound) {
        final IItemHandler externalInventory = context.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (externalInventory == null) {
            return false;
        }

        final ItemStackHandler requiredFilters = new ItemStackHandler(1);
        requiredFilters.deserializeNBT(tagCompound.getCompoundTag("FilterInventory"));
        final ItemStack requiredFilter = requiredFilters.getStackInSlot(0);

        if (requiredFilter.isEmpty()) {
            return clearFilter(externalInventory, false);
        }
        return swapFilter(requiredFilter, externalInventory);
    }
}
