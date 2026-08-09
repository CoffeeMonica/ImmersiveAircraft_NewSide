package immersive_aircraft.screen.slot;

import immersive_aircraft.Items;
import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.inventory.VehicleInventoryDescription;
import immersive_aircraft.item.upgrade.VehicleUpgradeRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UpgradeSlot extends Slot {

    private final InventoryVehicleEntity vehicle;
    private final int stackSize;

    public UpgradeSlot(InventoryVehicleEntity vehicle, int stackSize, Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);

        this.vehicle = vehicle;
        this.stackSize = stackSize;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Engines can only be placed in the dedicated engine upgrade slot
        if (isEngineItem(stack)) {
            return false;
        }
        return VehicleUpgradeRegistry.INSTANCE.hasUpgrade(stack.getItem())
                && vehicle.getSlots(VehicleInventoryDescription.UPGRADE).stream().noneMatch(s -> s.getItem() == stack.getItem());
    }

    private static boolean isEngineItem(ItemStack stack) {
        return stack.getItem() == Items.ECO_ENGINE.get()
                || stack.getItem() == Items.NETHER_ENGINE.get()
                || stack.getItem() == Items.ENGINEER_ENGINE.get();
    }

    @Override
    public int getMaxStackSize() {
        return stackSize;
    }
}
