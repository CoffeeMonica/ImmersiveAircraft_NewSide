package immersive_aircraft.entity;

import immersive_aircraft.Items;
import net.minecraft.server.level.ServerLevel;
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
 * UAV - unmanned aircraft used as a bomb_bay projectile.
 * Cannot be boarded or placed. Has high initial HP that drops to 1 after 2 seconds.
 * Ignores collisions with the aircraft that launched it.
 * Flies until its single coal fuel runs out, then explodes.
 */
public class UavEntity extends AirplaneEntity {
    // Vanilla coal burns for 600 ticks (30 seconds)
    private static final int FUEL_TICKS = 600;
    // Speed below which the UAV is considered "not moving" (1 block/second = 0.05 blocks/tick)
    private static final double STOP_SPEED = 0.05;
    // Ticks of being stopped before exploding
    private static final int STOP_EXPLODE_DELAY = 20;
    // Initial HP buffer, drops to 1 after 2 seconds
    private static final float INITIAL_HP_BUFFER = 1000.0f;
    // Time after spawn when HP drops (3 seconds = 60 ticks)
    protected static final int HP_DROP_DELAY = 60;

    private int fuelTicks = FUEL_TICKS;
    private int stoppedTicks = 0;
    private boolean engineStarted = false;
    protected int spawnTicks = 0;
    private UUID ownerUuid;

    public UavEntity(EntityType<? extends AircraftEntity> entityType, Level world) {
        super(entityType, world, true);
        setHealth(INITIAL_HP_BUFFER); // Start with high HP buffer
    }

    @Override
    protected float getEngineReactionSpeed() {
        return 50.0f;
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
            spawnTicks++;
            if (spawnTicks == getHpDropDelay()) {
                // Drop HP to 1 after 2 seconds
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
        level().explode(this, x, y, z, 10.0f, Level.ExplosionInteraction.MOB);
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
        // Ignore collisions with the launching aircraft
        Entity owner = getOwner(level());
        if (owner != null && owner == entity) {
            return false;
        }
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