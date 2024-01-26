package fi.dy.masa.servux.network;

import fi.dy.masa.servux.ServuxReference;
import fi.dy.masa.servux.network.test.ServerDebugSuite;

public class ServerNetworkPlayInitHandler
{
    /**
     * Should be called when Server is starting
     */
    public static void registerPlayChannels()
    {
        PayloadTypeRegister.registerDefaultTypes(ServuxReference.COMMON_NAMESPACE);
        PayloadTypeRegister.registerDefaultPlayChannels();
        ServerDebugSuite.checkGlobalChannels();
    }
    /**
     * Should be called when Client joins a server
     */
    public static void registerReceivers()
    {
        ServerNetworkPlayRegister.registerDefaultReceivers();
        ServerDebugSuite.checkGlobalChannels();
    }
    public static void unregisterReceivers()
    {
        ServerNetworkPlayRegister.unregisterDefaultReceivers();
        ServerDebugSuite.checkGlobalChannels();
    }
}
