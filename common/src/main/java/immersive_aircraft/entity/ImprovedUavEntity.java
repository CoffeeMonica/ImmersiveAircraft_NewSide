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
 * - Explosion power is configurable (improvedUavExplosionPower, default 13)
 */
public class ImprovedUavEntity extends UavEntity {
    // Explosion power comes from the config (improvedUavExplosionPower).
    @Override
    protected void explode() {
        if (isRemoved()) {
            return;
        }
        double x = getX();
        double y = getY();
        double z = getZ();
        discard();
        level().explode(this, x, y, z, immersive_aircraft.config.Config.getInstance().improvedUavExplosionPower,
                immersive_aircraft.config.Config.getInstance().weaponsAreDestructive ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
    }

    // Falls off over exactly ONE second (20 ticks) without the engine, unscaled by
    // its near-zero reaction speed - then instantly spools back up to 100%.
    @Override
    protected float getEngineDecayTicks(float reactionScale) {
        return Math.max(1, immersive_aircraft.config.Config.getInstance().engineDecayTicks);
    }

    public ImprovedUavEntity(EntityType<? extends AircraftEntity> entityType, Level world) {
        super(entityType, world);
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

}