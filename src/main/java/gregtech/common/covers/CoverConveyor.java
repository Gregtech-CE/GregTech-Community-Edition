package gregtech.common.covers;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregtech.api.GTValues;
import gregtech.api.capability.impl.ItemHandlerDelegate;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverWithUI;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.api.util.watch.WatchedItemStackHandler;
import gregtech.common.items.MetaItems;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.BiFunction;

public class CoverConveyor extends CoverBehavior implements CoverWithUI, ITickable {

    public final int tier;
    public final int maxItemTransferRate;
    protected int transferRate;
    protected ConveyorMode conveyorMode;
    protected final ItemStackHandler filterTypeInventory;
    protected FilterType filterMode;
    protected String oreDictionaryFilter;
    protected ItemStackHandler itemFilterSlots;
    protected boolean ignoreDamage = true;
    protected boolean ignoreNBTData = true;
    protected int itemsLeftToTransferLastSecond;
    private CoverableItemHandlerWrapper itemHandlerWrapper;
    
    public CoverConveyor(ICoverable coverable, EnumFacing attachedSide, int tier, int itemsPerSecond) {
        super(coverable, attachedSide);
        this.tier = tier;
        this.maxItemTransferRate = itemsPerSecond;
        this.transferRate = maxItemTransferRate;
        this.itemsLeftToTransferLastSecond = transferRate;
        this.conveyorMode = ConveyorMode.IMPORT;
        this.filterTypeInventory = new FilterItemStackHandler();
        this.filterMode = FilterType.NONE;
        this.oreDictionaryFilter = "";
        this.itemFilterSlots = new WatchedItemStackHandler(9) {
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    protected void setTransferRate(int transferRate) {
        this.transferRate = transferRate;
        coverHolder.markDirty();
    }

    protected void adjustTransferRate(int amount) {
        setTransferRate(MathHelper.clamp(transferRate + amount, 1, maxItemTransferRate));
    }

    protected void setConveyorMode(ConveyorMode conveyorMode) {
        this.conveyorMode = conveyorMode;
        coverHolder.markDirty();
    }

    protected void setIgnoreDamage(boolean ignoreDamage) {
        this.ignoreDamage = ignoreDamage;
        coverHolder.markDirty();
    }

    protected void setIgnoreNBTData(boolean ignoreNBTData) {
        this.ignoreNBTData = ignoreNBTData;
        coverHolder.markDirty();
    }

    protected void setOreDictionaryFilter(String filter) {
        this.oreDictionaryFilter = filter;
        coverHolder.markDirty();
    }

    @Override
    public void update() {
        doTransferAny();
    }

    protected void doTransferAny() {
        long timer = coverHolder.getTimer();
        if(timer % 5 == 0 && itemsLeftToTransferLastSecond > 0) {
            int[] itemsTransfer = doTransferItems(itemsLeftToTransferLastSecond, null,false);
            int totalTransferred = 0;
            for (int value : itemsTransfer) {
                totalTransferred += value;
            }
            this.itemsLeftToTransferLastSecond -= totalTransferred;
        }
        if(timer % 20 == 0) {
            this.itemsLeftToTransferLastSecond = transferRate;
        }
    }

    protected int[] doTransferItems(int maxTransferAmount, int[] transferLimit, boolean simulate) {
        TileEntity tileEntity = coverHolder.getWorld().getTileEntity(coverHolder.getPos().offset(attachedSide));
        IItemHandler itemHandler = tileEntity == null ? null : tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, attachedSide.getOpposite());
        IItemHandler myItemHandler = coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, attachedSide);
        if(itemHandler == null || myItemHandler == null) {
            return new int[filterMode.maxMatchSlots];
        }
        return doTransferItemsInternal(itemHandler, myItemHandler, maxTransferAmount, transferLimit, simulate);
    }

    protected int[] doTransferItemsInternal(IItemHandler itemHandler, IItemHandler myItemHandler, int maxTransferAmount, int[] transferLimit, boolean simulate) {
        if(conveyorMode == ConveyorMode.IMPORT) {
            return moveInventoryItems(itemHandler, myItemHandler, simulate, transferLimit, maxTransferAmount);
        } else if(conveyorMode == ConveyorMode.EXPORT) {
            return moveInventoryItems(myItemHandler, itemHandler, simulate, transferLimit, maxTransferAmount);
        }
        return new int[filterMode.maxMatchSlots];
    }

