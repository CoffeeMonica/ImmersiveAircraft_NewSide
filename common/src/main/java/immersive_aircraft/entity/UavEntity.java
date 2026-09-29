package immersive_aircraft.entity;

import immersive_aircraft.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * UAV - unmanned aircraft launched from a bomb bay.
 * Cannot be boarded or placed. Has a temporary HP buffer that drops to 1
 * after a few seconds of flight (see {@link #HP_DROP_DELAY}).
 * Flies until its fuel runs out or it comes to a stop, then explodes.
 */
public class UavEntity extends AirplaneEntity {
    // Flight duration per piece of fuel: 400 ticks = 20 seconds
    private static final int FUEL_TICKS = 400;
    // Speed below which the UAV is considered "not moving" (1 block/second = 0.05 blocks/tick)
    private static final double STOP_SPEED = 0.05;
    // Ticks of being stopped before exploding
    private static final int STOP_EXPLODE_DELAY = 20;
    // Temporary HP buffer set on spawn so early hits don't destroy the UAV right away
    private static final float INITIAL_HP_BUFFER = 1000.0f;
    // Time after spawn when the HP buffer drops to 1 (50 ticks = 2.5 seconds)
    protected static final int HP_DROP_DELAY = 50;
    // Contrails stay disabled for the first 2 seconds (40 ticks) after launch
    private static final int TRAIL_DELAY = 40;
    // Ticks during which the launch speed inherited from the carrier is held before the
    // UAV is allowed to decay towards its own top speed (1 second).
    private static final int MOMENTUM_TICKS = 20;

    private int fuelTicks = FUEL_TICKS;
    private int stoppedTicks = 0;
    private boolean engineStarted = false;
    protected int spawnTicks = 0;
    // Speed inherited from the launching aircraft, in blocks/tick. Decays to zero once the
    // UAV has flown for MOMENTUM_TICKS, after which normal friction takes over.
    private double momentumX;
    private double momentumY;
    private double momentumZ;
    private int momentumTicks = 0;
    private UUID ownerUuid;

    public UavEntity(EntityType<? extends AircraftEntity> entityType, Level world) {
        super(entityType, world, true);
        setHealth(INITIAL_HP_BUFFER); // Start with high HP buffer
    }

    /**
     * Inherits the launching aircraft's velocity, so a drone dropped from a plane doing
     * 10 blocks/second keeps that momentum instead of snapping to its own much lower
     * cruising speed on the very first tick.
     */
    public void setLaunchMomentum(double x, double y, double z) {
        this.momentumX = x;
        this.momentumY = y;
        this.momentumZ = z;
        this.momentumTicks = MOMENTUM_TICKS;
    }

    // Spool-up time is ENGINE_SPOOL_UP_TICKS * (getEngineReactionSpeed()/20) / accelStat,
    // so doubling the reaction speed halves the time to reach full thrust: a UAV goes from
    // 40 ticks (2 s) to 20 ticks (1 s) to spool up, i.e. it accelerates twice as fast.
    @Override
    protected float getEngineReactionSpeed() { 
        return 10f;
    }

    // UAVs use the tiny drone propeller sound (same as the quadrocopter),
    // not the loud biplane engine.
    @Override
    protected SoundEvent getEngineSound() {
        return immersive_aircraft.Sounds.PROPELLER_TINY.get();
    }

    // The drone propeller is quiet by nature - play it twice as loud.
    @Override
    public float getSoundVolumeMultiplier() {
        return 2.0f;
    }

    protected int getHpDropDelay() {
        return HP_DROP_DELAY;
    }

    public void setOwner(Entity owner) {
        this.ownerUuid = owner.getUUID();
    }

    public Entity getOwner(Level level) {
        if (ownerUuid == null) return null;
        return level.getEntity(ownerUuid);
    }

    @Override
    public float getDurability() {
        return 0.05f;
    }

    // No contrails during the first 2 seconds of a drone's life. Besides looking odd right
    // after launch, a partially filled trail buffer used to stretch back towards the world
    // origin, which read as the trail "jumping away" as soon as the drone appeared.
    @Override
    protected boolean shouldRecordTrails() {
        return spawnTicks > TRAIL_DELAY;
    }

    @Override
    public Item asItem() {
        return Items.UAV.get();
    }

    @Override
    public boolean canAddPassenger(net.minecraft.world.entity.Entity passenger) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (isInvulnerableToBase(source)) {
            return false;
        }
        if (isRemoved()) {
            return true;
        }
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            discard();
            return true;
        }
        float health = getHealth() - amount / getDurability() / immersive_aircraft.config.Config.getInstance().damagePerHealthPoint;
        if (health <= 0) {
            setHealth(0);
            explode();
        } else {
            setHealth(health);
        }
        return true;
    }

    @Override
    protected void drop() {
        // UAV does not drop as an item
    }

    @Override
    public float getFuelUtilization() {
        return fuelTicks > 0 ? 1.0f : 0.0f;
    }

    @Override
    public void tick() {
        // Start engine on first server tick: target 100% thrust, actual power starts at 0
        if (!engineStarted && !level().isClientSide()) {
            engineStarted = true;
            setEngineTarget(1.0f);
        }

        // Age on BOTH sides: the contrail delay is purely visual and is evaluated on the
        // client, while the momentum hold below has to match the server-side simulation.
        spawnTicks++;

        // Hold the speed inherited from the launching aircraft for a moment, so a drone
        // released at 10 blocks/second does not instantly collapse to its own much lower
        // cruising speed. The hold fades out linearly, after which normal friction and the
        // engine take over completely.
        if (momentumTicks > 0) {
            momentumTicks--;
            // Linear fade-out: full inherited speed on the first tick, zero on the last.
            float k = (momentumTicks + 1.0f) / MOMENTUM_TICKS;
            Vec3 boost = new Vec3(momentumX, momentumY, momentumZ).scale(k);
            Vec3 velocity = getDeltaMovement();
            double current = velocity.length();
            if (current > 1.0e-4) {
                // Component of the inherited velocity along the drone's CURRENT heading.
                double inherited = boost.dot(velocity.scale(1.0 / current));
                // Only ever add speed, never brake: a drone already outrunning the inherited
                // speed is left entirely to its own engine and aerodynamics.
                if (inherited > current) {
                    setDeltaMovement(velocity.scale(inherited / current));
                }
            } else if (boost.lengthSqr() > 1.0e-8) {
                // Launched from a near-hover: adopt the inherited velocity as is.
                setDeltaMovement(boost);
            }
            if (momentumTicks == 0) {
                momentumX = momentumY = momentumZ = 0.0;
            }
        }

        super.tick();

        // Reset damage wobble to prevent visual effects
        setDamageWobbleTicks(0);
        setDamageWobbleStrength(0.0f);
        setDamageWobbleSide(1);

        // Consume fuel
        if (!level().isClientSide() && fuelTicks > 0) {
            fuelTicks--;
        }

        // Handle invulnerability period and HP drop
        if (!level().isClientSide()) {
            if (spawnTicks == getHpDropDelay()) {
                // Drop the HP buffer down to 1 once the protection window is over
                setHealth(1.0f);
            }
        }

        // Check if stopped and should explode
        if (!level().isClientSide()) {
            double speed = getDeltaMovement().length();
            if (speed < STOP_SPEED) {
                stoppedTicks++;
                if (stoppedTicks > STOP_EXPLODE_DELAY) {
                    explode();
                }
            } else {
                stoppedTicks = 0;
            }
        }
    }

    protected void explode() {
        if (isRemoved()) {
            return;
        }
        double x = getX();
        double y = getY();
        double z = getZ();
        discard();
        level().explode(this, x, y, z, immersive_aircraft.config.Config.getInstance().uavExplosionPower,
                immersive_aircraft.config.Config.getInstance().weaponsAreDestructive ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        spawnExplosionParticles(x, y, z);
    }

    @Override
    public double getZoom() {
        return 3.0;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void animateHurt(float yaw) {
        // Disable damage wobble visual effects for UAV
    }

    @Override
    public float[] getRenderColor() {
        // UAV always renders with full color, ignoring health
        return new float[] {1.0f, 1.0f, 1.0f};
    }

    @Override
    public boolean canBeCollidedWith(net.minecraft.world.entity.Entity entity) {
        // The UAV is launched in mid-air and never acts as a solid collision target
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FuelTicks", fuelTicks);
        if (ownerUuid != null) {
            tag.putString("OwnerUuid", ownerUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        fuelTicks = tag.getIntOr("FuelTicks", FUEL_TICKS);
        tag.getString("OwnerUuid").ifPresent(uuid -> ownerUuid = UUID.fromString(uuid));
    }
}