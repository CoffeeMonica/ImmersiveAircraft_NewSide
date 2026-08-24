package immersive_aircraft.entity.weapon;

import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.config.Config;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.entity.misc.WeaponMount;
import immersive_aircraft.network.c2s.FireMessage;
import immersive_aircraft.resources.bbmodel.BBAnimationVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class HeavyCrossbow extends BulletWeapon {
    private float cooldown = 0.0f;

    private final float velocity;
    private final float inaccuracy;

    // Per-instance transform matrix, so we never mutate the shared WeaponMount
    private final Matrix4f localTransform = new Matrix4f();

    public HeavyCrossbow(VehicleEntity entity, ItemStack stack, WeaponMount mount, int slot) {
        this(entity, stack, mount, slot, Config.getInstance().heavyCrossBowVelocity, Config.getInstance().heavyCrossBowInaccuracy);
    }

    public HeavyCrossbow(VehicleEntity entity, ItemStack stack, WeaponMount mount, int slot, float velocity, float inaccuracy) {
        super(entity, stack, mount, slot);

        this.velocity = velocity;
        this.inaccuracy = inaccuracy;

        // Always initialize localTransform from the mount transform
        localTransform.set(mount.transform());

        // Create RotationalManager if this mount has rotation enabled
        if (mount.enableRotation()) {
            rotationalManager = new RotationalManager(this);
            setBaseTransform(new Matrix4f(mount.transform()));
        }
    }

    private float getMaxCooldown() {
        return Config.getInstance().heavyCrossBowCooldown;
    }

    @Override
    protected float getBarrelLength() {
        return 1.25f;
    }

    @Override
    protected Vector4f getBarrelOffset() {
        return new Vector4f(0.0f, 0.3f, 0.0f, 1.0f);
    }

    public float getVelocity() {
        return velocity;
    }

    public float getInaccuracy() {
        return inaccuracy;
    }

    @Override
    protected Entity getBullet(Vector4f position, Vector3f direction) {
        VehicleEntity entity = getEntity();
        Arrow arrow = new Arrow(getEntity().level(), position.x() + entity.getX(), position.y() + entity.getY(), position.z() + entity.getZ(), new ItemStack(net.minecraft.world.item.Items.ARROW), null);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        // Set owner to the vehicle itself so arrows don't collide with it
        arrow.setOwner(getEntity());
        // Random velocity spread (75%–125% of base velocity with 0.25 spread)
        float spread = Config.getInstance().heavyCrossBowVelocitySpread;
        float speed = getVelocity() * (1.0f + (getEntity().getRandom().nextFloat() - 0.5f) * 2.0f * spread);
        arrow.shoot(direction.x(), direction.y() + 0.1f, direction.z(), speed, getInaccuracy());
        return arrow;
    }

    @Override
    public void tick() {
        cooldown -= 1.0f / 20.0f;

        if (rotationalManager != null && getBaseTransform() != null) {
            Entity controllingPassenger = getEntity().getControllingPassenger();
            Entity gunner = getEntity().getGunner(getGunnerOffset());
            boolean hasValidGunner = gunner != null && gunner.isAlive() && gunner.getVehicle() == getEntity() && gunner == controllingPassenger;
            if (hasValidGunner) {
                rotationalManager.tick();
                rotationalManager.pointTo(getEntity());

                // Clamp angles to mount limits so the crossbow points to the closest
                // valid position when the camera exceeds the reachable range
                WeaponMount mount = getMount();
                float yawDeg = (float) Math.toDegrees(rotationalManager.yaw);
                float pitchDeg = (float) Math.toDegrees(rotationalManager.pitch);

                rotationalManager.yaw = (float) Math.toRadians(Math.max(mount.minYaw(), Math.min(mount.maxYaw(), yawDeg)));
                rotationalManager.pitch = (float) Math.toRadians(Math.max(mount.minPitch(), Math.min(mount.maxPitch(), pitchDeg)));

                // Write the rotation into the LOCAL matrix instead of the shared mount
                localTransform.set(getBaseTransform());
                localTransform.rotateY(-rotationalManager.yaw);
                localTransform.rotateX(rotationalManager.pitch);
            } else {
                // No valid pilot - reset the local matrix and rotation
                localTransform.set(getBaseTransform());
                rotationalManager.tick();
                rotationalManager.yaw = 0.0f;
                rotationalManager.pitch = 0.0f;
            }
        }
    }

    @Override
    public void fire(Vector3f direction) {
        if (spentAmmoItems(Config.getInstance().arrowAmmunition, 1)) {
            fireBullets(direction);
        }
    }

    /**
     * Fires bullets without consuming ammunition.
     * Used by subclasses that handle their own ammunition consumption.
     */
    protected void fireBullets(Vector3f direction) {
        super.fire(direction);
    }

    @Override
    public void clientFire(int index) {
        if (cooldown <= 0.0f) {
            cooldown = getMaxCooldown();
            NetworkHandler.sendToServer(new FireMessage(getSlot(), index, getDirection()));
        }
    }

    protected Vector3f getDirection() {
        // localTransform already includes clamped rotation (applied in tick())
        Vector3f direction = new Vector3f(0, 0, 1.0f);
        direction.mul(new Matrix3f(localTransform));
        direction.mul(getEntity().getVehicleNormalTransform());
        return direction;
    }

    @Override
    public Matrix4f getTransform() {
        return localTransform;
    }

    @Override
    public <T extends VehicleEntity> void setAnimationVariables(T entity, float time) {
        super.setAnimationVariables(entity, time);

        // Each crossbow unconditionally overwrites the global animation registry
        // with its own values (including zero for pilotless vehicles) right before
        // rendering, so no crossbow picks up stale values from another vehicle.
        if (rotationalManager != null) {
            float tickDelta = time % 1.0f;
            BBAnimationVariables.set("pitch", (float) (rotationalManager.getPitch(tickDelta) / Math.PI * 180.0f));
            BBAnimationVariables.set("yaw", (float) (rotationalManager.getYaw(tickDelta) / Math.PI * 180.0f));
        }
    }

    public float getCooldown() {
        return Math.max(0.0f, cooldown / getMaxCooldown());
    }
}