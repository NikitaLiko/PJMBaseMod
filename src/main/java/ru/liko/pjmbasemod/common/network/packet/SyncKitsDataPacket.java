package ru.liko.pjmbasemod.common.network.packet;

import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import ru.liko.pjmbasemod.Pjmbasemod;
import ru.liko.pjmbasemod.common.KitDefinition;
import ru.liko.pjmbasemod.common.player.PjmRank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Пакет для синхронизации данных о китах с клиентом.
 */
public record SyncKitsDataPacket(Map<String, Map<String, List<KitDefinition>>> kitsData) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncKitsDataPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_kits_data"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncKitsDataPacket> STREAM_CODEC = StreamCodec.of(
        SyncKitsDataPacket::encode,
        SyncKitsDataPacket::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buf, SyncKitsDataPacket packet) {
        // Количество классов
        buf.writeInt(packet.kitsData.size());
        
        for (Map.Entry<String, Map<String, List<KitDefinition>>> classEntry : packet.kitsData.entrySet()) {
            buf.writeUtf(classEntry.getKey()); // classId
            
            Map<String, List<KitDefinition>> teamKits = classEntry.getValue();
            buf.writeInt(teamKits.size()); // количество команд
            
            for (Map.Entry<String, List<KitDefinition>> teamEntry : teamKits.entrySet()) {
                buf.writeUtf(teamEntry.getKey()); // teamName
                
                List<KitDefinition> kits = teamEntry.getValue();
                buf.writeInt(kits.size()); // количество китов
                
                for (KitDefinition kit : kits) {
                    buf.writeUtf(kit.getId());
                    buf.writeUtf(kit.getDisplayName());
                    buf.writeUtf(kit.getMinRank().getId());
                    
                    List<String> items = kit.getItems();
                    buf.writeInt(items.size()); // количество предметов
                    for (String item : items) {
                        buf.writeUtf(item);
                    }
                }
            }
        }
    }

    public static SyncKitsDataPacket decode(RegistryFriendlyByteBuf buf) {
        Map<String, Map<String, List<KitDefinition>>> kitsData = new HashMap<>();
        
        int classCount = buf.readInt();
        for (int i = 0; i < classCount; i++) {
            String classId = buf.readUtf();
            
            Map<String, List<KitDefinition>> teamKits = new HashMap<>();
            int teamCount = buf.readInt();
            
            for (int j = 0; j < teamCount; j++) {
                String teamName = buf.readUtf();
                
                List<KitDefinition> kits = new ArrayList<>();
                int kitCount = buf.readInt();
                
                for (int k = 0; k < kitCount; k++) {
                    String id = buf.readUtf();
                    String displayName = buf.readUtf();
                    String rankId = buf.readUtf();
                    PjmRank minRank = PjmRank.fromString(rankId).orElse(PjmRank.PRIVATE);
                    
                    List<String> items = new ArrayList<>();
                    int itemCount = buf.readInt();
                    for (int m = 0; m < itemCount; m++) {
                        items.add(buf.readUtf());
                    }
                    
                    kits.add(new KitDefinition(id, displayName, minRank, items));
                }
                
                teamKits.put(teamName, kits);
            }
            
            kitsData.put(classId, teamKits);
        }
        
        return new SyncKitsDataPacket(kitsData);
    }

    public static void handle(SyncKitsDataPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            LOGGER.info("[PJM] Клиент получил данные о китах: {} классов", packet.kitsData().size());
            var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
            if (proxy != null) {
                proxy.handleSyncKitsData(packet.kitsData);
            }
        });
    }

    public Map<String, Map<String, List<KitDefinition>>> getKitsData() {
        return kitsData;
    }
}
