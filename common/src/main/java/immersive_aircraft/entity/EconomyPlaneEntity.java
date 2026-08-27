package immersive_aircraft.entity;

import immersive_aircraft.Items;
import immersive_aircraft.entity.misc.TrailDescriptor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

public class EconomyPlaneEntity extends AirplaneEntity {
    public EconomyPlaneEntity(EntityType<? extends AircraftEntity> entityType, Level world) {
        super(entityType, world, true);
    }

    @Override
    public Item asItem() {
        return Items.ECONOMY_PLANE.get();
    }

    /**
     * The economy plane is built to glide: its contrails fade in with speed exactly
     * like the biplane's, but start later (higher threshold) since it flies best clean.
     */
    @Override
    protected float getBaseTrailWidth(Matrix4f transform, int index, TrailDescriptor trail) {
        return (float) (Math.sqrt(getDeltaMovement().length()) * (0.5f + pressingInterpolatedX.getSmooth() * trail.x() * 0.025f) - 0.3f);
    }

    @Override
    public void tick() {
        super.tick();

        // Smoke from the engine exhaust
        emitSmokeParticle(
                0.5f * (tickCount % 2 == 0 ? -1.0f : 1.0f), 2.5f, -2.2f + 0.8f * ((tickCount / 2.0f) % 2),
                0.2f * (tickCount % 2 == 0 ? -1.0f : 1.0f), 0.0f, 0.0f
        );
    }

    @Override
    public double getZoom() {
        return 12.0;
    }
}
