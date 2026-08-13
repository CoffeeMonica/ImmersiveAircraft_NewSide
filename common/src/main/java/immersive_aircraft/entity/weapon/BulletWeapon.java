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
        // Calculate the position of the barrel
        Vector4f position = getBarrelOffset();
        VehicleEntity entity = getEntity();
        position.mul(getTransform());
        position.mul(entity.getVehicleTransform());

        Vec3 speed = entity.getSpeedVector();

        // Offset the position by the barrel length
        float barrelLength = getBarrelLength();
        position.add(direction.x() * barrelLength, direction.y() * barrelLength, direction.z() * barrelLength, 0.0f);

        // Spawn bullets
        for (int i = 0; i < getBulletCount(); i++) {
            Entity bullet = getBullet(position, direction);
            bullet.setDeltaMovement(bullet.getDeltaMovement().add(speed));
            entity.level().addFreshEntity(bullet);
        }

        // Fire-particle
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
