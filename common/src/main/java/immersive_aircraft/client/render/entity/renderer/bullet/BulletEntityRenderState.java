package immersive_aircraft.client.render.entity.renderer.bullet;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Render state for bullet entities.
 *
 * <p>Carries the tracer ribbon data: the bullet's world-space direction of travel, its
 * tracer colour and the ribbon length. The ribbon is drawn as camera-facing geometry rather
 * than a particle, so it keeps its exact colour and is never darkened by world lighting.
 */
public class BulletEntityRenderState extends EntityRenderState {
    public float scale;
    // World-space unit direction the bullet is travelling in.
    public float dirX, dirY, dirZ;
    // Tracer colour in 0..1 RGB.
    public float tracerR, tracerG, tracerB;
    // Ribbon length in blocks, derived from the bullet's speed.
    public float tracerLength;
}
