package gregtech.api.capability.tool;

public interface IConfiguratorItem {

    default boolean isAdvanced() {
        return false;
    }

    boolean damageItem(int damage, boolean simulate);

}
