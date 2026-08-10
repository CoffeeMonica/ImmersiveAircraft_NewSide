package immersive_aircraft.entity.weapon;

import immersive_aircraft.Entities;
import immersive_aircraft.Main;
import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.config.Config;
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

import java.util.Map;

public class BombBay extends BulletWeapon {
    private static final float MAX_COOLDOWN = 1.0f;
    private float cooldown = 0.0f;
    private boolean spawnUav = false;

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
            if (stack.getItem() == Items.UAV.get()) {
                return true;
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
            if (stack.getItem() == Items.UAV.get()) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    protected Entity getBullet(Vector4f position, Vector3f direction) {
        VehicleEntity entity = getEntity();

        if (spawnUav) {
            UavEntity uav = new UavEntity(Entities.UAV.get(), entity.level());
            uav.setYRot(entity.getYRot());
            uav.setXRot(entity.getXRot());
            uav.setZRot(entity.getRoll());
            uav.setOwner(entity);
            uav.setPos(position.x(), position.y() - 0.5, position.z());
            uav.setDeltaMovement(direction.x(), direction.y() * 0.5, direction.z());
            return uav;
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
            super.fire(direction);
            return;
        }

        if (hasUavInInventory()) {
            if (!spentUav()) {
                spawnUav = false;
                return;
            }
            spawnUav = true;
            super.fire(direction);
        } else {
            spawnUav = false;
            if (spentAmmo(Config.getInstance().bombBayAmmunition, 20)) {
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
            cooldown = MAX_COOLDOWN;
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