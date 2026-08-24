package immersive_aircraft.mixin.client;

import immersive_aircraft.client.RadarHandler;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes the vanilla glowing decision through the radar:
 * - targets detected by the local radar always appear glowing (their passengers too);
 * - own aircraft and its passengers never glow, even with a real glowing effect applied.
 * The geometry of aircraft themselves is drawn through a custom buffer source, so the
 * VehicleEntityRenderer additionally renders radar targets via the outline buffer.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void immersiveAircraft$radarGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!RadarHandler.isActive()) {
            return;
        }
        if (RadarHandler.isTarget(entity)) {
            cir.setReturnValue(true);
        } else if (entity instanceof VehicleEntity && RadarHandler.isOwnCrew(entity)) {
            cir.setReturnValue(false);
        }
    }
}
