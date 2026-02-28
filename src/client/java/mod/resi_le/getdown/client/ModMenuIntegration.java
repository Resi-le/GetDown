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
                    .setTitle(Text.literal("Get Down! 설정"));

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("일반"));
            ConfigCategory effect = builder.getOrCreateCategory(Text.literal("효과"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // --- 일반 설정 탭 ---
            general.addEntry(entryBuilder.startDoubleField(Text.literal("가이드 반경"), GetdownConfig.guideRadius)
                    .setDefaultValue(2.0).setMin(0.5).setMax(5.0)
                    .setSaveConsumer(newValue -> GetdownConfig.guideRadius = newValue)
                    .build());

            // --- 이펙트 설정 탭 ---
            effect.addEntry(entryBuilder.startColorField(Text.literal("가이드 색상"), GetdownConfig.particleColor)
                    .setDefaultValue(0x33CCFF)
                    .setSaveConsumer(newValue -> GetdownConfig.particleColor = newValue)
                    .build());

            effect.addEntry(entryBuilder.startEnumSelector(Text.literal("착지 파티클"), GetdownConfig.PopParticle.class, GetdownConfig.popParticle)
                    .setDefaultValue(GetdownConfig.PopParticle.POOF)
                    .setSaveConsumer(newValue -> GetdownConfig.popParticle = newValue)
                    .build());

            effect.addEntry(entryBuilder.startEnumSelector(Text.literal("착지 효과음"), GetdownConfig.PopSound.class, GetdownConfig.popSound)
                    .setDefaultValue(GetdownConfig.PopSound.ITEM_BREAK)
                    .setSaveConsumer(newValue -> GetdownConfig.popSound = newValue)
                    .build());

            effect.addEntry(entryBuilder.startIntSlider(Text.literal("효과음 음량 (%)"), (int)(GetdownConfig.soundVolume * 100), 0, 100)
                    .setDefaultValue(100)
                    .setSaveConsumer(newValue -> GetdownConfig.soundVolume = newValue / 100f) // 0~100을 0.0~1.0으로 변환
                    .build());

            builder.setSavingRunnable(() -> {
                GetdownConfig.save();
            });

            return builder.build();
        };
    }
}