    protected int[] doCountDestinationInventoryItems(IItemHandler itemHandler, IItemHandler myItemHandler) {
        if(conveyorMode == ConveyorMode.IMPORT) {
            return countInventoryItems(myItemHandler);
        } else if(conveyorMode == ConveyorMode.EXPORT) {
            return countInventoryItems(itemHandler);
        }
        return new int[filterMode.maxMatchSlots];
    }

    protected int[] moveInventoryItems(IItemHandler sourceInventory, IItemHandler targetInventory, boolean simulate, int[] transferLimit, int maxTransferAmount) {
        int itemsLeftToTransfer = maxTransferAmount;
        int[] itemTypesLeftToTransfer = transferLimit == null ? null : Arrays.copyOf(transferLimit, transferLimit.length);
        int[] itemsTransfer = new int[filterMode.maxMatchSlots];
        for(int srcIndex = 0; srcIndex < sourceInventory.getSlots(); srcIndex++) {
            ItemStack sourceStack = sourceInventory.extractItem(srcIndex, itemsLeftToTransfer, true);
            if(sourceStack.isEmpty()) {
                continue;
            }
            int transferSlotIndex = filterMode.matcher.apply(this, sourceStack);
            if(transferSlotIndex == -1) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(targetInventory, sourceStack, true);
            int amountToInsert = sourceStack.getCount() - remainder.getCount();
            if(itemTypesLeftToTransfer != null) {
                amountToInsert = Math.min(amountToInsert, itemTypesLeftToTransfer[transferSlotIndex]);
            }
            if(amountToInsert > 0) {
                sourceStack = sourceInventory.extractItem(srcIndex, amountToInsert, simulate);
                if(!sourceStack.isEmpty()) {
                    if(!simulate) {
                        ItemHandlerHelper.insertItemStacked(targetInventory, sourceStack, false);
                    }
                    itemsLeftToTransfer -= sourceStack.getCount();
                    itemsTransfer[transferSlotIndex] += sourceStack.getCount();
                    if(itemTypesLeftToTransfer != null) {
                        itemTypesLeftToTransfer[transferSlotIndex] -= sourceStack.getCount();
                    }
                    if(itemsLeftToTransfer == 0) break;
                }
            }
        }
        return itemsTransfer;
    }

    protected int[] countInventoryItems(IItemHandler inventory) {
        int[] itemsCount = new int[filterMode.maxMatchSlots];
        for(int srcIndex = 0; srcIndex < inventory.getSlots(); srcIndex++) {
            ItemStack itemStack = inventory.getStackInSlot(srcIndex);
            if (itemStack.isEmpty()) {
                continue;
            }
            int transferSlotIndex = filterMode.matcher.apply(this, itemStack);
            if (transferSlotIndex == -1) {
                continue;
            }
            itemsCount[transferSlotIndex] += itemStack.getCount();
        }
        return itemsCount;
    }

