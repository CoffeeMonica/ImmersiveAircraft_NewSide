package immersive_aircraft.screen.slot;

import immersive_aircraft.Items;
import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.inventory.VehicleInventoryDescription;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class EngineUpgradeSlot extends Slot {

    private final InventoryVehicleEntity vehicle;
    private final int stackSize;

    // Only these engines can be placed in the engine upgrade slot
    private static final Set<Item> ENGINE_ITEMS = Set.of(
            Items.ECO_ENGINE.get(),
            Items.NETHER_ENGINE.get(),
            Items.ENGINEER_ENGINE.get()
    );

    public EngineUpgradeSlot(InventoryVehicleEntity vehicle, int stackSize, Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);

        this.vehicle = vehicle;
        this.stackSize = stackSize;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return ENGINE_ITEMS.contains(stack.getItem())
                && vehicle.getSlots(VehicleInventoryDescription.ENGINE_UPGRADE).stream().noneMatch(s -> s.getItem() == stack.getItem());
    }

    @Override
    public int getMaxStackSize() {
        return stackSize;
    }
}