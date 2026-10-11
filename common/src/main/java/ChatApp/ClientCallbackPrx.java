package ChatApp;

public interface ClientCallbackPrx extends com.zeroc.Ice.ObjectPrx
{
    default void onUserConnected(String nickname)
    {
        onUserConnected(nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void onUserConnected(String nickname, java.util.Map<String, String> context)
    {
        _iceI_onUserConnectedAsync(nickname, context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<Void> onUserConnectedAsync(String nickname)
    {
        return _iceI_onUserConnectedAsync(nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> onUserConnectedAsync(String nickname, java.util.Map<String, String> context)
    {
        return _iceI_onUserConnectedAsync(nickname, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_onUserConnectedAsync(String iceP_nickname, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "onUserConnected", null, sync, null);
        f.invoke(false, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                 }, null);
        return f;
    }

    default void onUserDisconnected(String nickname)
    {
        onUserDisconnected(nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void onUserDisconnected(String nickname, java.util.Map<String, String> context)
    {
        _iceI_onUserDisconnectedAsync(nickname, context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<Void> onUserDisconnectedAsync(String nickname)
    {
        return _iceI_onUserDisconnectedAsync(nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> onUserDisconnectedAsync(String nickname, java.util.Map<String, String> context)
    {
        return _iceI_onUserDisconnectedAsync(nickname, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_onUserDisconnectedAsync(String iceP_nickname, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "onUserDisconnected", null, sync, null);
        f.invoke(false, context, null, ostr -> {
                     ostr.writeString(iceP_nickname);
                 }, null);
        return f;
    }

    default void onPrivateMessage(PrivateMessage message)
    {
        onPrivateMessage(message, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void onPrivateMessage(PrivateMessage message, java.util.Map<String, String> context)
    {
        _iceI_onPrivateMessageAsync(message, context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<Void> onPrivateMessageAsync(PrivateMessage message)
    {
        return _iceI_onPrivateMessageAsync(message, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> onPrivateMessageAsync(PrivateMessage message, java.util.Map<String, String> context)
    {
        return _iceI_onPrivateMessageAsync(message, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_onPrivateMessageAsync(PrivateMessage iceP_message, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "onPrivateMessage", null, sync, null);
        f.invoke(false, context, null, ostr -> {
                     PrivateMessage.ice_write(ostr, iceP_message);
                 }, null);
        return f;
    }

    default void onPrivateMessageFailed(long messageId, String recipient, String reason)
    {
        onPrivateMessageFailed(messageId, recipient, reason, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void onPrivateMessageFailed(long messageId, String recipient, String reason, java.util.Map<String, String> context)
    {
        _iceI_onPrivateMessageFailedAsync(messageId, recipient, reason, context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<Void> onPrivateMessageFailedAsync(long messageId, String recipient, String reason)
    {
        return _iceI_onPrivateMessageFailedAsync(messageId, recipient, reason, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> onPrivateMessageFailedAsync(long messageId, String recipient, String reason, java.util.Map<String, String> context)
    {
        return _iceI_onPrivateMessageFailedAsync(messageId, recipient, reason, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_onPrivateMessageFailedAsync(long iceP_messageId, String iceP_recipient, String iceP_reason, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "onPrivateMessageFailed", null, sync, null);
        f.invoke(false, context, null, ostr -> {
                     ostr.writeLong(iceP_messageId);
                     ostr.writeString(iceP_recipient);
                     ostr.writeString(iceP_reason);
                 }, null);
        return f;
    }

    default void onRoomCreated(String room, String owner)
    {
        onRoomCreated(room, owner, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void onRoomCreated(String room, String owner, java.util.Map<String, String> context)
    {
        _iceI_onRoomCreatedAsync(room, owner, context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<Void> onRoomCreatedAsync(String room, String owner)
    {
        return _iceI_onRoomCreatedAsync(room, owner, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> onRoomCreatedAsync(String room, String owner, java.util.Map<String, String> context)
    {
        return _iceI_onRoomCreatedAsync(room, owner, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_onRoomCreatedAsync(String iceP_room, String iceP_owner, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "onRoomCreated", null, sync, null);
        f.invoke(false, context, null, ostr -> {
                     ostr.writeString(iceP_room);
                     ostr.writeString(iceP_owner);
                 }, null);
        return f;
    }

    default void onRoomMessage(RoomMessage message)
    {
        onRoomMessage(message, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void onRoomMessage(RoomMessage message, java.util.Map<String, String> context)
    {
        _iceI_onRoomMessageAsync(message, context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<Void> onRoomMessageAsync(RoomMessage message)
    {
        return _iceI_onRoomMessageAsync(message, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> onRoomMessageAsync(RoomMessage message, java.util.Map<String, String> context)
    {
        return _iceI_onRoomMessageAsync(message, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_onRoomMessageAsync(RoomMessage iceP_message, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "onRoomMessage", null, sync, null);
        f.invoke(false, context, null, ostr -> {
                     RoomMessage.ice_write(ostr, iceP_message);
                 }, null);
        return f;
    }

    default void onRoomMemberJoined(String room, String nickname)
    {
        onRoomMemberJoined(room, nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void onRoomMemberJoined(String room, String nickname, java.util.Map<String, String> context)
    {
        _iceI_onRoomMemberJoinedAsync(room, nickname, context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<Void> onRoomMemberJoinedAsync(String room, String nickname)
    {
        return _iceI_onRoomMemberJoinedAsync(room, nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> onRoomMemberJoinedAsync(String room, String nickname, java.util.Map<String, String> context)
    {
        return _iceI_onRoomMemberJoinedAsync(room, nickname, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_onRoomMemberJoinedAsync(String iceP_room, String iceP_nickname, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "onRoomMemberJoined", null, sync, null);
        f.invoke(false, context, null, ostr -> {
                     ostr.writeString(iceP_room);
                     ostr.writeString(iceP_nickname);
                 }, null);
        return f;
    }

    default void onRoomMemberLeft(String room, String nickname)
    {
        onRoomMemberLeft(room, nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext);
    }

    default void onRoomMemberLeft(String room, String nickname, java.util.Map<String, String> context)
    {
        _iceI_onRoomMemberLeftAsync(room, nickname, context, true).waitForResponse();
    }

    default java.util.concurrent.CompletableFuture<Void> onRoomMemberLeftAsync(String room, String nickname)
    {
        return _iceI_onRoomMemberLeftAsync(room, nickname, com.zeroc.Ice.ObjectPrx.noExplicitContext, false);
    }

    default java.util.concurrent.CompletableFuture<Void> onRoomMemberLeftAsync(String room, String nickname, java.util.Map<String, String> context)
    {
        return _iceI_onRoomMemberLeftAsync(room, nickname, context, false);
    }

    default com.zeroc.IceInternal.OutgoingAsync<Void> _iceI_onRoomMemberLeftAsync(String iceP_room, String iceP_nickname, java.util.Map<String, String> context, boolean sync)
    {
        com.zeroc.IceInternal.OutgoingAsync<Void> f = new com.zeroc.IceInternal.OutgoingAsync<>(this, "onRoomMemberLeft", null, sync, null);
        f.invoke(false, context, null, ostr -> {
                     ostr.writeString(iceP_room);
                     ostr.writeString(iceP_nickname);
                 }, null);
        return f;
    }

    static ClientCallbackPrx checkedCast(com.zeroc.Ice.ObjectPrx obj)
    {
        return com.zeroc.Ice.ObjectPrx._checkedCast(obj, ice_staticId(), ClientCallbackPrx.class, _ClientCallbackPrxI.class);
    }

    static ClientCallbackPrx checkedCast(com.zeroc.Ice.ObjectPrx obj, java.util.Map<String, String> context)
    {
        return com.zeroc.Ice.ObjectPrx._checkedCast(obj, context, ice_staticId(), ClientCallbackPrx.class, _ClientCallbackPrxI.class);
    }

    static ClientCallbackPrx checkedCast(com.zeroc.Ice.ObjectPrx obj, String facet)
    {
        return com.zeroc.Ice.ObjectPrx._checkedCast(obj, facet, ice_staticId(), ClientCallbackPrx.class, _ClientCallbackPrxI.class);
    }

    static ClientCallbackPrx checkedCast(com.zeroc.Ice.ObjectPrx obj, String facet, java.util.Map<String, String> context)
    {
        return com.zeroc.Ice.ObjectPrx._checkedCast(obj, facet, context, ice_staticId(), ClientCallbackPrx.class, _ClientCallbackPrxI.class);
    }

    static ClientCallbackPrx uncheckedCast(com.zeroc.Ice.ObjectPrx obj)
    {
        return com.zeroc.Ice.ObjectPrx._uncheckedCast(obj, ClientCallbackPrx.class, _ClientCallbackPrxI.class);
    }

    static ClientCallbackPrx uncheckedCast(com.zeroc.Ice.ObjectPrx obj, String facet)
    {
        return com.zeroc.Ice.ObjectPrx._uncheckedCast(obj, facet, ClientCallbackPrx.class, _ClientCallbackPrxI.class);
    }

    @Override
    default ClientCallbackPrx ice_context(java.util.Map<String, String> newContext)
    {
        return (ClientCallbackPrx)_ice_context(newContext);
    }

    @Override
    default ClientCallbackPrx ice_adapterId(String newAdapterId)
    {
        return (ClientCallbackPrx)_ice_adapterId(newAdapterId);
    }

    @Override
    default ClientCallbackPrx ice_endpoints(com.zeroc.Ice.Endpoint[] newEndpoints)
    {
        return (ClientCallbackPrx)_ice_endpoints(newEndpoints);
    }

    @Override
    default ClientCallbackPrx ice_locatorCacheTimeout(int newTimeout)
    {
        return (ClientCallbackPrx)_ice_locatorCacheTimeout(newTimeout);
    }

    @Override
    default ClientCallbackPrx ice_invocationTimeout(int newTimeout)
    {
        return (ClientCallbackPrx)_ice_invocationTimeout(newTimeout);
    }

    @Override
    default ClientCallbackPrx ice_connectionCached(boolean newCache)
    {
        return (ClientCallbackPrx)_ice_connectionCached(newCache);
    }

    @Override
    default ClientCallbackPrx ice_endpointSelection(com.zeroc.Ice.EndpointSelectionType newType)
    {
        return (ClientCallbackPrx)_ice_endpointSelection(newType);
    }

    @Override
    default ClientCallbackPrx ice_secure(boolean b)
    {
        return (ClientCallbackPrx)_ice_secure(b);
    }

    @Override
    default ClientCallbackPrx ice_encodingVersion(com.zeroc.Ice.EncodingVersion e)
    {
        return (ClientCallbackPrx)_ice_encodingVersion(e);
    }

    @Override
    default ClientCallbackPrx ice_preferSecure(boolean b)
    {
        return (ClientCallbackPrx)_ice_preferSecure(b);
    }

    @Override
    default ClientCallbackPrx ice_router(com.zeroc.Ice.RouterPrx router)
    {
        return (ClientCallbackPrx)_ice_router(router);
    }

    @Override
    default ClientCallbackPrx ice_locator(com.zeroc.Ice.LocatorPrx locator)
    {
        return (ClientCallbackPrx)_ice_locator(locator);
    }

    @Override
    default ClientCallbackPrx ice_collocationOptimized(boolean b)
    {
        return (ClientCallbackPrx)_ice_collocationOptimized(b);
    }

    @Override
    default ClientCallbackPrx ice_twoway()
    {
        return (ClientCallbackPrx)_ice_twoway();
    }

    @Override
    default ClientCallbackPrx ice_oneway()
    {
        return (ClientCallbackPrx)_ice_oneway();
    }

    @Override
    default ClientCallbackPrx ice_batchOneway()
    {
        return (ClientCallbackPrx)_ice_batchOneway();
    }

    @Override
    default ClientCallbackPrx ice_datagram()
    {
        return (ClientCallbackPrx)_ice_datagram();
    }

    @Override
    default ClientCallbackPrx ice_batchDatagram()
    {
        return (ClientCallbackPrx)_ice_batchDatagram();
    }

    @Override
    default ClientCallbackPrx ice_compress(boolean co)
    {
        return (ClientCallbackPrx)_ice_compress(co);
    }

    @Override
    default ClientCallbackPrx ice_timeout(int t)
    {
        return (ClientCallbackPrx)_ice_timeout(t);
    }

    @Override
    default ClientCallbackPrx ice_connectionId(String connectionId)
    {
        return (ClientCallbackPrx)_ice_connectionId(connectionId);
    }

    @Override
    default ClientCallbackPrx ice_fixed(com.zeroc.Ice.Connection connection)
    {
        return (ClientCallbackPrx)_ice_fixed(connection);
    }

    static String ice_staticId()
    {
        return "::ChatApp::ClientCallback";
    }
}
