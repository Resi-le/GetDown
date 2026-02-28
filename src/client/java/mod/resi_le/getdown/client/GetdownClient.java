package mod.resi_le.getdown.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class GetdownClient implements ClientModInitializer {

    private boolean wasFalling = false;
    private Vec3d lastLandingPos = null;
    private Entity lastVehicle = null;
    private Vec3d lastVehiclePos = null;

    @Override
    public void onInitializeClient() {
        GetdownConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            if (player.hasVehicle()) {
                Entity vehicle = player.getRootVehicle();
                Vec3d currentPos = vehicle.getPos();

                Vec3d realVel = vehicle.getVelocity();

                if (lastVehicle == vehicle && lastVehiclePos != null) {
                    Vec3d diff = currentPos.subtract(lastVehiclePos);
                    if (realVel.lengthSquared() < 0.0001 && diff.lengthSquared() > 0.0001) {
                        realVel = diff;
                    }
                }

                lastVehicle = vehicle;
                lastVehiclePos = currentPos;

                boolean isFalling = realVel.y < -0.05;

                if (isFalling) {
                    Vec3d landingPos = calculateLandingPos(vehicle, player.getWorld(), realVel);

                    if (landingPos != null) {
                        double groundY = landingPos.y - 0.1;
                        double remainingFall = vehicle.getPos().y - groundY;

                        if (remainingFall >= 1.0 || wasFalling) {
                            renderLandingGuide(landingPos, player.getWorld());
                            lastLandingPos = landingPos;
                            wasFalling = true;
                        }
                    }
                } else {
                    if (wasFalling && lastLandingPos != null) {
                        renderPopEffect(lastLandingPos, player.getWorld(), player);
                    }
                    wasFalling = false;
                    lastLandingPos = null;
                }
            } else {
                wasFalling = false;
                lastLandingPos = null;
                lastVehicle = null;
                lastVehiclePos = null;
            }
        });
    }

    private Vec3d calculateLandingPos(Entity vehicle, World world, Vec3d startVel) {
        Vec3d simPos = vehicle.getPos();
        Vec3d simVel = startVel;

        for (int i = 0; i < 200; i++) {
            Vec3d nextPos = simPos.add(simVel);

            BlockHitResult hit = world.raycast(new RaycastContext(
                    simPos,
                    nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    vehicle
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit.getPos().add(0, 0.1, 0);
            }

            simPos = nextPos;
            simVel = simVel.multiply(0.91, 0.98, 0.91).subtract(0, 0.04, 0);
        }
        return null;
    }

    private void renderLandingGuide(Vec3d landPos, World world) {
        double radius = GetdownConfig.guideRadius;
        int fromColor = GetdownConfig.particleColor;
        int toColor = 0xCC33FF;

        DustColorTransitionParticleEffect gradientDust = new DustColorTransitionParticleEffect(fromColor, toColor, 1.0f);

        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * (360.0 / 20));
            double x = landPos.x + Math.cos(angle) * radius;
            double z = landPos.z + Math.sin(angle) * radius;

            world.addParticleClient(gradientDust, x, landPos.y, z, 0.0, 0.0, 0.0);
        }
    }

    private void renderPopEffect(Vec3d pos, World world, ClientPlayerEntity player) {
        if (GetdownConfig.popSound.event != null && GetdownConfig.soundVolume > 0) {
            world.playSound(player, pos.x, pos.y, pos.z, GetdownConfig.popSound.event, SoundCategory.PLAYERS, GetdownConfig.soundVolume, 1.0f);
        }

        for (int i = 0; i < 30; i++) {
            double velocityX = (Math.random() - 0.5) * 0.5;
            double velocityY = Math.random() * 0.5;
            double velocityZ = (Math.random() - 0.5) * 0.5;

            world.addParticleClient(GetdownConfig.popParticle.type, pos.x, pos.y, pos.z, velocityX, velocityY, velocityZ);
        }
    }
}