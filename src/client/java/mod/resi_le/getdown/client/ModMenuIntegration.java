package mod.resi_le.getdown.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Get Down! Setting"));

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
            ConfigCategory effect = builder.getOrCreateCategory(Text.literal("Effect"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // --- 일반 설정 탭 ---
            general.addEntry(entryBuilder.startDoubleField(Text.literal("Landing Ring Size"), GetdownConfig.guideRadius)
                    .setDefaultValue(2.0).setMin(0.5).setMax(5.0)
                    .setSaveConsumer(newValue -> GetdownConfig.guideRadius = newValue)
                    .build());

            // --- 이펙트 설정 탭 ---
            effect.addEntry(entryBuilder.startColorField(Text.literal("Landing Ring Color"), GetdownConfig.particleColor)
                    .setDefaultValue(0x33CCFF)
                    .setSaveConsumer(newValue -> GetdownConfig.particleColor = newValue)
                    .build());

            effect.addEntry(entryBuilder.startEnumSelector(Text.literal("Landing Particle"), GetdownConfig.PopParticle.class, GetdownConfig.popParticle)
                    .setDefaultValue(GetdownConfig.PopParticle.POOF)
                    .setSaveConsumer(newValue -> GetdownConfig.popParticle = newValue)
                    .build());

            effect.addEntry(entryBuilder.startEnumSelector(Text.literal("Landing SFX"), GetdownConfig.PopSound.class, GetdownConfig.popSound)
                    .setDefaultValue(GetdownConfig.PopSound.ITEM_BREAK)
                    .setSaveConsumer(newValue -> GetdownConfig.popSound = newValue)
                    .build());

            effect.addEntry(entryBuilder.startIntSlider(Text.literal("SFX Volume (%)"), (int)(GetdownConfig.soundVolume * 100), 0, 100)
                    .setDefaultValue(100)
                    .setSaveConsumer(newValue -> GetdownConfig.soundVolume = newValue / 100f)
                    .build());

            builder.setSavingRunnable(() -> {
                GetdownConfig.save();
            });

            return builder.build();
        };
    }
}