package fi.dy.masa.servux.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import fi.dy.masa.malilib.network.handler.CommonHandlerRegister;
import fi.dy.masa.malilib.network.handler.server.IPluginServerPlayHandler;
import fi.dy.masa.malilib.network.handler.server.ServerPlayHandler;
import fi.dy.masa.malilib.network.payload.PayloadCodec;
import fi.dy.masa.malilib.network.payload.PayloadManager;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.channel.ServuxStructuresPayload;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

public abstract class ServuxStructuresPlayListener<T extends CustomPayload> implements IPluginServerPlayHandler<T>
{
    private static final ServuxStructuresPlayListener<ServuxStructuresPayload> INSTANCE = new ServuxStructuresPlayListener<>() {
        @Override
        public void receive(ServuxStructuresPayload payload, ServerPlayNetworking.Context context)
        {
            // Servux doesn't need to use the networkHandler interface, but the code is here to do so.

            //ServerPlayNetworkHandler handler = context.player().networkHandler;
            CallbackInfo ci = new CallbackInfo("ServuxStructuresPlayListener", false);

            //if (handler != null)
            //{
                //ServuxStructuresPlayListener.INSTANCE.receiveC2SPlayPayload(PayloadType.SERVUX_STRUCTURES, payload, handler, ci);
            //}
            //else
                ServuxStructuresPlayListener.INSTANCE.receiveC2SPlayPayload(PayloadType.SERVUX_STRUCTURES, payload, context);
        }
    };
    public static ServuxStructuresPlayListener<ServuxStructuresPayload> getInstance() { return INSTANCE; }
    private final Map<PayloadType, Boolean> registered = new HashMap<>();
    private boolean register;

    @Override
    public PayloadType getPayloadType() { return PayloadType.SERVUX_STRUCTURES; }

    @Override
    public void reset(PayloadType type)
    {
        // Don't unregister
        this.register = false;
        ServuxStructuresPlayListener.INSTANCE.unregisterPlayHandler(type);
        if (this.registered.containsKey(type))
            this.registered.replace(type, false);
        else
            this.registered.put(type, false);
    }

