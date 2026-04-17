package ru.liko.pjmbasemod.common.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.liko.pjmbasemod.Pjmbasemod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Packet for syncing class selection data to client.
 * NeoForge 1.21.1 format.
 */
public record SyncClassSelectionDataPacket(
    Map<String, Integer> classLimits,
    Map<String, Integer> classPlayerCounts,
    String playerTeamName,
    Set<String> lockedClasses
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncClassSelectionDataPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pjmbasemod.MODID, "sync_class_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncClassSelectionDataPacket> STREAM_CODEC =
        StreamCodec.of(SyncClassSelectionDataPacket::encode, SyncClassSelectionDataPacket::decode);

    /** Factory method */
    public static SyncClassSelectionDataPacket create(Map<String, Integer> limits, Map<String, Integer> counts, String teamName, Set<String> locked) {
        return new SyncClassSelectionDataPacket(
            limits != null ? new HashMap<>(limits) : new HashMap<>(),
            counts != null ? new HashMap<>(counts) : new HashMap<>(),
            teamName != null ? teamName : "",
            locked != null ? new HashSet<>(locked) : new HashSet<>()
        );
    }

    private static void encode(RegistryFriendlyByteBuf buf, SyncClassSelectionDataPacket packet) {
        // Encode limits
        buf.writeVarInt(packet.classLimits.size());
        for (Map.Entry<String, Integer> entry : packet.classLimits.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        // Encode counts
        buf.writeVarInt(packet.classPlayerCounts.size());
        for (Map.Entry<String, Integer> entry : packet.classPlayerCounts.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        // Encode team name
        buf.writeUtf(packet.playerTeamName);
        
        // Encode locked classes
        buf.writeVarInt(packet.lockedClasses.size());
        for (String classId : packet.lockedClasses) {
            buf.writeUtf(classId);
        }
    }

    private static SyncClassSelectionDataPacket decode(RegistryFriendlyByteBuf buf) {
        Map<String, Integer> classLimits = new HashMap<>();
        Map<String, Integer> classPlayerCounts = new HashMap<>();

        int limitsSize = buf.readVarInt();
        for (int i = 0; i < limitsSize; i++) {
            classLimits.put(buf.readUtf(), buf.readVarInt());
        }

        int countsSize = buf.readVarInt();
        for (int i = 0; i < countsSize; i++) {
            classPlayerCounts.put(buf.readUtf(), buf.readVarInt());
        }

        String playerTeamName = buf.readUtf();
        
        Set<String> lockedClasses = new HashSet<>();
        int lockedSize = buf.readVarInt();
        for (int i = 0; i < lockedSize; i++) {
            lockedClasses.add(buf.readUtf());
        }

        return new SyncClassSelectionDataPacket(classLimits, classPlayerCounts, playerTeamName, lockedClasses);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncClassSelectionDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                var proxy = ru.liko.pjmbasemod.common.network.ClientPacketProxy.get();
                if (proxy != null) {
                    proxy.handleSyncClassSelectionData(packet);
                }
            }
        });
    }

    /** Compatibility getter */
    public Map<String, Integer> getClassLimits() {
        return classLimits;
    }

    /** Compatibility getter */
    public Map<String, Integer> getClassPlayerCounts() {
        return classPlayerCounts;
    }
    
    /** Compatibility getter */
    public String getPlayerTeamName() {
        return playerTeamName;
    }
    
    /** Compatibility getter */
    public Set<String> getLockedClasses() {
        return lockedClasses;
    }
    
    public boolean isClassLocked(String classId) {
        return lockedClasses.contains(classId);
    }
}

