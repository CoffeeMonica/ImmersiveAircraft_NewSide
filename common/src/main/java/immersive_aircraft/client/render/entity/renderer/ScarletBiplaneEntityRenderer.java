package immersive_aircraft.client.render.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import immersive_aircraft.Main;
import immersive_aircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import immersive_aircraft.entity.AircraftEntity;
import immersive_aircraft.entity.ScarletBiplaneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class ScarletBiplaneEntityRenderer<T extends ScarletBiplaneEntity> extends AircraftEntityRenderer<T> {
    private static final Identifier ID = Main.locate("scarlet_biplane");

    private final ModelPartRenderHandler<T> model = new ModelPartRenderHandler<T>()
            .add("dyed_body", (bbModel, object, vertexConsumerProvider, entity, matrixStack, light, time, modelPartRenderer) ->
                    renderDyed(bbModel, object, vertexConsumerProvider, entity, matrixStack, light, time, false, false))
            .add("dyed_body_highlights", (bbModel, object, vertexConsumerProvider, entity, matrixStack, light, time, modelPartRenderer) ->
                    renderDyed(bbModel, object, vertexConsumerProvider, entity, matrixStack, light, time, true, false));

    @Override
    protected Identifier getModelId() {
        return ID;
    }

    public ScarletBiplaneEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected ModelPartRenderHandler<T> getModel(AircraftEntity entity) {
        return model;
    }
}
