package immersive_aircraft.entity.weapon;

import immersive_aircraft.Entities;
import immersive_aircraft.Main;
import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.config.Config;
import immersive_aircraft.entity.ImprovedUavEntity;
import immersive_aircraft.entity.InventoryVehicleEntity;
import immersive_aircraft.entity.UavEntity;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.entity.misc.WeaponMount;
import immersive_aircraft.network.c2s.FireMessage;
import immersive_aircraft.Items;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class BombBay extends BulletWeapon {
    private float cooldown = 0.0f;
    private boolean spawnUav = false;
    private boolean spawnImprovedUav = false;

    public BombBay(VehicleEntity entity, ItemStack stack, WeaponMount mount, int slot) {
        super(entity, stack, mount, slot);
    }

    @Override
    protected float getBarrelLength() {
        return 0.25f;
    }

    @Override
    protected Vector4f getBarrelOffset() {
        return new Vector4f(0.0f, -0.8f, 0.0f, 1.0f);
    }

    public float getVelocity() {
        return 0.0f;
    }

    private boolean hasUavInInventory() {
        if (!(getEntity() instanceof InventoryVehicleEntity vehicle)) {
            return false;
        }
        for (int i = 0; i < vehicle.getInventory().getContainerSize(); i++) {
            ItemStack stack = vehicle.getInventory().getItem(i);
            if (stack.getItem() == Items.UAV.get() || stack.getItem() == Items.IMPROVED_UAV.get()) {
                return true;
            }
        }
        return false;
    }

    private boolean isFirstUavImproved() {
        if (!(getEntity() instanceof InventoryVehicleEntity vehicle)) {
            return false;
        }
        for (int i = 0; i < vehicle.getInventory().getContainerSize(); i++) {
            ItemStack stack = vehicle.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.IMPROVED_UAV.get()) {
                return true;
            }
            if (stack.getItem() == Items.UAV.get()) {
                return false;
            }
            String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (Config.getInstance().bombBayAmmunition.contains(key)) {
                return false;
            }
        }
        return false;
    }

    private boolean spentUav() {
        if (getEntity().isPilotCreative()) {
            return true;
        }
        if (!(getEntity() instanceof InventoryVehicleEntity vehicle)) {
            return false;
        }
        for (int i = 0; i < vehicle.getInventory().getContainerSize(); i++) {
            ItemStack stack = vehicle.getInventory().getItem(i);
            if (stack.getItem() == Items.UAV.get() || stack.getItem() == Items.IMPROVED_UAV.get()) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private boolean hasTntInInventory() {
        if (!(getEntity() instanceof InventoryVehicleEntity vehicle)) {
            return false;
        }
        for (int i = 0; i < vehicle.getInventory().getContainerSize(); i++) {
            ItemStack stack = vehicle.getInventory().getItem(i);
            String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (Config.getInstance().bombBayAmmunition.contains(key) && !stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean spentTnt() {
        if (getEntity().isPilotCreative()) {
            return true;
        }
        if (!(getEntity() instanceof InventoryVehicleEntity vehicle)) {
            return false;
        }
        for (int i = 0; i < vehicle.getInventory().getContainerSize(); i++) {
            ItemStack stack = vehicle.getInventory().getItem(i);
            String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (Config.getInstance().bombBayAmmunition.contains(key) && !stack.isEmpty()) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    // Returns true if the first valid ammo in inventory is UAV, false if it's TNT
    private boolean isFirstAmmoUav() {
        if (!(getEntity() instanceof InventoryVehicleEntity vehicle)) {
            return false;
        }
        for (int i = 0; i < vehicle.getInventory().getContainerSize(); i++) {
            ItemStack stack = vehicle.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.UAV.get() || stack.getItem() == Items.IMPROVED_UAV.get()) {
                return true;
            }
            String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (Config.getInstance().bombBayAmmunition.contains(key)) {
                return false;
            }
        }
        return false;
    }

    @Override
    protected Entity getBullet(Vector4f position, Vector3f direction) {
        VehicleEntity entity = getEntity();

        if (spawnUav) {
            if (spawnImprovedUav) {
                ImprovedUavEntity uav = new ImprovedUavEntity(Entities.IMPROVED_UAV.get(), entity.level());
                uav.setYRot(entity.getYRot());
                uav.setXRot(entity.getXRot());
                uav.setZRot(entity.getRoll());
                uav.setOwner(entity);
                uav.setPos(position.x(), position.y() - 0.5, position.z());
                uav.setDeltaMovement(direction.x(), direction.y() * 0.5, direction.z());
                return uav;
            } else {
                UavEntity uav = new UavEntity(Entities.UAV.get(), entity.level());
                uav.setYRot(entity.getYRot());
                uav.setXRot(entity.getXRot());
                uav.setZRot(entity.getRoll());
                uav.setOwner(entity);
                uav.setPos(position.x(), position.y() - 0.5, position.z());
                uav.setDeltaMovement(direction.x(), direction.y() * 0.5, direction.z());
                return uav;
            }
        } else {
            // Spawn tiny TNT - don't add aircraft velocity, just drop downward
            Entity tnt = Entities.TINY_TNT.get().create(entity.level(), EntitySpawnReason.TRIGGERED);
            if (tnt != null) {
                tnt.setPos(position.x(), position.y() - 0.5, position.z());
                // Only use the direction (which is downward), don't add aircraft speed
                tnt.setDeltaMovement(0, -0.1, 0);
            }
            return tnt;
        }
    }

    @Override
    public void fire(Vector3f direction) {
        VehicleEntity entity = getEntity();
        if (entity.isPilotCreative()) {
            spawnUav = hasUavInInventory();
            spawnImprovedUav = isFirstUavImproved();
            super.fire(direction);
            return;
        }

        // Use the first valid ammunition found in inventory (UAV or TNT)
        if (isFirstAmmoUav()) {
            spawnImprovedUav = isFirstUavImproved();
            if (!spentUav()) {
                spawnUav = false;
                return;
            }
            spawnUav = true;
            super.fire(direction);
        } else if (hasTntInInventory()) {
            spawnUav = false;
            spawnImprovedUav = false;
            if (spentTnt()) {
                super.fire(direction);
            }
        } else {
            spawnUav = false;
            spawnImprovedUav = false;
            if (spentAmmoItems(Config.getInstance().bombBayAmmunition, 1)) {
                super.fire(direction);
            }
        }
    }

    @Override
    public void tick() {
        cooldown -= 1.0f / 20.0f;
    }

    @Override
    public void clientFire(int index) {
        if (cooldown <= 0.0f) {
            cooldown = Config.getInstance().bombBayCooldown;
            NetworkHandler.sendToServer(new FireMessage(getSlot(), index, getDirection()));
        }
    }

    private Vector3f getDirection() {
        Vector3f direction = new Vector3f(0, 1.0f, 0);
        direction.mul(new Matrix3f(getMount().transform()));
        direction.mul(getEntity().getVehicleNormalTransform());
        return direction;
    }
}