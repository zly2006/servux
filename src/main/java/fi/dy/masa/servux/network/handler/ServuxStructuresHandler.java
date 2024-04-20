package fi.dy.masa.servux.network.handler;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.network.server.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.server.ServerPlayHandler;

public abstract class ServuxStructuresHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxStructuresHandler<ServuxStructuresPayload> INSTANCE = new ServuxStructuresHandler<>() {
        @Override
        public void receive(ServuxStructuresPayload payload, ServerPlayNetworking.Context context)
        {
            ServuxStructuresHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxStructuresHandler<ServuxStructuresPayload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = new Identifier("servux", "structures");
    public static final int PROTOCOL_VERSION = 2;
    public static final int PACKET_S2C_METADATA = 1;
    public static final int PACKET_S2C_STRUCTURE_DATA = 2;
    public static final int PACKET_C2S_STRUCTURES_REGISTER = 3;
    public static final int PACKET_C2S_STRUCTURES_UNREGISTER = 4;
    public static final int PACKET_S2C_SPAWN_METADATA = 10;
    public static final int PACKET_C2S_REQUEST_SPAWN_METADATA = 11;
    private boolean servuxRegistered;
    private boolean payloadRegistered = false;

    @Override
    public Identifier getPayloadChannel() { return CHANNEL_ID; }

    @Override
    public boolean isPlayRegistered(Identifier channel)
    {
        if (channel.equals(this.getPayloadChannel()))
        {
            return this.payloadRegistered;
        }

        return false;
    }

    @Override
    public void setPlayRegistered(Identifier channel)
    {
        if (channel.equals(this.getPayloadChannel()))
        {
            this.payloadRegistered = true;
        }
    }

    @Override
    public void decodeNbtCompound(Identifier channel, NbtCompound data, ServerPlayerEntity player)
    {
        int packetType = data.getInt("packetType");

        if (packetType == PACKET_C2S_STRUCTURES_REGISTER)
        {
            StructureDataProvider.INSTANCE.register(player);
        }
        else if (packetType == PACKET_C2S_REQUEST_SPAWN_METADATA)
        {
            StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, data);
        }
        else if (packetType == PACKET_C2S_STRUCTURES_UNREGISTER)
        {
            StructureDataProvider.INSTANCE.unregister(player);
        }
        else
        {
            Servux.logger.warn("ServuxStructuresHandler#decodeC2SNbtCompound(): Invalid packetType from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
        }
    }

    @Override
    public void reset(Identifier channel)
    {
        if (channel.equals(this.getPayloadChannel()) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
        }
    }

    @Override
    public void registerPlayPayload(Identifier channel)
    {
        Servux.logger.error("registerPlayPayload() called for {}", channel.toString());

        if (this.servuxRegistered == false && this.payloadRegistered == false &&
        ServerPlayHandler.getInstance().isServerPlayChannelRegistered(this) == false)
        {
            Servux.logger.error("registerPlayPayload() registering for {}", channel.toString());

            PayloadTypeRegistry.playC2S().register(ServuxStructuresPayload.TYPE, ServuxStructuresPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(ServuxStructuresPayload.TYPE, ServuxStructuresPayload.CODEC);
        }

        this.payloadRegistered = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(Identifier channel)
    {
        Servux.logger.error("registerPlayHandler() called for {}", channel.toString());

        if (channel.equals(this.getPayloadChannel()) && this.payloadRegistered)
        {
            ServerPlayNetworking.registerGlobalReceiver((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE, this);
            this.servuxRegistered = true;
        }
    }

    @Override
    public void unregisterPlayHandler(Identifier channel)
    {
        Servux.logger.error("unregisterPlayHandler() called for {}", channel.toString());

        if (channel.equals(this.getPayloadChannel()) && this.payloadRegistered)
        {
            reset(channel);

            ServerPlayNetworking.unregisterGlobalReceiver(ServuxStructuresPayload.TYPE.id());
        }
    }

    @Override
    public <P extends CustomPayload> void receivePlayPayload(P payload, ServerPlayNetworking.Context ctx)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()))
        {
            ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;
            ServerPlayerEntity player = ctx.player();

            ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).decodeC2SNbtCompound(this.getPayloadChannel(), packet.data(), player);
        }
    }

    @Override
    public void encodeNbtCompound(NbtCompound data, ServerPlayerEntity player)
    {
        ServuxStructuresHandler.INSTANCE.sendPlayPayload(new ServuxStructuresPayload(data), player);
    }

    @Override
    public <P extends CustomPayload> void sendPlayPayload(P payload, ServerPlayerEntity player)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()) && this.payloadRegistered &&
            ServerPlayNetworking.canSend(player, payload.getId()))
        {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public <P extends CustomPayload> void sendPlayPayload(P payload, ServerPlayNetworkHandler handler)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()) && this.payloadRegistered)
        {
            Packet<?> packet = new CustomPayloadS2CPacket(payload);

            if (handler != null && handler.accepts(packet))
            {
                handler.sendPacket(packet);
            }
        }
    }
}
