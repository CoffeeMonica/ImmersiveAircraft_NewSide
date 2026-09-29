package immersive_aircraft.entity.bullet;

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
    // Tracer colour drawn behind this bullet. The ribbon itself is rendered as geometry
    // (see BulletEntityRenderer) so this colour is exact and unaffected by world lighting.
    private int tracerColor = RotaryCannonTracer.ROTARY_COLOR;
    // Direction the bullet travelled this tick (unit vector) and the resulting ribbon
    // length, consumed by the renderer to draw the camera-facing tracer.
    private float tracerDirX, tracerDirY, tracerDirZ;
    private float tracerLength;

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

    /** Selects which colour draws this bullet's tracer (rotary vs reinforced). */
    public void setTrailParticle(int color) {
        this.tracerColor = color;
    }

    public int getTracerColor() {
        return tracerColor;
    }

    public float getTracerDirX() {
        return tracerDirX;
    }

    public float getTracerDirY() {
        return tracerDirY;
    }

    public float getTracerDirZ() {
        return tracerDirZ;
    }

    public float getTracerLength() {
        return tracerLength;
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

        // The tracer itself is drawn as camera-facing geometry in BulletEntityRenderer, not
        // as particles: a Dust particle is a light-textured sprite multiplied by world
        // lighting, so it rendered black in shade and washed both cannons' colours out to
        // the same grey. Geometry keeps the exact colour at any light level and any range.
        // Remember where the bullet was heading and how fast, for the renderer to consume.
        double dx = getX() - px, dy = getY() - py, dz = getZ() - pz;
        double travel = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (travel > 1.0e-4) {
            tracerDirX = (float) (dx / travel);
            tracerDirY = (float) (dy / travel);
            tracerDirZ = (float) (dz / travel);
            // Ribbon length grows with speed, capped so a tracer never fills the screen.
            tracerLength = (float) Math.min(0.55 * travel, 3.0);
        }

        if (getDeltaMovement().lengthSqr() < 0.1) {
            discard();
        }
    }
}
