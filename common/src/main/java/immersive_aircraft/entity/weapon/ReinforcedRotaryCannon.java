package immersive_aircraft.entity.weapon;

import immersive_aircraft.Sounds;
import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.config.Config;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.entity.bullet.BulletEntity;
import immersive_aircraft.entity.misc.WeaponMount;
import immersive_aircraft.network.c2s.FireMessage;
import immersive_aircraft.resources.bbmodel.BBAnimationVariables;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static immersive_aircraft.Entities.BULLET;

public class ReinforcedRotaryCannon extends BulletWeapon {
    private final RotationalManager rotationalManager = new RotationalManager(this);
    private float cooldown = 0.0f;
    // Angular velocity of the barrel (radians/tick). It is "recharged" on every shot and
    // decays exponentially, so after you stop firing the barrel coasts for a few ticks
    // (inertia) then settles - instead of slamming to a stop or spinning too long.
    private float spinSpeed = 0.0f;

    public ReinforcedRotaryCannon(VehicleEntity entity, ItemStack stack, WeaponMount mount, int slot) {
        super(entity, stack, mount, slot);
        setBaseTransform(new Matrix4f(mount.transform()));
    }

    @Override
    protected Vector4f getBarrelOffset() {
        return new Vector4f(0.0f, 0.825f, -0.375f, 1.0f);
    }

    public float getVelocity() {
        return Config.getInstance().reinforcedRotaryCannonVelocity;
    }

    public float getInaccuracy() {
        return Config.getInstance().reinforcedRotaryCannonInaccuracy;
    }

    private float getMaxCooldown() {
        // Seconds between shots; the default of 4/60s is 3x shorter than the rotary cannon's.
        // Because the cooldown is decremented once per tick (1/20s), this lands on one shot
        // every 2 ticks in practice, i.e. about twice the effective fire rate.
        return Config.getInstance().reinforcedRotaryCannonCooldown;
    }

    @Override
    protected Entity getBullet(Vector4f position, Vector3f direction) {
        BulletEntity bullet = BULLET.get().create(getEntity().level(), EntitySpawnReason.TRIGGERED);
        assert bullet != null;
        bullet.setDamage(Config.getInstance().reinforcedRotaryCannonDamage);
        bullet.setTrailParticle(net.minecraft.core.particles.ParticleTypes.ASH);
        bullet.setPos(position.x() + getEntity().getX(), position.y() + getEntity().getY(), position.z() + getEntity().getZ());
        bullet.setOwner(getEntity().getControllingPassenger());
        bullet.shoot(direction.x(), direction.y(), direction.z(), getVelocity(), getInaccuracy());
        return bullet;
    }

    @Override
    public void tick() {
        cooldown -= 1.0f / 20.0f;
        rotationalManager.tick();
        rotationalManager.pointTo(getEntity());

        // Smooth barrel spin with inertia. We advance roll every tick (after the manager
        // captured the previous value) so the renderer interpolates a continuous turn, and
        // decay the velocity so it coasts briefly after the trigger is released.
        if (spinSpeed > 0.0008f) {
            rotationalManager.roll += spinSpeed;
            spinSpeed *= getSpinDecay();
            if (spinSpeed <= 0.0008f) {
                spinSpeed = 0.0f;
            }
        }
    }

    /**
     * One shot means a smooth 90° barrel turn. The angular velocity is therefore
     * scaled by the fire rate (1 shot per cooldown seconds), which also covers the
     * faster-firing reinforced cannon automatically.
     */
    private float getSpinPerTick() {
        float interval = Math.max(1.0f, getMaxCooldown() * 20.0f); // ticks per shot
        return (float) (Math.PI / 2.0) / interval;
    }

    /**
     * Exponential decay that makes a single shot deliver exactly one 90° turn before
     * settling, while repeated shots keep the barrel spinning at full cadence.
     */
    private float getSpinDecay() {
        float interval = Math.max(1.0f, getMaxCooldown() * 20.0f);
        return 1.0f - 1.0f / interval;
    }

    @Override
    public void fire(Vector3f direction) {
        if (spentAmmoItems(Config.getInstance().reinforcedRotaryCannonAmmunition, 1)) {
            super.fire(direction);
        }
    }

    @Override
    public SoundEvent getSound() {
        return Sounds.CANNON.get();
    }

    private Vector3f getDirection() {
        return rotationalManager.screenToGlobal(getEntity());
    }

    @Override
    public void clientFire(int index) {
        if (cooldown <= 0.0f) {
            cooldown = getMaxCooldown();

            // Recharge the spin for the inertia animation. Holding fire keeps the barrel
            // turning; letting go lets the recycled velocity in tick() coast to a stop.
            spinSpeed = Math.max(spinSpeed, getSpinPerTick());

            // Send a fire message for every actual shot so ammo consumption
            // matches the number of bullets that leave the barrel.
            NetworkHandler.sendToServer(new FireMessage(getSlot(), index, getDirection()));
        }
    }

    @Override
    public <T extends VehicleEntity> void setAnimationVariables(T entity, float time) {
        super.setAnimationVariables(entity, time);

        float tickDelta = time % 1.0f;
        BBAnimationVariables.set("pitch", (float) (rotationalManager.getPitch(tickDelta) / Math.PI * 180.0f));
        BBAnimationVariables.set("yaw", (float) (rotationalManager.getYaw(tickDelta) / Math.PI * 180.0f));
        BBAnimationVariables.set("roll", (float) (rotationalManager.getRoll(tickDelta) / Math.PI * 180.0f));
    }
}