package immersive_aircraft.entity.bullet;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BulletEntity extends AbstractHurtingProjectile {
    private float damage = 1.0f;
    // Light tracer particle used to draw the dark, thin trail behind this bullet.
    // Different cannons set a different particle so their tracers read differently
    // (rotary -> SMOKE, reinforced -> ASH), while the trail length is always derived
    // from the bullet's own speed.
    private ParticleOptions trailParticle = ParticleTypes.SMOKE;

    public BulletEntity(EntityType<? extends BulletEntity> entityType, Level level) {
        super(entityType, level);
    }

    public float getScale() {
        return 0.35f;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    /** Selects which dark particle draws this bullet's trail (rotary vs reinforced). */
    public void setTrailParticle(ParticleOptions particle) {
        this.trailParticle = particle;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (canHitEntity(result.getEntity())) {
            result.getEntity().hurt(level().damageSources().thrown(this, this.getOwner()), damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize() * 10.0;
        if (Double.isNaN(d)) {
            d = 10.0;
        }
        return distance < (d *= 64.0) * d * getScale();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (target.isSpectator() || !target.isAlive() || !target.isPickable()) {
            return false;
        }
        Entity entity = this.getOwner();
        if (entity == null) {
            return false;
        }
        // Cannot hit the shooter itself
        if (target == entity) {
            return false;
        }
        // Cannot hit passengers riding the same vehicle
        if (entity.isPassengerOfSameVehicle(target)) {
            return false;
        }
        // Cannot hit the vehicle the shooter is riding
        if (target == entity.getVehicle()) {
            return false;
        }
        return true;
    }

    @Override
    public void tick() {
        // Rotary-cannon bullets are ballistic: a slight, graceful arc. Gravity is applied on
        // BOTH sides (client + server) so the locally-simulated client bullet follows the same
        // trajectory as the server one. Otherwise the client bullet flew straight while the
        // server one dropped, and the occasional resync made it look like it fell in sudden jumps.
        setDeltaMovement(getDeltaMovement().add(0.0, -0.035, 0.0));

        double px = getX(), py = getY(), pz = getZ();
        super.tick();

        // Visible tracer: a dark, thin smoke trail that traces the bullet's ACTUAL path over
        // this tick (so it is continuous, not dotted across several blocks -> no "jumping").
        // Length scales with speed but is capped to 2-3 blocks; the puffs drift and fade on
        // their own so the trail slowly expires for the whole flight.
        if (level().isClientSide()) {
            double dx = getX() - px, dy = getY() - py, dz = getZ() - pz;
            double travel = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (travel > 0.4) {
                double trailLen = Math.min(0.9 * travel, 3.0);
                int n = Math.max(2, (int) Math.ceil(trailLen / 0.5));
                double inv = 1.0 / travel;
                double udx = dx * inv, udy = dy * inv, udz = dz * inv;
                for (int i = 0; i < n; i++) {
                    double back = (i + 0.5) * trailLen / n;
                    level().addParticle(trailParticle,
                            getX() - udx * back,
                            getY() - udy * back,
                            getZ() - udz * back,
                            (float) (udx * 0.05 * back),
                            (float) (udy * 0.05 * back),
                            (float) (udz * 0.05 * back));
                }
            }
        }

        if (getDeltaMovement().lengthSqr() < 0.1) {
            discard();
        }
    }
}
