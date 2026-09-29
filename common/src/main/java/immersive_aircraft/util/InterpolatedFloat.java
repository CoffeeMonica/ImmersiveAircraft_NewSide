package immersive_aircraft.util;

public class InterpolatedFloat {
    private float value;
    private float last;
    private float valueSmooth;
    private float lastSmooth;

    private float steps;
    // When > 0, valueSmooth moves LINEARLY towards the target by this absolute
    // amount per tick (span of 1.0 = 100%), instead of the default exponential
    // smoothing. Used by the engine power ramp for exact tick-accurate timings.
    private float linearStep = 0.0f;

    public InterpolatedFloat(float steps) {
        this.steps = 1.0f / steps;
    }

    public InterpolatedFloat() {
        this(5.0f);
    }

    /** Enables exact linear movement towards the target (full sweep in 1/linearStep ticks). */
    public void setLinearStep(float stepPerTick) {
        this.linearStep = Math.max(0.0f, stepPerTick);
    }

    public void update(float n) {
        last = value;
        value = n;

        lastSmooth = valueSmooth;
        decay(n, steps);
    }

    public void decay(float towards, float decay) {
        if (linearStep > 0.0f) {
            if (valueSmooth < towards) {
                valueSmooth = Math.min(valueSmooth + linearStep, towards);
            } else {
                valueSmooth = Math.max(valueSmooth - linearStep, towards);
            }
            return;
        }
        valueSmooth = valueSmooth * (1.0f - decay) + towards * decay;
    }

    public void reset() {
        value = 0;
        last = 0;
        valueSmooth = 0;
        lastSmooth = 0;
    }

    /**
     * Jumps every stored value straight to {@code n}, without any interpolation.
     * Used when a vehicle is created again (chunk/world load, tracking start) and the
     * value it should resume from is already known - ramping up from 0 would make a
     * machine that was left running appear to restart (and, for aircraft, fall until the
     * ramp caught up).
     */
    public void snapTo(float n) {
        value = n;
        last = n;
        valueSmooth = n;
        lastSmooth = n;
    }

    public void setSteps(float steps) {
        this.steps = 1.0f / steps;
    }

    public float get(float tickDelta) {
        return last + tickDelta * (value - last);
    }

    public float getSmooth(float tickDelta) {
        return lastSmooth + tickDelta * (valueSmooth - lastSmooth);
    }

    public float getSmooth() {
        return valueSmooth;
    }

    public float getValue() {
        return value;
    }

    public float getDiff() {
        return value - last;
    }
}
