package immersive_aircraft.entity;

import immersive_aircraft.AircraftStats;
import immersive_aircraft.Items;
import immersive_aircraft.Sounds;
import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.config.Config;
import immersive_aircraft.entity.inventory.VehicleInventoryDescription;
import immersive_aircraft.entity.inventory.slots.SlotDescription;
import immersive_aircraft.item.upgrade.VehicleStat;
import immersive_aircraft.network.c2s.EnginePowerMessage;
import immersive_aircraft.resources.bbmodel.BBAnimationVariables;
import immersive_aircraft.util.InterpolatedFloat;
import immersive_aircraft.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.EnumMap;
import java.util.List;


public abstract class EngineVehicle extends InventoryVehicleEntity {
    protected static final EntityDataAccessor<Float> ENGINE = SynchedEntityData.defineId(EngineVehicle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> UTILIZATION = SynchedEntityData.defineId(EngineVehicle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LOW_ON_FUEL = SynchedEntityData.defineId(EngineVehicle.class, EntityDataSerializers.BOOLEAN);

    public final InterpolatedFloat engineRotation = new InterpolatedFloat();
    public final InterpolatedFloat enginePower = new InterpolatedFloat(20.0f);
    public float engineSpinUpStrength = 0.0f;
    public float engineSound = 0.0f;
    public int mainWarning = 0;
    public int mslWarning = 0;
    public final EnumMap<Cautions, Integer> cautions = new EnumMap<>(Cautions.class);
    // Tracks which player was last notified about fuel, per-pilot warning reset
    private java.util.UUID lastFuelNotifiedPlayer = null;
    private int highAltitudeWarningCooldown = 0;

    protected enum FuelState {
        NEVER,
        EMPTY,
        FUELED,
        LOW
    }

    public enum Cautions {
        PULL_UP,
        VOID,
        DAMAGED,
        TOO_HIGH,
        LOW_FUEL,
        FUEL_OUT
    }

    FuelState lastFuelState = FuelState.NEVER;

    protected int lastDismountTick = -100;
    
    protected float engineTargetAtDismount = 0.0f;

    public static final int TARGET_FUEL = 1000;

    /**
     * A "low fuel" warning is raised when the remaining fuel reserve (tank + unburned
     * items) can sustain the current consumption for at most this many seconds.
     */
    public static final float LOW_FUEL_SECONDS = 10.0f;
    // Reference values for the low-fuel threshold: one stack of planks
    // (vanilla plank burn time is 300 ticks).
    public static final int REFERENCE_STACK_SIZE = 64;
    public static final int PLANKS_BURN_TIME = 300;

    /**
     * Fraction of the regular fuel consumption charged to a hover-capable vehicle
     * ({@link #canHover()}) while it holds its position in the air: 20%.
     */
    public static final float HOVER_FUEL_FACTOR = 0.2f;

    /**
     * Speed (blocks per tick) below which a hovering vehicle counts as standing still
     * and therefore qualifies for the hover fuel reduction.
     * 0.05 blocks/tick = 1 block/second, the same "has stopped" threshold the UAV uses.
     */
    public static final double HOVER_STOP_SPEED = 0.05;

    private final int[] fuel;

    public enum GUI_STYLE {
        NONE,
        ENGINE
    }

    public GUI_STYLE getGuiStyle() {
        return GUI_STYLE.ENGINE;
    }

    public EngineVehicle(EntityType<? extends EngineVehicle> entityType, Level world, boolean canExplodeOnCrash) {
        super(entityType, world, canExplodeOnCrash);

        fuel = new int[getInventoryDescription().getSlots(VehicleInventoryDescription.BOILER).size()];

        for (EngineVehicle.Cautions c : EngineVehicle.Cautions.values()) {
            cautions.put(c, 0);
        }
    }

    protected SoundEvent getEngineStartSound() {
        return Sounds.ENGINE_START.get();
    }

    protected SoundEvent getEngineSound() {
        return Sounds.PROPELLER.get();
    }

    protected float getEngineVolume() {
        return 0.25f;
    }

    /** Distance (blocks) at which the engine sound fades to silence. Halved by the muffler upgrade (via soundRange). */
    public float getSoundRange() {
        return Math.max(8.0f, getProperties().get(VehicleStat.SOUND_RANGE));
    }

    /** Ticks for a stock biplane (reaction 20, no upgrades) to reach full power at multiplier 1.0: 8 seconds. */
    private static final float ENGINE_SPOOL_UP_TICKS = 160.0f;

    /**
     * True when a muffler upgrade is installed: the engine plays at HALF volume
     * and its audible range is halved as well (via the soundRange stat).
     */
    private boolean hasMufflerUpgrade() {
        return hasUpgrade(immersive_aircraft.Items.MUFFLER.get());
    }

    /** Applies the muffler's 50% loudness reduction to an engine sound volume. */
    private float muffledVolume(float volume) {
        return hasMufflerUpgrade() ? volume * 0.5f : volume;
    }

    /** Per-vehicle loudness multiplier for engine sounds (UAV drones play at 2x). */
    public float getSoundVolumeMultiplier() {
        return 1.0f;
    }

    /**
     * Ticks for the spool-DOWN phase (real power 100% -> 0%). Base: config value,
     * scaled by each vehicle's engine reaction speed so heavier engines keep their
     * character. Vehicles may override (ImprovedUav uses a fixed one-second fall).
     */
    protected float getEngineDecayTicks(float reactionScale) {
        return Config.getInstance().engineDecayTicks * reactionScale;
    }

    /**
     * Computes the volume for a positional engine sound so that it fades strictly
     * LINEARLY from full loudness at the source to silence at {@code range} blocks.
     * <p>
     * Why: Minecraft multiplies the instance volume by its own linear attenuation
     * over the sound's {@code attenuation_distance} (sounds.json), so a big volume
     * like range/16 produced a loud plateau near the source and a sudden drop.
     * Instead we now pass the final audible gain directly (volume &le; 1) and keep
     * sounds.json's attenuation_distance far above every range (1024) so the
     * built-in falloff is negligible and the gain itself is the linear curve.
     */
    private float linearSoundVolume(float range) {
        net.minecraft.world.entity.player.Player nearest = level().getNearestPlayer(getX(), getY(), getZ(), range, false);
        if (nearest == null) {
            return 0.0f;
        }
        return Mth.clamp(1.0f - nearest.distanceTo(this) / Math.max(1.0f, range), 0.0f, 1.0f);
    }

    protected float getEnginePitch() {
        return 1.0f;
    }

    protected float getEngineReactionSpeed() {
        return 20.0f;
    }

    public boolean worksUnderWater() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);

        entityData.define(ENGINE, 0.0f);
        entityData.define(UTILIZATION, 0.0f);
        entityData.define(LOW_ON_FUEL, false);
    }