    @Override
    public <P extends CustomPayload> void receiveC2SPlayPayload(PayloadType type, P payload, ServerPlayNetworking.Context ctx)
    {
        ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;
        ServerPlayerEntity player = ctx.player();
        Servux.printDebug("ServuxStructuresPlayListener#receiveS2CPlayPayload(): handling packet from player {} via Fabric Network API.", player.getName().getLiteralString());
        // ctx.responseSender(); == ServerPlayNetworkHandler

        ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).decodeC2SNbtCompound(PayloadType.SERVUX_STRUCTURES, packet.data(), player);
    }

    @Override
    public <P extends CustomPayload> void receiveC2SPlayPayload(PayloadType type, P payload, ServerPlayNetworkHandler handler, CallbackInfo ci)
    {
        // Can store the network handler here if wanted
        ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;
        ServerPlayerEntity player = handler.getPlayer();
        Servux.printDebug("ServuxStructuresPlayListener#receiveS2CPlayPayload(): handling packet from player {} via network handler.", player.getName().getLiteralString());

        ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).decodeC2SNbtCompound(PayloadType.SERVUX_STRUCTURES, packet.data(), player);

        // Cancel remaining processing
        if (ci.isCancellable())
            ci.cancel();
    }

    @Override
    public void decodeC2SNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);
        if (codec == null)
        {
            Servux.printDebug("ServuxStructuresPlayListener#decodeC2SNbtCompound(): [ERROR] CODEC = null, ignoring packet from player {}.", player.getName().getLiteralString());
            return;
        }
        this.register = true;
        // Decode
        if (Objects.equals(type, StructureDataProvider.INSTANCE.getNetworkChannel()))
        {
            int packetType = data.getInt("packetType");
            if (packetType == PacketType.Structures.PACKET_C2S_REQUEST_METADATA)
            {
                Servux.printDebug("ServuxStructuresPlayListener#decodeC2SNbtCompound(): received a metadata request packet from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.refreshMetadata(player, data);
            }
            else if (packetType == PacketType.Structures.PACKET_C2S_REQUEST_SPAWN_METADATA)
            {
                Servux.printDebug("ServuxStructuresPlayListener#decodeC2SNbtCompound(): received a Spawn metadata refresh packet from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.refreshSpawnMetadata(player, data);
            }
            else if (packetType == PacketType.Structures.PACKET_C2S_STRUCTURES_ACCEPT)
            {
                Servux.printDebug("ServuxStructuresPlayListener#decodeC2SNbtCompound(): received a STRUCTURES_ACCEPT request from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.acceptStructuresFromPlayer(player, data);
            }
            else if (packetType == PacketType.Structures.PACKET_C2S_STRUCTURES_DECLINED)
            {
                Servux.printDebug("ServuxStructuresPlayListener#decodeC2SNbtCompound(): received a STRUCTURES_DECLINED request from player: {}.", player.getName().getLiteralString());
                StructureDataProvider.INSTANCE.declineStructuresFromPlayer(player);
            }
            else
                Servux.printDebug("ServuxStructuresPlayListener#decodeC2SNbtCompound(): Invalid packetType from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
        }
        else
            Servux.printDebug("ServuxStructuresPlayListener#decodeC2SNbtCompound(): received unhandled packet from player: {}, of size in bytes: {}.", player.getName(), data.getSizeInBytes());
    }

    @Override
    public void encodeS2CNbtCompound(PayloadType type, NbtCompound data, ServerPlayerEntity player)
    {
        // Encode
        ServuxStructuresPayload payload = new ServuxStructuresPayload(data);

        // TODO -- In case you want to use the networkHandler interface to send packets,
        //  instead of the Fabric Networking API, this is how you can do it easily.
        //ServerPlayNetworkHandler handler = player.networkHandler;
        //if (handler != null)
            //ServuxStructuresPlayListener.INSTANCE.sendS2CPlayPayload(type, payload, handler);
        //else
            ServuxStructuresPlayListener.INSTANCE.sendS2CPlayPayload(type, payload, player);
    }

    @Override
    public <P extends CustomPayload> void sendS2CPlayPayload(PayloadType type, P payload, ServerPlayerEntity player)
    {
        if (ServerPlayNetworking.canSend(player, payload.getId()))
        {
            Servux.printDebug("ServuxStructuresPlayListener#sendS2CPlayPayload(): [FabricAPI].CanSend() -> {} is true type: {}", player.getName().getLiteralString(), type.toString());
            ServerPlayNetworking.send(player, payload);
        }
        else
            Servux.printDebug("ServuxStructuresPlayListener#sendS2CPlayPayload(): [ERROR] CanSend() -> {} is false", player.getName().getLiteralString());
    }

    @Override
    public <P extends CustomPayload> void sendS2CPlayPayload(PayloadType type, P payload, ServerPlayNetworkHandler handler)
    {
        Packet<?> packet = new CustomPayloadS2CPacket(payload);

        if (handler == null)
        {
            Servux.printDebug("ServuxStructuresPlayListener#sendC2SPlayPayload(): [ERROR] networkHandler = null");
            return;
        }
        ServerPlayerEntity player = handler.getPlayer();
        if (handler.accepts(packet))
        {
            Servux.printDebug("ServuxStructuresPlayListener#sendS2CPlayPayload(): [Handler].accepts() -> {} is true type: {}", player.getName().getLiteralString(), type.toString());
            handler.sendPacket(packet);
        }
        else
            Servux.printDebug("ServuxStructuresPlayListener#sendS2CPlayPayload(): [ERROR] accepts() -> {} is false", player.getName().getLiteralString());
    }

    @Override
    public void registerPlayPayload(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec == null)
        {
            return;
        }
        if (!codec.isPlayRegistered())
        {
            PayloadManager.getInstance().registerPlayChannel(type, CommonHandlerRegister.getInstance().getPayloadType(type), CommonHandlerRegister.getInstance().getPacketCodec(type));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec == null)
        {
            return;
        }
        if (codec.isPlayRegistered())
        {
            CommonHandlerRegister.getInstance().registerPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE, this);
            if (this.registered.containsKey(type))
                this.registered.replace(type, true);
            else
                this.registered.put(type, true);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec == null)
        {
            return;
        }
        if (codec.isPlayRegistered())
        {
            CommonHandlerRegister.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE);
            if (this.registered.containsKey(type))
                this.registered.replace(type, false);
            else
                this.registered.put(type, false);
        }
    }
}
