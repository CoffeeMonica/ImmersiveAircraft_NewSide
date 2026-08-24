package immersive_aircraft.client;

import immersive_aircraft.config.Config;
import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.item.upgrade.VehicleStat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side radar upgrade handler.
 *
 * Each client riding a vehicle with the radar upgrade computes its own set of highlighted
 * targets: enemy vehicles with running engines (within each target's radarDetectability)
 * plus the passengers of those vehicles. A single UI ping plays whenever new targets are
 * detected. No server synchronization is involved, so players without the radar never see
 * the highlight.
 */
public class RadarHandler {
    private static final int SCAN_INTERVAL_TICKS = 10;

    /** Entity ids currently highlighted for THIS client. */
    private static final Set<Integer> TARGETS = new HashSet<>();
    private static boolean active = false;
    private static int scanTimer = 0;

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.level == null || player == null) {
            deactivate();
            return;
        }

        Entity root = player.getRootVehicle();
        if (!(root instanceof VehicleEntity vehicle) || !vehicle.hasRadarUpgrade()) {
            deactivate();
            return;
        }

        active = true;
        if (++scanTimer < SCAN_INTERVAL_TICKS) {
            return;
        }
        scanTimer = 0;
        scan(vehicle);
    }

    private static void scan(VehicleEntity self) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        // Candidate query bound: the largest possible detection range (config render distance,
        // possibly x1.5 with the radar's own detectability penalty).
        float maxRange = Config.getInstance().renderDistance * 1.5f + 16.0f;

        Set<Integer> found = new HashSet<>();
        for (EngineVehicle target : client.level.getEntitiesOfClass(EngineVehicle.class,
                self.getBoundingBox().inflate(maxRange),
                target -> target != self && target.isAlive() && !target.isRemoved() && target.getEngineTarget() > 0)) {

            float detect = target.getProperties().get(VehicleStat.RADAR_DETECTABILITY);
            if (detect < 0.0f) {
                // Negative value = multiple of the config render distance (-1 = exactly it).
                detect = Config.getInstance().renderDistance * -detect;
            }
            float cap = Config.getInstance().radarRange;
            if (cap > 0.0f) {
                detect = Math.min(detect, cap);
            }
            if (self.distanceToSqr(target) > detect * detect) {
                continue;
            }

            found.add(target.getId());
            for (Entity passenger : target.getPassengers()) {
                found.add(passenger.getId());
            }
        }

        boolean newTarget = false;
        for (int id : found) {
            if (!TARGETS.contains(id)) {
                newTarget = true;
                break;
            }
        }
        if (newTarget) {
            // One ping per scan, no matter how many planes entered at once.
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f));
        }

        TARGETS.clear();
        TARGETS.addAll(found);
    }

    private static void deactivate() {
        scanTimer = 0;
        if (active || !TARGETS.isEmpty()) {
            TARGETS.clear();
            active = false;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isTarget(Entity entity) {
        return active && TARGETS.contains(entity.getId());
    }

    /** Own ship and its passengers are never highlighted - even with a real glowing effect applied. */
    public static boolean isOwnCrew(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }
        Entity root = client.player.getRootVehicle();
        return root != null && (root == entity || root.hasPassenger(entity));
    }
}