    @Override
    public void tick() {
        if (getPassengers().isEmpty()) {
            if (lastDismountTick < 0) {
                lastDismountTick = tickCount;
                engineTargetAtDismount = getEngineTarget();
            }
        } else {
            lastDismountTick = -100;
            engineTargetAtDismount = 0.0f;
        }
        
        super.tick();
        
        // --- Engine spool-up / spool-down (exact linear ramp) ---
        // Spool-UP: full power is reached in 160 ticks (8 s) for a stock biplane
        // when engineAccelerationMultiplier = 1.0, scaled by each vehicle's
        // getEngineReactionSpeed()/20 and divided by the ACCELERATION stat and the
        // config multiplier. Acceleration upgrades affect ONLY this phase.
        // Spool-DOWN: real power falls linearly from 100% to 0% in
        // Config.engineDecayTicks ticks (scaled by reactionSpeed/20), never
        // affected by acceleration upgrades or the config multiplier.
        float accelerationStat = Math.max(0.001f, getProperties().get(VehicleStat.ACCELERATION));
        float reactionScale = getEngineReactionSpeed() / 20.0f;
        // Spool-UP time: full power in ENGINE_SPOOL_UP_TICKS (8 s for a stock biplane),
        // scaled by the vehicle's getEngineReactionSpeed()/20 and divided by the
        // ACCELERATION stat and the config multiplier. Acceleration upgrades affect
        // ONLY this phase.
        float spoolUpTicks = ENGINE_SPOOL_UP_TICKS * reactionScale
                / accelerationStat
                / Math.max(0.001f, Config.getInstance().engineAccelerationMultiplier);
        // Spool-DOWN time: real power falls linearly from 100% to 0% in
        // Config.engineDecayTicks ticks (scaled by reactionSpeed/20), never affected
        // by acceleration upgrades or the config multiplier.
        float decayTicks = getEngineDecayTicks(reactionScale);
        float rampTicks;
        if (hasInertiaEngine()) {
            // The inertia engine does NOT drop RPM instantly: its spool-down lasts
            // exactly as long as its own (slow) spool-up, instead of the short decay.
            // Spool-up still uses the same slow time, so both phases are symmetric.
            rampTicks = spoolUpTicks;
        } else if (getEngineTarget() <= enginePower.getSmooth()) {
            rampTicks = decayTicks;
        } else {
            rampTicks = spoolUpTicks;
        }
        enginePower.setSteps(Math.max(1f, rampTicks));
        // Linear mode: move exactly 100%/rampTicks per tick, so timings are exact
        enginePower.setLinearStep(1.0f / Math.max(1f, rampTicks));

        float altitudePenalty = getAltitudePowerPenalty();
        float targetPower = getEngineTarget() * altitudePenalty * (isInWater() && !worksUnderWater() ? 0.1f : 1.0f);

        if (!level().isClientSide() && getControllingPassenger() instanceof ServerPlayer player) {
            if (altitudePenalty < 1.0f) {
                if (highAltitudeWarningCooldown <= 0) {
                    // With the gyroscope HUD upgrade installed the warning is displayed in the
                    // HUD's caution strip instead (the TOO_HIGH caution is already raised in
                    // handleWarnings), so action-bar messages don't overlap with the HUD.
                    if (!hasGyroscopeHudUpgrade()) {
                        player.displayClientMessage(Component.translatable("immersive_aircraft.too_high_to_fly"), true);
                    }
                    highAltitudeWarningCooldown = 40;
                }
            } else {
                highAltitudeWarningCooldown = 0;
            }
        }

        highAltitudeWarningCooldown = Math.max(0, highAltitudeWarningCooldown - 1);

        enginePower.update(targetPower);

        engineSpinUpStrength = Math.max(0.0f, engineSpinUpStrength + enginePower.getDiff() - 0.01f);

        if (level().isClientSide()) {
            engineRotation.update((engineRotation.getValue() + getPropellerSpeed()) % 1000);
        }
        if (getPassengers().isEmpty() && onGround() && tickCount - lastDismountTick > 20) {
            setEngineTarget(0.0f, true);
        }
        // Hovering vehicles (airships and drones, all Rotorcraft subclasses) park on
        // the ground WITHOUT a pilot: their engine cancels gravity, so the vanilla
        // onGround() flag goes stale (no vertical movement -> no collision update).
        // Detect ground contact with an explicit collision probe just below the
        // bounding box instead. SERVER-SIDE ONLY: writing ENGINE on the client would
        // desync the propellers/sound, and the server would never re-send its
        // (unchanged) value to correct it. The shutdown still spools the engine
        // down smoothly over the configured decay time (see setEngineTarget).
        // Planes (AirplaneEntity etc.) are handled by the onGround() block above -
        // their gravity is never cancelled, so the flag is reliable for them.
        if (!level().isClientSide() && this instanceof Rotorcraft
                && getPassengers().isEmpty()
                && tickCount - lastDismountTick > 20
                && !level().noCollision(this, getBoundingBox().expandTowards(0.0, -0.05, 0.0))) {
            setEngineTarget(0.0f, true);
            engineTargetAtDismount = 0.0f;
        }

        // Mirror the fuel cautions on the client so the gyroscope HUD lamp works in
        // multiplayer too (the server-side warning block only runs on the logical server).
        if (level().isClientSide() && hasGyroscopeHudUpgrade()) {
            float syncedUtilization = entityData.get(UTILIZATION);
            if (syncedUtilization > 0 && entityData.get(LOW_ON_FUEL)) {
                cautions.put(Cautions.LOW_FUEL, 40);
            } else if (syncedUtilization <= 0) {
                cautions.put(Cautions.FUEL_OUT, 40);
            }
        }

        if (level().isClientSide()) {
            engineSound += getEnginePower() * 0.25f;
            if (engineSound > 1.0f) {
                engineSound--;
                if (isFuelLow()) {
                    engineSound -= random.nextInt(2);
                }
                float chugVol = Math.min(2.0f, muffledVolume(linearSoundVolume(getSoundRange())) * getSoundVolumeMultiplier());
                if (chugVol > 0.0f) {
                    level().playLocalSound(getX(), getY() + getBbHeight() * 0.5, getZ(), getEngineSound(), getSoundSource(), chugVol, (random.nextFloat() * 0.1f + 0.95f) * getEnginePitch(), false);
                }
            }
        }

        // Fuel
        if (fuel.length > 0 && !level().isClientSide()) {
            consumeFuel(getFuelConsumption());

            // Tanks ran dry: cut the THROTTLE TARGET, not the power itself - the
            // engine then spools down smoothly over the configured decay time
            // (engineDecayTicks) instead of snapping to 0 instantly. Refueling does
            // NOT restart the engine: the pilot has to re-apply the throttle, which
            // spools back up through the normal (slow) spool-up phase.
            if (getEngineTarget() > 0.0f && getFuelUtilization() <= 0.0f) {
                setEngineTarget(0.0f, true);
                engineTargetAtDismount = 0.0f;
            }
        }

        // Refuel continuously, even without a pilot aboard
        if (!level().isClientSide()) {
            refuel();
        }

        // Fuel notification
        if (getControllingPassenger() instanceof ServerPlayer player) {
            // Reset the state tracking when the pilot changes, so a new pilot always
            // gets notified of the CURRENT fuel situation on mount.
            if (!player.getUUID().equals(lastFuelNotifiedPlayer)) {
                lastFuelState = FuelState.FUELED;
                lastFuelNotifiedPlayer = player.getUUID();
            }
            float utilization = getFuelUtilization();
            boolean hudInstalled = hasGyroscopeHudUpgrade();
            if (utilization > 0 && isFuelLow()) {
                // Route the warning into the gyroscope HUD's caution strip when installed;
                // refreshed every tick so the lamp stays lit until the condition clears.
                if (hudInstalled) {
                    cautions.put(Cautions.LOW_FUEL, 40);
                }
                if (lastFuelState != FuelState.LOW) {
                    if (!hudInstalled) {
                        player.displayClientMessage(Component.translatable("immersive_aircraft." + getFuelType() + ".low"), true);
                    }
                    lastFuelState = FuelState.LOW;
                }
            } else if (utilization > 0) {
                lastFuelState = FuelState.FUELED;
            } else {
                if (hudInstalled) {
                    cautions.put(Cautions.FUEL_OUT, 40);
                }
                if (lastFuelState != FuelState.EMPTY) {
                    if (!hudInstalled) {
                        player.displayClientMessage(Component.translatable("immersive_aircraft." + getFuelType() + "." + (lastFuelState == FuelState.FUELED ? "out" : "none")), true);
                    }
                    lastFuelState = FuelState.EMPTY;
                }
            }
        }

        mainWarning = Math.max(0, mainWarning - 1);
        mslWarning = Math.max(0, mslWarning - 1);
        for (Cautions caution : Cautions.values()) {
            cautions.compute(caution, (cautions, integer) -> integer == null ? 0 : Math.max(0, --integer));
        }

        handleWarnings();
    }

