package immersive_aircraft.screen.slot;

import immersive_aircraft.Items;
import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.Rotorcraft;
import immersive_aircraft.entity.inventory.VehicleInventoryDescription;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EngineUpgradeSlot extends Slot {

    private final InventoryVehicleEntity vehicle;
    private final int stackSize;

    public EngineUpgradeSlot(InventoryVehicleEntity vehicle, int stackSize, Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);

        this.vehicle = vehicle;
        this.stackSize = stackSize;
    }

    /**
     * Engine can only be swapped at zero RPM: while the engine is running
     * (or spooling up/down) neither removing nor inserting is allowed.
     * Used both by the slot itself and by the GUI renderer for the red hover highlight.
     * <p>
     * Hovering vehicles (airships and drones, all Rotorcraft subclasses) are the
     * exception: their engine target is always 100% while occupied, so the RPM rule
     * would lock their slot forever - instead they allow swapping whenever they are
     * standing ON THE GROUND.
     */
    public static boolean isEngineBlocked(InventoryVehicleEntity vehicle) {
        if (vehicle instanceof Rotorcraft rotorcraft) {
            return !rotorcraft.onGround();
        }
        return vehicle instanceof EngineVehicle engine && engine.isEngineRunning();
    }

    @Override
    public boolean mayPickup(Player player) {
        return !isEngineBlocked(vehicle) && super.mayPickup(player);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Only items tagged as engines (data/immersive_aircraft/tags/item/engines.json)
        // can be placed in the engine upgrade slot
        return !isEngineBlocked(vehicle)
                && stack.is(Items.ENGINE_TAG)
                && vehicle.getSlots(VehicleInventoryDescription.ENGINE_UPGRADE).stream().noneMatch(s -> s.getItem() == stack.getItem());
    }

    @Override
    public int getMaxStackSize() {
        return stackSize;
    }
}