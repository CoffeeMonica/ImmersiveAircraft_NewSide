package immersive_aircraft.client.render.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import immersive_aircraft.Main;
import immersive_aircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import immersive_aircraft.entity.AircraftEntity;
import immersive_aircraft.entity.ImprovedUavEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class ImprovedUavEntityRenderer<T extends ImprovedUavEntity> extends AircraftEntityRenderer<T> {
    private static final Identifier ID = Main.locate("uav");

    protected Identifier getModelId() {
        return ID;
    }

    private final ModelPartRenderHandler<T> model = new ModelPartRenderHandler<>();

    public ImprovedUavEntityRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius = 0.6f;
    }

    @Override
    protected ModelPartRenderHandler<T> getModel(AircraftEntity entity) {
        return model;
    }

    @Override
    protected void applyModelTransform(PoseStack matrixStack) {
        // Same 180 degree spin as the plain UAV - both entities share uav.bbmodel.
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0f));
    }
}