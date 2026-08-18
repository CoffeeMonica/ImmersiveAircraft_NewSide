package immersive_aircraft.entity;

import immersive_aircraft.Items;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Improved UAV - enhanced version of the standard UAV.
 * - Reaches maximum speed instantly
 * - Mass of 5
 * - Completely immune to wind effects
 * - Explosion radius is 3 units larger (13.0f vs 10.0f)
 */
public class ImprovedUavEntity extends UavEntity {
    // Explosion radius is 3 units larger than the standard UAV (10.0f)
    private static final float EXPLOSION_RADIUS = 13.0f;

    public ImprovedUavEntity(EntityType<? extends AircraftEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected float getEngineReactionSpeed() {
        // Very small value makes enginePower reach target instantly
        return 0.001f;
    }

    @Override
    protected int getHpDropDelay() {
        // HP drops after 40 ticks (2 seconds) instead of 60 ticks (3 seconds)
        return 40;
    }

    @Override
    public float getWindStrength() {
        // Completely immune to wind effects
        return 0.0f;
    }

    @Override
    public Item asItem() {
        return Items.IMPROVED_UAV.get();
    }

    @Override
    protected void explode() {
        if (isRemoved()) {
            return;
        }
        double x = getX();
        double y = getY();
        double z = getZ();
        discard();
        level().explode(this, x, y, z, EXPLOSION_RADIUS, Level.ExplosionInteraction.MOB);
    }

}