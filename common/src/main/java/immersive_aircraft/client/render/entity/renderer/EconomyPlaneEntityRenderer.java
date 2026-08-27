package immersive_aircraft.client.render.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import immersive_aircraft.Main;
import immersive_aircraft.client.render.entity.renderer.utils.BBModelRenderer;
import immersive_aircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import immersive_aircraft.entity.AircraftEntity;
import immersive_aircraft.entity.EconomyPlaneEntity;
import immersive_aircraft.resources.bbmodel.BBBone;
import immersive_aircraft.resources.bbmodel.BBObject;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

import java.util.Random;

public class EconomyPlaneEntityRenderer<T extends EconomyPlaneEntity> extends AircraftEntityRenderer<T> {
    private static final Identifier ID = Main.locate("economy_plane");

    private final Random random = new Random();

    private final ModelPartRenderHandler<T> model = new ModelPartRenderHandler<T>()
            .add("engine_block",
                    (entity, yaw, time, matrixStack) -> {
                        double p = entity.enginePower.getSmooth() / 32.0;
                        matrixStack.translate((random.nextDouble() - 0.5) * p, (random.nextDouble() - 0.5) * p, (random.nextDouble() - 0.5) * p);
                    })
            .add("belt",
                    (bbModel, object, vertexConsumerProvider, entity, matrixStack, light, time, modelPartRenderer) -> {
                        if (object instanceof BBBone bone) {
                            int frame = (int) entity.engineRotation.getSmooth(time % 1.0f);
                            BBObject belt = bone.children.get(Math.floorMod(frame, bone.children.size()));
                            BBModelRenderer.renderObject(bbModel, belt, matrixStack, vertexConsumerProvider, light, time, entity, null, 1.0f, 1.0f, 1.0f, 1.0f);
                        }
                    })
            .add("dyed_body", (bbModel, object, vertexConsumerProvider, entity, matrixStack, light, time, modelPartRenderer) ->
                    renderDyed(bbModel, object, vertexConsumerProvider, entity, matrixStack, light, time, false, false));

    @Override
    protected Identifier getModelId() {
        return ID;
    }

    public EconomyPlaneEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected ModelPartRenderHandler<T> getModel(AircraftEntity entity) {
        return model;
    }
}