    private void handleWarnings() {
        double altRate = getSpeedVector().y * 10.0d;

        // pull-up caution
        if (getEnginePower() >= 0.5 && altRate < -2 && getY() + altRate * 3 < level().getSeaLevel()) {
            cautions.put(Cautions.PULL_UP, 40);
        }

        // void warning
        if (getY() < level().dimensionType().minY()) {
            cautions.put(Cautions.VOID, 10);
            mainWarning = 6;
        }

        // damaged warning
        if (getHealth() * 100 < 20) {
            cautions.put(Cautions.DAMAGED, 10);
            mainWarning = 6;
        }

        if (getAltitudePowerPenalty() < 1.0f) {
            cautions.put(Cautions.TOO_HIGH, 40);
            mainWarning = 6;
        }
    }

    private float getAltitudePowerPenalty() {
        if (getEngineTarget() <= 0.0f) {
            return 1.0f;
        }

        int maxHeight = level().getHeight() - 180;
        int startPenaltyY = maxHeight - 10;
        double altitudeAboveThreshold = Math.max(0.0, getY() - startPenaltyY);
        if (altitudeAboveThreshold <= 0.0) {
            return 1.0f;
        }

        // Engine power drops by 10% for every 10 blocks above the threshold
        float penalty = 1.0f - 0.1f * (float)(altitudeAboveThreshold / 10.0d);
        return Math.max(0.0f, penalty);
    }