    @Override
    public boolean canAttach() {
        return coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, attachedSide) != null;
    }

    @Override
    public void onRemoved() {
        NonNullList<ItemStack> drops = NonNullList.create();
        MetaTileEntity.clearInventory(drops, filterTypeInventory);
        for(ItemStack itemStack : drops) {
            Block.spawnAsEntity(coverHolder.getWorld(), coverHolder.getPos(), itemStack);
        }
    }

    @Override
    public void renderCover(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline, Cuboid6 plateBox) {
        Textures.CONVEYOR_OVERLAY.renderSided(attachedSide, plateBox, renderState, pipeline, translation);
    }

    protected void onFilterModeUpdated() {
    }

    @Override
    public EnumActionResult onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, float hitX, float hitY, float hitZ) {
        if(!coverHolder.getWorld().isRemote) {
            openUI((EntityPlayerMP) playerIn);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, T defaultValue) {
        if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            IItemHandler delegate = (IItemHandler) defaultValue;
            if(itemHandlerWrapper == null || itemHandlerWrapper.delegate != delegate) {
                this.itemHandlerWrapper = new CoverableItemHandlerWrapper(delegate);
            }
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandlerWrapper);
        }
        return defaultValue;
    }

    protected String getUITitle() {
        return "cover.conveyor.title";
    }

    protected WidgetGroup buildPrimaryUIGroup() {
        WidgetGroup primaryGroup = new WidgetGroup();
        primaryGroup.addWidget(new LabelWidget(10, 5, getUITitle(), GTValues.VN[tier]));
        primaryGroup.addWidget(new ClickButtonWidget(10, 20, 20, 20, "-10", data -> adjustTransferRate(data.isShiftClick ? -100 : -10)));
        primaryGroup.addWidget(new ClickButtonWidget(146, 20, 20, 20, "+10", data -> adjustTransferRate(data.isShiftClick ? +100 : +10)));
        primaryGroup.addWidget(new ClickButtonWidget(30, 20, 20, 20, "-1", data -> adjustTransferRate(data.isShiftClick ? -5 : -1)));
        primaryGroup.addWidget(new ClickButtonWidget(126, 20, 20, 20, "+1", data -> adjustTransferRate(data.isShiftClick ? +5 : +1)));
        primaryGroup.addWidget(new ImageWidget(50, 20, 76, 20, GuiTextures.DISPLAY));
        primaryGroup.addWidget(new SimpleTextWidget(88, 30, "cover.conveyor.transfer_rate", 0xFFFFFF, () -> Integer.toString(transferRate)));

        primaryGroup.addWidget(new CycleButtonWidget(10, 45, 75, 20,
            GTUtility.mapToString(ConveyorMode.values(), it -> it.localeName),
            () -> conveyorMode.ordinal(), newMode -> setConveyorMode(ConveyorMode.values()[newMode])));
        primaryGroup.addWidget(new LabelWidget(10, 70, "cover.conveyor.item_filter.title"));
        primaryGroup.addWidget(new SlotWidget(filterTypeInventory, 0, 10, 85)
            .setBackgroundTexture(GuiTextures.SLOT, GuiTextures.FILTER_SLOT_OVERLAY));
        return primaryGroup;
    }

    protected ModularUI buildUI(ModularUI.Builder builder, EntityPlayer player) {
        return builder.build(this, player);
    }

    @Override
    public ModularUI createUI(EntityPlayer player) {
        WidgetGroup primaryGroup = buildPrimaryUIGroup();
        ServerWidgetGroup itemFilterGroup = new ServerWidgetGroup(() -> filterMode == FilterType.ITEM_FILTER);
        for(int i = 0; i < 9; i++) {
            itemFilterGroup.addWidget(new PhantomSlotWidget(itemFilterSlots, i, 10 + 18 * (i % 3), 106 + 18 * (i / 3))
                .setBackgroundTexture(GuiTextures.SLOT));
        }
        itemFilterGroup.addWidget(new ToggleButtonWidget(74, 105, 20, 20, GuiTextures.BUTTON_FILTER_DAMAGE, () -> ignoreDamage, this::setIgnoreDamage));
        itemFilterGroup.addWidget(new ToggleButtonWidget(99, 105, 20, 20, GuiTextures.BUTTON_FILTER_NBT, () -> ignoreNBTData, this::setIgnoreNBTData));

        ServerWidgetGroup oreDictFilterGroup = new ServerWidgetGroup(() -> filterMode == FilterType.ORE_DICTIONARY_FILTER);
        oreDictFilterGroup.addWidget(new LabelWidget(10, 106, "cover.ore_dictionary_filter.title1"));
        oreDictFilterGroup.addWidget(new LabelWidget(10, 116, "cover.ore_dictionary_filter.title2"));
        oreDictFilterGroup.addWidget(new TextFieldWidget(10, 126, 100, 12, true, () -> oreDictionaryFilter, this::setOreDictionaryFilter)
            .setMaxStringLength(64).setValidator(str -> CoverOreDictionaryFilter.ORE_DICTIONARY_FILTER.matcher(str).matches()));

        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND_EXTENDED, 176, 198)
            .widget(primaryGroup)
            .widget(itemFilterGroup)
            .widget(oreDictFilterGroup)
            .bindPlayerHotbar(player.inventory, GuiTextures.SLOT, 8, 170);
        return buildUI(builder, player);
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setInteger("TransferRate", transferRate);
        tagCompound.setInteger("ConveyorMode", conveyorMode.ordinal());
        tagCompound.setTag("FilterInventory", filterTypeInventory.serializeNBT());
        tagCompound.setTag("ItemFilter", itemFilterSlots.serializeNBT());
        tagCompound.setString("OreDictionaryFilter", oreDictionaryFilter);
        tagCompound.setBoolean("IgnoreDamage", ignoreDamage);
        tagCompound.setBoolean("IgnoreNBT", ignoreNBTData);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        this.transferRate = tagCompound.getInteger("TransferRate");
        this.conveyorMode = ConveyorMode.values()[tagCompound.getInteger("ConveyorMode")];
        this.filterTypeInventory.deserializeNBT(tagCompound.getCompoundTag("FilterInventory"));
        this.itemFilterSlots.deserializeNBT(tagCompound.getCompoundTag("ItemFilter"));
        this.oreDictionaryFilter = tagCompound.getString("OreDictionaryFilter");
        this.ignoreDamage = tagCompound.getBoolean("IgnoreDamage");
        this.ignoreNBTData = tagCompound.getBoolean("IgnoreNBT");
    }

    public enum ConveyorMode {
        IMPORT("cover.conveyor.mode.import"),
        EXPORT("cover.conveyor.mode.export");

        public final String localeName;

        ConveyorMode(String localeName) {
            this.localeName = localeName;
        }
    }

    public enum FilterType {
        NONE(1, (it, stack) -> 0),
        ITEM_FILTER(9, (it, stack) -> CoverItemFilter.itemFilterMatch(it.itemFilterSlots, it.ignoreDamage, it.ignoreNBTData, stack)),
        ORE_DICTIONARY_FILTER(1, (it, stack) -> CoverOreDictionaryFilter.oreDictionaryFilterMatch(it.oreDictionaryFilter, stack));

        public final BiFunction<CoverConveyor, ItemStack, Integer> matcher;
        public final int maxMatchSlots;

        FilterType(int maxMatchSlots, BiFunction<CoverConveyor, ItemStack, Integer> matcher) {
            this.maxMatchSlots = maxMatchSlots;
            this.matcher = matcher;
        }
    }

    private class FilterItemStackHandler extends ItemStackHandler {

        public FilterItemStackHandler() {
            super(1);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (!MetaItems.ITEM_FILTER.isItemEqual(stack) &&
                !MetaItems.ORE_DICTIONARY_FILTER.isItemEqual(stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onLoad() {
            onContentsChanged(0);
        }

        @Override
        protected void onContentsChanged(int slot) {
            ItemStack itemStack = getStackInSlot(slot);
            CoverConveyor.this.filterMode = getFilterMode(itemStack);
            onFilterModeUpdated();
        }

        private FilterType getFilterMode(ItemStack itemStack) {
            if(itemStack.isEmpty()) {
                return FilterType.NONE;
            } else if(MetaItems.ITEM_FILTER.isItemEqual(itemStack)) {
                return FilterType.ITEM_FILTER;
            } else if(MetaItems.ORE_DICTIONARY_FILTER.isItemEqual(itemStack)) {
                return FilterType.ORE_DICTIONARY_FILTER;
            } else return FilterType.NONE;
        }
    }

    private class CoverableItemHandlerWrapper extends ItemHandlerDelegate {

        public CoverableItemHandlerWrapper(IItemHandler delegate) {
            super(delegate);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if(conveyorMode == ConveyorMode.EXPORT) {
                return stack;
            }
            if(filterMode.matcher.apply(CoverConveyor.this, stack) == -1) {
               return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if(conveyorMode == ConveyorMode.IMPORT) {
                return ItemStack.EMPTY;
            }
            ItemStack resultStack = super.extractItem(slot, amount, true);
            if(filterMode.matcher.apply(CoverConveyor.this, resultStack) == -1) {
                return ItemStack.EMPTY;
            }
            if(!simulate) {
                super.extractItem(slot, amount, false);
            }
            return resultStack;
        }
    }
}
