package immersive_aircraft.entity.weapon;

import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.entity.misc.WeaponMount;
import immersive_aircraft.network.s2c.FireResponse;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Random;
import java.util.Set;

public abstract class BulletWeapon extends Weapon {
    private final Random random = new Random();

    public BulletWeapon(VehicleEntity entity, ItemStack stack, WeaponMount mount, int slot) {
        super(entity, stack, mount, slot);
    }

    protected float getBarrelLength() {
        return 1.0f;
    }

    protected Vector4f getBarrelOffset() {
        return new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
    }

    protected int getBulletCount() {
        return 1;
    }

    public void fire(Vector3f direction) {
        // Calculate the LOCAL position of the barrel (weapon transform only).
        // The vehicle transform is rotation-only now; the entity's double-precision
        // world position is added inside getBullet() implementations.
        Vector4f position = getBarrelOffset();
        VehicleEntity entity = getEntity();
        position.mul(getTransform());

        Vec3 speed = entity.getSpeedVector();

        // Offset the position by the barrel length along the aim direction,
        // converted into vehicle-local space so 'position' stays fully local
        float barrelLength = getBarrelLength();
        Matrix3f inverseVehicleRotation = new Matrix3f(entity.getVehicleNormalTransform()).invert();
        Vector3f localDirection = inverseVehicleRotation.transform(new Vector3f(direction.x(), direction.y(), direction.z()));
        position.add(localDirection.x * barrelLength, localDirection.y * barrelLength, localDirection.z * barrelLength, 0.0f);

        // Rotate the local offset into world-aligned offsets. The vehicle transform is
        // ROTATION ONLY (no absolute translation), so this is precision-safe even far
        // from the world origin - getBullet() then adds the entity position in doubles.
        position.mul(entity.getVehicleTransform());

        // Spawn bullets
        for (int i = 0; i < getBulletCount(); i++) {
            Entity bullet = getBullet(position, direction);
            bullet.setDeltaMovement(bullet.getDeltaMovement().add(speed));
            entity.level().addFreshEntity(bullet);
        }

        // Fire-particle - push the muzzle flash a bit further past the barrel so it
        // clears the weapon geometry and reads as coming from the barrel tip.
        position.add(direction.x * 0.75f, direction.y * 0.75f, direction.z * 0.75f, 0.0f);
        direction.mul(0.25f);
        direction.add((float) speed.x, (float) speed.y, (float) speed.z);
        FireResponse fireMessage = new FireResponse(position, direction);
        for (ServerPlayer player : ((ServerLevel) entity.level()).players()) {
            NetworkHandler.sendToPlayer(fireMessage, player);
        }

        // Play sound
        getEntity().playSound(getSound(), 1.0f, random.nextFloat() * 0.2f + 0.9f);
    }

    protected abstract Entity getBullet(Vector4f position, Vector3f direction);

    public SoundEvent getSound() {
        return SoundEvents.CROSSBOW_SHOOT;
    }

    protected boolean spentAmmoItems(Set<String> ammunition, int itemCount) {
        if (getEntity().isPilotCreative()) {
            return true;
        }

        if (getEntity() instanceof InventoryVehicleEntity vehicle) {
            // First check if we have enough ammo before consuming any
            int available = 0;
            for (int i = 0; i < vehicle.getInventory().getContainerSize(); i++) {
                ItemStack stack = vehicle.getInventory().getItem(i);
                String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (ammunition.contains(key) && !stack.isEmpty()) {
                    available += stack.getCount();
                }
            }

            if (available < itemCount) {
                if (getEntity().getControllingPassenger() instanceof Player player) {
                    player.displayClientMessage(Component.translatable("immersive_aircraft.out_of_ammo"), true);
                }
                return false;
            }

            // Now consume the ammo
            for (int spent = 0; spent < itemCount; spent++) {
                for (int i = 0; i < vehicle.getInventory().getContainerSize(); i++) {
                    ItemStack stack = vehicle.getInventory().getItem(i);
                    String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (ammunition.contains(key) && !stack.isEmpty()) {
                        stack.shrink(1);
                        break;
                    }
                }
            }
        } else {
            if (getEntity().getControllingPassenger() instanceof Player player) {
                player.displayClientMessage(Component.translatable("immersive_aircraft.out_of_ammo"), true);
            }
            return false;
        }

        return true;
    }
}