    public float consumeFuel(float consumption) {
        while (consumption > 0 && (consumption >= 1 || random.nextFloat() < consumption)) {
            for (int i = 0; i < fuel.length; i++) {
                if (fuel[i] > 0) {
                    fuel[i]--;
                }
            }
            consumption--;
        }
        return consumption;
    }

    public float getPropellerSpeed() {
        return getEnginePower();
    }

    /**
     * True when the remaining fuel will last for at most LOW_FUEL_SECONDS of flight
     * at the CURRENT consumption (engine target x vehicle FUEL stat x config rate).
     * The estimate sums the already-burned fuel in every boiler tank PLUS the unburned
     * fuel items still stored in the boiler slots.
     * Gated by Config.lowFuelWarning: when disabled, never reports low fuel.
     */
    public boolean isFuelLow() {
        if (!Config.getInstance().burnFuelInCreative && isPilotCreative()) {
            return false;
        }

        if (level().isClientSide()) {
            return entityData.get(LOW_ON_FUEL);
        } else {
            boolean low = computeLowFuel();
            entityData.set(LOW_ON_FUEL, low);
            return low;
        }
    }

    private boolean computeLowFuel() {
        // Warning disabled in the config - never report low fuel.
        if (!Config.getInstance().lowFuelWarning) {
            return false;
        }

        float consumption = getFuelConsumption();
        // No fuel burn at all (0% throttle or fuel consumption disabled) - nothing to warn about.
        if (consumption <= 0.0f) {
            return false;
        }

        List<SlotDescription> boilerSlots = getInventoryDescription().getSlots(VehicleInventoryDescription.BOILER);
        long remaining = 0;
        for (int i = 0; i < fuel.length && i < boilerSlots.size(); i++) {
            // Fuel already burned into the tank...
            remaining += fuel[i];
            // ...plus the fuel item sitting in the boiler slot, not yet burned.
            // The WHOLE stack counts, not just its first item (a stack of 64 aviation
            // fuel holds 64x the burn time of a single item).
            ItemStack stack = getInventory().getItem(boilerSlots.get(i).index());
            if (!stack.isEmpty()) {
                remaining += (long) Utils.getFuelTime(stack) * stack.getCount();
            }
        }

        // Flight time in seconds the current fuel reserve can sustain:
        // remaining fuel-ticks / (fuel-ticks burned per real tick) / 20 ticks per second.
        double remainingSeconds = (double) remaining / consumption / 20.0;
        return remainingSeconds <= LOW_FUEL_SECONDS;
    }

