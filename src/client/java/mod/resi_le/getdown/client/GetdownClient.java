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

    // 실제 속도 계산을 위해 이전 틱의 탈것과 위치를 저장하는 변수
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

                // 1. 클라이언트가 인식하는 기본 속도를 가져옵니다.
                Vec3d realVel = vehicle.getVelocity();

                // 2. 서버 플러그인 등으로 강제 텔레포트되는 탈것(대구 등)을 위한 실제 속도(Delta) 보정
                if (lastVehicle == vehicle && lastVehiclePos != null) {
                    Vec3d diff = currentPos.subtract(lastVehiclePos);
                    // 엔진 속도는 0에 가까운데 실제 위치가 변하고 있다면 실제 변화량을 속도로 사용
                    if (realVel.lengthSquared() < 0.0001 && diff.lengthSquared() > 0.0001) {
                        realVel = diff;
                    }
                }

                // 다음 틱을 위해 현재 상태 저장
                lastVehicle = vehicle;
                lastVehiclePos = currentPos;

                // 3. 고장나기 쉬운 isOnGround() 조건을 제거하고, 실제 Y축 하락 속도만으로 판정
                boolean isFalling = realVel.y < -0.05;

                if (isFalling) {
                    // 계산식에 보정된 실제 속도(realVel)를 넘겨줍니다.
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

    // startVel(시작 속도) 매개변수 추가됨
    private Vec3d calculateLandingPos(Entity vehicle, World world, Vec3d startVel) {
        Vec3d simPos = vehicle.getPos();
        Vec3d simVel = startVel; // 보정된 실제 속도를 기반으로 시뮬레이션 시작

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
        // 설정된 가이드 반경 가져오기
        double radius = GetdownConfig.guideRadius;

        // 시작 색상은 설정에서 가져온 int 값을 그대로 사용합니다.
        int fromColor = GetdownConfig.particleColor;

        // 끝 색상은 기존과 비슷한 보라색 톤(0xCC33FF)으로 설정합니다.
        int toColor = 0xCC33FF;

        // 최신 버전용 생성자: (시작 색상 int, 끝 색상 int, 크기 float)
        DustColorTransitionParticleEffect gradientDust = new DustColorTransitionParticleEffect(fromColor, toColor, 1.0f);

        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * (360.0 / 20));
            double x = landPos.x + Math.cos(angle) * radius;
            double z = landPos.z + Math.sin(angle) * radius;

            world.addParticleClient(gradientDust, x, landPos.y, z, 0.0, 0.0, 0.0);
        }
    }

    private void renderPopEffect(Vec3d pos, World world, ClientPlayerEntity player) {
        // 1. 소리 재생 (MUTE가 아닐 때만, 설정된 음량으로)
        if (GetdownConfig.popSound.event != null && GetdownConfig.soundVolume > 0) {
            world.playSound(player, pos.x, pos.y, pos.z, GetdownConfig.popSound.event, SoundCategory.PLAYERS, GetdownConfig.soundVolume, 1.0f);
        }

        // 2. 설정된 파티클 생성
        for (int i = 0; i < 30; i++) {
            double velocityX = (Math.random() - 0.5) * 0.5;
            double velocityY = Math.random() * 0.5;
            double velocityZ = (Math.random() - 0.5) * 0.5;

            world.addParticleClient(GetdownConfig.popParticle.type, pos.x, pos.y, pos.z, velocityX, velocityY, velocityZ);
        }
    }
}