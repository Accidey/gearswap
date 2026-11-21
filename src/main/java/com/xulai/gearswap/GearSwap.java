package com.xulai.gearswap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.xulai.gearswap.network.GearNetwork;
import com.xulai.gearswap.network.SyncPresetPayload;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Gear Swap 主类。
 *
 * <p>功能：为每名玩家保存 3 套装备预设（主手武器 + 头盔/胸甲/护腿/靴子），
 * 在背包界面提供一个入口按钮打开预设界面，并用组合键（默认 R+1/2/3）在战斗中整体换装。</p>
 */
@Mod(GearSwap.MODID)
public class GearSwap {
    public static final String MODID = "gearswap";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 玩家预设数据以数据附件（Data Attachment）的形式挂在玩家身上。 */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GearPresetData>> GEAR_PRESETS =
            ATTACHMENT_TYPES.register("gear_presets",
                    () -> AttachmentType.serializable(GearPresetData::new).copyOnDeath().build());

    public GearSwap(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, GearConfig.SPEC);
        ATTACHMENT_TYPES.register(modEventBus);
        ModMenus.register(modEventBus);
        modEventBus.addListener(GearNetwork::registerPayloads);
        NeoForge.EVENT_BUS.addListener(GearSwap::onPlayerLogin);
    }

    /** 玩家进服时把「当前预设」同步给客户端，HUD 提示才显示得对。 */
    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GearPresetData data = player.getData(GEAR_PRESETS);
            PacketDistributor.sendToPlayer(player, new SyncPresetPayload(data.getActive()));
        }
    }
}
