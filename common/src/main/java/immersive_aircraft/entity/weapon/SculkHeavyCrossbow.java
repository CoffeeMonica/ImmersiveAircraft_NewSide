package immersive_aircraft.entity.weapon;

import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.config.Config;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.entity.misc.WeaponMount;
import immersive_aircraft.network.c2s.FireMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SculkHeavyCrossbow extends HeavyCrossbow {
    private float cooldown = 0.0f;

    public SculkHeavyCrossbow(VehicleEntity entity, ItemStack stack, WeaponMount mount, int slot) {
        super(entity, stack, mount, slot,
                Config.getInstance().heavyCrossBowVelocity,
                Config.getInstance().heavyCrossBowInaccuracy);
    }

    @Override
    protected int getBulletCount() {
        return 1;
    }

    private float getMaxCooldown() {
        return Config.getInstance().sculkHeavyCrossBowCooldown;
    }

    @Override
    public void tick() {
        super.tick();
        cooldown -= 1.0f / 20.0f;
    }

    @Override
    public void fire(Vector3f direction) {
        if (spentAmmoItems(Config.getInstance().arrowAmmunition, 1)) {
            shootSonicBeam(direction);
        }
    }

    private Vec3 getMountWorldPos() {
        // Calculate the beam start position from the MOUNT position (barrel tip), not entity center
        Vector4f mountPos = new Vector4f(getBarrelOffset());
        mountPos.mul(getTransform());

        Vector3f localPos = new Vector3f(mountPos.x, mountPos.y, mountPos.z);
        localPos.mul(getEntity().getVehicleNormalTransform());

        return new Vec3(
                getEntity().getX() + localPos.x,
                getEntity().getY() + localPos.y + 0.625f,
                getEntity().getZ() + localPos.z
        );
    }

    private void shootSonicBeam(Vector3f direction) {
        ServerLevel world = (ServerLevel) getEntity().level();
        Entity shooter = getEntity();
        Vec3 shooterPos = getMountWorldPos();
        Vec3 dir = new Vec3(direction.x(), direction.y(), direction.z()).normalize();
        double maxDistance = Config.getInstance().sculkHeavyCrossBowRange;
        float radius = Config.getInstance().sculkHeavyCrossBowRadius;

        // Play sound from mount position
        world.playSound(null, shooterPos.x, shooterPos.y, shooterPos.z,
                SoundEvents.WARDEN_SONIC_BOOM, shooter.getSoundSource(), 2.0F, 1.0F);

        // Damage and particles along the entire beam until it hits a block
        double stepSize = 1.0;
        double totalDistance = 0;

        Set<Entity> damagedEntities = new HashSet<>();

        while (totalDistance < maxDistance) {
            Vec3 beamPos = shooterPos.add(dir.scale(totalDistance));
            AABB box = new AABB(beamPos.x - radius, beamPos.y - radius, beamPos.z - radius,
                    beamPos.x + radius, beamPos.y + radius, beamPos.z + radius);

            // Damage all entities (including vehicles) whose bounding boxes intersect the beam
            List<Entity> entities = world.getEntitiesOfClass(Entity.class, box, entity -> {
                return entity != shooter
                        && entity.isAlive()
                        && entity.isPickable()
                        && !damagedEntities.contains(entity);
            });

            for (Entity target : entities) {
                // Check if the entity's main bounding box or any additional bounding boxes intersect the beam
                boolean hit = target.getBoundingBox().intersects(box);
                if (!hit && target instanceof VehicleEntity vehicle) {
                    for (AABB shape : vehicle.getShapes()) {
                        if (shape.intersects(box)) {
                            hit = true;
                            break;
                        }
                    }
                }

                if (hit) {
                    damagedEntities.add(target);

                    DamageSource damageSource = world.damageSources().sonicBoom(shooter);
                    target.hurt(damageSource, Config.getInstance().sculkHeavyCrossBowDamage);

                    if (target instanceof LivingEntity living) {
                        double dx = target.getX() - shooter.getX();
                        double dz = target.getZ() - shooter.getZ();
                        living.knockback(1.5F, dx, dz);
                    }
                }
            }

            // Spawn particles at every step - stepSize of 1 block provides good coverage
            world.sendParticles(
                    ParticleTypes.SONIC_BOOM,
                    true,
                    false,
                    beamPos.x,
                    beamPos.y,
                    beamPos.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );

            // Check if beam hits a solid block
            BlockPos blockPos = BlockPos.containing(beamPos);
            BlockState state = world.getBlockState(blockPos);
            if (!state.getCollisionShape(world, blockPos).isEmpty()) {
                world.sendParticles(
                        ParticleTypes.SONIC_BOOM,
                        true,
                        false,
                        beamPos.x,
                        beamPos.y,
                        beamPos.z,
                        3,
                        0.1,
                        0.1,
                        0.1,
                        0.0
                );
                break;
            }

            totalDistance += stepSize;
        }
    }

    @Override
    public void clientFire(int index) {
        if (cooldown <= 0.0f) {
            cooldown = getMaxCooldown();
            NetworkHandler.sendToServer(new FireMessage(getSlot(), index, getDirection()));
        }
    }

    protected Vector3f getDirection() {
        Vector3f direction = new Vector3f(0, 0, 1.0f);
        direction.mul(new Matrix3f(getTransform()));
        direction.mul(getEntity().getVehicleNormalTransform());
        return direction;
    }

    @Override
    public <T extends VehicleEntity> void setAnimationVariables(T entity, float time) {
        super.setAnimationVariables(entity, time);
    }

    public float getCooldown() {
        return Math.max(0.0f, cooldown / getMaxCooldown());
    }
}