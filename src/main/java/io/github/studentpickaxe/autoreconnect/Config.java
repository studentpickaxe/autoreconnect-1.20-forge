package io.github.studentpickaxe.autoreconnect;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AutoReconnect.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final  ForgeConfigSpec         SPEC;

    private static final ForgeConfigSpec.BooleanValue AR_ENABLED;
    private static final ForgeConfigSpec.IntValue     AR_DELAY;

    static {
        AR_ENABLED = BUILDER
                .comment("Auto Reconnect Enabled")
                .define("autoReconnectEnabled", true);
        AR_DELAY = BUILDER
                .comment("Auto Reconnect Delay")
                .defineInRange("autoReconnectDelay-new", 5000, 50, 10_000);
        SPEC = BUILDER.build();
    }

    public static boolean isAutoReconnectEnabled() {
        return AR_ENABLED.get();
    }

    public static int getAutoReconnectDelay() {
        return AR_DELAY.get();
    }
}
