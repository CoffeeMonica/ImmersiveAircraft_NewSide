package immersive_aircraft.entity.weapon;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.entity.misc.WeaponMount;
import net.minecraft.world.item.ItemStack;

public abstract class Weapon {
    private final VehicleEntity entity;
    private final ItemStack stack;
    private final WeaponMount mount;
    private final int slot;
    private int gunnerOffset;
    protected RotationalManager rotationalManager;
    private Matrix4f baseTransform;

    public Weapon(VehicleEntity entity, ItemStack stack, WeaponMount mount, int slot) {
        this.entity = entity;
        this.stack = stack;
        this.mount = mount;
        this.slot = slot;
    }

    public VehicleEntity getEntity() {
        return entity;
    }

    public ItemStack getStack() {
        return stack;
    }

    public WeaponMount getMount() {
        return mount;
    }

    public int getSlot() {
        return slot;
    }

    public int getGunnerOffset() {
        return gunnerOffset;
    }

    public void setGunnerOffset(int gunnerOffset) {
        this.gunnerOffset = gunnerOffset;
    }

    public RotationalManager getRotationalManager() {
        return rotationalManager;
    }

    public Matrix4f getBaseTransform() {
        return baseTransform;
    }

    public void setBaseTransform(Matrix4f baseTransform) {
        this.baseTransform = baseTransform;
    }

    /**
     * Returns the current transform matrix for this weapon instance.
     * By default returns the shared mount transform; subclasses that keep
     * their own per-instance rotation should override this.
     */
    public Matrix4f getTransform() {
        return mount != null ? mount.transform() : new Matrix4f();
    }

    public abstract void tick();

    public abstract void fire(Vector3f direction);

    public abstract void clientFire(int index);

    public <T extends VehicleEntity> void setAnimationVariables(T entity, float time) {
        // nop
    }
}