    /**
     * True when the gyroscope HUD upgrade is installed. Mirrors the exact condition the
     * HUD overlay uses to render (VehicleStat.HUD == 0), so warnings can be rerouted
     * into the HUD instead of the action bar.
     */
    private boolean hasGyroscopeHudUpgrade() {
        return getProperties().get(VehicleStat.HUD) == 0.0f;
    }

    public String getFuelType() {
        return "fuel";
    }

    /**
     * Whether this vehicle can hover on the spot, i.e. hold its position in the air
     * without any thrust input. Hover-capable vehicles burn only {@link #HOVER_FUEL_FACTOR}
     * of their regular fuel while doing so (see {@link #getFuelConsumption()}).
     * <p>
     * Hovercraft ({@link Rotorcraft}: airships, cargo airships, warships) return true.
     * Drones and the gyrodyne return false ON PURPOSE: they may look like they are
     * hovering, but they must keep burning fuel at the full rate. Planes and the UAV
     * return false as well - holding a position in the air is not a valid flight state
     * for them, and with the engine off they already consume no fuel at all.
     */
    public boolean canHover() {
        return false;
    }

    /**
     * True while this vehicle is a hover-capable craft ({@link #canHover()}) that is
     * currently hovering on the spot: airborne, holding its position and not applying any
     * thrust. Only in that state the fuel consumption is scaled by
     * {@link #HOVER_FUEL_FACTOR}.
     * <p>
     * "Standing still" requires BOTH the current velocity and the averaged speed vector
     * (the real position change over the last 10 ticks, the measure the HUD, the distance
     * stat and the pull-up warning use) to be below {@link #HOVER_STOP_SPEED}, so a stale
     * or zeroed velocity alone cannot fake a hover while the craft is actually cruising.
     * <p>
     * The vanilla onGround() flag is unreliable for Rotorcraft: their engine cancels
     * gravity, so a landed airship keeps reporting "on ground" and a parked one is
     * detected with the same collision probe the parking code in tick() uses (a block
     * just below the bounding box). Planes keep using onGround() - their gravity is
     * never cancelled, so the flag stays accurate for them.
     */
    protected boolean isHoveringInPlace() {
        if (!canHover()) {
            return false;
        }

        boolean airborne = this instanceof Rotorcraft
                ? level().noCollision(this, getBoundingBox().expandTowards(0.0, -0.05, 0.0))
                : !onGround();

        return airborne
                && getDeltaMovement().length() < HOVER_STOP_SPEED
                && getSpeedVector().length() < HOVER_STOP_SPEED;
    }

