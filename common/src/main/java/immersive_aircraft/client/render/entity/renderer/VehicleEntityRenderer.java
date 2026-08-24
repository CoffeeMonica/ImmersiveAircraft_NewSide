package immersive_aircraft.client.render.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import immersive_aircraft.Main;
import immersive_aircraft.client.RadarHandler;
import immersive_aircraft.client.render.entity.renderer.utils.BBModelRenderer;
import immersive_aircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import immersive_aircraft.config.Config;
import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.resources.BBModelLoader;
import immersive_aircraft.resources.bbmodel.BBAnimationVariables;
import immersive_aircraft.resources.bbmodel.BBModel;
import immersive_aircraft.resources.bbmodel.BBObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;

public abstract class VehicleEntityRenderer<T extends VehicleEntity> extends EntityRenderer<T, VehicleEntityRenderState> {
    public VehicleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    protected abstract ModelPartRenderHandler<T> getModel(T entity);

    protected abstract Identifier getModelId();

    @Override
    public @NotNull VehicleEntityRenderState createRenderState() {
        return new VehicleEntityRenderState();
    }

    @Override
    public void extractRenderState(T entity, VehicleEntityRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.entity = entity;
        state.yaw = entity.getYRot();
        state.tickDelta = tickDelta;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void submit(VehicleEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        T entity = (T) state.entity;

        // Don't render the vehicle the player is scoping through - in first person the
        // own airframe must never block the scoped view, regardless of config.
        if (entity instanceof InventoryVehicleEntity inv && inv.isScoping() && Main.firstPersonGetter.isFirstPerson()) {
            return;
        }
        // Optionally hide it in third person too, per config.
        if (entity instanceof InventoryVehicleEntity inv && inv.isScoping() && Config.getInstance().hideVehicleWhileScoping) {
            return;
        }
        float yaw = Mth.lerp(state.tickDelta, entity.yRotO, entity.getYRot());
        float tickDelta = state.tickDelta;

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean radarTarget = RadarHandler.isTarget(entity);

        PoseStack.Pose peek = matrixStack.last();

        // Normal pass: textured model, weapons, sails and trails.
        matrixStack.pushPose();
        applyVehicleTransforms(entity, yaw, tickDelta, matrixStack);
        renderLocal(entity, yaw, tickDelta, matrixStack, peek, bufferSource, state.lightCoords);
        matrixStack.popPose();

        bufferSource.endLastBatch();

        // Radar outline pass: redraw ONLY the bare model into the outline buffer, so its
        // silhouette shows through walls for the radar crew. The normal textured pass above
        // stays untouched. Trails are NOT redrawn here - the outline buffer source rejects
        // non-entity render types and would crash.
        if (radarTarget) {
            OutlineBufferSource outline = Minecraft.getInstance().renderBuffers().outlineBufferSource();
            outline.setColor(0xFF000000 | Config.getInstance().radarGlowColor);
            matrixStack.pushPose();
            applyVehicleTransforms(entity, yaw, tickDelta, matrixStack);
            BBModel bbModel = BBModelLoader.MODELS.get(getModelId());
            if (bbModel != null) {
                float time = (entity.level().getGameTime() % 24000 + tickDelta) / 20.0f;
                BBModelRenderer.renderModel(bbModel, matrixStack, outline, state.lightCoords, time, entity, null, 1.0f, 1.0f, 1.0f, 1.0f);
            }
            matrixStack.popPose();
        }

        // F3+B debug: visualize every detailed vehicle bounding box with a purple
        // outline. The shapes are the SAME rotated world-space envelopes used for
        // collision (they follow the aircraft's attitude), drawn relative to the
        // entity position on this pose stack.
        if (Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) {
            VertexConsumer lines = bufferSource.getBuffer(RenderTypes.LINES);
            for (AABB shape : entity.getAdditionalShapes()) {
                Vec3 c = shape.getCenter();
                drawHitboxOutline((float) c.x - (float) state.x, (float) c.y - (float) state.y, (float) c.z - (float) state.z,
                        (float) (shape.getXsize() / 2), (float) (shape.getYsize() / 2), (float) (shape.getZsize() / 2),
                        matrixStack.last(), lines);
            }
        }

        super.submit(state, matrixStack, collector, cameraState);
    }

    /** Draws a purple wireframe box centered at (cx,cy,cz) with half-extents (hx,hy,hz). */
    private static void drawHitboxOutline(float cx, float cy, float cz,
                                          float hx, float hy, float hz,
                                          PoseStack.Pose pose, VertexConsumer lines) {
        float x0 = cx - hx, y0 = cy - hy, z0 = cz - hz;
        float x1 = cx + hx, y1 = cy + hy, z1 = cz + hz;
        float r = 0.55F, g = 0.15F, b = 0.9F, a = 1.0F;

        // bottom rectangle
        line(pose, lines, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(pose, lines, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(pose, lines, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(pose, lines, x0, y0, z1, x0, y0, z0, r, g, b, a);
        // top rectangle
        line(pose, lines, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(pose, lines, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(pose, lines, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(pose, lines, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // pillars
        line(pose, lines, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(pose, lines, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(pose, lines, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(pose, lines, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer lines,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-5F) {
            return;
        }
        Vector3f normal = pose.normal().transform(new Vector3f(dx / len, dy / len, dz / len));
        lines.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setLineWidth(2.0F).setNormal(normal.x, normal.y, normal.z);
        lines.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setLineWidth(2.0F).setNormal(normal.x, normal.y, normal.z);
    }

    /** Rotation + damage-wobble transform shared by both rendering passes. */
    private void applyVehicleTransforms(T entity, float yaw, float tickDelta, PoseStack matrixStack) {
        matrixStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        matrixStack.mulPose(Axis.XP.rotationDegrees(entity.getViewXRot(tickDelta)));
        matrixStack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll(tickDelta)));

        // Wobble
        float h = (float) entity.getDamageWobbleTicks() - tickDelta;
        float j = entity.getDamageWobbleStrength() - tickDelta;
        if (j < 0.0f) {
            j = 0.0f;
        }
        if (h > 0.0f) {
            matrixStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(h) * h * j / 10.0f * (float) entity.getDamageWobbleSide()));
        }
    }

    public void renderLocal(T entity, float yaw, float tickDelta, PoseStack matrixStack, PoseStack.Pose peek, MultiBufferSource vertexConsumerProvider, int light) {
        //Wobble
        float h = (float) entity.getDamageWobbleTicks() - tickDelta;
        float j = entity.getDamageWobbleStrength() - tickDelta;
        if (j < 0.0f) {
            j = 0.0f;
        }
        if (h > 0.0f) {
            matrixStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(h) * h * j / 10.0f * (float) entity.getDamageWobbleSide()));
        }

        // Updated variables
        float time = (entity.level().getGameTime() % 24000 + tickDelta) / 20.0f;
        BBAnimationVariables.set("time", time);
        entity.setAnimationVariables(tickDelta);

        // Freeze looping model animations (e.g. the propeller spin-up loop) while the
        // engine is off - otherwise a parked vehicle keeps replaying them forever.
        float animationTime = time;
        if (entity instanceof EngineVehicle engine && engine.getEnginePower() <= 0.01f) {
            animationTime = 0.0f;
        }

        // Render model
        BBModel bbModel = BBModelLoader.MODELS.get(getModelId());
        if (bbModel != null) {
            float[] color = entity.getRenderColor();
            BBModelRenderer.renderModel(bbModel, matrixStack, vertexConsumerProvider, light, animationTime, entity, getModel(entity), color[0], color[1], color[2], 1.0f);
        }
    }

    public void renderOptionalObject(String name, BBModel model, MultiBufferSource vertexConsumerProvider, T entity, PoseStack matrixStack, int light, float time) {
        renderOptionalObject(name, model, vertexConsumerProvider, entity, matrixStack, light, time, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void renderOptionalObject(String name, BBModel model, MultiBufferSource vertexConsumerProvider, T entity, PoseStack matrixStack, int light, float time, float red, float green, float blue, float alpha) {
        BBObject object = model.objectsByName.get(name);
        if (object != null) {
            BBModelRenderer.renderObject(model, object, matrixStack, vertexConsumerProvider, light, time, entity, null, red, green, blue, alpha);
        }
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        // Don't render the vehicle the player is scoping through
        if (entity instanceof InventoryVehicleEntity inv && inv.isScoping() && Config.getInstance().hideVehicleWhileScoping) {
            return false;
        }
        if (!entity.shouldRender(x, y, z)) {
            return false;
        }
        AABB box = getBoundingBoxForCulling(entity).inflate(getCullingBoundingBoxInflation());
        return frustum.isVisible(box);
    }

    protected double getCullingBoundingBoxInflation() {
        return 1.0;
    }

    @Override
    protected AABB getBoundingBoxForCulling(T entity) {
        return entity.getBoundingBoxForCulling();
    }
}
