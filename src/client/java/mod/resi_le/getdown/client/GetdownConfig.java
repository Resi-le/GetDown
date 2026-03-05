package mod.resi_le.getdown.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GetdownConfig {

    // ── 링 기본 설정 ──────────────────────────────────
    public static double guideRadius = 2.0;

    // ── 거리 기반 색상 ────────────────────────────────
    public static int    nearColor               = 0xFF3333;
    public static int    farColor                = 0x33CCFF;
    public static double colorTransitionDistance = 10.0;

    // ── Structure Void X 표시 ─────────────────────────
    public static boolean showVoidX = true;

    // ── 기능 1: 착지 데미지 HUD 표시 ─────────────────
    public static boolean showDamageHud = true;

    // ── 기능 6: 높이별 레이어 링 ─────────────────────
    public static boolean showLayerRings   = true;
    public static double  layerRingSpacing = 5.0;  // 링 간격 (블록)
    public static int     layerRingMax     = 4;    // 최대 레이어 수

    // ── 기능 7: 탈것별 정밀 물리 시뮬레이션 ──────────
    public static boolean useVehiclePhysics = true;

    // ── 사운드 / 파티클 ───────────────────────────────
    public static float      soundVolume = 1.0f;
    public static PopParticle popParticle = PopParticle.POOF;
    public static PopSound    popSound    = PopSound.ITEM_BREAK;

    private static final File CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("getdown.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ── Enum 정의 ─────────────────────────────────────
    public enum PopParticle {
        POOF(ParticleTypes.POOF),
        END_ROD(ParticleTypes.END_ROD),
        FLAME(ParticleTypes.FLAME),
        CLOUD(ParticleTypes.CLOUD),
        FIREWORK(ParticleTypes.FIREWORK);

        public final ParticleEffect type;
        PopParticle(ParticleEffect type) { this.type = type; }
    }

    public enum PopSound {
        ITEM_BREAK(SoundEvents.ENTITY_ITEM_BREAK.value()),
        GLASS_BREAK(SoundEvents.BLOCK_GLASS_BREAK),
        ANVIL_LAND(SoundEvents.BLOCK_ANVIL_LAND),
        EXPERIENCE_ORB(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
        MUTE(null);

        public final SoundEvent event;
        PopSound(SoundEvent event) { this.event = event; }
    }

    // ── 유틸 메서드 ───────────────────────────────────

    /**
     * remainingFall에 따라 farColor → nearColor 선형 보간 색상 반환.
     */
    public static int getLerpedColor(double remainingFall) {
        double t = Math.clamp(remainingFall / colorTransitionDistance, 0.0, 1.0);

        int fr = (farColor  >> 16) & 0xFF, fg = (farColor  >> 8) & 0xFF, fb = farColor  & 0xFF;
        int nr = (nearColor >> 16) & 0xFF, ng = (nearColor >> 8) & 0xFF, nb = nearColor & 0xFF;

        int r = (int)(nr + (fr - nr) * t);
        int g = (int)(ng + (fg - ng) * t);
        int b = (int)(nb + (fb - nb) * t);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * 낙하 높이(블록)로 예상 피해량(하트) 계산.
     * 마인크래프트 낙하 데미지: (높이 - 3) 하트, 최소 0
     * 크리에이티브/무적 등은 호출 전에 걸러야 함.
     */
    public static float calcFallDamage(double remainingFall) {
        // 마인크래프트 기본 공식: fallDistance - 3 (단위: 하트)
        float damage = (float)(remainingFall - 3.0);
        return Math.max(0f, damage);
    }

    // ── 저장 / 불러오기 ──────────────────────────────
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            ConfigData d = new ConfigData();
            d.guideRadius             = guideRadius;
            d.nearColor               = nearColor;
            d.farColor                = farColor;
            d.colorTransitionDistance = colorTransitionDistance;
            d.showVoidX               = showVoidX;
            d.showDamageHud           = showDamageHud;
            d.showLayerRings          = showLayerRings;
            d.layerRingSpacing        = layerRingSpacing;
            d.layerRingMax            = layerRingMax;
            d.useVehiclePhysics       = useVehiclePhysics;
            d.soundVolume             = soundVolume;
            d.popParticle             = popParticle;
            d.popSound                = popSound;
            GSON.toJson(d, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) { save(); return; }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData d = GSON.fromJson(reader, ConfigData.class);
            if (d != null) {
                guideRadius             = d.guideRadius;
                nearColor               = d.nearColor;
                farColor                = d.farColor;
                colorTransitionDistance = d.colorTransitionDistance;
                showVoidX               = d.showVoidX;
                showDamageHud           = d.showDamageHud;
                showLayerRings          = d.showLayerRings;
                layerRingSpacing        = d.layerRingSpacing;
                layerRingMax            = d.layerRingMax;
                useVehiclePhysics       = d.useVehiclePhysics;
                soundVolume             = d.soundVolume;
                popParticle             = d.popParticle;
                popSound                = d.popSound;
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static class ConfigData {
        double  guideRadius             = 2.0;
        int     nearColor               = 0xFF3333;
        int     farColor                = 0x33CCFF;
        double  colorTransitionDistance = 10.0;
        boolean showVoidX               = true;
        boolean showDamageHud           = true;
        boolean showLayerRings          = true;
        double  layerRingSpacing        = 5.0;
        int     layerRingMax            = 4;
        boolean useVehiclePhysics       = true;
        float   soundVolume             = 1.0f;
        PopParticle popParticle         = PopParticle.POOF;
        PopSound    popSound            = PopSound.ITEM_BREAK;
    }
}