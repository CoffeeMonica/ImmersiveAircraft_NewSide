package immersive_aircraft.entity;

import immersive_aircraft.Items;
import immersive_aircraft.entity.misc.TrailDescriptor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

public class ScarletBiplaneEntity extends AirplaneEntity {
    public ScarletBiplaneEntity(EntityType<? extends AircraftEntity> entityType, Level world) {
        super(entityType, world, true);
    }

    @Override
    public Item asItem() {
        return Items.SCARLET_BIPLANE.get();
    }

    /**
     * Trail strength scales with speed (like the vanilla biplane): below the threshold
     * no contrail is drawn, while aggressive rolls make the outer-wing trails stronger.
     */
    @Override
    protected float getBaseTrailWidth(Matrix4f transform, int index, TrailDescriptor trail) {
        return (float) (Math.sqrt(getDeltaMovement().length()) * (0.5f + (pressingInterpolatedX.getSmooth() * trail.x()) * 0.025f) - 0.25f);
    }

    @Override
    public void tick() {
        super.tick();

        // Smoke from the engine exhaust
        emitSmokeParticle(
                (tickCount % 2 == 0 ? -1.0f : 1.0f), 1.2f, 0.0f,
                0.0f, 0.0f, -0.5f
        );
    }

    @Override
    public double getZoom() {
        return 8.0;
    }

    @Override
    public int getDefaultDyeColor() {
        return 0xEF2323;
    }
}
