package immersive_aircraft.client.render.entity.renderer.bullet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import immersive_aircraft.Main;
import immersive_aircraft.entity.bullet.BulletEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BulletEntityRenderer<T extends BulletEntity> extends EntityRenderer<T, BulletEntityRenderState> {
    private static final Identifier TEXTURE = Main.locate("textures/entity/bullet.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutNoCull(TEXTURE);
    // Same unlit beam render type the aircraft contrails use, so the tracer is immune to
    // world lighting. A Dust particle is a smoke-textured sprite multiplied by the light at
    // its position, which is why it turned black in shade and both cannon colours washed
    // out to the same grey.
    private static final RenderType TRACER_RENDER_TYPE = RenderTypes.beaconBeam(Main.locate("textures/entity/trail.png"), true);
    private static final int FULLBRIGHT = 15728640;

    // Half width of the tracer ribbon, in blocks. Kept small so it is a thin hairline
    // rather than a fat blob, and constant in world space so the shape never changes.
    private static final float TRACER_HALF_WIDTH = 0.03f;

    public BulletEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull BulletEntityRenderState createRenderState() {
        return new BulletEntityRenderState();
    }

    @Override
    public void extractRenderState(T entity, BulletEntityRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.scale = entity.getScale();
        state.dirX = entity.getTracerDirX();
        state.dirY = entity.getTracerDirY();
        state.dirZ = entity.getTracerDirZ();
        state.tracerLength = entity.getTracerLength();
        int color = entity.getTracerColor();
        state.tracerR = ((color >> 16) & 0xFF) / 255.0f;
        state.tracerG = ((color >> 8) & 0xFF) / 255.0f;
        state.tracerB = (color & 0xFF) / 255.0f;
    }

    @Override
    public void submit(BulletEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Tracer first, in the incoming world-aligned matrix (before the bullet's own
        // billboard/scale below), so it stays a straight world-space ribbon.
        if (state.tracerLength > 1.0e-4f) {
            renderTracer(state, matrixStack, collector);
        }

        matrixStack.pushPose();
        float scale = state.scale;
        matrixStack.scale(scale, scale, scale);
        matrixStack.translate(0.0, 0.5, 0.0);
        matrixStack.mulPose(cameraState.orientation);
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        PoseStack.Pose pose = matrixStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        collector.submitCustomGeometry(matrixStack, RENDER_TYPE, (poseEntry, vertexConsumer) -> {
            vertex(vertexConsumer, matrix4f, matrix3f, state.lightCoords, 0.0f, 0.0f, 0.0f, 1.0f);
            vertex(vertexConsumer, matrix4f, matrix3f, state.lightCoords, 1.0f, 0.0f, 1.0f, 1.0f);
            vertex(vertexConsumer, matrix4f, matrix3f, state.lightCoords, 1.0f, 1.0f, 1.0f, 0.0f);
            vertex(vertexConsumer, matrix4f, matrix3f, state.lightCoords, 0.0f, 1.0f, 1.0f, 0.0f);
        });

        matrixStack.popPose();
        super.submit(state, matrixStack, collector, cameraState);
    }

    /**
     * Tracer ribbon drawn as two thin crossed quads trailing behind the bullet along its
     * travel direction. Two crossed quads keep some width from every viewing angle (a
     * single quad would disappear edge-on), and the whole tracer is just one cross of
     * geometry per bullet, so the density stays very low - a clean line, not a cloud.
     */
    private void renderTracer(BulletEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector collector) {
        Vector3f axis = new Vector3f(state.dirX, state.dirY, state.dirZ);
        // Any non-parallel reference vector gives a stable perpendicular basis.
        Vector3f ref = Math.abs(axis.y()) > 0.99f ? new Vector3f(1.0f, 0.0f, 0.0f) : new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f side = new Vector3f().cross(axis, ref).normalize();
        Vector3f other = new Vector3f().cross(axis, side).normalize();
        ribbon(state, matrixStack, collector, side);
        ribbon(state, matrixStack, collector, other);
    }

    private void ribbon(BulletEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector collector, Vector3f side) {
        Vector3f axis = new Vector3f(state.dirX, state.dirY, state.dirZ);
        float len = state.tracerLength;
        float w = TRACER_HALF_WIDTH;
        // Near end sits at the bullet, far end trails behind it, each split to either side.
        Vector3f nearPos = new Vector3f(side.x() * w, side.y() * w, side.z() * w);
        Vector3f nearNeg = new Vector3f(-side.x() * w, -side.y() * w, -side.z() * w);
        Vector3f farPos = new Vector3f(-axis.x() * len + side.x() * w, -axis.y() * len + side.y() * w, -axis.z() * len + side.z() * w);
        Vector3f farNeg = new Vector3f(-axis.x() * len - side.x() * w, -axis.y() * len - side.y() * w, -axis.z() * len - side.z() * w);

        Matrix4f matrix = matrixStack.last().pose();
        collector.submitCustomGeometry(matrixStack, TRACER_RENDER_TYPE, (poseEntry, vertexConsumer) -> {
            tracerVertex(vertexConsumer, matrix, state, nearPos, 0.0f, 0.0f);
            tracerVertex(vertexConsumer, matrix, state, farPos, 1.0f, 0.0f);
            tracerVertex(vertexConsumer, matrix, state, farNeg, 1.0f, 1.0f);
            tracerVertex(vertexConsumer, matrix, state, nearNeg, 0.0f, 1.0f);
        });
    }

    private void tracerVertex(VertexConsumer vertexConsumer, Matrix4f matrix, BulletEntityRenderState state,
                              Vector3f offset, float u, float v) {
        vertexConsumer.addVertex(matrix, offset.x(), offset.y(), offset.z())
                .setColor(state.tracerR, state.tracerG, state.tracerB, 1.0f)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                // Fullbright so the tracer keeps its exact colour regardless of lighting.
                .setLight(FULLBRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    private static void vertex(VertexConsumer vertexConsumer, Matrix4f matrix4f, Matrix3f matrix3f, int light, float x, float y, float u, float v) {
        Vector3f n = matrix3f.transform(new Vector3f(0.0f, 1.0f, 0.0f));
        vertexConsumer.addVertex(matrix4f, x - 0.5f, y - 0.5f, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(n.x(), n.y(), n.z());
    }
}
