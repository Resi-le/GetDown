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
    // 저장될 데이터 구조
    public static double guideRadius = 2.0;
    public static int particleColor = 0x33CCFF;
    public static float soundVolume = 1.0f;
    public static PopParticle popParticle = PopParticle.POOF;
    public static PopSound popSound = PopSound.ITEM_BREAK;

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("getdown.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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

    // 파일 저장 로직
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            ConfigData data = new ConfigData();
            data.guideRadius = guideRadius;
            data.particleColor = particleColor;
            data.soundVolume = soundVolume;
            data.popParticle = popParticle;
            data.popSound = popSound;
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 파일 불러오기 로직
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                guideRadius = data.guideRadius;
                particleColor = data.particleColor;
                soundVolume = data.soundVolume;
                popParticle = data.popParticle;
                popSound = data.popSound;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        double guideRadius;
        int particleColor;
        float soundVolume;
        PopParticle popParticle;
        PopSound popSound;
    }
}