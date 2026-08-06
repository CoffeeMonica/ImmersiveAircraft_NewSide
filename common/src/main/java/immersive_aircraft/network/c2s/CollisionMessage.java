package immersive_aircraft.network.c2s;

import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.config.Config;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.item.upgrade.VehicleStat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class CollisionMessage extends Message {
    public static final StreamCodec<RegistryFriendlyByteBuf, CollisionMessage> STREAM_CODEC = StreamCodec.ofMember(CollisionMessage::encode, CollisionMessage::new);
    public static final CustomPacketPayload.Type<CollisionMessage> TYPE = Message.createType("collision");

    public CustomPacketPayload.Type<CollisionMessage> type() {
        return TYPE;
    }

    private final float damage;

    public CollisionMessage(float damage) {
        this.damage = damage;
    }

    public CollisionMessage(RegistryFriendlyByteBuf b) {
        damage = b.readFloat();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf b) {
        b.writeFloat(damage);
    }

    @Override
    public void receiveServer(ServerPlayer e) {
        if (e.getRootVehicle() instanceof VehicleEntity vehicle && vehicle.hasPassenger(e) && Config.getInstance().collisionDamage) {
            // Speed damage is 1.5x stronger, mass multiplies the final damage
            float mass = vehicle.getVehicleData().getProperties().getOrDefault(VehicleStat.MASS, 1.0f);
            float appliedDamage = damage * 1.5f * Config.getInstance().collisionDamageMultiplier * Math.max(1, mass / 2);
            vehicle.hurt(e.level().damageSources().fall(), appliedDamage);
            if (vehicle.isRemoved()) {
                float crashDamage = appliedDamage * Config.getInstance().crashDamage;
                boolean hasTntBundle = vehicle.hasUpgrade(immersive_aircraft.Items.TNT_BUNDLE.get());
                if (Config.getInstance().preventKillThroughCrash && !hasTntBundle) {
                    crashDamage = Math.min(crashDamage, e.getHealth() - 1.0f);
                }
                e.hurt(e.level().damageSources().fall(), crashDamage);
            }
        }
    }
}
