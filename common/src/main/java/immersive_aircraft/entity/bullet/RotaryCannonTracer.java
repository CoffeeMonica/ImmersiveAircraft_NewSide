package immersive_aircraft.entity.bullet;

/**
 * Tracer colours for the two rotary cannons.
 *
 * <p>The tracer is drawn as unlit, fullbright geometry by {@code BulletEntityRenderer}, so
 * these values are rendered exactly as written - no world lighting, no smoke texture
 * tinting. The two cannons use clearly different hues so the tracer itself tells you which
 * gun is firing:
 * <ul>
 *   <li>{@code rotary_cannon} - pale steel blue</li>
 *   <li>{@code reinforced_rotary_cannon} - hot orange</li>
 * </ul>
 */
public final class RotaryCannonTracer {
    public static final int ROTARY_COLOR = 0x9FD0FF;
    public static final int REINFORCED_COLOR = 0xFF8C2B;

    private RotaryCannonTracer() {
    }
}
