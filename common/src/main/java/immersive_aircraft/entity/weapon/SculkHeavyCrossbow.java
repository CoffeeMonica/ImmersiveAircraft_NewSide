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
            // Recompute the beam direction SERVER-SIDE from this vehicle's own turret
            // state. Trusting the client-sent direction desynchronizes origin and beam
            // in multiplayer: the beam misses aircraft hit boxes and can clip the pilot.
            shootSonicBeam(getDirection());
        }
    }

    private Vec3 getMountWorldPos() {
        // Same transform chain BulletWeapon.fire uses to spawn bullets (proven to line up
        // with the visual weapon): barrel offset -> mount space -> world space.
        // The vehicle transform is rotation-only; the entity position is added in
        // double precision so the beam origin stays exact far from the world origin.
        Vector4f position = new Vector4f(getBarrelOffset());
        position.mul(getTransform());
        return getEntity().transformToWorld(position.x(), position.y(), position.z());
    }

    private void shootSonicBeam(Vector3f direction) {
        ServerLevel world = (ServerLevel) getEntity().level();
        Entity shooter = getEntity();
        Vec3 shooterPos = getMountWorldPos();
        Vec3 dir = new Vec3(direction.x(), direction.y(), direction.z()).normalize();
        double maxDistance = Config.getInstance().sculkHeavyCrossBowRange;
        // Never allow a zero-size query box (e.g. radius set to 0 in a stale config),
        // otherwise getEntities would find almost nothing and hits would be dropped.
        float radius = Math.max(0.5f, Config.getInstance().sculkHeavyCrossBowRadius);

        // Play sound from mount position
        world.playSound(null, shooterPos.x, shooterPos.y, shooterPos.z,
                SoundEvents.WARDEN_SONIC_BOOM, shooter.getSoundSource(), 2.0F, 1.0F);

        // Damage and particles along the entire beam until it hits a block
        double stepSize = 1.0;
        double totalDistance = 0;

        Set<Entity> damagedEntities = new HashSet<>();

        // Collect candidate vehicles along the ENTIRE beam corridor up front. A vehicle's
        // detailed hit boxes (wings, balloons) extend far beyond its main entity box, so
        // per-step main-box based entity retrieval silently skipped aircraft that were
        // hit only on a wing - especially in multiplayer at longer ranges.
        List<VehicleEntity> vehicles = world.getEntitiesOfClass(VehicleEntity.class,
                new AABB(shooterPos, shooterPos.add(dir.scale(maxDistance))).inflate(radius + 16.0),
                v -> v != shooter
                        && v.getRootVehicle() != shooter
                        && v.isAlive()
                        && v.isPickable());

        Vec3 prevPos = shooterPos;

        while (totalDistance < maxDistance) {
            Vec3 beamPos = shooterPos.add(dir.scale(totalDistance));
            AABB box = new AABB(beamPos.x - radius, beamPos.y - radius, beamPos.z - radius,
                    beamPos.x + radius, beamPos.y + radius, beamPos.z + radius);
            // Query volume also covers the segment travelled since the previous step,
            // so thin shapes cannot slip between two sampling points.
            AABB queryBox = box.minmax(new AABB(prevPos.x - radius, prevPos.y - radius, prevPos.z - radius,
                    prevPos.x + radius, prevPos.y + radius, prevPos.z + radius));

            // Damage all non-vehicle entities whose bounding boxes intersect the beam.
            // Skip the first 2 blocks to avoid hitting the pilot.
            if (totalDistance >= 2.0) {
                List<Entity> entities = world.getEntitiesOfClass(Entity.class, queryBox, entity -> {
                    return !(entity instanceof VehicleEntity)
                            && entity != shooter
                            && entity.getRootVehicle() != shooter
                            && entity.isAlive()
                            && entity.isPickable()
                            && !damagedEntities.contains(entity);
                });

                for (Entity target : entities) {
                    boolean hit = target.getBoundingBox().intersects(queryBox);

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
            }

            // Aircraft: exact oriented-box test against this beam segment - detailed
            // boxes are clipped in the vehicle's LOCAL space, so they follow its
            // orientation perfectly and thin parts (wings, balloons) cannot slip
            // between two beam sampling steps.
            for (VehicleEntity vehicle : vehicles) {
                if (damagedEntities.contains(vehicle)) {
                    continue;
                }
                boolean hit = vehicle.getBoundingBox().intersects(queryBox)
                        || vehicle.clipDetailed(prevPos, beamPos, radius).isPresent();
                if (hit) {
                    damagedEntities.add(vehicle);
                    vehicle.hurt(world.damageSources().sonicBoom(shooter), Config.getInstance().sculkHeavyCrossBowDamage);
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
            prevPos = beamPos;
        }
    }

    @Override
    public void clientFire(int index) {
        if (cooldown <= 0.0f) {
            cooldown = getMaxCooldown();
            NetworkHandler.sendToServer(new FireMessage(getSlot(), index, getDirection()));
        }
    }

    public float getCooldown() {
        return Math.max(0.0f, cooldown / getMaxCooldown());
    }
}