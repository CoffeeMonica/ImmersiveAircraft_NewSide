package immersive_aircraft.entity.inventory.slots;

import com.google.gson.JsonObject;
import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.screen.slot.EngineUpgradeSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class EngineUpgradeSlotDescription extends TooltippedSlotDescription {
    public EngineUpgradeSlotDescription(String type, int index, int x, int y, JsonObject json) {
        super(type, index, x, y, json);
    }

    public EngineUpgradeSlotDescription(String type, RegistryFriendlyByteBuf buffer) {
        super(type, buffer);
    }

    public Slot getSlot(InventoryVehicleEntity vehicle, Container inventory) {
        return new EngineUpgradeSlot(vehicle, 1, inventory, index, x, y);
    }
}