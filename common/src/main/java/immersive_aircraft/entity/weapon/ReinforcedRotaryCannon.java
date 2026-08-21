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
        bullet.setPos(position.x(), position.y(), position.z());
        bullet.setOwner(getEntity().getControllingPassenger());
        bullet.shoot(direction.x(), direction.y(), direction.z(), getVelocity(), getInaccuracy());
        return bullet;
    }

    @Override
    public void tick() {
        cooldown -= 1.0f / 20.0f;
        rotationalManager.tick();
        rotationalManager.pointTo(getEntity());
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

            // Advance the barrel rotation for animation.
            rotationalManager.roll += 0.25f;

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