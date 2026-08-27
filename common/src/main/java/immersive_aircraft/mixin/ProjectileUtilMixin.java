package immersive_aircraft.mixin;

import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {
    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("RETURN"), cancellable = true)
    private static void ia$getEntityHitResult(Entity shooter, Vec3 startVec, Vec3 endVec, AABB boundingBox, Predicate<Entity> filter, double distance, CallbackInfoReturnable<EntityHitResult> cir) {
        ia$vehicleTrace(cir.getReturnValue(), shooter.level(), shooter, startVec, endVec, boundingBox,filter, 0.0f, distance).ifPresent(cir::setReturnValue);
    }

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("RETURN"), cancellable = true)
    private static void ia$getEntityHitResult(Level level, Projectile projectile, Vec3 startVec, Vec3 endVec, AABB boundingBox, Predicate<Entity> filter, CallbackInfoReturnable<EntityHitResult> cir) {
        ia$vehicleTrace(cir.getReturnValue(), level, projectile, startVec, endVec, boundingBox, filter, 0.0f, Double.MAX_VALUE).ifPresent(cir::setReturnValue);
    }

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("RETURN"), cancellable = true)
    private static void ia$getEntityHitResult(Level level, Entity projectile, Vec3 startVec, Vec3 endVec, AABB boundingBox, Predicate<Entity> filter, float inflationAmount, CallbackInfoReturnable<EntityHitResult> cir) {
        ia$vehicleTrace(cir.getReturnValue(), level, projectile, startVec, endVec, boundingBox, filter, inflationAmount, Double.MAX_VALUE).ifPresent(cir::setReturnValue);
    }

    @Inject(method = "getManyEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Z)Ljava/util/Collection;", at = @At("RETURN"), cancellable = true)
    private static void ia$getManyEntityHitResult(Level level, Entity projectile, Vec3 startVec, Vec3 endVec, AABB boundingBox, Predicate<Entity> filter, boolean flag, CallbackInfoReturnable<Collection<EntityHitResult>> cir) {
        // 1.21.11 arrows (and tridents) scan entities through this multi-hit overload, which
        // only tests each entity's MAIN bounding box - additional vehicle hit boxes were
        // silently skipped. Trace the shapes and append the missing vehicle hit.
        Optional<EntityHitResult> optional = ia$vehicleTrace(null, level, projectile, startVec, endVec, boundingBox, filter, 0.0f, Double.MAX_VALUE);
        if (optional.isEmpty()) {
            return;
        }
        EntityHitResult vehicleHit = optional.get();
        Collection<EntityHitResult> current = cir.getReturnValue();
        // If vanilla already found this vehicle through its main box, leave the result untouched.
        for (EntityHitResult existing : current) {
            if (existing.getEntity() == vehicleHit.getEntity()) {
                return;
            }
        }
        List<EntityHitResult> result = new ArrayList<>(current);
        result.add(vehicleHit);
        cir.setReturnValue(result);
    }

    @Unique
    private static Optional<EntityHitResult> ia$vehicleTrace(@Nullable EntityHitResult previous, Level level, @Nullable Entity source, Vec3 startVec, Vec3 endVec, AABB boundingBox, Predicate<Entity> filter, float inflationAmount, double distance) {
        // Use squared distance for comparison, since distanceToSqr returns squared distance
        double bestDistance = previous == null ? Double.MAX_VALUE : previous.getLocation().distanceToSqr(startVec);
        Entity entity = null;
        Vec3 collision = null;

        // Apply the filter to properly exclude entities that should not be considered
        // as hit targets (e.g. the shooter or the shooter's own vehicle).
        // Without this, the mixin would find hits on the shooter's own vehicle,
        // which are then blocked by BulletEntity.canHitEntity(), resulting in
        // no damage being applied at all.
        Predicate<Entity> combinedFilter = e -> e != source && VehicleEntity.class.isInstance(e) && (filter == null || filter.test(e));

        for (Entity e : level.getEntities(source, boundingBox.inflate(16.0), combinedFilter)) {
            if (e instanceof VehicleEntity vehicle && vehicle.isPickable() && !vehicle.isRemoved()) {
                if (source instanceof Projectile projectile) {
                    Entity owner = projectile.getOwner();
                    if (owner != null && (owner == vehicle || owner == vehicle.getVehicle() || owner.isPassengerOfSameVehicle(vehicle))) {
                        continue;
                    }
                }

                // Exact oriented-box test: main entity box (vanilla-style volume) plus the
                // detailed boxes clipped in the vehicle's LOCAL space, so wings/tail/balloon
                // are hit precisely at any orientation without envelope inflation.
                Optional<Vec3> mainHit = vehicle.getBoundingBox().inflate(inflationAmount).clip(startVec, endVec);
                if (mainHit.isPresent()) {
                    Vec3 newCollision = mainHit.get();
                    double dist = startVec.distanceToSqr(newCollision);
                    if (dist < bestDistance) {
                        entity = vehicle;
                        collision = newCollision;
                        bestDistance = dist;
                    }
                }
                Optional<Vec3> detailedHit = vehicle.clipDetailed(startVec, endVec, inflationAmount);
                if (detailedHit.isPresent()) {
                    Vec3 newCollision = detailedHit.get();
                    double dist = startVec.distanceToSqr(newCollision);
                    if (dist < bestDistance) {
                        entity = vehicle;
                        collision = newCollision;
                        bestDistance = dist;
                    }
                }
            }
        }

        return entity == null ? Optional.empty() : Optional.of(new EntityHitResult(entity, collision));
    }
}
