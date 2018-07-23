package gregtech.integration.theoneprobe;

import gregtech.integration.theoneprobe.element.ElementProgressDecimal;
import gregtech.integration.theoneprobe.element.ElementTextAdvanced;
import gregtech.integration.theoneprobe.provider.ElectricContainerInfoProvider;
import gregtech.integration.theoneprobe.provider.WorkableInfoProvider;
import mcjty.theoneprobe.TheOneProbe;
import mcjty.theoneprobe.api.ITheOneProbe;

public class TheOneProbeCompatibility {

    public static int ELEMENT_TEXT_ADVANCED;
    public static int ELEMENT_PROGRESS_DECIMAL;

    public static void registerCompatibility() {
        ITheOneProbe oneProbe = TheOneProbe.theOneProbeImp;
        oneProbe.registerProvider(new ElectricContainerInfoProvider());
        oneProbe.registerProvider(new WorkableInfoProvider());

        ELEMENT_TEXT_ADVANCED = TheOneProbe.theOneProbeImp.registerElementFactory(ElementTextAdvanced::new);
        ELEMENT_PROGRESS_DECIMAL = TheOneProbe.theOneProbeImp.registerElementFactory(ElementProgressDecimal::new);
    }

}
