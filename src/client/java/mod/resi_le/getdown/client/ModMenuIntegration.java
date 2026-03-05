package mod.resi_le.getdown.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    // 모든 번역 키에 "getdown." 네임스페이스를 자동으로 붙여주는 헬퍼
    private static Text t(String key) {
        return Text.translatable("getdown." + key);
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder    builder     = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(t("config.title"));
            ConfigEntryBuilder eb = builder.entryBuilder();

            // ═══════════════════════════════════════════
            // General
            // ═══════════════════════════════════════════
            ConfigCategory general = builder.getOrCreateCategory(t("config.category.general"));

            general.addEntry(eb.startDoubleField(t("config.general.guide_radius"), GetdownConfig.guideRadius)
                    .setDefaultValue(2.0).setMin(0.5).setMax(5.0)
                    .setTooltip(t("config.general.guide_radius.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.guideRadius = v)
                    .build());

            general.addEntry(eb.startBooleanToggle(t("config.general.show_void_x"), GetdownConfig.showVoidX)
                    .setDefaultValue(true)
                    .setTooltip(t("config.general.show_void_x.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.showVoidX = v)
                    .build());

            // ═══════════════════════════════════════════
            // Color
            // ═══════════════════════════════════════════
            ConfigCategory color = builder.getOrCreateCategory(t("config.category.color"));

            color.addEntry(eb.startColorField(t("config.color.far_color"), GetdownConfig.farColor)
                    .setDefaultValue(0x33CCFF)
                    .setTooltip(t("config.color.far_color.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.farColor = v)
                    .build());

            color.addEntry(eb.startColorField(t("config.color.near_color"), GetdownConfig.nearColor)
                    .setDefaultValue(0xFF3333)
                    .setTooltip(t("config.color.near_color.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.nearColor = v)
                    .build());

            color.addEntry(eb.startDoubleField(t("config.color.transition_distance"), GetdownConfig.colorTransitionDistance)
                    .setDefaultValue(10.0).setMin(1.0).setMax(50.0)
                    .setTooltip(t("config.color.transition_distance.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.colorTransitionDistance = v)
                    .build());

            // ═══════════════════════════════════════════
            // HUD
            // ═══════════════════════════════════════════
            ConfigCategory hud = builder.getOrCreateCategory(t("config.category.hud"));

            hud.addEntry(eb.startBooleanToggle(t("config.hud.show_damage"), GetdownConfig.showDamageHud)
                    .setDefaultValue(true)
                    .setTooltip(t("config.hud.show_damage.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.showDamageHud = v)
                    .build());

            // ═══════════════════════════════════════════
            // Layer Rings
            // ═══════════════════════════════════════════
            ConfigCategory rings = builder.getOrCreateCategory(t("config.category.layer_rings"));

            rings.addEntry(eb.startBooleanToggle(t("config.rings.show"), GetdownConfig.showLayerRings)
                    .setDefaultValue(true)
                    .setTooltip(t("config.rings.show.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.showLayerRings = v)
                    .build());

            rings.addEntry(eb.startDoubleField(t("config.rings.spacing"), GetdownConfig.layerRingSpacing)
                    .setDefaultValue(5.0).setMin(1.0).setMax(20.0)
                    .setTooltip(t("config.rings.spacing.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.layerRingSpacing = v)
                    .build());

            rings.addEntry(eb.startIntSlider(t("config.rings.max_count"), GetdownConfig.layerRingMax, 1, 8)
                    .setDefaultValue(4)
                    .setTooltip(t("config.rings.max_count.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.layerRingMax = v)
                    .build());

            // ═══════════════════════════════════════════
            // Physics
            // ═══════════════════════════════════════════
            ConfigCategory physics = builder.getOrCreateCategory(t("config.category.physics"));

            physics.addEntry(eb.startBooleanToggle(t("config.physics.vehicle_physics"), GetdownConfig.useVehiclePhysics)
                    .setDefaultValue(true)
                    .setTooltip(t("config.physics.vehicle_physics.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.useVehiclePhysics = v)
                    .build());

            // ═══════════════════════════════════════════
            // Effect
            // ═══════════════════════════════════════════
            ConfigCategory effect = builder.getOrCreateCategory(t("config.category.effect"));

            effect.addEntry(eb.startEnumSelector(t("config.effect.particle"), GetdownConfig.PopParticle.class, GetdownConfig.popParticle)
                    .setDefaultValue(GetdownConfig.PopParticle.POOF)
                    .setTooltip(t("config.effect.particle.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.popParticle = v)
                    .build());

            effect.addEntry(eb.startEnumSelector(t("config.effect.sound"), GetdownConfig.PopSound.class, GetdownConfig.popSound)
                    .setDefaultValue(GetdownConfig.PopSound.ITEM_BREAK)
                    .setTooltip(t("config.effect.sound.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.popSound = v)
                    .build());

            effect.addEntry(eb.startIntSlider(t("config.effect.volume"), (int)(GetdownConfig.soundVolume * 100), 0, 100)
                    .setDefaultValue(100)
                    .setTooltip(t("config.effect.volume.tooltip"))
                    .setSaveConsumer(v -> GetdownConfig.soundVolume = v / 100f)
                    .build());

            builder.setSavingRunnable(GetdownConfig::save);
            return builder.build();
        };
    }
}