    public float getFuelConsumption() {
        float consumption = getEngineTarget() * getProperties().get(VehicleStat.FUEL) * Config.getInstance().fuelConsumption;
        // Hovering on the spot is cheap for hover-capable vehicles: they only burn a fifth
        // of the regular amount while they hold their position in the air. Thrusting,
        // climbing, descending or drifting at speed still costs the full amount.
        return isHoveringInPlace() ? consumption * HOVER_FUEL_FACTOR : consumption;
    }

    private void refuel(int i) {
        List<SlotDescription> slots = getInventoryDescription().getSlots(VehicleInventoryDescription.BOILER);
        while (fuel[i] <= TARGET_FUEL && i < slots.size()) {
            ItemStack stack = getInventory().getItem(slots.get(i).index());
            int time = Utils.getFuelTime(stack);
            if (time > 0) {
                fuel[i] += time;
                Item item = stack.getItem();
                stack.shrink(1);

                if (getControllingPassenger() instanceof ServerPlayer player) {
                    player.awardStat(AircraftStats.FUEL_BURNED, time);
                }

                if (stack.isEmpty()) {
                    ItemStack remainingItem = item.getCraftingRemainder();
                    getInventory().setItem(slots.get(i).index(), remainingItem);
                }
            } else {
                break;
            }
        }
    }

    private void refuel() {
        for (int i = 0; i < fuel.length; i++) {
            refuel(i);
        }
    }

    /**
     * Real engine power follows ONLY the interpolated enginePower ramp, so running
     * out of fuel (or refueling) can never snap the RPM instantly - the throttle
     * target is what changes (see the fuel-out handling in tick()), and the power
     * then spools down/up smoothly over the configured ramp time.
     */
    public float getEnginePower() {
        return enginePower.getSmooth();
    }

    public float getEngineTarget() {
        return entityData.get(ENGINE);
    }

    /**
     * True while the engine is running or still spooling up/down (any nonzero RPM).
     * Used to lock the engine upgrade slot: swapping engines is only allowed at zero RPM.
     */
    public boolean isEngineRunning() {
        return enginePower.getSmooth() > 0.0f || getEngineTarget() > 0.0f;
    }

