package mod.resi_le.getdown.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class GetdownClient implements ClientModInitializer {

    private boolean wasFalling      = false;
    private Vec3d   lastLandingPos  = null;
    private boolean lastLandingIsVoid = false;

    private Entity  lastVehicle     = null;
    private Vec3d   lastVehiclePos  = null;

    // 기능 1: HUD에 표시할 데이터
    // hudDamage는 낙하 시작 시점에 한 번만 계산 → 착지 직전에도 값 유지
    private float   hudDamage       = 0f;
    private boolean hudVisible      = false;
    private double  fallStartY      = Double.NaN; // 낙하 시작 Y좌표

    // 기능 5: 탈것 이동 방향 (팝 이펙트 방향성에 사용)
    private Vec3d   lastVehicleVel  = Vec3d.ZERO;

    @Override
    public void onInitializeClient() {
        GetdownConfig.load();

        // ── 틱 루프 ──────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null) { hudVisible = false; return; }

            if (player.hasVehicle()) {
                Entity vehicle = player.getRootVehicle();
                Vec3d currentPos = vehicle.getPos();
                Vec3d realVel    = vehicle.getVelocity();

                if (lastVehicle == vehicle && lastVehiclePos != null) {
                    Vec3d diff = currentPos.subtract(lastVehiclePos);
                    if (realVel.lengthSquared() < 0.0001 && diff.lengthSquared() > 0.0001) {
                        realVel = diff;
                    }
                }

                lastVehicle    = vehicle;
                lastVehiclePos = currentPos;
                lastVehicleVel = realVel; // 기능 5용 저장

                boolean isFalling = realVel.y < -0.05;

                if (isFalling) {
                    LandingResult result = calculateLandingResult(vehicle, player.getWorld(), realVel);

                    if (result != null) {
                        double groundY       = result.pos.y - 0.1;
                        double remainingFall = vehicle.getPos().y - groundY;

                        if (remainingFall >= 1.0 || wasFalling) {
                            if (result.isVoid && GetdownConfig.showVoidX) {
                                renderVoidX(result.pos, player.getWorld());
                            } else {
                                // 기본 링
                                renderLandingGuide(result.pos, player.getWorld(), remainingFall);
                                // 기능 6: 레이어 링
                                if (GetdownConfig.showLayerRings) {
                                    renderLayerRings(result.pos, vehicle.getPos().y, player.getWorld(), remainingFall);
                                }
                            }

                            // 기능 1: HUD 데이터
                            // 낙하 시작 시점(wasFalling이 처음 true가 되는 틱)에만 계산하고 이후엔 고정
                            if (GetdownConfig.showDamageHud && !player.isCreative() && !player.isSpectator()) {
                                if (!wasFalling) {
                                    // 낙하 시작: 현재 Y를 시작점으로 기록
                                    fallStartY = vehicle.getPos().y;
                                }
                                // 총 낙하 거리 = 낙하 시작 Y - 착지 Y
                                double totalFall = fallStartY - (result.pos.y - 0.1);
                                hudDamage  = GetdownConfig.calcFallDamage(totalFall);
                                hudVisible = true;
                            } else {
                                hudVisible = false;
                            }

                            lastLandingPos    = result.pos;
                            lastLandingIsVoid = result.isVoid;
                            wasFalling        = true;
                        }
                    }
                } else {
                    if (wasFalling && lastLandingPos != null) {
                        renderPopEffect(lastLandingPos, player.getWorld(), player);
                    }
                    wasFalling        = false;
                    lastLandingPos    = null;
                    lastLandingIsVoid = false;
                    hudVisible        = false;
                    fallStartY        = Double.NaN;
                }
            } else {
                wasFalling        = false;
                lastLandingPos    = null;
                lastLandingIsVoid = false;
                lastVehicle       = null;
                lastVehiclePos    = null;
                hudVisible        = false;
                fallStartY        = Double.NaN;
            }
        });

        // ── 기능 1: HUD 렌더링 ───────────────────────
        HudRenderCallback.EVENT.register((drawContext, tickDeltaManager) -> {
            if (!hudVisible || !GetdownConfig.showDamageHud) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            TextRenderer tr     = client.textRenderer;
            int          sw     = client.getWindow().getScaledWidth();
            int          sh     = client.getWindow().getScaledHeight();

            // 색상: 데미지 없으면 초록, 치명적(10+)이면 빨강, 중간은 노랑
            int textColor;
            if (hudDamage <= 0) {
                textColor = 0x55FF55; // 초록
            } else if (hudDamage >= 10) {
                textColor = 0xFF5555; // 빨강
            } else {
                textColor = 0xFFAA00; // 노랑
            }

            String label;
            if (hudDamage <= 0) {
                label = "GOOD!";
            } else {
                // 하트 단위로 표시 (1하트 = 2HP)
                float hearts = hudDamage / 2f;
                label = String.format("Predicted: %.1f ❤", hearts);
            }

            int x = sw / 2 - tr.getWidth(label) / 2;
            int y = sh / 2 + 20; // 크로스헤어 약간 아래

            // 반투명 배경
            drawContext.fill(x - 3, y - 2, x + tr.getWidth(label) + 3, y + tr.fontHeight + 2, 0x88000000);
            drawContext.drawText(tr, label, x, y, textColor, true);
        });
    }

    // ─────────────────────────────────────────────────
    // 착지 결과 데이터 클래스
    // ─────────────────────────────────────────────────
    private static class LandingResult {
        final Vec3d   pos;
        final boolean isVoid;

        LandingResult(Vec3d pos, boolean isVoid) {
            this.pos    = pos;
            this.isVoid = isVoid;
        }
    }

    // ─────────────────────────────────────────────────
    // 기능 7: 탈것별 물리 상수
    // ─────────────────────────────────────────────────
    private static class VehiclePhysics {
        final double dragXZ;  // 수평 드래그
        final double dragY;   // 수직 드래그
        final double gravity; // 중력 가속도

        VehiclePhysics(double dragXZ, double dragY, double gravity) {
            this.dragXZ  = dragXZ;
            this.dragY   = dragY;
            this.gravity = gravity;
        }
    }

    private VehiclePhysics getPhysicsFor(Entity vehicle) {
        if (!GetdownConfig.useVehiclePhysics) {
            return new VehiclePhysics(0.91, 0.98, 0.04); // 기존 근사치
        }
        if (vehicle instanceof BoatEntity) {
            // 보트: 수평 드래그 낮음, 중력 작음 (물 위 설계)
            return new VehiclePhysics(0.90, 0.99, 0.03);
        } else if (vehicle instanceof MinecartEntity) {
            // 미니카트: 레일 마찰, 낙하 시 중력 표준
            return new VehiclePhysics(0.92, 0.98, 0.04);
        } else if (vehicle instanceof HorseEntity) {
            // 말: 점프 후 낙하, 수평 드래그 높음
            return new VehiclePhysics(0.89, 0.98, 0.08);
        } else {
            // 기타 (돼지, 라마 등): 표준
            return new VehiclePhysics(0.91, 0.98, 0.04);
        }
    }

    // ─────────────────────────────────────────────────
    // 착지 예측 (Structure Void 감지 + 탈것별 물리)
    // ─────────────────────────────────────────────────
    private LandingResult calculateLandingResult(Entity vehicle, World world, Vec3d startVel) {
        VehiclePhysics physics = getPhysicsFor(vehicle);

        Vec3d simPos = vehicle.getPos();
        Vec3d simVel = startVel;

        for (int i = 0; i < 200; i++) {
            Vec3d nextPos = simPos.add(simVel);

            BlockHitResult hit = world.raycast(new RaycastContext(
                    simPos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    vehicle
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3d    landPos  = hit.getPos().add(0, 0.1, 0);
                BlockPos blockPos = hit.getBlockPos();
                boolean  isVoid   = world.getBlockState(blockPos).isOf(Blocks.STRUCTURE_VOID);
                return new LandingResult(landPos, isVoid);
            }

            simPos = nextPos;
            // 탈것별 물리 상수 적용
            simVel = simVel
                    .multiply(physics.dragXZ, physics.dragY, physics.dragXZ)
                    .subtract(0, physics.gravity, 0);
        }
        return null;
    }

    // ─────────────────────────────────────────────────
    // 기본 착지 가이드 링 (거리 기반 색상)
    // ─────────────────────────────────────────────────
    private void renderLandingGuide(Vec3d landPos, World world, double remainingFall) {
        double radius      = GetdownConfig.guideRadius;
        int    lerpedColor = GetdownConfig.getLerpedColor(remainingFall);
        int    toColor     = GetdownConfig.nearColor;

        DustColorTransitionParticleEffect dust =
                new DustColorTransitionParticleEffect(lerpedColor, toColor, 1.0f);

        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * (360.0 / 20));
            world.addParticleClient(dust,
                    landPos.x + Math.cos(angle) * radius,
                    landPos.y,
                    landPos.z + Math.sin(angle) * radius,
                    0.0, 0.0, 0.0);
        }
    }

    // ─────────────────────────────────────────────────
    // 기능 6: 높이별 레이어 링
    // 착지 지점 위로 layerRingSpacing 블록 간격으로 반투명 링 추가
    // ─────────────────────────────────────────────────
    private void renderLayerRings(Vec3d landPos, double vehicleY, World world, double remainingFall) {
        double spacing = GetdownConfig.layerRingSpacing;
        int    maxRings = GetdownConfig.layerRingMax;
        double radius   = GetdownConfig.guideRadius * 0.75; // 레이어 링은 약간 작게

        for (int layer = 1; layer <= maxRings; layer++) {
            double ringY = landPos.y + spacing * layer;

            // 탈것 위치보다 높으면 그릴 필요 없음
            if (ringY >= vehicleY) break;

            // 높이에 따라 알파(투명도) 감소: 위로 갈수록 희미하게
            // DustColorTransition은 알파 조절 불가 → size를 줄여서 시각적으로 표현
            float size = Math.max(0.4f, 1.0f - (layer * 0.2f));

            // 레이어마다 높이 비율로 색상 보간
            double layerFall  = remainingFall - spacing * layer;
            int    layerColor = GetdownConfig.getLerpedColor(Math.max(0, layerFall));

            DustColorTransitionParticleEffect dust =
                    new DustColorTransitionParticleEffect(layerColor, GetdownConfig.farColor, size);

            int particleCount = 16 - layer * 2; // 위로 갈수록 파티클 수 감소
            particleCount = Math.max(8, particleCount);

            for (int i = 0; i < particleCount; i++) {
                double angle = Math.toRadians(i * (360.0 / particleCount));
                world.addParticleClient(dust,
                        landPos.x + Math.cos(angle) * radius,
                        ringY,
                        landPos.z + Math.sin(angle) * radius,
                        0.0, 0.0, 0.0);
            }
        }
    }

    // ─────────────────────────────────────────────────
    // Structure Void 경고 X 표시
    // ─────────────────────────────────────────────────
    private void renderVoidX(Vec3d landPos, World world) {
        double radius = GetdownConfig.guideRadius;
        int    steps  = 12;

        DustColorTransitionParticleEffect xDust =
                new DustColorTransitionParticleEffect(0xFF0000, 0xAA00FF, 1.2f);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double dx = (t * 2 - 1) * radius;
            world.addParticleClient(xDust, landPos.x + dx, landPos.y, landPos.z + dx,  0.0, 0.0, 0.0);
            world.addParticleClient(xDust, landPos.x + dx, landPos.y, landPos.z - dx,  0.0, 0.0, 0.0);
        }
    }

    // ─────────────────────────────────────────────────
    // 기능 5: 착지 팝 이펙트 (방향성 적용)
    // 탈것이 이동하던 수평 방향으로 파티클이 퍼짐
    // ─────────────────────────────────────────────────
    private void renderPopEffect(Vec3d pos, World world, ClientPlayerEntity player) {
        if (GetdownConfig.popSound.event != null && GetdownConfig.soundVolume > 0) {
            world.playSound(player, pos.x, pos.y, pos.z,
                    GetdownConfig.popSound.event, SoundCategory.PLAYERS,
                    GetdownConfig.soundVolume, 1.0f);
        }

        // 착지 직전 수평 이동 방향 벡터 (정규화)
        Vec3d hVel   = new Vec3d(lastVehicleVel.x, 0, lastVehicleVel.z);
        double speed = hVel.length();

        // 수평 속도가 충분하면 방향성 팝, 아니면 기존 랜덤 팝
        boolean directional = speed > 0.05;
        Vec3d   dir         = directional ? hVel.normalize() : Vec3d.ZERO;

        for (int i = 0; i < 30; i++) {
            double spread = 0.3;
            double vx, vy, vz;

            if (directional) {
                // 이동 방향 기준으로 ±spread 퍼짐 + 위쪽 분산
                vx = dir.x * (0.2 + Math.random() * 0.3) + (Math.random() - 0.5) * spread;
                vy = Math.random() * 0.5;
                vz = dir.z * (0.2 + Math.random() * 0.3) + (Math.random() - 0.5) * spread;
            } else {
                // 기존 랜덤
                vx = (Math.random() - 0.5) * 0.5;
                vy = Math.random() * 0.5;
                vz = (Math.random() - 0.5) * 0.5;
            }

            world.addParticleClient(GetdownConfig.popParticle.type,
                    pos.x, pos.y, pos.z, vx, vy, vz);
        }
    }
}