package immersive_aircraft.client.render.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import immersive_aircraft.Main;
import immersive_aircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import immersive_aircraft.entity.AircraftEntity;
import immersive_aircraft.entity.UavEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class UavEntityRenderer<T extends UavEntity> extends AircraftEntityRenderer<T> {
    private static final Identifier ID = Main.locate("uav");

    protected Identifier getModelId() {
        return ID;
    }

    private final ModelPartRenderHandler<T> model = new ModelPartRenderHandler<>();

    public UavEntityRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius = 0.27f;
    }

    @Override
    protected ModelPartRenderHandler<T> getModel(AircraftEntity entity) {
        return model;
    }

    @Override
    public void renderLocal(T entity, float yaw, float tickDelta, PoseStack matrixStack, PoseStack.Pose peek, MultiBufferSource vertexConsumerProvider, int light) {
        // UAV is 3x smaller than the biplane
        matrixStack.scale(1.0f / 3.0f, 1.0f / 3.0f, 1.0f / 3.0f);
        super.renderLocal(entity, yaw, tickDelta, matrixStack, peek, vertexConsumerProvider, light);
    }
}