    /**
     * True when the inertia engine is installed: its spool-up time equals its
     * spool-down time (it does not drop RPM instantly).
     */
    public boolean hasInertiaEngine() {
        return getSlots(VehicleInventoryDescription.ENGINE_UPGRADE).stream()
                .anyMatch(s -> s.is(Items.INERTIA_ENGINE.get()));
    }

    public void setEngineTarget(float engineTarget) {
        setEngineTarget(engineTarget, false);
    }

    protected void setEngineTarget(float engineTarget, boolean force) {
        // If pilot has dismounted, restore and lock the engine target to preserve RPM
        if (!force && lastDismountTick >= 0 && getPassengers().isEmpty()) {
            // Restore the saved engine target to prevent it from being reduced
            entityData.set(ENGINE, engineTargetAtDismount);
            return;
        }

        if (getFuelUtilization() > 0 || engineTarget == 0 || getPassengers().isEmpty()) {
            if (level().isClientSide()) {
                if (getEngineTarget() != engineTarget) {
                    NetworkHandler.sendToServer(new EnginePowerMessage(engineTarget));
                }
                if (getFuelUtilization() > 0 && getEngineTarget() == 0.0 && engineTarget > 0) {
                    float startVol = Math.min(2.0f, muffledVolume(linearSoundVolume(getSoundRange() * 0.75f)) * getSoundVolumeMultiplier());
                    if (startVol > 0.0f) {
                        level().playLocalSound(getX(), getY() + getBbHeight() * 0.5, getZ(), getEngineStartSound(), getSoundSource(), startVol, getEnginePitch(), false);
                    }
                }
            }
            entityData.set(ENGINE, engineTarget);
        }
    }

    public float getFuelUtilization() {
        if (Config.getInstance().fuelConsumption == 0) {
            return 1.0f;
        }
        if (!Config.getInstance().burnFuelInCreative && isPilotCreative()) {
            if (!level().isClientSide()) {
                entityData.set(UTILIZATION, 1.0f);
            }
            return 1.0f;
        }
        if (fuel.length == 0) {
            return 1.0f;
        }
        if (level().isClientSide()) {
            return entityData.get(UTILIZATION);
        } else {

            int running = 0;
            for (int i : fuel) {
                if (i > 0) {
                    running++;
                }
            }
            if (running == 0) {
                entityData.set(UTILIZATION, 0.0f);
                return 0.0f;
            }
            float utilization = (float) running / fuel.length;
            entityData.set(UTILIZATION, utilization);
            return utilization;
        }
    }

    public void emitSmokeParticle(float x, float y, float z, float nx, float ny, float nz) {
        if (!isWithinParticleRange() || !level().isClientSide()) {
            return;
        }

        Matrix3f normalTransform = getVehicleNormalTransform();

        float power = getEnginePower();
        if (power > 0.05) {
            for (int i = 0; i < 1 + engineSpinUpStrength * 4; i++) {
                Vec3 p = transformToWorld(x, y, z);
                Vector3f vel = transformVector(normalTransform, nx, ny, nz);
                Vec3 velocity = getDeltaMovement();
                if (random.nextFloat() < engineSpinUpStrength * 0.1) {
                    vel.mul(0.5f);
                    level().addParticle(ParticleTypes.SMALL_FLAME, p.x(), p.y(), p.z(), vel.x() + velocity.x, vel.y() + velocity.y, vel.z() + velocity.z);
                } else {
                    level().addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, vel.x + velocity.x, vel.y + velocity.y, vel.z + velocity.z);
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput tag) {
        super.addAdditionalSaveData(tag);

        for (int i = 0; i < fuel.length; i++) {
            tag.putInt("Fuel" + i, fuel[i]);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput tag) {
        super.readAdditionalSaveData(tag);

        for (int i = 0; i < fuel.length; i++) {
            fuel[i] = tag.getIntOr("Fuel" + i, 0);
        }
    }

    @Override
    public void setAnimationVariables(float tickDelta) {
        super.setAnimationVariables(tickDelta);

        BBAnimationVariables.set("engine_rotation", engineRotation.getSmooth(tickDelta));
    }
}