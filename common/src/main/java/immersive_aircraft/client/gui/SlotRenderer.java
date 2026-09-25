package immersive_aircraft.client.gui;

import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.entity.inventory.slots.SlotDescription;
import immersive_aircraft.screen.slot.EngineUpgradeSlot;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SlotRenderer {
    private static final Map<String, Renderer> RENDERERS = new HashMap<>();

    public static void register(String type, Renderer renderer) {
        RENDERERS.put(type, renderer);
    }

    static {
        register("inventory", (screen, context, slot, mouseX, mouseY, delta) ->
                screen.drawImage(context, screen.getX() + slot.x() - 1, screen.getY() + slot.y() - 1, 284, 0, 18, 18));
        register("default", (screen, context, slot, mouseX, mouseY, delta) ->
                screen.drawImage(context, screen.getX() + slot.x() - 3, screen.getY() + slot.y() - 3, 262, 0, 22, 22));
        register("boiler", (screen, context, slot, mouseX, mouseY, delta) -> {
            screen.drawImage(context, screen.getX() + slot.x() - 4, screen.getY() + slot.y() - 18, 318, 0, 24, 39);
            if (screen.getMenu().getVehicle() instanceof EngineVehicle engineAircraft && engineAircraft.getFuelUtilization() > 0.0) {
                screen.drawImage(context, screen.getX() + slot.x() - 4, screen.getY() + slot.y() - 18, 318 + 30, 0, 24, 39);
            }
        });
        register("weapon", new BasicRenderer(262, 22));
        register("upgrade", new BasicRenderer(262, 22 * 2));
        register("engineUpgrade", new BasicRenderer(262, 22 * 6) {
            @Override
            public void render(VehicleScreen screen, @NotNull GuiGraphics context, SlotDescription slot, int mouseX, int mouseY, float delta) {
                super.render(screen, context, slot, mouseX, mouseY, delta);
                // Hovered engine slot turns slightly red while the engine is running
                // (engine can not be swapped at non-zero RPM)
                if (mouseX >= screen.getX() + slot.x() && mouseX < screen.getX() + slot.x() + 16
                        && mouseY >= screen.getY() + slot.y() && mouseY < screen.getY() + slot.y() + 16
                        && EngineUpgradeSlot.isEngineBlocked(screen.getMenu().getVehicle())) {
                    context.fill(screen.getX() + slot.x(), screen.getY() + slot.y(),
                            screen.getX() + slot.x() + 16, screen.getY() + slot.y() + 16, 0x60FF3C3C);
                }
            }
        });
        register("banner", new BasicRenderer(262, 22 * 3));
        register("dye", new BasicRenderer(262, 22 * 4));
        register("booster", new BasicRenderer(262, 22 * 5));
    }

    public static Renderer get(String type) {
        return RENDERERS.getOrDefault(type, RENDERERS.get("default"));
    }

    public static class BasicRenderer implements Renderer {
        final int u, v;

        public BasicRenderer(int u, int v) {
            this.u = u;
            this.v = v;
        }

        public void render(VehicleScreen screen, @NotNull GuiGraphics context, SlotDescription slot, int mouseX, int mouseY, float delta) {
            if (screen.getMenu().getVehicle().getInventory().getItem(slot.index()).isEmpty()) {
                screen.drawImage(context, screen.getX() + slot.x() - 3, screen.getY() + slot.y() - 3, u, v, 22, 22);
            } else {
                screen.drawImage(context, screen.getX() + slot.x() - 3, screen.getY() + slot.y() - 3, 262, 0, 22, 22);
            }
        }
    }

    public interface Renderer {
        void render(VehicleScreen screen, @NotNull GuiGraphics context, SlotDescription slot, int mouseX, int mouseY, float delta);
    }
}
