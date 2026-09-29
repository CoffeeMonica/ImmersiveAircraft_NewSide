package immersive_aircraft.client.render.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import immersive_aircraft.Main;
import immersive_aircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import immersive_aircraft.entity.AircraftEntity;
import immersive_aircraft.entity.UavEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class UavEntityRenderer<T extends UavEntity> extends AircraftEntityRenderer<T> {
    private static final Identifier ID = Main.locate("uav");

    protected Identifier getModelId() {
        return ID;
    }

    private final ModelPartRenderHandler<T> model = new ModelPartRenderHandler<>();

    public UavEntityRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius = 0.6f;
    }

    @Override
    protected ModelPartRenderHandler<T> getModel(AircraftEntity entity) {
        return model;
    }

    @Override
    protected void applyModelTransform(PoseStack matrixStack) {
        // The uav.bbmodel is authored nose-towards -Z (fuselage runs z=-10.5..10.5 with the
        // canopy at the negative end), while the vehicle's forward axis is +Z
        // (see VehicleEntity#getForwardDirection). Without this spin the drone flies
        // tail-first. Must live in applyModelTransform so the radar outline pass matches.
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0f));
    }
}